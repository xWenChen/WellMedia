package com.mustly.wellmedia

import androidx.core.content.ContextCompat

/**
 * 返回颜色资源的颜色值
 * */
fun Int.colorRes() = this.let { colorRes ->
    ContextCompat.getColor(MediaApplication.getAppContext(), colorRes)
}