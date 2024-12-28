package com.mustly.wellmedia.video

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.ConditionVariable
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch

/**
 * 本页面功能：Camera2 录像，MediaCodec 编解码，存入 mp4 文件。
 */
@Route(PageRoute.CAMERA2_RECORD_VIDEO)
class Camera2RecordVideoFragment : BaseBindingFragment<FragmentCamera2RecordVideoBinding>() {
    companion object {
        private const val TAG = "Camera2RecordVideo"
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

    /** Condition variable for blocking until the recording completes */
    private val cvRecordingStarted = ConditionVariable(false)
    private val cvRecordingComplete = ConditionVariable(false)

    private var videoSize = Size(1080, 1920)

    private var mEncoder: MediaCodec? = null

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
                    createVideoEncoder()
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

        // 用于预览和录像的 target surfaces
        val targets = listOf(targetSurface())
        // 8. 创建会话
        session = createCaptureSession(camera!!, targets, cameraHandler) ?: return
        // 9. 创建预览请求
        val previewRequest = camera!!.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            // 预览画面输出到 SurfaceView
            addTarget(targetSurface())
        }.build()
        // 9. 提交预览请求，重复发送请求，直到调用了 session.stopRepeating() 方法
        session!!.setRepeatingRequest(previewRequest, null, cameraHandler)
        // 10. 监听拍照按钮的点击
        binding.captureButton.setOnTouchListener { view, event ->
            dealRecordVideo(view, event)
            true // 拦截操作
        }
    }

    private fun dealRecordVideo(view: View?, event: MotionEvent?) {
        view ?: return
        event ?: return

        // 异步处理录像操作
        lifecycleScope.launch(Dispatchers.IO) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!recordingStarted) {
                        startRecord()
                    }
                }
                MotionEvent.ACTION_UP -> {

                }
            }
        }
    }

    private fun startRecord() {

    }

    private fun createVideoEncoder() {
        mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_MPEG4)
        //　todo 添加后续代码
    }

    private fun targetSurface() = binding.viewFinder.holder.surface

    override fun initData(rootView: View) {

    }
}