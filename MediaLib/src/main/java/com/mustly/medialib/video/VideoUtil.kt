package com.mustly.medialib.video

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities
import com.mustly.medialib.base.ColorFormat
import com.mustly.medialib.video.base.BitrateMode

object VideoUtil {
    fun convertToWrapBitrateMode(mode: Int): BitrateMode {
        return when(mode) {
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR -> BitrateMode.CBR
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR -> BitrateMode.VBR
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ -> BitrateMode.CQ
            else -> BitrateMode.UNKNOWN
        }
    }

    fun convertToHardBitrateMode(mode: BitrateMode): Int {
        return when(mode) {
            BitrateMode.CBR -> MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
            BitrateMode.VBR -> MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
            BitrateMode.CQ -> MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ
            else -> MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
        }
    }

    fun convertWrapColorFormat(integer: Int): ColorFormat {
        return when(integer) {
            CodecCapabilities.COLOR_Format16bitRGB565 -> ColorFormat.RGB_565
            CodecCapabilities.COLOR_Format24bitBGR888,
            CodecCapabilities.COLOR_FormatRGBFlexible -> ColorFormat.RGB_888
            CodecCapabilities.COLOR_Format32bitABGR8888,
            CodecCapabilities.COLOR_FormatRGBAFlexible -> ColorFormat.RGBA_8888
            CodecCapabilities.COLOR_FormatRawBayer8bit,
            CodecCapabilities.COLOR_FormatRawBayer10bit,
            CodecCapabilities.COLOR_FormatRawBayer8bitcompressed -> ColorFormat.BAYER
            CodecCapabilities.COLOR_FormatYUV420Flexible -> ColorFormat.YUV420
            CodecCapabilities.COLOR_FormatYUV422Flexible -> ColorFormat.YUV422
            CodecCapabilities.COLOR_FormatYUV444Flexible -> ColorFormat.YUV444
            else -> ColorFormat.RGBA_8888
        }
    }
}