package com.mustly.wellmedia.video

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.BuildConfig
import com.mustly.wellmedia.MediaApplication
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseBindingFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentCamera2RecordVideoBinding
import com.mustly.wellmedia.image.Camera2TakePhotoFragment
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.commonlib.utils.LensFacing
import com.mustly.wellmedia.lib.commonlib.utils.OrientationLiveData
import com.mustly.wellmedia.lib.commonlib.utils.createCaptureSession
import com.mustly.wellmedia.lib.commonlib.utils.findCameraId
import com.mustly.wellmedia.lib.commonlib.utils.getPreviewOutputSize
import com.mustly.wellmedia.lib.commonlib.utils.openCamera
import com.mustly.wellmedia.utils.HardwareDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本页面功能：Camera2 录像，MediaCodec 编解码，存入 mp4 文件。
 */
@Route(PageRoute.CAMERA2_RECORD_VIDEO)
class Camera2RecordVideoFragment : BaseBindingFragment<FragmentCamera2RecordVideoBinding>() {
    companion object {
        private const val TAG = "Camera2RecordVideo"
        const val BIT_RATE = 10_000_000
        const val FPS = 30
        private const val MIN_TIME_MILLIS: Long = 1000L // 最少1秒
    }

    /**
     * 相机执行操作的线程
     * */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    private var cameraManager: CameraManager? = null

    private var cameraId = ""
    private var camera: CameraDevice? = null

    private var characteristics: CameraCharacteristics? = null

    private var session: CameraCaptureSession? = null

    // 监听镜头方向旋转的 livedata
    private lateinit var relativeOrientation: OrientationLiveData

    @Volatile
    private var recordingStarted = false

    @Volatile
    private var recordingComplete = false

    private var videoSize = Size(1920, 1080)

    /**
     * 视频编码相关
     * */
    private var codec: MediaCodec? = null
    private lateinit var codecSurface: Surface
    private val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    private val mBufferInfo = MediaCodec.BufferInfo()
    // 视频路径
    private val outputFile: File by lazy {
        File(
            "${MediaApplication.getAppContext().externalCacheDir}/视频录制/camera2+MediaCodec录制.mp4"
        ).apply {
            this.parentFile?.let { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            if (!this.exists()) {
                createNewFile()
            }
        }
    }
    private val mMuxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    /**
     * 录像数据的帧数。
     * */
    private var mFrameNum = 0
    private var recordingStartMillis = 0L

    override fun initView(rootView: View) {
        val surface = binding.viewFinder
        // 第一步：监听 Surface 创建 OK
        surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    // 初始化相机
                    createCamera()
                    // 10. 监听拍照按钮的点击
                    binding.captureButton.setOnTouchListener { view, event ->
                        dealRecordVideo(view, event)
                        true // 拦截操作
                    }
                } catch (e: Exception) {
                    LogUtil.e(TAG, e)
                }
            }
        })
    }

    private fun createCamera() = lifecycleScope.launch(Dispatchers.Main) {
        val surfaceView = binding.viewFinder
        if (!preInitCamera()) {
            LogUtil.e(TAG, "preInitCamera fail, exit")
            finish()
            return@launch
        }
        // 选择合适的尺寸，并配置 surface
        videoSize = getPreviewOutputSize(
            // 获取屏幕尺寸
            surfaceView.display,
            characteristics!!,
            SurfaceHolder::class.java
        )
        LogUtil.d(TAG, "view size: ${surfaceView.width} x ${surfaceView.height}, Selected preview size: $videoSize")
        surfaceView.setAspectRatio(
            videoSize.width,
            videoSize.height
        )
        // 根据尺寸创建 codec 和 surface。
        createMediaCodec()

        // 4. surface 可用时，再初始化相机
        initializeCamera()
        // 监听角度旋转
        relativeOrientation = OrientationLiveData(requireContext(), characteristics!!).apply {
            observe(viewLifecycleOwner) { orientation ->
                LogUtil.d(Camera2TakePhotoFragment.TAG, "Orientation changed: $orientation")
            }
        }
    }

    /**
     * 预初始化部分相机信息
     * */
    private suspend fun preInitCamera(): Boolean {
        // 1. 检查 CAMERA 权限
        val isCameraGranted = suspendRequestPermission(
            Manifest.permission.CAMERA,
            "请求相机权限",
            "我们需要使用相机以进行预览和录像",
        )
        if (!isCameraGranted) {
            LogUtil.e(TAG, "camera permission is denied. can not preview")
            return false
        }

        // 2. 获取 CameraManager
        cameraManager = requireContext().applicationContext
            .getSystemService(Context.CAMERA_SERVICE) as? CameraManager

        if (cameraManager == null) {
            LogUtil.e(TAG, "can not obtain cameraManager")
            return false
        }

        cameraId = findCameraId(cameraManager!!, LensFacing.BACK)
        if (cameraId.isBlank()) {
            LogUtil.e(TAG, "can not obtain camera id")
            return false
        }

        // 3. 获取相机的属性集
        characteristics = cameraManager!!.getCameraCharacteristics(cameraId)

        return true
    }

    /**
     * surface 可用后，初始化相机
     * */
    private suspend fun initializeCamera() {
        // 5. surface 可用时，打开相机
        camera = openCamera(cameraManager!!, cameraId, cameraHandler) ?: return

        val previewSurface = binding.viewFinder.holder.surface

        // 用于预览和录像的 target surfaces
        val targets = listOf(previewSurface, codecSurface)
        // 8. 创建会话
        session = createCaptureSession(camera!!, targets, cameraHandler) ?: return
        // 9. 创建预览请求
        val previewRequest = camera!!.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            // 预览画面输出到 SurfaceView
            addTarget(previewSurface)
        }.build()
        // 9. 提交预览请求，重复发送请求，直到调用了 session.stopRepeating() 方法
        session!!.setRepeatingRequest(previewRequest, null, cameraHandler)
    }

    private fun dealRecordVideo(view: View?, event: MotionEvent?) {
        view ?: return
        event ?: return

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!recordingStarted) {
                    startRecord()
                }
            }
            MotionEvent.ACTION_UP -> {
                stopRecord()
            }
        }
    }

    private fun startRecord() {
        // 预览和编码不在一个线程和 surface 上。
        val previewSurface = binding.viewFinder.holder.surface ?: return
        val mCamera = camera ?: return
        // 异步处理录像操作
        lifecycleScope.launch(Dispatchers.IO) {
            recordingStartMillis = System.currentTimeMillis()
            recordingStarted = true
            recordingComplete = false
            val recordRequest = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(codecSurface)

                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(FPS, FPS))
            }.build()

            Log.d(TAG, "Recording started")

            // Set color to RED and show timer when recording begins
            withContext(Dispatchers.Main) {
                binding.captureButton.setBackgroundResource(R.drawable.ic_shutter_pressed)
                binding.captureTimer.visibility = View.VISIBLE
                binding.captureTimer.start()
            }

            // 创建录像请求，并获取数据。
            session?.setRepeatingRequest(
                recordRequest,
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                        // 捕获一帧数据成功。
                        if (isCurrentlyRecording()) {
                            try {
                                LogUtil.w(TAG, "encodeData")
                                if (encodeData()) {
                                    mFrameNum++
                                }
                            } catch (e: Exception) {
                                LogUtil.e(HardwareDecoder.TAG, e)
                            }
                        }
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        LogUtil.e(TAG, "record fail, error is ${failure.reason}")
                    }
                },
                cameraHandler
            )
        }
    }

    private fun stopRecord() {
        lifecycleScope.launch(Dispatchers.IO) {
            session?.apply {
                stopRepeating()
                close()
            }

            withContext(Dispatchers.Main) {
                binding.captureButton.setBackgroundResource(R.drawable.ic_shutter_normal)
                binding.captureTimer.visibility = View.GONE
                binding.captureTimer.stop()
            }

            val elapsedTimeMillis = System.currentTimeMillis() - recordingStartMillis
            if (elapsedTimeMillis < MIN_TIME_MILLIS) {
                delay(MIN_TIME_MILLIS - elapsedTimeMillis)
            }
            // 延时 100 毫秒结束
            delay(100L)
            codec?.apply {
                stop()
                release()
            }
            // 通知系统扫描文件
            MediaScannerConnection.scanFile(requireView().context, arrayOf(outputFile.absolutePath), null, null)
            // 打开系统预览页
            if (outputFile.exists()) {
                val authority = "${BuildConfig.APPLICATION_ID}.provider"
                val uri = FileProvider.getUriForFile(requireView().context, authority, outputFile)
                startActivity(Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(
                        uri,
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(outputFile.extension)
                    )
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
            }
            recordingStarted = false
            recordingComplete = true
        }
    }

    /**
     * 使用 MediaCodec 编码数据。
     * */
    private fun encodeData(): Boolean {
        val mEncoder = codec ?: return false
        var encodedFrame = false
        var mEncodedFormat: MediaFormat? = mEncoder.outputFormat
        var mVideoTrack: Int = -1

        while (true) {
            val encoderIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0)
            if (encoderIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 获取编码器的结构，在可用 index 回调之前应该会触发一次。
                mEncodedFormat = mEncoder.outputFormat
            } else if (encoderIndex < 0) {
                // 报了其他错误，没有可用的缓冲区
                break
            } else {
                val encodedData = mEncoder.getOutputBuffer(encoderIndex)

                if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // INFO_OUTPUT_FORMAT_CHANGED 被触发时，拿到的是 MediaCodec 的配置数据，配置数据不是我们需要的视频数据，忽略掉。
                    mBufferInfo.size = 0
                }

                // 有可写入的数据。
                if (mBufferInfo.size != 0) {
                    // 限制 ByteBuffer 的数据量，以使其匹配上 BufferInfo
                    encodedData?.position(mBufferInfo.offset)
                    encodedData?.limit(mBufferInfo.offset + mBufferInfo.size)

                    if (mVideoTrack == -1) {
                        mVideoTrack = mMuxer.addTrack(mEncodedFormat!!)
                        // 设置方向
                        mMuxer.setOrientationHint(characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0)
                        mMuxer.start()
                        LogUtil.w(TAG, "mMuxer.start()")
                    }
                    // 将编码数据写入文件。
                    encodedData?.let {
                        mMuxer.writeSampleData(mVideoTrack, it, mBufferInfo)
                        encodedFrame = true
                    }
                }

                // 写完释放缓存区
                mEncoder.releaseOutputBuffer(encoderIndex, false)

                if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.w(TAG, "到达流的尾部。")
                    break
                }
            }
        }

        return encodedFrame
    }

    private fun isCurrentlyRecording(): Boolean {
        return recordingStarted && !recordingComplete
    }

    /**
     * MediaCodec 用于编码视频
     * */
    private fun createMediaCodec() {
        codec = MediaCodec.createEncoderByType(mimeType)
        //　创建 MediaFormat
        val format = MediaFormat.createVideoFormat(mimeType, videoSize.width, videoSize.height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
        // 每秒一个关键帧
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        // 视频预览和视频编码不是一个 surface 和 线程。
        codecSurface = codec?.createInputSurface() ?: return
        codec?.start()
    }

    override fun initData(rootView: View) {

    }
}