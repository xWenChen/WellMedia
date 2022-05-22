package com.mustly.wellmedia.video

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

class VideoMainFragment : BaseFragment(R.layout.fragment_video_main) {
    private var rv: RecyclerView? = null

    override fun initArguments(savedInstanceState: Bundle?) {

    }

    override fun initView(rootView: View) {
        rv = rootView.findViewById(R.id.rv_video_main)
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
            ItemBean(
                R.drawable.ic_video_play_1,
                R.string.video_view_play,
                FragmentConstant.Tag.TAG_VIDEO_VIEW_PLAY
            ),
            ItemBean(
                R.drawable.ic_audio_record,
                R.string.audio_record,
                FragmentConstant.Tag.TAG_AUDIO_RECORD_FRAGMENT
            )
        )
    }
}