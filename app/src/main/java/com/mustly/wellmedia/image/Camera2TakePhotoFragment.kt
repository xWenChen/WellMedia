package com.mustly.wellmedia.image

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceHolder
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.databinding.FragmentCamera2TakePhotoBinding
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.commonlib.utils.OrientationLiveData
import com.mustly.wellmedia.lib.commonlib.utils.setNoDoubleClickListener
import com.mustly.wellmedia.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Camera2 拍照功能，代码参考自官方示例：https://github.com/android/camera-samples
 *
 * 使用 Camera2 拍照的步骤为：
 *
 * 1. 检查 <uses-permission android:name="android.permission.CAMERA" /> 权限
 * */
class Camera2TakePhotoFragment : BaseFragment<FragmentCamera2TakePhotoBinding>() {
    companion object {
        const val TAG = "Camera2TakePhotoFragment"

        /**
         * 图片的最大缓存数量
         * */
        const val IMAGE_BUFFER_SIZE: Int = 3
        const val ANIMATION_FAST_MILLIS = 50L
        const val ANIMATION_SLOW_MILLIS = 100L
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
        const val IMAGE_CAPTURE_TIMEOUT_MILLIS = 5000L
    }

    private var cameraManager: CameraManager? = null

    private var cameraId = ""
    private var camera: CameraDevice? = null

    private var characteristics: CameraCharacteristics? = null

    private var session: CameraCaptureSession? = null

    /**
     * 相机执行操作的线程
     * */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    /**
     * 执行 buffer reading 操作的线程
     * */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)
    // 用于获取拍摄的图片数据
    private var imageReader: ImageReader? = null

    private val animationTask: Runnable by lazy {
        Runnable {
            // 白色动画模拟曝光
            binding.overlay.background = Color.argb(150, 255, 255, 255).toDrawable()
            // 等待 50 毫秒
            binding.overlay.postDelayed({
                // 移除白色背景
                binding.overlay.background = null
            }, ANIMATION_FAST_MILLIS)
        }
    }

    val format = ImageFormat.JPEG
    // 监听镜头方向旋转的 livedata
    private lateinit var relativeOrientation: OrientationLiveData

    override fun initView(rootView: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            realInitView(rootView)
        }
    }

    override fun initData(context: Context) {
    }

    override fun onStop() {
        super.onStop()

        camera.safeClose()
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraManager = null
        camera = null
        characteristics = null

        cameraThread.quitSafely()
        cameraHandler.removeCallbacksAndMessages(null)
        imageReader = null

        session?.close()
        session = null
    }

    private suspend fun realInitView(rootView: View) {
        if (!preInitCamera()) {
            LogUtil.e(TAG, "preInitCamera fail, exit")
            finish()
        }

        binding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                // Selects appropriate preview size and configures view finder
                val previewSize = getPreviewOutputSize(
                    // 获取屏幕尺寸
                    binding.viewFinder.display,
                    characteristics!!,
                    SurfaceHolder::class.java
                )
                LogUtil.d(TAG, "view size: ${binding.viewFinder.width} x ${binding.viewFinder.height}")
                LogUtil.d(TAG, "Selected preview size: $previewSize")
                binding.viewFinder.setAspectRatio(
                    previewSize.width,
                    previewSize.height
                )

                // 4. surface 可用时，再初始化相机
                binding.root.post { initializeCamera() }
            }
        })

        relativeOrientation = OrientationLiveData(requireContext(), characteristics!!).apply {
            observe(viewLifecycleOwner) { orientation ->
                LogUtil.d(TAG, "Orientation changed: $orientation")
            }
        }
    }

    /**
     * 预初始化部分相机信息
     * */
    suspend fun preInitCamera(): Boolean {
        // 1. 检查 CAMERA 权限
        val isCameraGranted = suspendRequestPermission(
            Manifest.permission.CAMERA,
            "请求相机权限",
            "我们需要使用相机以进行预览和拍照",
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

        if (characteristics == null) {
            LogUtil.e(TAG, "can not obtain camera characteristics")
            return false
        }

        return true
    }

    /**
     * surface 可用后，初始化相机
     * */
    fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        // 5. surface 可用时，打开相机
        camera = openCamera(cameraManager!!, cameraId, cameraHandler)
        if (camera == null) {
            LogUtil.e(TAG, "can not obtain camera device")
            return@launch
        }

        // 6. 获取相片尺寸
        val size = characteristics.getMaxSize(format)
        // 7. 创建用于获取图片数据的 ImageReader
        imageReader = ImageReader.newInstance(size.width, size.height, format, IMAGE_BUFFER_SIZE)
        // 用于预览和拍照的 target surfaces
        val targets = listOf(binding.viewFinder.holder.surface, imageReader!!.surface)
        // 8. 创建会话
        session = createCaptureSession(camera!!, targets, cameraHandler)
        if (session == null) {
            LogUtil.e(TAG, "can not create capture session")
            return@launch
        }
        // 9. 创建预览请求
        val previewRequest = camera!!.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            // 预览画面输出到 SurfaceView
            addTarget(binding.viewFinder.holder.surface)
        }.build()
        // 9. 提交预览请求
        // 重复发送请求，直到调用了 session.stopRepeating() 方法
        session!!.setRepeatingRequest(previewRequest, null, cameraHandler)
        // 10. 监听拍照按钮的点击
        binding.captureButton.setNoDoubleClickListener {
            // 防止同时发出多个请求
            it?.isEnabled = false
            // 异步执行 io 操作
            lifecycleScope.launch(Dispatchers.IO) {
                takePhoto()
                it?.post { it.isEnabled = true }
            }
        }
    }

    private suspend fun takePhoto(): CaptureResultWrapper? = suspendCancellableCoroutine { cont ->
        val localImageReader = imageReader
        val localSession = session
        if (localImageReader == null || localSession == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        // 清空 image reader 中已有的内容
        @Suppress("ControlFlowWithEmptyBody")
        while (localImageReader.acquireNextImage() != null) {
        }

        // 11. 注册 image available 回调，使用新的 image queue 缓存数据
        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
        localImageReader.setOnImageAvailableListener({ reader ->
            val image = reader?.acquireNextImage() ?: return@setOnImageAvailableListener
            LogUtil.d(TAG, "image available in queue: ${image.timestamp}")
            imageQueue.add(image)
        }, imageReaderHandler)

        // 12. 创建拍照请求并提交
        val captureRequest = localSession.device.createCaptureRequest(
            CameraDevice.TEMPLATE_STILL_CAPTURE
        ).apply {
            addTarget(localImageReader.surface)
        }.build()
        localSession.capture(
            captureRequest,
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureStarted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    timestamp: Long,
                    frameNumber: Long
                ) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber)
                    // 13. 拍照开始，显示 50 毫秒曝光动画
                    binding.viewFinder.post(animationTask)
                }

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    // 14. 拍照完成，进行保存
                    // 获取图像的时间戳
                    val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                    LogUtil.d(TAG, "Capture result received: $resultTimestamp")
                    // 设置等待拍照结果返回的最大时长
                    val timeoutRunnable = Runnable { cont.resume(null) }
                    imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)
                    // 在协程的上下文中循环，直到出现具有匹配时间戳的图像。我们需要再次启动协程上下文，
                    // 因为回调是在提供给 `capture` 方法的处理程序中完成的，而不是在我们的协程上下文中
                    lifecycleScope.launch(cont.context) {
                        while (true) {
                            // 从队列中取出图像，如果队列中没有图像，则会阻塞等待
                            val image = imageQueue.take()
                            // 非 DEPTH_JPEG 需要检查时间戳
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                && image.format != ImageFormat.DEPTH_JPEG
                                && image.timestamp != resultTimestamp
                            ) {
                                continue
                            }
                            LogUtil.d(TAG, "Matching image dequeued: ${image.timestamp}")

                            imageReaderHandler.removeCallbacks(timeoutRunnable)
                            imageReader?.setOnImageAvailableListener(null, null)

                            // 清空剩余缓存图片
                            while (imageQueue.size > 0) {
                                imageQueue.take().close()
                            }

                            // 计算 EXIF 方向的 metadata
                            val rotation = relativeOrientation.value ?: 0
                            // 前置摄像头需要反转
                            val mirrored = characteristics!!.get(
                                CameraCharacteristics.LENS_FACING
                            ) == CameraCharacteristics.LENS_FACING_FRONT

                        }
                    }
                }
            },
            cameraHandler
        )

    }
}