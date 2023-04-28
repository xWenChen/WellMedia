package com.mustly.wellmedia.video

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentCamera2RecordBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.commonlib.utils.*
import com.mustly.wellmedia.utils.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * MediaCodec + Camera2 录制视频
 *
 * https://developer.android.com/training/camera2
 *
 * 相机的图像方向，通常和手机的方向相差 90 度，设备必须旋转 SENSOR_ORIENTATION 度数(可选度数：0、90、180、270)，
 * 前置摄像头逆时针选项，后置摄像头顺时针旋转：
 *  https://developer.android.com/static/images/training/camera/camera2/camera-preview/camera_preview_portrait_front_facing.png
 *
 * 摄像头图像的旋转度数，需要是 SENSOR_ORIENTATION + 设备旋转度数
 *
 * Camera2 使用步骤：
 *  1. 获取 CameraManager
 *  2. 获取 CameraManager#CameraCharacteristics
 *  3. 打开相机，获取 CameraDevice 并设置回调
 *
 * Camera2 使用第一步 >>> 获取目标 surface，surface 可能来自于不同地方
 *      1. 如果想把图像直接展示给用户，可以使用 SurfaceView 的 surface
 *      2. 如果想要读取每一帧图像或者执行逐帧分析，可以使用 ImageReader 的 surface
 *      3. 如果进行并行处理，可以使用 RenderScript.Allocation 的 surface
 *      4. 尽管不推荐使用(基于可维护性考虑)，也可以使用 OpenGL Texture 或 TextureView 的 surface
 * */
@Route(PageRoute.CAMERA2_RECORD_VIDEO)
class Camera2RecordVideoFragment : BaseFragment<FragmentCamera2RecordBinding>() {
    companion object {
        const val TAG = "Camera2RecordVideoFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

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

    private val animationTask: java.lang.Runnable by lazy {
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
        // surface 的回调监听不能太晚，否则会出现间歇性黑屏(出现概率较高)
        binding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                realInitView()
            }
        })
    }

    override fun initData(context: Context) {
    }

    override fun onStop() {
        super.onStop()

        camera.safeClose()
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraThread.quitSafely()
        imageReaderThread.quitSafely()
    }

    private fun realInitView() = lifecycleScope.launch(Dispatchers.Main) {
        if (!preInitCamera()) {
            LogUtil.e(TAG, "preInitCamera fail, exit")
            finish()
        }
        // 选择合适的尺寸，并配置 surface
        val previewSize = getPreviewOutputSize(
            // 获取屏幕尺寸
            binding.viewFinder.display,
            characteristics!!,
            SurfaceHolder::class.java
        )
        LogUtil.d(TAG, "view size: ${binding.viewFinder.width} x ${binding.viewFinder.height}, Selected preview size: $previewSize")
        binding.viewFinder.setAspectRatio(
            previewSize.width,
            previewSize.height
        )

        // 4. surface 可用时，再初始化相机
        initializeCamera()
        // 监听角度旋转
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
                // 拍照并保存
                takePhoto()?.use { result ->
                    saveResult(result)
                }
                it?.post { it.isEnabled = true }
            }
        }
    }

    private suspend fun saveResult(result: CaptureResultWrapper?) {
        LogUtil.d(TAG, "Result received: $result")
        if (result == null) {
            return
        }

        val output = realSaveResult(result)

        LogUtil.d(TAG, "Image saved: ${output?.absolutePath}")

        if (output == null) {
            return
        }

        // 如果格式是 JPG，则更新 EXIF metadata 的旋转信息
        if (output.extension == "jpg") {
            val exif = ExifInterface(output.absolutePath)
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                result.orientation.toString()
            )
            exif.saveAttributes()
            LogUtil.d(TAG, "EXIF metadata saved: ${output.absolutePath}")
        }
    }

    private suspend fun realSaveResult(
        result: CaptureResultWrapper
    ) = suspendCoroutine<File?> { cont ->
        LogUtil.d(TAG, "Result received: $result")

        when (result.format) {
            // 如果格式是 JPEG 或者 DEPTH_JPEG，则我们可以使用 InputStream 保存
            ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                try {
                    // 图片数据写入文件
                    val outputs = createFile(requireContext(), "jpg")
                    FileOutputStream(outputs).use { it.write(bytes) }
                    cont.resume(outputs)
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Unable to write JPEG image to file", e)
                    cont.resume(null)
                }
            }
            // 如果格式是 RAW 格式，则我们需要用 DngCreator 工具类保存
            ImageFormat.RAW_SENSOR -> {
                try {
                    val dngCreator = DngCreator(characteristics!!, result.metadata)
                    // 图片数据写入文件
                    val outputs = createFile(requireContext(), "dng")
                    FileOutputStream(outputs).use { dngCreator.writeImage(it, result.image) }
                    cont.resume(outputs)
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Unable to write DNG image to file", e)
                    cont.resume(null)
                }
            }
            // 其他格式不支持
            else -> {
                LogUtil.e(TAG, "Unknown image format: ${result.image.format}")
                cont.resume(null)
            }
        }
    }

    private fun createFile(context: Context, extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA)
        return File(context.externalCacheDir, "IMG_${sdf.format(Date())}.$extension")
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
            getCaptureCallback(cont, imageQueue),
            cameraHandler
        )

    }

    private fun getCaptureCallback(
        cont: CancellableContinuation<CaptureResultWrapper?>,
        imageQueue: ArrayBlockingQueue<Image>
    ) = object : CameraCaptureSession.CaptureCallback() {
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
            checkCaptureResult(result, cont, imageQueue)
        }
    }

    private fun checkCaptureResult(
        result: TotalCaptureResult,
        cont: CancellableContinuation<CaptureResultWrapper?>,
        imageQueue: ArrayBlockingQueue<Image>
    ) {
        // 14. 拍照完成，获取正确的图像
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
                val mirrored = characteristics?.get(
                    CameraCharacteristics.LENS_FACING
                ) == CameraCharacteristics.LENS_FACING_FRONT
                val exifOrientation = computeExifOrientation(rotation, mirrored)

                cont.resume(
                    CaptureResultWrapper(
                        image,
                        result,
                        exifOrientation,
                        imageReader?.imageFormat ?: ImageFormat.JPEG
                    )
                )
            }
        }
    }
}