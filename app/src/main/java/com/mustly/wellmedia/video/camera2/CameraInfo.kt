package com.mustly.wellmedia.video.camera2

import android.util.Size

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2023/5/20 15:49
 * version   1.0
 * modify by
 */
data class CameraInfo(
    val name: String,
    val cameraId: String,
    val size: Size,
    val fps: Int,
    val previewStabilization: Boolean = false,
    val useHardware: Boolean = false,
    val filterOn: Boolean = false,
)