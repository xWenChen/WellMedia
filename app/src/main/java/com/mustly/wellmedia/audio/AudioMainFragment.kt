package com.mustly.wellmedia.audio

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseAdapter
import com.mustly.wellmedia.base.BaseBindingFragment
import com.mustly.wellmedia.base.ItemBean
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentAudioMainBinding
import com.mustly.wellmedia.lib.annotation.Route

@Route(PageRoute.AUDIO_MAIN_FRAGMENT)
class AudioMainFragment : BaseBindingFragment<FragmentAudioMainBinding>() {

    override fun initView(rootView: View) {

    }

    override fun initData(rootView: View) {
        binding.rvAudioMain.layoutManager = LinearLayoutManager(activity)
        binding.rvAudioMain.adapter = BaseAdapter<ItemBean>()
            .setData(getFuncList())
            .setLayoutRes {
                R.layout.layout_rv_item
            }
            .bindView {holderRootView, _, data ->
                val iv = holderRootView.findViewById<ImageView>(R.id.iv_icon)
                val tv = holderRootView.findViewById<TextView>(R.id.tv_text)
                iv.setImageResource(data.iconResId)
                tv.setText(data.textRes)
                holderRootView.setOnClickListener {
                    startFunctionActivity(data.route)
                }
            }
            .refresh()
    }

    private fun getFuncList(): MutableList<ItemBean> {
        return mutableListOf(
            ItemBean(R.drawable.ic_audio_play, R.string.audio_play, PageRoute.AUDIO_PLAY_FRAGMENT),
            ItemBean(R.drawable.ic_audio_record, R.string.audio_record, PageRoute.AUDIO_RECORD_FRAGMENT),
            ItemBean(R.drawable.ic_audio_record, R.string.audio_record_mp3, PageRoute.MP3_RECORD_FRAGMENT),
        )
    }
}