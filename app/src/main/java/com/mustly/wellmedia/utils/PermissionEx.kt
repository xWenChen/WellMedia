package com.mustly.wellmedia.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

fun ComponentActivity.checkAndRequestPermission(permission: String, callback: ((Boolean) -> Unit)) {
    when {
        isPermissionGranted(permission) -> {
            // 已授权
            callback.invoke(true)
        }
        ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
            // 提示为什么申请权限
            // TODO 设计权限提示弹框
            requestPermission(permission, callback)
        }
        else -> {
            // 申请权限
            requestPermission(permission, callback)
        }
    }
}

fun Activity.isPermissionGranted(permission: String) = ActivityCompat.checkSelfPermission(
    this,
    permission
) == PackageManager.PERMISSION_GRANTED

fun ComponentActivity.requestPermission(permission: String, callback: ((Boolean) -> Unit)) {
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        callback.invoke(it)
    }.launch(permission)
}