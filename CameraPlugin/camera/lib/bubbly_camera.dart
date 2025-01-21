import 'dart:async';
import 'package:flutter/services.dart';

class BubblyCamera {
  static const MethodChannel _channel = MethodChannel('bubbly_camera');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<void> get showAlertDialog async {
    await _channel.invokeMethod('showAlertDialog');
  }

  static Future<void> get startRecording async {
    await _channel.invokeMethod('start');
  }

  static Future<void> get pauseRecording async {
    await _channel.invokeMethod('pause');
  }

  static Future<void> get resumeRecording async {
    await _channel.invokeMethod('resume');
  }

  static Future<void> get stopRecording async {
    await _channel.invokeMethod('stop');
  }

  static Future<void> get toggleCamera async {
    await _channel.invokeMethod('toggle');
  }

  static Future<void> get flashOnOff async {
    await _channel.invokeMethod('flash');
  }

  static Future<void> shareToInstagram(String text) async {
    await _channel.invokeMethod('shareToInstagram', {'text': text});
  }

  static Future<void> inAppPurchase(String productID) async {
    await _channel.invokeMethod('in_app_purchase_id', {'productID': productID});
  }

  static Future<void> saveImage(String path) async {
    await _channel.invokeMethod('path', {'path': path});
  }

  static Future<void> mergeAudioVideo(String path) async {
    await _channel.invokeMethod('merge_audio_video', {'path': path});
  }

  static Future<bool> get cameraDispose async {
    return await _channel.invokeMethod('cameraDispose');
  }
}
