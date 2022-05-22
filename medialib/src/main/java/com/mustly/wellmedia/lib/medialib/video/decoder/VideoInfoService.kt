package com.mustly.wellmedia.lib.medialib.video.decoder

import com.mustly.wellmedia.lib.medialib.video.base.bean.VideoInfo


interface VideoInfoService {
    /**
     * 获取视频信息
     *
     * @param videoPath 待解析的视频的路径
     * */
    fun getVideoInfo(videoPath: String): VideoInfo

    /**
     * 是否支持 读取/设置 比特率模式
     *
     * @param mimeType 待查询的 mimeType
     *
     * @return true 表示支持，false 表示不支持
     * */
    fun isSupportBitrateMode(mimeType: String): Boolean
}