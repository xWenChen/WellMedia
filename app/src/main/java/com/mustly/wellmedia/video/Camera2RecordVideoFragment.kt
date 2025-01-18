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
        const val BIT_RATE = 20_000_000
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
        // 1. 检查 CAMERA 权限
        val isCameraGranted = suspendRequestPermission(
            Manifest.permission.CAMERA,
            "请求相机权限",
            "我们需要使用相机以进行预览和录像",
        )
        if (!isCameraGranted) {
            LogUtil.e(TAG, "camera permission is denied. can not preview")
            return@launch
        }

        // 2. 获取 CameraManager
        cameraManager = requireContext().applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

        if (cameraManager == null) {
            LogUtil.e(TAG, "can not obtain cameraManager")
            return@launch
        }

        cameraId = findCameraId(cameraManager!!, LensFacing.BACK)
        if (cameraId.isBlank()) {
            LogUtil.e(TAG, "can not obtain camera id")
            return@launch
        }

        // 3. 获取相机的属性集
        characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
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
        // 5. surface 可用时，打开相机
        camera = openCamera(cameraManager!!, cameraId, cameraHandler) ?: return@launch

        val previewSurface = binding.viewFinder.holder.surface

        // 用于预览和录像的 target surfaces
        val targets = listOf(previewSurface, codecSurface)
        // 8. 创建会话
        session = createCaptureSession(camera!!, targets, cameraHandler) ?: return@launch
        // 9. 创建预览请求
        val previewRequest = camera!!.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            // 预览画面输出到 SurfaceView
            addTarget(previewSurface)
        }.build()
        // 10. 提交预览请求，重复发送请求，直到调用了 session.stopRepeating() 方法
        session!!.setRepeatingRequest(previewRequest, null, cameraHandler)
        // 监听角度旋转
        relativeOrientation = OrientationLiveData(requireContext(), characteristics!!).apply {
            observe(viewLifecycleOwner) { orientation ->
                LogUtil.d(Camera2TakePhotoFragment.TAG, "Orientation changed: $orientation")
            }
        }
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

            val recordRequest = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(codecSurface)
            }.build()

            LogUtil.d(TAG, "Recording start.")

            // 更新布局信息
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
                                encodeData()
                                LogUtil.w(TAG, "encodeData end")
                            } catch (e: Exception) {
                                LogUtil.e(TAG, e)
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
        lifecycleScope.launch(Dispatchers.Main) {
            session?.apply {
                stopRepeating()
                close()
            }

            binding.captureButton.setBackgroundResource(R.drawable.ic_shutter_normal)
            binding.captureTimer.visibility = View.GONE
            binding.captureTimer.stop()

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
            mMuxer.stop()
            mMuxer.release()
            LogUtil.d(TAG, "停止录像。")
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
        }
    }

    /**
     * 使用 MediaCodec 编码数据。
     * */
    private fun encodeData(): Boolean {
        val mEncoder = codec ?: return false
        var encodedFrame = false
        var mVideoTrack: Int = -1
        var mEncodedFormat: MediaFormat? = null

        while (true) {
            // 这里catch下，startRecord 和 stopRecord 位于不同的线程。处理录像操作可能已停止，但仍在获取OutputBuffer的情况。
            val encoderStatus = try {
                mEncoder.dequeueOutputBuffer(mBufferInfo, -1)
            } catch (e: Exception) {
                LogUtil.e(TAG, e)
                MediaCodec.INFO_TRY_AGAIN_LATER
            }

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                LogUtil.w(TAG, "not get $encoderStatus, quit")
                break;
            }

            if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                LogUtil.d(TAG, "encoder output format changed: $mEncodedFormat")
                // 在收到buffer数据前回调，通常只回调一次，MediaFormat包含MediaMuxer需要的csd-0和csd-1数据。
                mEncodedFormat = mEncoder.outputFormat
                continue
            }

            if (encoderStatus < 0 || mEncodedFormat == null) {
                LogUtil.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                continue
            }

            val encodedData = mEncoder.getOutputBuffer(encoderStatus)
            if (encodedData == null) {
                LogUtil.e(TAG, "encoderOutputBuffer $encoderStatus was null")
                continue
            }

            if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                LogUtil.d(TAG, "忽略 BUFFER_FLAG_CODEC_CONFIG")
                continue
            }

            if (mBufferInfo.size == 0) {
                LogUtil.d(TAG, "mBufferInfo.size == 0.")
                continue
            }

            // 限制 ByteBuffer 的数据量，以使其匹配上 BufferInfo
            encodedData.position(mBufferInfo.offset)
            encodedData.limit(mBufferInfo.offset + mBufferInfo.size)

            if (mVideoTrack == -1) {
                mVideoTrack = mMuxer.addTrack(mEncodedFormat)
                mMuxer.setOrientationHint(characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0)
                mMuxer.start()
                LogUtil.d(TAG, "Started media muxer")
            }

            mMuxer.writeSampleData(mVideoTrack, encodedData, mBufferInfo)
            encodedFrame = true

            LogUtil.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts= ${mBufferInfo.presentationTimeUs}")
            mEncoder.releaseOutputBuffer(encoderStatus, false)

            if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                LogUtil.w(TAG, "reached end of stream unexpectedly.")
                break
            }
        }

        return encodedFrame
    }

    private fun isCurrentlyRecording(): Boolean {
        return recordingStarted
    }

    /**
     * MediaCodec 用于编码视频
     * */
    private fun createMediaCodec() {
        codec = MediaCodec.createEncoderByType(mimeType)
        //　创建 MediaFormat
        val format = MediaFormat.createVideoFormat(mimeType, videoSize.width, videoSize.height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
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