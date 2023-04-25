package com.mustly.wellmedia.lib.commonlib.utils

import android.view.View

fun View?.setNoDoubleClickListener(
    interval: Long = 500L, // 单位：毫秒
    noDoubleClick: ((View?) -> Unit) = {}
) {
    this?.setOnClickListener(NoDoubleClickListener(interval, noDoubleClick))
}

// 防止过快点击的 Listener
class NoDoubleClickListener(
    private val interval: Long = 500L, // 单位：毫秒
    private val noDoubleClick: ((View?) -> Unit) = {}
) : View.OnClickListener {
    // 单位：毫秒
    private var lastClickTime = 0L

    override fun onClick(v: View?) {
        if (ignoreClick()) {
            return
        }
        noDoubleClick(v)
    }

    private fun ignoreClick(): Boolean {
        val nowTime = System.currentTimeMillis()
        val ignore = nowTime - lastClickTime < interval
        if (!ignore) {
            lastClickTime = nowTime
        }
        return ignore
    }
}