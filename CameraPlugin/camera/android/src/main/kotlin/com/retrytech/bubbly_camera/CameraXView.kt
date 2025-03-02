package com.retrytech.bubbly_camera

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

internal class CameraXView(
    private val context: Activity?,
    id: Int,
    creationParams: Map<String?, Any?>?,
    private val channel: MethodChannel
) : PlatformView, MethodChannel.MethodCallHandler {

    private lateinit var camera: Camera
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(CAMERA, android.Manifest.permission.RECORD_AUDIO).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private var view1: View? = null

    override fun getView(): View {
        if (view1 != null) {
            return view1!!
        }

        view1 = LayoutInflater.from(context).inflate(R.layout.item_camera, null, false)
        channel.setMethodCallHandler(this)
        viewFinder = view1!!.findViewById(R.id.viewFinder)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                context!!, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        return view1!!
    }

    override fun dispose() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context!!)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.d(TAG, "onMethodCall: ${call.method}")

        when (call.method) {
            "start" -> captureVideo()
            "pause" -> recording?.pause()
            "resume" -> recording?.resume()
            "stop" -> {
                recording?.stop()
                recording = null
            }
            "toggle" -> {
                isFrontCamera = !isFrontCamera
                startCamera()
            }
            "flash" -> {
                isFlashOn = !isFlashOn
                camera.cameraControl.enableTorch(isFlashOn)
            }
            else -> result.notImplemented()
        }
    }

    private fun captureVideo() {
        val videoCapture = videoCapture ?: return

        // Create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        // Prepare the recording
        recording = videoCapture.output
            .prepareRecording(context!!, FileOutputOptions.Builder(getOutputMediaFile()).build())
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        context, android.Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d(TAG, "Recording started")
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            channel.invokeMethod("url_path", getOutputMediaFile().absolutePath)
                            Log.d(TAG, "Video capture succeeded: ${recordEvent.outputResults.outputUri}")
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture error: ${recordEvent.error}")
                        }
                    }
                }
            }
    }

    private fun getOutputMediaFile(): File {
        val state: String = Environment.getExternalStorageState()
        val filesDir: File? = if (Environment.MEDIA_MOUNTED == state) {
            context?.getExternalFilesDir(null)
        } else {
            context?.filesDir
        }

        val file = File(filesDir, "finalvideo.mp4")
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    private var isFlashOn: Boolean = false
    private var isFrontCamera: Boolean = false

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context!!)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Preview
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(viewFinder.surfaceProvider)
        }

        // Select front or back camera
        val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Create VideoCapture and UseCaseGroup
            val recorder = Recorder.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)

            val viewPort = ViewPort.Builder(
                Rational(context!!.window.decorView.width, context.window.decorView.height),
                context.window.decorView.rotation.toInt()
            ).build()

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture!!)
                .setViewPort(viewPort)
                .build()

            // Bind camera
            camera = cameraProvider.bindToLifecycle(
                context!!, cameraSelector, useCaseGroup
            )

            // Focus on touch
            viewFinder.setOnTouchListener { _, motionEvent ->
                val meteringPoint = viewFinder.meteringPointFactory.createPoint(motionEvent.x, motionEvent.y)
                val action = FocusMeteringAction.Builder(meteringPoint)
                    .addPoint(meteringPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()

                camera.cameraControl.startFocusAndMetering(action)
                true
            }
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context!!, it) == PackageManager.PERMISSION_GRANTED
    }
}
