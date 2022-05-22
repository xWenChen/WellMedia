package com.mustly.wellmedia.lib.medialib.video.decoder

/**
 * 表示视频解析出错
 * */
class VideoParseException: Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}