package com.mustly.wellmedia.audio

import android.content.Context
import android.os.Bundle
import android.view.View
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.FragmentAudioPlayBinding
import com.mustly.wellmedia.lib.annotation.Route

@Route(PageRoute.AUDIO_PLAY_FRAGMENT)
class AudioPlayFragment : BaseFragment<FragmentAudioPlayBinding>(R.layout.fragment_audio_play) {

    override fun initView(rootView: View) {
        // TODO 实现播放音乐时，柱状图的特效/图片转圈的特效
    }

    override fun initData(context: Context) {

    }
}