package com.mustly.wellmedia.lib.medialib.base.bean

import android.media.AudioFormat
import android.media.MediaFormat
import android.os.Build
import com.mustly.wellmedia.lib.medialib.base.MediaType

/**
 * description:
 *
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
    // 通用
    val duration: Long
        get() {
            return if (mediaFormat?.containsKey(MediaFormat.KEY_DURATION) == true)
                mediaFormat.getLong(MediaFormat.KEY_DURATION)
            else
                0L
        }
    // 适用于视频
    val width: Int
        get() {
            return if (mediaFormat?.containsKey(MediaFormat.KEY_WIDTH) == true)
                mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
            else
                0
        }
    // 适用于视频
    val height: Int
        get() {
            return if (mediaFormat?.containsKey(MediaFormat.KEY_HEIGHT) == true)
                mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
            else
                0
        }
    // 采样率，适用于音频
    val sampleRate: Int
        get() {
            return if (mediaFormat?.containsKey(MediaFormat.KEY_SAMPLE_RATE) == true)
                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            else
                0
        }
    // 音轨数量，适用于音频
    val channelCount: Int
        get() {
            return if (mediaFormat?.containsKey(MediaFormat.KEY_CHANNEL_COUNT) == true)
                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            else
                0
        }
    // 采样位深，适用于音频
    val sampleDepth: Int
        get() {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                AudioFormat.ENCODING_PCM_16BIT
            } else if (mediaFormat?.containsKey(MediaFormat.KEY_PCM_ENCODING) == true) {
                mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
        }
    // 声道配置，适用于音频
    val voiceTrack: Int
        get() {
            return if (channelCount == 1) {
                AudioFormat.CHANNEL_OUT_MONO // 单声道
            } else {
                AudioFormat.CHANNEL_OUT_STEREO // 双声道
            }
        }
}