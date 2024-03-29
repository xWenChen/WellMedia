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
 * date：    2022/8/9 16:19
 * version   1.0
 * modify by
 */
class PlayManager(val fileUri: Uri) {
    companion object {
        const val TAG = "PlayManager"
    }

    private var videoDecoder: HardwareDecoder? = null

    private var audioDecoder: HardwareDecoder? = null

    private var job: Job? = null

    private var activity: FragmentActivity? = null
    private var surface: Surface? = null

    init {
        videoDecoder = HardwareDecoder(true, fileUri)
        audioDecoder = HardwareDecoder(false, fileUri)
    }

    fun start(
        activity: FragmentActivity?,
        surface: Surface? = null,
    ) {
        if (activity == null) {
            LogUtil.e(TAG, "start decode fail, activity == null")
            return
        }
        // 取消上个解码任务
        cancelLastJob()
        this.activity = activity
        this.surface = surface
        job = activity.lifecycleScope.launch(Dispatchers.Main) {
            try {
                activity.keepScreenOn(true)

                withContext(Dispatchers.IO) {
                    syncStartTime()
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

    private fun syncStartTime() {
        // 同步时间，用于音频、视频 PTS 同步校准
        val startTime = System.currentTimeMillis()

        videoDecoder?.startMs = startTime
        audioDecoder?.startMs = startTime
    }

    // 跳到指定位置，单位 毫秒
    fun seekTo(process: Int) {
        // 停止现在的播放
        val time = (getDuration().toDouble() * process / 100).toLong()
        videoDecoder?.seekTo(time)
        audioDecoder?.seekTo(time)
    }

    fun pause() {
        videoDecoder?.pause()
        audioDecoder?.pause()
    }

    fun destroy() {
        cancelLastJob()
        videoDecoder?.release()
        videoDecoder = null
        audioDecoder?.release()
        audioDecoder = null

        activity = null
        surface = null
    }

    private fun cancelLastJob() {
        if (job?.isActive == true) {
            job?.cancel()
            job = null
        }
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
        // 文件中 音频、视频 的时长可能不等，取平均数
        return (videoDecoder?.currentSampleTime to audioDecoder?.currentSampleTime)
            .takeIf {
                it.first != null && it.second != null
            }?.run {
                // 2000 = 2 * 1000
                max(first!!.toInt(), second!!.toInt())
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