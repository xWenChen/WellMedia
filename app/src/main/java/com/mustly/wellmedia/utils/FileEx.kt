package com.mustly.wellmedia.utils

import android.os.Environment
import android.util.SparseArray
import com.mustly.wellmedia.MediaApplication
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

annotation class DirType {
    companion object {
        /**
         * root
         * */
        const val DEFAULT = 0
        /**
         * 视频
         * */
        const val VIDEO = 1
        /**
         * 音频
         * */
        const val AUDIO = 2
        /**
         * 图像
         * */
        const val IMAGE = 3

        val fileName = SparseArray<String>().apply {
            this.put(DEFAULT, "")
            this.put(VIDEO, "video")
            this.put(AUDIO, "audio")
            this.put(IMAGE, "image")
        }

        fun getDirName(@DirType type: Int) = fileName.get(type)
    }
}

fun String.getInnerPath(@DirType type: Int = DirType.DEFAULT) = MediaApplication.getAppContext().filesDir?.let {
    val rootDir = it.absolutePath.checkDirSeparator()
    val dir = DirType.getDirName(type)
    if (dir.isNullOrBlank()) {
        rootDir
    } else {
        rootDir + dir + File.separator + this
    }
}

fun String.getExternalVideoPath(@DirType type: Int = DirType.DEFAULT) = MediaApplication.getAppContext().let { context ->
    if (!isExternalStorageWritable()) {
        return@let ""
    }

    // 根据 WinNTFileSystem.resolve 的实现，子路径为空，则会返回父目录
    val dir = DirType.getDirName(type)
    context.getExternalFilesDir(dir)?.absolutePath?.checkDirSeparator() + this
}

// 判断外部存储权限是否可写入
fun isExternalStorageWritable() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

// 判断外部存储是否可读
fun isExternalStorageReadable() = Environment.getExternalStorageState() in setOf(
    Environment.MEDIA_MOUNTED,
    Environment.MEDIA_MOUNTED_READ_ONLY
)

fun String.checkDirSeparator() = if (endsWith(File.separator)) {
    this
} else {
    this + File.separator
}

// 将文件分批次读入内存，计算 md5。避免内存溢出
fun String.getFileMd5(): String? {
    var bi: BigInteger? = null
    val buffer = ByteArray(8192)
    var len = 0
    try {
        val md = MessageDigest.getInstance("MD5")
        FileInputStream(File(this)).use { fis ->
            while (fis.read(buffer).also { len = it } != -1) {
                md.update(buffer, 0, len)
            }
        }
        val b = md.digest()
        bi = BigInteger(1, b)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return bi?.toString(16)
}