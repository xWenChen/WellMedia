package com.mustly.wellmedia.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.view.Surface
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.medialib.base.PlayState
import com.mustly.wellmedia.lib.medialib.base.bean.HardwareMediaInfo
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
class PlayManager(val fileUri: Uri) {
    companion object {
        const val TAG = "DecodeManager"
    }

    private var videoDecoder: HardwareDecoder? = null

    private var audioDecoder: HardwareDecoder? = null
    private var audioTrack: AudioTrack? = null
    private var audioPlayState = PlayState.UNINITIALIZED

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

                    val videoDefer = async {
                        videoDecoder?.decode(activity) {
                            surface
                        }
                    }

                    val audioDefer = async {
                        audioDecoder?.decode(activity) {
                            audioTrack = createAndConfigAudioPlayer(audioDecoder!!.mediaInfo)
                            audioTrack
                        }
                    }

                    videoDefer.await() to audioDefer.await()
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

    private fun createAndConfigAudioPlayer(mediaInfo: HardwareMediaInfo?): AudioTrack {
        // 3. 创建音频播放器
        // 初始化 AudioTrack
        val minBufferSize = AudioTrack.getMinBufferSize(
            mediaInfo!!.sampleRate,
            mediaInfo.voiceTrack,
            mediaInfo.sampleDepth
        )
        // 说明 https://stackoverflow.com/questions/50866991/android-audiotrack-playback-fast
        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(mediaInfo.sampleRate)
                .setChannelMask(mediaInfo.voiceTrack)
                .setEncoding(mediaInfo.sampleDepth)
                .build(),
            minBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack.play()
        updatePlayState(audioTrack)

        return audioTrack
    }

    /**
     * AudioTrack 状态说明：
     * 名词解释：
     *   ● AP：Application
     *   ● ADSP：Application DSP
     *   ● DSP：Digital Signal Processing：数字信号处理单元，通常是硬件，并且通常是芯片的一个组成部分，其他的单元如射频单元等等。
     *
     * 四种播放模式(https://www.cnblogs.com/wulizhi/p/8183658.html)：
     *   ● Deep buffer Playback
     *   ● Low latency Playback
     *   ● Offload playback
     *   ● Mutichannel Playback
     * 两种状态：
     *   ● state：状态
     *       ● STATE_UNINITIALIZED(默认状态)：AudioTrack 创建时没有成功地初始化
     *       ● STATE_INITIALIZED：AudioTrack 已经是可以使用了
     *       ● STATE_NO_STATIC_DATA：表示当前是使用 MODE_STATIC ，但是还没往缓冲区中写入数据。当接收数据之后会变为 STATE_INITIALIZED 状态
     *   ● playstate：播放状态
     *       ● PLAYSTATE_STOPPED：停止
     *       ● PLAYSTATE_PAUSED：暂停
     *       ● PLAYSTATE_PLAYING：播放中
     *       ● PLAYSTATE_STOPPING(私有状态，外部获取不到)：停止中，等价于：PLAYSTATE_PLAYING
     *       ● PLAYSTATE_PAUSED_STOPPING(私有状态，外部获取不到)：已暂停，停止中：等价于：PLAYSTATE_PAUSED
     * 两种数据获取模式：
     *   ● stream mode：不停写入音频数据
     *   ● static mode: 一次写入所有音频数据
     * 关键方法：
     *   ● AudioTrack 构造函数
     *      ● static mode：state = STATE_NO_STATIC_DATA
     *      ● stream mode：state = STATE_INITIALIZED
     *   ● play()
     *      ● mState != STATE_INITIALIZED：抛异常
     *      ● PLAYSTATE_PAUSED_STOPPING：mPlayState = PLAYSTATE_STOPPING
     *      ● 正常流程：mPlayState = PLAYSTATE_PLAYING(播放中)
     *   ● pause()
     *      ● mState != STATE_INITIALIZED：抛异常
     *      ● PLAYSTATE_STOPPING：mPlayState = PLAYSTATE_PAUSED_STOPPING
     *      ● 正常流程：mPlayState = PLAYSTATE_PAUSED(已暂停)
     *   ● stop()
     *      ● mState != STATE_INITIALIZED：抛异常
     *      ● mOffloaded && mPlayState != PLAYSTATE_PAUSED_STOPPING：mPlayState = PLAYSTATE_STOPPING
     *      ● 正常流程：mPlayState = PLAYSTATE_STOPPED(已停止)
     *   ● flush()
     *      ● mState != STATE_INITIALIZED：不做任何操作
     *      ● static mode: 不操作
     *      ● 暂停态：不操作
     *      ● 停止态：不操作
     *      ● stream mode 并且 playing 状态下，清除已入队但未播放的数据
     *   ● release()
     *      ● 先 stop()
     *      ● mState = STATE_UNINITIALIZED; mPlayState = PLAYSTATE_STOPPED;
     * */
    private fun updatePlayState(audioTrack: AudioTrack) {
        // 需要结合 state 和 PlayState 一起确定
        audioPlayState = when (audioTrack.state) {
            AudioTrack.STATE_UNINITIALIZED -> PlayState.UNINITIALIZED
            AudioTrack.STATE_INITIALIZED -> updateInitializedAudioState(audioTrack)
            AudioTrack.STATE_NO_STATIC_DATA -> PlayState.PREPARED
            else -> PlayState.ERROR
        }
    }

    private fun updateInitializedAudioState(audioTrack: AudioTrack) = when (audioTrack.playState) {
        AudioTrack.PLAYSTATE_PLAYING -> PlayState.PLAYING
        AudioTrack.PLAYSTATE_PAUSED -> PlayState.PAUSED
        AudioTrack.PLAYSTATE_STOPPED -> PlayState.PAUSED
        else -> PlayState.ERROR
    }
}