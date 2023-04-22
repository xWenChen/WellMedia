package com.mustly.wellmedia.utils

import android.app.Activity
import android.content.pm.PackageManager
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
            showHintDialog(title, desc) { clickConfirm ->
                if (clickConfirm) {
                    requestPermission(permission, callback)
                } else {
                    callback.invoke(false)
                }
            }
        }
        else -> {
            // 申请权限
            requestPermission(permission, callback)
        }
    }
}

fun FragmentActivity.checkAndRequestPermissions(
    permissions: Array<String>,
    title: String,
    desc: String,
    callback: (Map<String, Boolean>) -> Unit
) {
    val result = permissions.associateWith { false }.toMutableMap()

    permissions.forEach { if (isPermissionGranted(it)) { result[it] = true } }

    // 筛选出未授权的权限
    result.filterValues { !it }.takeIf { it.any() }?.also { map ->
        // 提示为什么申请权限，该动作涉及到异步过程
        showHintDialog(title, desc) { clickConfirm ->
            if (clickConfirm) {
                requestPermissions(map.keys.toTypedArray()) {
                    result.putAll(it)
                    callback.invoke(result)
                }
            } else {
                // 点取消直接返回结果
                callback.invoke(result)
            }
        }
    } ?: callback.invoke(result) // 所有权限都已授权
}

private fun FragmentActivity.showHintDialog(
    title: String,
    desc: String,
    callback: (Boolean) -> Unit,
) {
    // 提示为什么申请权限
    val realTitle = title.ifBlank { "权限申请说明" }
    val realDesc = desc.ifBlank { "为了确保功能的正常使用，需要申请权限" }
    ConfirmDialog(
        realTitle,
        realDesc,
        cancelCallback = { callback.invoke(false) },
        confirmCallback = { callback.invoke(true) }
    ).show(supportFragmentManager)
}

fun Activity.isPermissionGranted(permission: String) =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Activity.allPermissionsGranted(permissions: Array<String>) = permissions.all { isPermissionGranted(it) }

fun FragmentActivity.requestPermission(permission: String, callback: ((Boolean) -> Unit)) {
    registerForActivityResultOnDemand(ActivityResultContracts.RequestPermission()) {
        callback.invoke(it)
    }.launch(permission)
}

fun FragmentActivity.requestPermissions(permission: Array<String>, callback: ((Map<String, Boolean>) -> Unit)) {
    registerForActivityResultOnDemand(ActivityResultContracts.RequestMultiplePermissions()) {
        callback.invoke(it)
    }.launch(permission)
}