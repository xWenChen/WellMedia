package com.mustly.wellmedia.audio

import android.Manifest
import android.media.MediaPlayer
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.FragmentAudioPlayBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.utils.AudioPlayManager
import com.mustly.wellmedia.utils.stringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 音频播放效果展示：
 * 1、图片转圈功能。
 * 2、fft(快速傅里叶变换)振幅效果展示。
 * 3、音频播放与进度控制。
 * */
@Route(PageRoute.AUDIO_PLAY_FRAGMENT)
class AudioPlayFragment : BaseBindingFragment<FragmentAudioPlayBinding>() {

    var anim: Animation? = null

    private var isSeekBarChanging = false
    private var scheduledJob: Job? = null

    override fun initView(rootView: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.fftSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                LogUtil.d(TAG, "switch check, now state is $isChecked.")
                if (isChecked) {
                    // 柱状图使用 Visualizer
                    tryOpenVisualizer()
                } else {
                    AudioPlayManager.enableVisualizer(false)
                    binding.audioVisualizer.visibility = View.GONE
                }
            }

            // 图片转圈的特效
            openAnim()

            binding.tvCurrentTime.text = R.string.zero_time_text.stringRes
            binding.tvTimeEnd.text = R.string.zero_time_text.stringRes

            AudioPlayManager.init(context)
            val mediaPlayer = AudioPlayManager.mediaPlayer

            binding.sbProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    binding.tvCurrentTime.text = mediaPlayer?.currentPosition?.formattedTime()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // 通知用户已经开始一个触摸拖动手势
                    isSeekBarChanging = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // 通知用户触摸手势已经结束
                    isSeekBarChanging = false
                    mediaPlayer?.seekTo(seekBar.progress)
                    if (mediaPlayer?.notPlay() == true) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            startPlay()
                        }
                    }
                    binding.tvCurrentTime.text = seekBar.progress.formattedTime()
                }
            })
            binding.btnPlay.setOnClickListener {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (mediaPlayer?.isPlaying != true) {
                        startPlay()
                        binding.btnPlay.text = "暂停"
                    } else {
                        stopPlay()
                        binding.btnPlay.text = "播放"
                    }
                }
            }

            AudioPlayManager.apply {
                startPlay()
                startCheckTime {
                    binding.tvCurrentTime.text = mediaPlayer?.currentPosition?.formattedTime()
                    binding.sbProgress.progress = mediaPlayer?.currentPosition ?: 0
                }
            }
        }
    }

    private fun openAnim() {
        anim = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate)
        anim?.interpolator = LinearInterpolator()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.ivRotate.clearAnimation()
        anim?.cancel()
        anim = null
        AudioPlayManager.apply {
            stop()
            release()
        }
        stopCheckTime()
    }

    private fun tryOpenVisualizer() = lifecycleScope.launch(Dispatchers.Main) {
        // 1. 检查麦克风权限
        val isGranted = suspendRequestPermission(
            Manifest.permission.RECORD_AUDIO,
            "请求麦克风权限",
            "我们需要使用麦克风以解析音频数据"
        )
        if (!isGranted) {
            LogUtil.w(TAG, "openVisualizer， 没有权限，无法打开。")
            binding.fftSwitch.isChecked = false
            return@launch
        }
        AudioPlayManager.apply {
            binding.audioVisualizer.visibility = View.VISIBLE
            initVisualizer(binding.audioVisualizer)
        }
    }

    private fun MediaPlayer.notPlay(): Boolean {
        return !isPlaying
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

    suspend fun startPlay() {
        binding.sbProgress.max = AudioPlayManager.mediaPlayer?.duration ?: 0
        binding.tvTimeEnd.text = AudioPlayManager.mediaPlayer?.duration?.formattedTime()
        AudioPlayManager.start(this@AudioPlayFragment.context)
        binding.ivRotate.startAnimation(anim)
        startCheckTime {
            binding.tvCurrentTime.text = AudioPlayManager.mediaPlayer?.currentPosition?.formattedTime()
            binding.sbProgress.progress = AudioPlayManager.mediaPlayer?.currentPosition ?: 0
        }
    }

    private fun stopPlay() {
        AudioPlayManager.pause()
        binding.ivRotate.clearAnimation()
        stopCheckTime()
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
    private fun stopCheckTime() {
        scheduledJob?.apply {
            if (!isCancelled) {
                cancel()
            }
        }
    }

    override fun initData(rootView: View) {

    }
}