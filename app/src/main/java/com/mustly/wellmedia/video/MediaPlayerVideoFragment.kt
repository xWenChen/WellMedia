package com.mustly.wellmedia.video

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentMediaPlayerVideoBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.medialib.base.PlayState
import com.mustly.wellmedia.lib.medialib.base.isPlayState
import com.mustly.wellmedia.utils.runResult
import com.mustly.wellmedia.utils.stringRes
import com.mustly.wellmedia.utils.uriPath
import kotlinx.coroutines.*

/**
 * MediaPlayer 状态说明：
 * 当您创建新的 MediaPlayer 时，它处于“Idle”状态
 * 此时，您应该通过调用 setDataSource() 初始化该类，使其处于“Initialized”状态
 * 然后，您必须使用 prepare() 或 prepareAsync() 方法完成准备工作
 * 当 MediaPlayer 准备就绪后，它便会进入“Prepared”状态，这也意味着您可以通过调用 start() 使其播放媒体内容
 * 此时，如图所示，您可以通过调用 start()、pause() 和 seekTo() 等方法在“Started”、“Paused”和“PlaybackCompleted”状态之间切换
 * 不过请注意，当您调用 stop() 时，除非您再次准备 MediaPlayer，否则将无法再次调用 start()，需要重新走 Prepare 流程
 *
 * MediaPlayer 状态图：https://developer.android.com/images/mediaplayer_state_diagram.gif?hl=zh-cn
 * */
@Route(PageRoute.MEDIA_PLAYER_PLAY_VIDEO)
class MediaPlayerVideoFragment : BaseFragment<FragmentMediaPlayerVideoBinding>(R.layout.fragment_media_player_video) {
    companion object {
        const val TAG = "MediaPlayerVideo"
    }

    private var playState = PlayState.UNINITIALIZED

    private var isSeekBarChanging = false

    private var scheduledJob: Job? = null

    val mediaPlayer = MediaPlayer()

    override fun initView(rootView: View) {
        binding.svVideo.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )
                mediaPlayer.setDisplay(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // 跳转到上次播放的位置进行播放
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // 记录当前正在播放的位置
            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {}
        })

        binding.tvCurrentTime.text = R.string.zero_time_text.stringRes
        binding.tvTimeEnd.text = R.string.zero_time_text.stringRes

        binding.sbProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvCurrentTime.text = mediaPlayer.currentPosition.formattedTime()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 通知用户已经开始一个触摸拖动手势
                isSeekBarChanging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // 通知用户触摸手势已经结束
                isSeekBarChanging = false
                mediaPlayer.seekTo(seekBar.progress)
                if (mediaPlayer.notPlay()) {
                    startPlay()
                }
                binding.tvCurrentTime.text = seekBar.progress.formattedTime()
            }
        })
        binding.btnPlay.setOnClickListener {
            startPlay()
        }
        binding.btnPause.setOnClickListener {
            if (playState.isPlayState(PlayState.PLAYING)) {
                stopPlay(true)
            }
        }
        binding.btnReset.setOnClickListener {
            if (playState.isPlayState(PlayState.ERROR)) {
                mediaPlayer.prePareAndStart()
            } else {
                stopPlay()
                startPlay()
            }
        }
    }

    override fun initData(context: Context) {
        mediaPlayer.apply {
            mediaPlayer.reset()
            // 循环播放
            mediaPlayer.isLooping = true
            setScreenOnWhilePlaying(true)
            setDataSource(context, Uri.parse(R.raw.tanaka_asuka.uriPath()))

            setOnVideoSizeChangedListener { mMediaPlayer, width, height ->
                changeViewSize(width, height)
            }

            prePareAndStart()
            /*setOnPreparedListener {

            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "视频解析异常", IllegalStateException("parse error: what=$what, extra=$extra"))
                true
            }
            setOnCompletionListener {
                // OnErrorListener 返回 false 时，会调用这个接口
            }
            prepareAsync()*/
        }
    }

    private fun changeViewSize(videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0) {
            return
        }

        binding.svVideo.post {
            val viewWidth = binding.svVideo.measuredWidth
            val viewHeight = (videoHeight.toFloat() / videoWidth * viewWidth).toInt()
            val lp = binding.svVideo.layoutParams
            lp.width = viewWidth
            lp.height = viewHeight
            binding.svVideo.layoutParams = lp
        }
    }

    private fun MediaPlayer.prePareAndStart() {
        lifecycleScope.runResult(
            doOnIo = {
                prepare()
            },
            doOnSuccess = {
                playState = PlayState.PREPARED
                realStartPlay()
            },
            doOnFailure = {
                playState = PlayState.ERROR
            }
        )
    }

    override fun onResume() {
        super.onResume()

        startPlay()
    }

    override fun onPause() {
        super.onPause()

        stopPlay(true)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }

        mediaPlayer.release()

        stopCheckTime()
    }

    private fun MediaPlayer.notPlay(): Boolean {
        return playState.isPlayState(PlayState.PREPARED) || playState.isPlayState(PlayState.PAUSED)
    }

    // 毫秒转成格式化的时间字符串
    private fun Int.formattedTime(): String {
        if (this <= 0) {
            return "00:00"
        }

        val minutes = this / 1000 / 60
        val seconds = this / 1000 % 60

        return "${checkTimeText(minutes)}:${checkTimeText(seconds)}"
    }

    private fun checkTimeText(timeNumber: Int) = if (timeNumber in 0..9) {
        "0$timeNumber"
    } else {
        "$timeNumber"
    }

    private fun startCheckTime(action: () -> Unit) {
        stopCheckTime()
        scheduledJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) {
                    action.invoke()
                }
                // 500 毫秒，刷新一次计时
                delay(500)
            }
        }
    }

    private fun startPlay() {
        if (playState == PlayState.PLAYING) {
            return
        }

        if (playState.isPlayState(PlayState.UNINITIALIZED)) {
            mediaPlayer.prePareAndStart()
            return
        }
        if (playState.isPlayState(PlayState.STOPPED)) {
            mediaPlayer.seekTo(0)
        }
        if (mediaPlayer.notPlay()) {
            realStartPlay()
        }
    }

    private fun realStartPlay() {
        binding.sbProgress.max = mediaPlayer.duration
        binding.tvTimeEnd.text = mediaPlayer.duration.formattedTime()
        mediaPlayer.start()
        startCheckTime {
            binding.tvCurrentTime.text = mediaPlayer.currentPosition.formattedTime()
            binding.sbProgress.progress = mediaPlayer.currentPosition
        }
        playState = PlayState.PLAYING
    }

    private fun stopPlay(isPaused: Boolean = false) {
        if (isPaused) {
            mediaPlayer.pause()
            playState = PlayState.PAUSED
        } else {
            mediaPlayer.stop()
            playState = PlayState.UNINITIALIZED
        }
        stopCheckTime()
    }

    private fun stopCheckTime() {
        scheduledJob?.apply {
            if (!isCancelled) {
                cancel()
            }
        }
    }
}