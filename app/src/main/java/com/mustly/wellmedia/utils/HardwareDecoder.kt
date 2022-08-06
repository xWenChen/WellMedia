package com.mustly.wellmedia.utils

import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2022/8/5 19:25
 * version   1.0
 * modify by
 */
class HardwareDecoder(val isVideo: Boolean) {
    companion object {
        const val TAG = "HardwareDecoder"
        const val TIMEOUT = 10000L
    }

    /**
     * 音视频分离器
     * */
    var mExtractor: MediaExtractor? = MediaExtractor()
}