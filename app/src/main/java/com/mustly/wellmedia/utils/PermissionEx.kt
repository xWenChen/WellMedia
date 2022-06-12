package com.mustly.wellmedia.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.mustly.wellmedia.lib.commonlib.dialog.ConfirmDialog

fun FragmentActivity.checkAndRequestPermission(
    permission: String,
    title: String,
    desc: String,
    callback: (Boolean) -> Unit
) {
    when {
        isPermissionGranted(permission) -> {
            // 已授权
            callback.invoke(true)
        }
        ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
            // 提示为什么申请权限
            val realTitle = title.ifBlank { "权限申请说明" }
            val realDesc = desc.ifBlank { "为了确保功能的正常使用，需要申请权限" }
            ConfirmDialog(
                realTitle,
                realDesc,
                cancelCallback = { callback.invoke(false) },
                confirmCallback = { requestPermission(permission, callback) }
            ).show(supportFragmentManager)
        }
        else -> {
            // 申请权限
            requestPermission(permission, callback)
        }
    }
}

fun Activity.isPermissionGranted(permission: String) =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun ComponentActivity.requestPermission(permission: String, callback: ((Boolean) -> Unit)) {
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        callback.invoke(it)
    }.launch(permission)
}