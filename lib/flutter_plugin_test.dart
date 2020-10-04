
import 'dart:async';

import 'package:flutter/services.dart';

class FlutterPluginTest {
  static const MethodChannel _channel =
      const MethodChannel('flutter_plugin_test');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }


  static get initPlugin async {
    await _channel.invokeMethod('initPlugin');
  }

  static get tapButton async {
    await _channel.invokeMethod('tapButton');
  }

  static Future<int> get getTapCount async {
    final int tapCount = await _channel.invokeMethod('getTapCount');
    return tapCount;
  }
}
