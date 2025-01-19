package com.mustly.wellmedia.audio

import android.Manifest
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.FragmentAudioPlayBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.utils.AudioPlayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Route(PageRoute.AUDIO_PLAY_FRAGMENT)
class AudioPlayFragment : BaseBindingFragment<FragmentAudioPlayBinding>() {

    var anim: Animation? = null

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

            AudioPlayManager.apply {
                init(context)
                start(context)
            }
        }
    }

    private fun openAnim() {
        anim = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate)
        anim?.interpolator = LinearInterpolator()
        binding.ivRotate.animation = anim
        anim?.start()
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

    override fun initData(rootView: View) {

    }
}