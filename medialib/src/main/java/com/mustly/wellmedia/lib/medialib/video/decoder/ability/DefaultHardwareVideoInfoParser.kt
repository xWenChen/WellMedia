package com.mustly.wellmedia.lib.medialib.video.decoder.ability

import android.media.*
import android.os.Build
import com.mustly.wellmedia.lib.medialib.audio.base.bean.AudioTrackInfo
import com.mustly.wellmedia.lib.medialib.base.MediaType
import com.mustly.wellmedia.lib.medialib.base.bean.MediaTrackInfo
import com.mustly.wellmedia.lib.medialib.video.VideoUtil
import com.mustly.wellmedia.lib.medialib.video.base.BitrateMode
import com.mustly.wellmedia.lib.medialib.video.base.bean.VideoInfo
import com.mustly.wellmedia.lib.medialib.video.base.bean.VideoTrackInfo
import com.mustly.wellmedia.lib.medialib.video.decoder.VideoParseException

/**
 * 通过硬件的方式，解码文件信息
 * */
class DefaultHardwareVideoInfoParser {
    /**
     * 解析文件，提取文件信息
     * */
    @Throws(Exception::class)
    fun parse(videoPath: String): VideoInfo {
        val videoInfo = VideoInfo()
        val mediaExtractor = MediaExtractor().apply {
            setDataSource(videoPath)
        }
        val count = mediaExtractor.trackCount
        if (count <= 0) {
            throw VideoParseException("video track count must greater than zero")
        }

        for (i in 0 until count) {
            val format = mediaExtractor.getTrackFormat(i)
            val mimeType = format.getString(MediaFormat.KEY_MIME)
            var trackInfo: MediaTrackInfo = MediaTrackInfo()
            mimeType?.apply {
                if (startsWith("video/")) {
                    trackInfo = VideoTrackInfo()
                    val temp = trackInfo as VideoTrackInfo
                    parseVideoInfo(temp, format, mimeType)
                } else if (startsWith("audio/")) {
                    trackInfo = AudioTrackInfo()
                    // 提取音频信息
                    trackInfo.mediaType = MediaType.AUDIO
                }

                trackInfo.mimeType = this
            }

            trackInfo.index = i

            // 提取公共信息
            trackInfo.bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
            trackInfo.duration = format.getLong(MediaFormat.KEY_DURATION)
            trackInfo.bitrateMode = VideoUtil.convertToWrapBitrateMode(
                format.getInteger(MediaFormat.KEY_BITRATE_MODE)
            )

            videoInfo.trackInfoList.add(trackInfo)
        }

        return videoInfo
    }

    /**
     * 是否支持 读取/设置 比特率模式
     *
     * @param mimeType 待查询的 mimeType
     *
     * @return true 表示支持，false 表示不支持
     * */
    fun isSupportBitrateMode(mimeType: String, mode: BitrateMode): Boolean {
        val format = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, mimeType)
        }
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        // 找不到 mimeType 对应的解码器
        if(list.findDecoderForFormat(format).isNullOrEmpty()) {
            return false
        }
        val decoder = MediaCodec.createDecoderByType(mimeType)
        val codecInfo = decoder.codecInfo.getCapabilitiesForType(mimeType) ?: return false
        return codecInfo.encoderCapabilities.isBitrateModeSupported(VideoUtil.convertToHardBitrateMode(mode))
    }

    private fun parseVideoInfo(temp: VideoTrackInfo, format: MediaFormat, mimeType: String) {
        // 提取视频信息
        temp.mediaType = MediaType.VIDEO
        temp.width = format.getInteger(MediaFormat.KEY_WIDTH)
        temp.height = format.getInteger(MediaFormat.KEY_HEIGHT)
        temp.colorFormat = VideoUtil.convertWrapColorFormat(
            format.getInteger(MediaFormat.KEY_COLOR_FORMAT)
        )
        temp.frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
        temp.captureRate = format.getInteger(MediaFormat.KEY_CAPTURE_RATE)
        // 参数仅限于编码器使用
        temp.iFrameInterval = format.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL)
        // 参数仅限于编码器使用
        if (isSupportIntraRefresh(mimeType)) {
            temp.intraRefreshPeriod = format.getInteger(MediaFormat.KEY_INTRA_REFRESH_PERIOD)
        }

    }

    /**
     * 是否支持 intra refresh 技术，此技术仅编码器使用
     *
     * @param mimeType 待查询的 mimeType
     *
     * @return true 表示支持，false 表示不支持
     * */
    fun isSupportIntraRefresh(mimeType: String): Boolean {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }
        val format = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, mimeType)
        }
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        // 找不到 mimeType 对应的解码器
        if(list.findEncoderForFormat(format).isNullOrEmpty()) {
            return false
        }
        val encoder = MediaCodec.createEncoderByType(mimeType)
        val codecInfo = encoder.codecInfo.getCapabilitiesForType(mimeType) ?: return false

        return codecInfo.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_IntraRefresh)
    }
}