package com.mustly.wellmedia.audio

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.FragmentAudioPlayBinding
import com.mustly.wellmedia.lib.annotation.Route

@Route(PageRoute.AUDIO_PLAY_FRAGMENT)
class AudioPlayFragment : BaseBindingFragment<FragmentAudioPlayBinding>() {

    var anim: Animation? = null

    override fun initView(rootView: View) {
        // TODO 实现播放音乐时，柱状图的特效/图片转圈的特效
        // 图片转圈的特效
        anim = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate)
        anim?.interpolator = LinearInterpolator()
        binding.ivRotate.animation = anim
        anim?.start()
        // 柱状图使用 Visualizer，参考这片文章 https://juejin.cn/post/7289330618977599544
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.ivRotate.clearAnimation()
        anim?.cancel()
        anim = null
    }

    override fun initData(rootView: View) {

    }
}