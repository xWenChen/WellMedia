package com.mustly.wellmedia.utils

import android.app.Activity
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import com.mustly.wellmedia.MediaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 返回颜色资源的颜色值
 * */
val Int.colorRes
    get() = this.let { colorRes ->
        ContextCompat.getColor(MediaApplication.getAppContext(), colorRes)
    }
/**
 * 返回字符串资源的文本
 * */
val Int.stringRes
    get() = this.let { stringRes ->
        MediaApplication.getAppContext().getString(stringRes)
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
    doOnSuccess: (CoroutineScope.(T) -> Unit)? = null,
    doOnFailure: (CoroutineScope.(Throwable) -> Unit)? = null,
) = launch(Dispatchers.Main) {
    kotlin.runCatching {
        withContext(Dispatchers.IO) {
            doOnIo.invoke(this)
        }
    }.onSuccess {
        doOnSuccess?.invoke(this, it)
    }.onFailure {
        Log.e("runResult", "", it)
        doOnFailure?.invoke(this, it)
    }
}

fun Activity.keepScreenOn(enable: Boolean) {
    if (enable) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@MainThread
fun <I, O> FragmentActivity.registerForActivityResultOnDemand(
    contract: ActivityResultContract<I, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<I> {
    return if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        val fragment = Fragment()
        val registry = fragment.registerForActivityResult(contract) {
            callback.onActivityResult(it)
            supportFragmentManager.commit(true) {
                remove(fragment)
            }
        }
        supportFragmentManager.commitNow(true) {
            add(fragment, fragment.hashCode().toString())
        }
        registry
    } else {
        registerForActivityResult(contract, callback)
    }
}