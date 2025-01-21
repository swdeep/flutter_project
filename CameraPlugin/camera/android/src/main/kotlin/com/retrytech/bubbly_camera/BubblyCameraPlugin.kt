package com.retrytech.bubbly_camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/** BubblyCameraPlugin */
class BubblyCameraPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private var productId: String? = null

    private lateinit var channel: MethodChannel
    private var context: Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "bubbly_camera")
        channel.setMethodCallHandler(this)
        flutterPluginBinding.platformViewRegistry
            .registerViewFactory("camera", NativeViewFactory(channel))
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        context = binding.activity
        binding.addActivityResultListener { requestCode, resultCode, data ->
            Log.e("TAG", "onReattachedToActivityForConfigChanges: requestCode=$requestCode resultCode=$resultCode data=$data")
            true
        }

        checkAndRequestPermissions()
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

    override fun onDetachedFromActivity() {}

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.e("TAG", "onMethodCall: ${call.method}")
        when (call.method) {
            "in_app_purchase_id" -> {

            }
            "shareToInstagram" -> {
                val text = call.arguments as? String
                text?.let {
                    shareTextToInstagram(it)
                    result.success(null)
                } ?: result.error("INVALID_ARGUMENT", "Text argument is missing", null)
            }
            else -> result.notImplemented()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(context!!, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(context!!, permissionsToRequest.toTypedArray(), 1000)
        }
    }

    private fun shareTextToInstagram(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.instagram.android") 
        }

        val pm = context?.packageManager
        val resolvedActivity = pm?.resolveActivity(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolvedActivity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            } else {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            }
            context?.startActivity(shareIntent)
        } else {
            Log.e("TAG", "Instagram is not installed.")
        }
    }
}
