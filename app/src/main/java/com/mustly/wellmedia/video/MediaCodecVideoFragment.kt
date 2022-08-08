package com.mustly.wellmedia.video

import android.content.Context
import android.media.*
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentMediaCodecVideoBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.medialib.base.PlayState
import com.mustly.wellmedia.lib.medialib.base.isPlayState
import com.mustly.wellmedia.utils.HardwareDecoder
import com.mustly.wellmedia.utils.runResult
import com.mustly.wellmedia.utils.stringRes
import com.mustly.wellmedia.utils.uriPath
import kotlinx.coroutines.*

/**
 * MediaCodec 播放视频
 *
 * MediaCodec 解码音视频数据流，surface 播放视频，AudioTrack 播放音频
 *
 * MediaCodec 架构：https://developer.android.com/images/media/mediacodec_buffers.svg
 * MediaCodec 状态图：https://developer.android.com/images/media/mediacodec_states.svg
 * MediaCodec 文章介绍：https://www.jianshu.com/p/7cdf5b495ada
 * MediaCodec 解码 mp4 介绍：https://juejin.cn/post/7085658254641987615
 *
 * MediaCodec 使用流程：https://upload-images.jianshu.io/upload_images/6857764-95c08618974be63f.png
 *
 * MediaCodec 一般和MediaExtractor(音视频轨道信息提取)、MediaSync(音视频同步)、MediaMuxer(音视频混合操作)、
 * MediaCrypto(音视频加解密)、MediaDrm(音视频数字签名)、Image(获取 raw 视频图像信息)、Surface、
 * AudioTrack(音频轨道)搭配使用
 * */
@Route(PageRoute.MEDIA_CODEC_PLAY_VIDEO)
class MediaCodecVideoFragment : BaseFragment<FragmentMediaCodecVideoBinding>(R.layout.fragment_media_codec_video) {
    companion object {
        const val TAG = "MediaCodecVideoFragment"
    }

    private var playState = PlayState.UNINITIALIZED

    private var isSeekBarChanging = false

    private var scheduledJob: Job? = null

    override fun initView(rootView: View) {
        binding.svVideo.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                lifecycleScope.runResult(
                    doOnIo = {
                        HardwareDecoder(
                            true,
                            Uri.parse(R.raw.tanaka_asuka.uriPath())
                        ).start(requireContext(), holder.surface)
                    }
                )
                lifecycleScope.runResult(
                    doOnIo = {
                        HardwareDecoder(
                            false,
                            Uri.parse(R.raw.tanaka_asuka.uriPath())
                        ).start(requireContext())
                    }
                )
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
                // binding.tvCurrentTime.text = mediaPlayer.currentPosition.formattedTime()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 通知用户已经开始一个触摸拖动手势
                isSeekBarChanging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // 通知用户触摸手势已经结束
                isSeekBarChanging = false
                /*mediaPlayer.seekTo(seekBar.progress)
                if (mediaPlayer.notPlay()) {
                    startPlay()
                }*/
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
                //mediaPlayer.prePareAndStart()
            } else {
                stopPlay()
                startPlay()
            }
        }
    }

    override fun initData(context: Context) {
        /*mediaPlayer.apply {
            mediaPlayer.reset()
            // 循环播放
            mediaPlayer.isLooping = true
            setScreenOnWhilePlaying(true)
            setDataSource(context, Uri.parse(R.raw.tanaka_asuka.uriPath()))

            setOnVideoSizeChangedListener { mMediaPlayer, width, height ->
                changeViewSize(width, height)
            }

            prePareAndStart()
            *//*setOnPreparedListener {

            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "视频解析异常", IllegalStateException("parse error: what=$what, extra=$extra"))
                true
            }
            setOnCompletionListener {
                // OnErrorListener 返回 false 时，会调用这个接口
            }
            prepareAsync()*//*
        }*/
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

        /*if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }

        mediaPlayer.release()*/

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

        /*if (playState.isPlayState(PlayState.UNINITIALIZED)) {
            mediaPlayer.prePareAndStart()
            return
        }
        if (playState.isPlayState(PlayState.COMPLETED)) {
            mediaPlayer.seekTo(0)
        }
        if (mediaPlayer.notPlay()) {
            realStartPlay()
        }*/
    }

    private fun realStartPlay() {
        /*binding.sbProgress.max = mediaPlayer.duration
        binding.tvTimeEnd.text = mediaPlayer.duration.formattedTime()
        mediaPlayer.start()
        startCheckTime {
            binding.tvCurrentTime.text = mediaPlayer.currentPosition.formattedTime()
            binding.sbProgress.progress = mediaPlayer.currentPosition
        }
        playState = PlayState.PLAYING*/
    }

    private fun stopPlay(isPaused: Boolean = false) {
        /*if (isPaused) {
            mediaPlayer.pause()
            playState = PlayState.PAUSED
        } else {
            mediaPlayer.stop()
            playState = PlayState.UNINITIALIZED
        }
        stopCheckTime()*/
    }

    private fun stopCheckTime() {
        scheduledJob?.apply {
            if (!isCancelled) {
                cancel()
            }
        }
    }
}