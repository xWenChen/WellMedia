package com.mustly.wellmedia.lib.commonlib.log

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2022/8/9 9:20
 * version   1.0
 * modify by
 */
object LogUtil {
    /**
     * 日志打印器
     * */
    private var logger: ILog = SLog()

    // 设置自定义日志打印
    fun setLogger(logger: ILog) {
        this.logger = logger
    }

    fun v(tag: String = "", msg: String = "") {
        logger.v(tag, msg)
    }
    fun d(tag: String = "", msg: String = "") {
        logger.d(tag, msg)
    }
    fun i(tag: String = "", msg: String = "") {
        logger.i(tag, msg)
    }
    fun w(tag: String = "", msg: String = "") {
        logger.w(tag, msg)
    }
    fun e(tag: String = "", error: Throwable? = null) {
        logger.e(tag, error)
    }
    fun e(tag: String = "", msg: String = "", error: Throwable? = null) {
        logger.e(tag, msg, error)
    }
}