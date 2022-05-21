package com.mustly.medialib.video.base.bean

import com.mustly.medialib.base.MediaType
import com.mustly.medialib.base.bean.MediaInfo
import com.mustly.medialib.base.bean.MediaTrackInfo

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