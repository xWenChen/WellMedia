package com.mustly.wellmedia.utils

import android.net.Uri
import android.view.Surface
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import kotlinx.coroutines.*

/**
 * description:
 *
 * 统一管理视频解码和音频解码过程
 *
 * @author   wchenzhang
 * date：    2022/8/9 16:19
 * version   1.0
 * modify by
 */
class DecodeManager(val fileUri: Uri) {
    companion object {
        const val TAG = "DecodeManager"
    }

    private var videoDecoder: HardwareDecoder? = null
    private var audioDecoder: HardwareDecoder? = null

    private var job: Job? = null

    init {
        init()
    }

    private fun init() {
        videoDecoder = HardwareDecoder(true, fileUri)
        audioDecoder = HardwareDecoder(false, fileUri)
    }

    fun start(
        activity: FragmentActivity?,
        surface: Surface? = null
    ) {
        if (activity == null) {
            LogUtil.e(TAG, "start decode fail, activity == null")
            return
        }
        job = activity.lifecycleScope.launch(Dispatchers.Main) {
            try {
                activity.keepScreenOn(true)
                withContext(Dispatchers.IO) {
                    // 勇于音频、视频 PTS 同步校准
                    val startTime = System.currentTimeMillis()

                    videoDecoder?.startMs = startTime
                    audioDecoder?.startMs = startTime

                    launch {  videoDecoder?.decode(activity, surface) }
                    launch { audioDecoder?.decode(activity) }
                }
                activity.keepScreenOn(false)
            } catch (e: Exception) {
                LogUtil.e(TAG, e)
                activity.keepScreenOn(false)
            }
        }
    }

    fun stop() {
        if (job?.isActive == true) {
            job?.cancel()
            job = null
        }
        videoDecoder?.release()
        videoDecoder = null
        audioDecoder?.release()
        audioDecoder = null
    }
}