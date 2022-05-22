package com.mustly.wellmedia.lib.medialib.video.base.bean

import com.mustly.wellmedia.lib.medialib.base.MediaType
import com.mustly.wellmedia.lib.medialib.base.bean.MediaInfo
import com.mustly.wellmedia.lib.medialib.base.bean.MediaTrackInfo

open class VideoInfo: MediaInfo() {
    override var mediaType = MediaType.VIDEO
    /**
     * 视频的帧率
     * */
    var frameRate = 0
    /**
     * 轨道相关的信息
     * */
    var trackInfoList = mutableListOf<MediaTrackInfo>()
}