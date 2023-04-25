package com.mustly.wellmedia.image

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceHolder
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.databinding.FragmentCamera2TakePhotoBinding
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.commonlib.utils.setNoDoubleClickListener
import com.mustly.wellmedia.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    // 用于获取拍摄的图片数据
    private var imageReader: ImageReader? = null

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

        if (camera == null) {
            LogUtil.e(TAG, "can not obtain camera device, exit")
            finish()
            return
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
        val format = ImageFormat.JPEG
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

        }
    }
}