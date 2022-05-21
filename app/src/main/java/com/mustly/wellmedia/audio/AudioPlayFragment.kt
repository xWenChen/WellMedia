package com.mustly.wellmedia.audio

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseAdapter
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.FragmentConstant
import com.mustly.wellmedia.base.ItemBean

class AudioPlayFragment : BaseFragment(R.layout.fragment_audio_play) {
    private var rv: RecyclerView? = null

    override fun initArguments(savedInstanceState: Bundle?) {
        // TODO
    }

    override fun initView(rootView: View) {
        // TODO 实现播放音乐时，柱状图的特效/图片转圈的特效
    }

    override fun initData(context: Context) {
        rv?.layoutManager = LinearLayoutManager(activity)
        rv?.adapter = BaseAdapter<ItemBean>()
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
                    startFunctionActivity(data.tag)
                }
            }
            .refresh()
    }

    private fun getFuncList(): MutableList<ItemBean> {
        return mutableListOf(
            ItemBean(R.drawable.ic_audio_play, R.string.audio_play, FragmentConstant.Tag.TAG_AUDIO_PLAY_FRAGMENT),
            ItemBean(R.drawable.ic_audio_record, R.string.audio_record, FragmentConstant.Tag.TAG_AUDIO_RECORD_FRAGMENT)
        )
    }
}