package com.mustly.wellmedia.utils

import android.net.Uri
import android.view.Surface
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import kotlinx.coroutines.*
import kotlin.math.max

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
class PlayManager(val fileUri: Uri) {
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
                    // 用于音频、视频 PTS 同步校准
                    val startTime = System.currentTimeMillis()

                    videoDecoder?.startMs = startTime
                    audioDecoder?.startMs = startTime

                    launch { videoDecoder?.decode(activity, surface) }

                    launch { audioDecoder?.decode(activity) }
                }
                activity.keepScreenOn(false)
            } catch (e: Exception) {
                LogUtil.e(TAG, e)
                activity.keepScreenOn(false)
            }
        }
    }

    fun pause() {
        videoDecoder?.pause()
        audioDecoder?.pause()
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

    fun isStopped(): Boolean {
        return videoDecoder?.isStopped() == true
            && videoDecoder?.isStopped() == true
    }

    fun isPaused(): Boolean {
        return videoDecoder?.isPaused() == true
            && videoDecoder?.isPaused() == true
    }

    fun resume() {
        videoDecoder?.resume()
        audioDecoder?.resume()
    }

    fun isPlaying(): Boolean {
        return videoDecoder?.isPlaying() == true
            && audioDecoder?.isPlaying() == true
    }

    // 单位毫秒
    fun getDuration(): Int {
        // 文件中 音频、视频 的时长可能不等，取较长的一个时长
        return (videoDecoder?.getDuration() to audioDecoder?.getDuration())
            .takeIf {
                it.first != null && it.second != null
            }?.run {
                max(first!!.toInt(), second!!.toInt())
            } ?: 0
    }

    // 单位毫秒
    fun getCurrentTime(): Int {
        // 文件中 音频、视频 的时长可能不等，取较长的一个时长
        return (videoDecoder?.currentSampleTime to audioDecoder?.currentSampleTime)
            .takeIf {
                it.first != null && it.second != null
            }?.run {
                max(first!!.toInt(), second!!.toInt()) / 1000
            } ?: 0
    }

    fun getCurrentPosition(): Int {
        if (getDuration() == 0) {
            return 0
        }
        return ((getCurrentTime()).toFloat() / getDuration() * 100).toInt()
    }

    // 当前进度位置

    fun reset() {
        videoDecoder?.reset()
        audioDecoder?.reset()
    }
}