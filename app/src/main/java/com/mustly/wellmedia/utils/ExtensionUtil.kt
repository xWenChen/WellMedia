package com.mustly.wellmedia

import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 返回颜色资源的颜色值
 * */
fun Int.colorRes() = this.let { colorRes ->
    ContextCompat.getColor(MediaApplication.getAppContext(), colorRes)
}
/**
 * 获取资源 id 的 uri 路径表示
 * */
fun Int.uriPath(): String = "android.resource://${MediaApplication.getAppContext().packageName}/$this"
/**
 * 通用携程扩展
 * */
fun <T> CoroutineScope.runResult(
    doOnIo: (CoroutineScope.() -> T),
    doOnSuccess: (CoroutineScope.(T) -> Unit),
    doOnFailure: (CoroutineScope.(Throwable) -> Unit)? = null,
) = launch(Dispatchers.Main) {
    kotlin.runCatching {
        withContext(Dispatchers.IO) {
            doOnIo.invoke(this)
        }
    }.onSuccess {
        doOnSuccess.invoke(this, it)
    }.onFailure {
        Log.e("runResult", "", it)
        doOnFailure?.invoke(this, it)
    }
}
