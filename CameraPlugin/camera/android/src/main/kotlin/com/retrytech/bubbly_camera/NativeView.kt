package com.retrytech.bubbly_camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import androidx.core.app.ActivityCompat
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


internal class NativeView(
    private val context: Context?,
    id: Int,
    creationParams: Map<String?, Any?>?,
    private val channel: MethodChannel
) : PlatformView, MethodChannel.MethodCallHandler {

    private var mBackgroundThread: HandlerThread? = null
    private val view: View =
        LayoutInflater.from(context).inflate(R.layout.item_camera_view, null, false)

    override fun getView(): View {
        return view
    }

    override fun dispose() {
        stopBackgroundThread()
        closeCamera()
    }

    private val methodChannel: MethodChannel = channel
    private var mTextureView: AutoFitTextureView? = null

    init {
        methodChannel.setMethodCallHandler(this)
        mTextureView = view.findViewById(R.id.viewFinder)
    }

    private fun startCamera() {
        if (mTextureView?.isAvailable == true) {
            mTextureView?.let { openCamera(it.width, it.height) }
            return
        }
        mTextureView?.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera(width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    override fun onFlutterViewAttached(flutterView: View) {
        startBackgroundThread()
        startCamera()
    }

    override fun onFlutterViewDetached() {
        stopBackgroundThread()
        closeCamera()
    }

    var isFlashOn = false
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start" -> startRecordingVideo()
            "pause" -> mMediaRecorder?.pause()
            "resume" -> mMediaRecorder?.resume()
            "stop" -> {
                mMediaRecorder?.stop()
                channel.invokeMethod("url_path", getOutputMediaFile()?.absolutePath)
            }
            "toggle" -> {
                cameraFacing = if (cameraFacing == 0) 1 else 0
                restartCamera()
            }
            "flash" -> {
                isFlashOn = !isFlashOn
                restartCamera()
            }
        }
    }

    private fun restartCamera() {
        stopBackgroundThread()
        closeCamera()
        startBackgroundThread()
        startCamera()
    }

    private val mCameraOpenCloseLock = Semaphore(1)
    private var mSensorOrientation: Int? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var mVideoSize: Size? = null
    private var mPreviewSize: Size? = null
    private var cameraFacing = 0

    private fun openCamera(width: Int, height: Int) {
        val manager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Timeout waiting to lock camera opening.")
            }

            val cameraId = manager.cameraIdList[cameraFacing]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            if (map == null) {
                throw RuntimeException("Cannot get available preview/video sizes")
            }

            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            mPreviewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                width, height, mVideoSize!!
            )

            mPreviewSize?.let {
                mTextureView?.setAspectRatio(it.height, it.width)
            }

            mMediaRecorder = MediaRecorder()
            if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            manager.openCamera(cameraId, mStateCallback, null)
        } catch (e: CameraAccessException) {
            Log.e("TAG", "Camera access exception: Cannot access camera.")
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            closePreviewSession()
            mCameraDevice?.close()
            mCameraDevice = null
            mMediaRecorder?.release()
            mMediaRecorder = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    private var mCameraDevice: CameraDevice? = null
    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            startPreview()
            mCameraOpenCloseLock.release()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }
    }

    private var mBackgroundHandler: Handler? = Handler(Looper.getMainLooper())
    private fun startPreview() {
        if (mCameraDevice == null || mTextureView == null || !mTextureView!!.isAvailable || mPreviewSize == null) {
            return
        }

        try {
            closePreviewSession()

            val texture = mTextureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)

            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            val previewSurface = Surface(texture)

            val surfaces: MutableList<Surface> = ArrayList()
            surfaces.add(previewSurface)
            mPreviewBuilder!!.addTarget(previewSurface)

            mCameraDevice!!.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mPreviewSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("TAG", "Camera session configuration failed")
                    }
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        if (mCameraDevice == null) return

        try {
            if (isFlashOn) {
                mPreviewBuilder?.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
            } else {
                mPreviewBuilder?.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
            }
            setUpCaptureRequestBuilder(mPreviewBuilder!!)
            mPreviewSession!!.setRepeatingRequest(mPreviewBuilder!!.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    private var mPreviewSession: CameraCaptureSession? = null
    private fun closePreviewSession() {
        mPreviewSession?.close()
        mPreviewSession = null
    }

    private fun chooseVideoSize(choices: Array<Size>): Size? {
        for (size in choices) {
            if (size.width == 1920 && size.height == 1080) return size
        }
        return choices[choices.size - 1]
    }

    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size? {
        val bigEnough: MutableList<Size> = ArrayList()
        for (option in choices) {
            if (option.height == option.width * aspectRatio.height / aspectRatio.width
                && option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }
        return bigEnough.minByOrNull { it.width * it.height } ?: choices[0]
    }

    private var mCurrentFile: File? = null
    private val VIDEO_DIRECTORY_NAME = "AndroidWave"
    private var mMediaRecorder: MediaRecorder? = null

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        mMediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mCurrentFile = getOutputMediaFile()
            setOutputFile(mCurrentFile?.absolutePath)
            val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P)
            setVideoFrameRate(profile.videoFrameRate)
            setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
            setVideoEncodingBitRate(profile.videoBitRate)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(profile.audioBitRate)
            setAudioSamplingRate(profile.audioSampleRate)
            setOrientationHint(90)
            prepare()
        }
    }

    private fun getOutputMediaFile(): File? {
        val state = Environment.getExternalStorageState()
        val filesDir = if (Environment.MEDIA_MOUNTED == state) {
            context?.getExternalFilesDir(null)
        } else {
            context?.filesDir
        }
        return File(filesDir, "finalvideo.mp4")
    }
}
