import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_plugin_test/flutter_plugin_test.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  FlutterPluginTest.initPlugin;

  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _errorMessage = 'none';
  int _tapCount = 0;
  int _cacheControlMode = 0;
  bool _progressIndicatorOffset = true;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await FlutterPluginTest.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  Future<void> getError() async {
    String data;
    try {
      data = await FlutterPluginTest.getError;
    } on PlatformException catch (e) {
      data = "| ${e.code} |\n| ${e.message} |\n| ${e.details} |";
      print(data);
    }
    if (!mounted) return;
    print(data);
    setState(() {
      _errorMessage = data;
    });
  }

  Future<void> getTapsCount() async {
    int tapCount;
    try {
      tapCount = await FlutterPluginTest.getTapCount(_cacheControlMode);
    } on PlatformException catch (e) {
      tapCount = -1;
      _errorMessage = "| ${e.code} |\n| ${e.message} |\n| ${e.details} |";
    }
    if (!mounted) return;

    setState(() {
      _progressIndicatorOffset = true;
      _tapCount = tapCount;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Running on: $_platformVersion\n'),
              FlatButton(
                color: Colors.lightBlueAccent,
                onPressed: () {
                  setState(() {
                    _progressIndicatorOffset = false;
                  });
                  getTapsCount();
                },
                child: Text("Get Tap Count = $_tapCount"),
              ),
              FlatButton(
                color: Colors.redAccent,
                onPressed: () {
                  getError();
                },
                child: Text("Get Error"),
              ),
              Text('Error is: $_errorMessage\n'),
              new Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  new Radio(
                    value: 0,
                    groupValue: _cacheControlMode,
                    onChanged: (value) {
                      setState(() {
                        _cacheControlMode = 0;
                      });
                    },
                  ),
                  new Text(
                    'Default',
                  ),
                  new Radio(
                    value: 1,
                    groupValue: _cacheControlMode,
                    onChanged: (value) {
                      setState(() {
                        _cacheControlMode = 1;
                      });
                    },
                  ),
                  new Text(
                    'Max age',
                  ),
                  new Radio(
                    value: 2,
                    groupValue: _cacheControlMode,
                    onChanged: (value) {
                      setState(() {
                        _cacheControlMode = 2;
                      });
                    },
                  ),
                  new Text(
                    'No cache',
                  ),
                ],
              ),
              Offstage(
                offstage: _progressIndicatorOffset,
                child: CircularProgressIndicator(),
              )
            ],
          ),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            FlutterPluginTest.tapButton;
          },
          child: Text("Tap"),
        ),
      ),
    );
  }
}
