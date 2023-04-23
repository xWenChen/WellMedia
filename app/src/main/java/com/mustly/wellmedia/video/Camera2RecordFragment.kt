package com.mustly.wellmedia.video

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.ImageReader
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentCamera2RecordBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.utils.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

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
class Camera2RecordFragment : BaseFragment<FragmentCamera2RecordBinding>() {
    companion object {
        const val TAG = "Camera2RecordFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private var isInitialized = false

    // 第 1 步，获取相机
    private var cameraManager: CameraManager? = null
    private var cameraInfo: CameraInfo? = null
    private var previewSurface: Surface? = null
    private var readerSurface: Surface? = null
    private val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            // 当相机成功打开时回调该方法，接下来可以执行创建预览的操作
            cameraInfo?.curUsableDevice = device // 获取到可用的 CameraDevice 实例
            binding.tvLoading.visibility = View.GONE
            startPreview()
        }

        override fun onDisconnected(device: CameraDevice) {
            // 当相机断开连接时回调该方法，应该在此执行释放相机的操作
            cameraInfo?.curUsableDevice = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            // 当相机打开失败时，应该在此执行释放相机的操作
            cameraInfo?.curUsableDevice = null
        }
    }
    private val deviceEnableCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            if (cameraId == cameraInfo?.cameraId) {
                cameraManager?.openCamera(cameraInfo?.cameraId ?: "", callback, null)
            }
        }
    }

    override fun initView(rootView: View) {
        /*binding.btnVideoRecord.setOnClickListener {
            if (isInitialized) {
                recordVideo()
            }
        }
        binding.preview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                realInitData()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })*/
    }

    override fun initData(context: Context) {
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.unregisterAvailabilityCallback(deviceEnableCallback)
    }

    private fun realInitData() {
        /*checkAndRequestPermissions(REQUIRED_PERMISSIONS) { resultMap ->
            if (resultMap.all { it.value }) {
                isInitialized = true
                binding.tvLoading.visibility = View.VISIBLE
                if (!initCamera()) {
                    binding.tvLoading.visibility = View.GONE
                }
            }
        }*/
    }

    private fun initCamera(): Boolean {
        val realActivity = activity

        if (realActivity == null) {
            LogUtil.e(TAG, "activity is null, can not init camera.")
            return false
        }

        // 第 1 步，获取 CameraManager
        cameraManager = activity?.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            LogUtil.e(TAG, "cameraManager is null, can not init camera.")
            return false
        }

        // 第 2 步，获取 CameraCharacteristics。每个Camera设备都有一组静态属性信息，这组静态属性信息描述该Camera设备的能力，可用的参数等
        getCameraInfo(CameraCharacteristics.LENS_FACING_BACK)
        if (cameraInfo?.properties == null) {
            LogUtil.e(TAG, "cameraCharacteristics is null, can not init camera.")
            return false
        }

        // 第 3 步，打开相机，获取 CameraDevice 并设置回调
        cameraManager?.registerAvailabilityCallback(deviceEnableCallback, null)

        // 第 1 步，获取 surface
        readerSurface = ImageReader.newInstance(1080, 1920, ImageFormat.YUV_420_888, 30).surface
        previewSurface = binding.preview.holder.surface

        return true
    }

    private fun getCameraInfo(facing: Int) {
        cameraInfo = CameraInfo()
        // 获取可用相机
        cameraManager?.cameraIdList?.forEach {
            val characteristics = cameraManager?.getCameraCharacteristics(it)
            if (characteristics?.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) == true
                && facing == characteristics.get(CameraCharacteristics.LENS_FACING)) {
                cameraInfo?.cameraId = it
                cameraInfo?.properties = characteristics
                return
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun recordVideo() {
        /*val videoCapture = videoCapture ?: return

        val curRecording = recording
        if (curRecording != null) {
            // 停止当前的视频录制
            curRecording.stop()
            recording = null
            return
        }

        // 创建新的视频录制会话，并开始
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output.prepareRecording(requireContext(), mediaStoreOutputOptions)
            .apply {
                if (activity?.isPermissionGranted(REQUIRED_PERMISSIONS[1]) == true) {
                    withAudioEnabled()
                }
            }
            .start(Dispatchers.Main.asExecutor()) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.btnVideoRecord.text = R.string.stop_record.stringRes
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (recordEvent.hasError()) {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: ${recordEvent.error}")
                        } else {
                            val msg = "视频录制成功，保存路径: ${recordEvent.outputResults.outputUri}"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                        }

                        binding.btnVideoRecord.text = R.string.start_record.stringRes
                    }
                }
            }*/
    }

    private fun startPreview() {

    }
}

class CameraInfo {
    var cameraId = ""
    var properties: CameraCharacteristics? = null
    var curUsableDevice: CameraDevice? = null
}