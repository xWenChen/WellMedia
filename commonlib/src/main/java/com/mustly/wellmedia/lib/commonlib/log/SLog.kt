package com.mustly.wellmedia.lib.commonlib.log

import android.util.Log

/**
 * description:
 *
 * System Log
 *
 * @author   wchenzhang
 * date：    2022/8/8 20:29
 * version   1.0
 * modify by
 */
class SLog(isLogEnable: Boolean = true) : ILog(isLogEnable) {
    override fun realV(tag: String, msg: String) {
        Log.v(tag, msg)
    }

    override fun realD(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun realI(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    override fun realW(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    override fun realE(tag: String, error: Throwable?) {
        Log.e(tag, "", error)
    }

    override fun realE(tag: String, msg: String, error: Throwable?) {
        Log.e(tag, msg, error)
    }
}