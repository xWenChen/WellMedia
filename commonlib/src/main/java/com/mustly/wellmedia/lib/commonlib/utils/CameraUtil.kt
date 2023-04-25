package com.mustly.wellmedia.utils

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.Point
import android.hardware.camera2.*
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.Surface
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

data class FormatItem(val cameraId: String, val format: Int)

/**
 * 相机镜头朝向
 * */
enum class LensFacing {
    /**
     * 后置摄像头与屏幕方向相反
     * */
    BACK,
    /**
     * 前置摄像头与屏幕方向相同
     * */
    FRONT,
    /**
     * 外置摄像头的朝向不确定，与屏幕方向可同可反
     * */
    EXTERNAL,
    UNKNOWN
}

/**
 * 获取指定格式的最大可用尺寸
 * */
fun CameraCharacteristics?.getMaxSize(format: Int): Size {
    return this?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        ?.getOutputSizes(format)
        ?.maxByOrNull {
            it.height * it.width
        } ?: SIZE_1080P.size
}

fun CameraDevice?.safeClose() {
    try {
        this?.close()
    } catch (e: Exception) {
        LogUtil.e("CameraDevice", "Error closing camera", e)
    }
}

@SuppressLint("MissingPermission")
suspend fun openCamera(
    cameraManager: CameraManager,
    cameraId: String,
    handler: Handler? = null
): CameraDevice? = suspendCancellableCoroutine { cont ->
    val TAG = "openCamera"

    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) = cont.resume(device)

        override fun onDisconnected(device: CameraDevice) {
            Log.w(TAG, "Camera $cameraId has been disconnected")
        }

        override fun onError(device: CameraDevice, error: Int) {
            val msg = when (error) {
                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                ERROR_CAMERA_DISABLED -> "Device policy"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                else -> "Unknown"
            }
            val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
            LogUtil.e(TAG, exc)

            if (cont.isActive) {
                cont.resume(null)
            }
        }
    }, handler)
}

/**
 * 创建 [CameraCaptureSession]
 */
suspend fun createCaptureSession(
    device: CameraDevice,
    targets: List<Surface>,
    handler: Handler? = null
): CameraCaptureSession? = suspendCancellableCoroutine { cont ->
    // Create a capture session using the predefined targets; this also involves defining the
    // session state callback to be notified of when the session is ready
    device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

        override fun onConfigureFailed(session: CameraCaptureSession) {
            val exc = RuntimeException("Camera ${device.id} session configuration failed")
            LogUtil.e("createCaptureSession", exc)
            cont.resume(null)
        }
    }, handler)
}

/**
 * 找到特定朝向的相机 id
 * */
fun findCameraId(cameraManager: CameraManager, facing: LensFacing): String {
    val idList = findCameraList(cameraManager)

    if (idList.isEmpty()) {
        return ""
    }

    val lensFacing = facing.value()

    for (id in idList) {
        val characteristics = cameraManager.getCameraCharacteristics(id.cameraId)
        val itemFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

        if (itemFacing == lensFacing) {
            return id.cameraId
        }
    }

    return ""
}

/**
 * 列举出支持 [ImageFormat.JPEG]、[ImageFormat.RAW_SENSOR]、[ImageFormat.DEPTH_JPEG] 格式的相机
 *
 * @param cameraManager 相机管理器的实例
 * @param onlyJPEG 是否只列举 JPEG 格式的相机
 * */
fun findCameraList(
    cameraManager: CameraManager,
    onlyJPEG: Boolean = true
): List<FormatItem> {
    val availableCameras = mutableListOf<FormatItem>()

    // 获取所有兼容相机
    val cameraIds = cameraManager.cameraIdList.filter {
        val characteristics = cameraManager.getCameraCharacteristics(it)
        val capabilities = characteristics.get(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
        )
        capabilities?.contains(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
        ) ?: false
    }

    for (id in cameraIds) {
        // 所有相机都支持 JPEG 格式，我们无需再做额外的属性检查
        availableCameras.add(FormatItem(id, ImageFormat.JPEG))

        if (onlyJPEG) {
            continue
        }

        // 查询相机属性集
        val characteristics = cameraManager.getCameraCharacteristics(id)
        // 查询兼容性和输出格式
        val capabilities = characteristics.get(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
        )
        val outputFormats = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )?.outputFormats

        if (capabilities == null || outputFormats == null) {
            continue
        }

        // 检查是否支持 RAW 格式
        if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
            && outputFormats.contains(ImageFormat.RAW_SENSOR)
        ) {
            availableCameras.add(FormatItem(id, ImageFormat.RAW_SENSOR))
        }

        // 检查是否支持 JPEG DEPTH 格式
        if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT)
            && outputFormats.contains(ImageFormat.DEPTH_JPEG)
        ) {
            availableCameras.add(FormatItem(id, ImageFormat.DEPTH_JPEG))
        }
    }

    return availableCameras
}

fun Int?.enum() = when(this) {
    CameraCharacteristics.LENS_FACING_BACK -> LensFacing.BACK
    CameraCharacteristics.LENS_FACING_FRONT -> LensFacing.FRONT
    CameraCharacteristics.LENS_FACING_EXTERNAL -> LensFacing.EXTERNAL
    else -> LensFacing.UNKNOWN
}

/**
 * 相机镜头朝向的描述文本
 * */
fun LensFacing.value() = when(this) {
    LensFacing.BACK -> CameraCharacteristics.LENS_FACING_BACK
    LensFacing.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
    LensFacing.EXTERNAL -> CameraCharacteristics.LENS_FACING_EXTERNAL
    else -> CameraCharacteristics.LENS_FACING_FRONT
}

/**
 * 尺寸封装，增加了获取长边和短边的能力
 * */
class SmartSize(width: Int, height: Int) {
    var size = Size(width, height)
    var long = max(size.width, size.height)
    var short = min(size.width, size.height)
    override fun toString() = "SmartSize(${long}x${short})"
}

/**
 * 图片和视频的标准高清尺寸
 * */
val SIZE_1080P: SmartSize = SmartSize(1920, 1080)

fun getDisplaySmartSize(display: Display): SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}

/**
 * 返回可用的预览尺寸. 更多信息见:
 *
 * https://d.android.com/reference/android/hardware/camera2/CameraDevice and
 * https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap
 */
fun <T>getPreviewOutputSize(
    display: Display,
    characteristics: CameraCharacteristics,
    targetClass: Class<T>,
    format: Int? = null
): Size {
    // 取屏幕尺寸和 1080p 之间的较小值
    val screenSize = getDisplaySmartSize(display)
    val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
    val maxSize = if (hdScreen) SIZE_1080P else screenSize

    // 如果提供了 format，则根据 format 决定尺寸；否则使用目标 class 决定尺寸
    val config = characteristics.get(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
    ) ?: return maxSize.size

    val allSizes = if (format == null) {
        config.getOutputSizes(targetClass)
    } else {
        config.getOutputSizes(format)
    }

    // 按照面积从大到小排序
    val validSizes = allSizes.sortedWith(
        compareBy { it.height * it.width }
    ).map {
        SmartSize(it.width, it.height)
    }.reversed()

    if (validSizes.isEmpty()) {
        return maxSize.size
    }

    // 获取可用的面积最大的尺寸
    return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
}