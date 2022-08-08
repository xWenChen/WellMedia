package com.mustly.wellmedia.lib.medialib.base.bean

import android.media.MediaFormat
import com.mustly.wellmedia.lib.medialib.base.MediaType

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2022/8/8 11:39
 * version   1.0
 * modify by
 */
data class HardwareMediaInfo(
    val mimeType: String,
    val trackIndex: Int,
    val mediaFormat: MediaFormat?,
) : MediaInfo() {
    override var mediaType: MediaType = when {
        mimeType.startsWith("video/") -> MediaType.VIDEO
        mimeType.startsWith("audio/") -> MediaType.AUDIO
        else -> MediaType.UNKNOWN
    }
}