package com.test.flutter_plugin_test

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.JSONUtil
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import okhttp3.*
import java.io.IOException


/* FlutterPluginTestPlugin
class FlutterPluginTestPlugin(registrar: Registrar) : MethodCallHandler {

    private var context: Context = registrar.activity().applicationContext
    private var application: Application = registrar.activity().application

   companion object {
        private lateinit var APP: Application

        fun registerWith(registrar: PluginRegistry.Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_plugin_test")
            channel.setMethodCallHandler(FlutterPluginTestPlugin())
            APP = registrar.activity().application
        }
    }*/


/** FlutterPluginTestPlugin */
class FlutterPluginTestPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel : MethodChannel
    private lateinit var context: Context
    private lateinit var application: Application
    private val appOAuthId = "7feabb1988104ecd980a795778b1db06"
    private val appMetricaId = 3706627
    private val token = "AgAAAAATJ2WtAAahb8KDqtasmEp9u3b5DpR4sdg"

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
            getTapCount()
        }
        else -> {
          result.notImplemented()
        }
    }
  }

    private fun initMetrica() {
        val config: YandexMetricaConfig = YandexMetricaConfig.newConfigBuilder("c3bd5105-f4cc-4e0b-a98a-e1d11947bc02").build()
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(context, config)
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(application)
    }

    private fun tapButton() {
        val eventParam = mapOf<String, Unit>()
        eventParam.plus(Pair("onTapCount", 1))
        Log.d(TAG, "onTap event!!!")
        YandexMetrica.reportEvent("onTap")
    }

    private fun getTapCount(): Int {
        val client = OkHttpClient()
        var returnData = 0
        val url = "https://api.appmetrica.yandex.ru/logs/v1/export/events.json?application_id=$appMetricaId&date_since=2020-10-01&date_until=2020-10-10&fields=event_name&oauth_token=$token"
        val OAuthUrl = "https://oauth.yandex.ru/authorize?response_type=token&client_id=$appOAuthId"

        val request: Request = Request.Builder()
                .url(url)
                .addHeader("Cache-Control", "max-age=30")
                //.addHeader("Cache-Control", "no-cache")
                .get()
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                Log.d(TAG, "onFailure")
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call?, response: Response) {
                Log.d(TAG, "onResponse: $url")
                Log.d(TAG, "response.code(): " + response.code())
                Log.d(TAG, "response.message(): " + response.message())
                val res: String = response.body()?.string() ?: ""

                if (response.code() == 200) {
                    val buf = Gson().fromJson(res, Taps::class.java)
                    returnData = buf.data.size
                    Log.d(TAG, "BUF: $returnData")
                }
                Log.d(TAG, "response.body().string(): $res")
                if (!response.isSuccessful()) {
                    Log.d(TAG, "IOException")
                    throw IOException("Unexpected code $response")
                }
            }
        })

        return returnData
    }

    data class Taps (
            val data: List<EventName>
    )
    data class EventName (
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
