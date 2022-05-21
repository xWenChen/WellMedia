package com.mustly.medialib.video.base

/**
 * 比特率模式
 * */
enum class BitrateMode {
    /**
     * 未知类型
     * */
    UNKNOWN,
    /**
     * Variable Bitrate
     *
     * 可变比特率
     * */
    VBR,
    /**
     * Constants Bitrate
     *
     * 恒定比特率
     * */
    CBR,
    /**
     * Constants Quality
     *
     * 固定品质
     * */
    CQ
}

/**
 * 视频编码格式
 */
enum class VideoFormat {
    MP4
}

/**
 * 颜色模式
 * */
enum class VideoColorMode {

}