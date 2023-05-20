package com.mustly.wellmedia.video

import android.content.Context
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseBindingFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentMediaCodecVideoBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.medialib.base.PlayState
import com.mustly.wellmedia.lib.medialib.base.isPlayState
import com.mustly.wellmedia.utils.*
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
class MediaCodecVideoFragment : BaseBindingFragment<FragmentMediaCodecVideoBinding>() {
    companion object {
        const val TAG = "MediaCodecVideoFragment"
    }

    private var playState = PlayState.UNINITIALIZED

    private var scheduledJob: Job? = null

    private var player: PlayManager? = null

    private var surface: Surface? = null

    override fun initView(rootView: View) {
        player = PlayManager(Uri.parse(R.raw.tanaka_asuka.uriPath()))

        binding.svVideo.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surface = holder.surface
                startPlay()
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
                // binding.tvCurrentTime.text = player?.getCurrentPosition().formattedTime()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 通知用户已经开始一个触摸拖动手势
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // 通知用户触摸手势已经结束
                player?.seekTo(seekBar.progress)
            }
        })
        binding.btnPlay.setOnClickListener {
            startPlay()
        }
        binding.btnPause.setOnClickListener {
            if (playState.isPlayState(PlayState.PLAYING)) {
                pausePlay()
            }
        }
        binding.btnReset.setOnClickListener {
            player?.reset()
            binding.tvTimeEnd.text = R.string.zero_time_text.stringRes
        }
    }

    override fun initData(rootView: View) {}

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

    override fun onPause() {
        super.onPause()

        pausePlay()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopCheckTime()

        player?.destroy()
        player = null
    }

    // 毫秒转成格式化的时间字符串
    private fun Int?.formattedTime(): String {
        if (this == null || this <= 0) {
            return ""
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

    // 检查进度条时间
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
        if (player?.isPlaying() == true) {
            return
        }
        if (player?.isPaused() == true) {
            player?.resume()
        } else {
            // 不是暂停，重新开始播放
            player?.start(activity, surface)
        }

        setControlInfo()
    }

    private fun setControlInfo() {
        startCheckTime {
            binding.tvTimeEnd.apply {
                if (text == R.string.zero_time_text.stringRes || text.isNullOrBlank()) {
                    text = player?.getDuration().formattedTime()
                }
            }
            binding.tvCurrentTime.text = player?.getCurrentTime().formattedTime()
            binding.sbProgress.progress = player?.getCurrentPosition() ?: 0
        }
        playState = PlayState.PLAYING
    }

    private fun pausePlay() {
        player?.pause()
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