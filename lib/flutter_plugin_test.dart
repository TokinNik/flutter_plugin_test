import 'dart:async';

import 'package:flutter/services.dart';

class FlutterPluginTest {
  static const int CACHE_CONTROL_MODE_DEFAULT = 0;
  static const int CACHE_CONTROL_MODE_NO_CACHE = 1;
  static const int CACHE_CONTROL_MODE_MAX_AGE = 2;

  static const MethodChannel _channel =
      const MethodChannel('flutter_plugin_test');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> get getError async {
    final String data = await _channel.invokeMethod('getError');
    return data;
  }

  static get initPlugin async {
    await _channel.invokeMethod('initPlugin');
  }

  static get tapButton async {
    await _channel.invokeMethod('tapButton');
  }

  static Future<int> getTapCount(int cacheControlMode,
      {int maxAge = 30}) async {
    final int tapCount = await _channel.invokeMethod('getTapCount',
        {'cacheControlMode': cacheControlMode, 'maxAge': maxAge});
    return tapCount;
  }
}
