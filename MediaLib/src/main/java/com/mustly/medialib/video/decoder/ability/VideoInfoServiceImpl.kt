package com.mustly.medialib.video.decoder.ability

import com.mustly.medialib.video.base.bean.VideoInfo
import com.mustly.medialib.video.decoder.VideoInfoService

object VideoInfoServiceImpl: VideoInfoService {
    const val TAG = "VideoInfoServiceImpl"

    private val parser = DefaultHardwareVideoInfoParser()

    /**
     * 获取视频信息
     * */
    override fun getVideoInfo(videoPath: String): VideoInfo {
        return VideoInfo()
    }

    /**
     * 是否支持 读取/设置 比特率模式
     *
     * @param mimeType 待查询的 mimeType
     *
     * @return true 表示支持，false 表示不支持
     * */
    override fun isSupportBitrateMode(mimeType: String): Boolean {
        // return parser.isSupportBitrateMode(mimeType)
        return false
    }
}