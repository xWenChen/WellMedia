package com.mustly.medialib.video.base.bean

import com.mustly.medialib.base.ColorFormat
import com.mustly.medialib.base.MediaType
import com.mustly.medialib.base.bean.MediaTrackInfo

class VideoTrackInfo : MediaTrackInfo() {
    override var mediaType: MediaType = MediaType.VIDEO
        get() = MediaType.VIDEO
        set(value) {field = MediaType.VIDEO}
    /**
     * 视频的宽度
     * */
    var width = 0
    /**
     * 视频的高度
     * */
    var height = 0
    /**
     * 颜色模式
     * */
    var colorFormat = ColorFormat.RGBA_8888
}