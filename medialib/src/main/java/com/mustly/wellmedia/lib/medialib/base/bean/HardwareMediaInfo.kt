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

    val duration: Long
        get() {
            return if (mediaFormat?.containsKey(MediaFormat.KEY_DURATION) == true)
                mediaFormat.getLong(MediaFormat.KEY_DURATION)
            else
                0L
        }
    val width: Int
        get() {
            return if (mediaFormat?.containsKey(MediaFormat.KEY_WIDTH) == true)
                mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
            else
                0
        }
    val height: Int
        get() {
            return if (mediaFormat?.containsKey(MediaFormat.KEY_HEIGHT) == true)
                mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
            else
                0
        }
}