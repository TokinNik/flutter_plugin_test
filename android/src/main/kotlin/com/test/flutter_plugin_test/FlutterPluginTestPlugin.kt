package com.test.flutter_plugin_test

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import okhttp3.*
import java.io.IOException
import java.util.*
import java.util.concurrent.*

/** FlutterPluginTestPlugin */
class FlutterPluginTestPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var application: Application
    private val yMetricaApiKey = "c3bd5105-f4cc-4e0b-a98a-e1d11947bc02"
    private val appOAuthId = "7feabb1988104ecd980a795778b1db06"
    private val appMetricaId = 3706627
    private val token = "AgAAAAATJ2WtAAahb8KDqtasmEp9u3b5DpR4sdg"

    private var executor = ScheduledThreadPoolExecutor(1)
    private var endTime = Date()

    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_plugin_test")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "initPlugin" -> {
                initMetrica()
            }
            "tapButton" -> {
                tapButton()
            }
            "getTapCount" -> {
                getTapCount(call, result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun initMetrica() {
        val config: YandexMetricaConfig = YandexMetricaConfig.newConfigBuilder(yMetricaApiKey).build()
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(context, config)
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(application)

        handlerThread = HandlerThread("YandexMetrica", THREAD_PRIORITY_BACKGROUND)
        handlerThread.start()
        handler = Handler(Looper.getMainLooper())
    }

    private fun tapButton() {
        val eventParam = mapOf<String, Unit>()
        eventParam.plus(Pair("onTapCount", 1))
        Log.d(TAG, "onTap event!!!")
        YandexMetrica.reportEvent("onTap")
    }

    private fun getTapCount(call: MethodCall, result: Result) {
        if (executor.taskCount > 0) {
            executor.shutdownNow()
            executor = ScheduledThreadPoolExecutor(1)
        }
        val client = OkHttpClient()
        val url = "https://api.appmetrica.yandex.ru/logs/v1/export/events.json?application_id=$appMetricaId&date_since=2020-10-01&date_until=2020-10-10&fields=event_name&oauth_token=$token"
        val OAuthUrl = "https://oauth.yandex.ru/authorize?response_type=token&client_id=$appOAuthId"

        val request: Request = Request.Builder()
                .apply {
                    url(url)
                    get()
                    when (call.argument<Int>("cacheControlMode")) {
                        1 -> addHeader("Cache-Control", "max-age=${call.argument<Int>("maxAge")}")
                        2 -> addHeader("Cache-Control", "no-cache")
                    }
                }.build()
        endTime = Date(Date().time + 30000)
        executor.scheduleAtFixedRate(
                {
                    if (Date().after(endTime)) {
                        handler.post {
                            result.error("Request failure", "Timeout", "")
                        }
                        executor.shutdown()
                    }
                    getTapCountCall(client, request, url, result)
                },
                0, 2, TimeUnit.SECONDS)
    }

    private fun getTapCountCall(client: OkHttpClient, request: Request, url: String, result: Result) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                Log.d(TAG, "onFailure")
                e.printStackTrace()
                handler.post {
                    result.error("Request failure", e.message, e.cause)
                }
                executor.shutdown()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call?, response: Response) {
                Log.d(TAG, "onResponse: $url\n${request.headers()}")
                Log.d(TAG, "response.code(): " + response.code())
                Log.d(TAG, "response.message(): " + response.message())
                val res: String = response.body()?.string() ?: ""

                when (response.code()) {
                    200 -> {
                        val buf = Gson().fromJson(res, Taps::class.java)
                        val returnData = buf.data.size
                        Log.d(TAG, "BUF: $returnData")
                        handler.post {
                            result.success(returnData)
                        }
                        executor.shutdown()
                    }
                    202 -> {
                        Log.d(TAG, "response.body().string(): $res")
                    }
                    400 -> {
                        handler.post {
                            result.error("Request failure", "Authorization error", "")
                        }
                        executor.shutdown()
                    }
                    403 -> {
                        handler.post {
                            result.error("Request failure", "Wrong parameter", "")
                        }
                        executor.shutdown()
                    }
                    429 -> {
                        handler.post {
                            result.error("Request failure", "Exceeded quota", "")
                        }
                        executor.shutdown()
                    }
                    else -> {
                        if (!response.isSuccessful) {
                            Log.d(TAG, "IOException")
                            throw IOException("Unexpected code $response")
                        }
                        executor.shutdown()
                    }
                }

            }
        })
    }


    data class Taps(
            val data: List<EventName>
    )

    data class EventName(
            val event_name: String
    )

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        application = binding.activity.application;
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

    override fun onDetachedFromActivity() {}
}
