package com.mustly.wellmedia.lib.commonlib.log

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2022/8/8 20:22
 * version   1.0
 * modify by
 */
abstract class ILog(private val isLogEnable: Boolean = true) {
    fun v(tag: String = "", msg: String = "") {
        if (!isLogEnable) {
            return
        }
        realV(tag, msg)
    }
    fun d(tag: String = "", msg: String = "") {
        if (!isLogEnable) {
            return
        }
        realD(tag, msg)
    }
    fun i(tag: String = "", msg: String = "") {
        if (!isLogEnable) {
            return
        }
        realI(tag, msg)
    }
    fun w(tag: String = "", msg: String = "") {
        if (!isLogEnable) {
            return
        }
        realW(tag, msg)
    }
    fun e(tag: String = "", error: Throwable? = null) {
        if (!isLogEnable) {
            return
        }
        realE(tag, error)
    }
    fun e(tag: String = "", msg: String = "", error: Throwable? = null) {
        if (!isLogEnable) {
            return
        }
        realE(tag, msg, error)
    }

    abstract fun realV(tag: String = "", msg: String = "")
    abstract fun realD(tag: String = "", msg: String = "")
    abstract fun realI(tag: String = "", msg: String = "")
    abstract fun realW(tag: String = "", msg: String = "")
    abstract fun realE(tag: String = "", error: Throwable? = null)
    abstract fun realE(tag: String = "", msg: String = "", error: Throwable? = null)
}