package com.mustly.wellmedia.image

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.FragmentImageMainBinding

class ImageMainFragment : BaseFragment<FragmentImageMainBinding>(R.layout.fragment_image_main) {

    override fun initArguments(savedInstanceState: Bundle?) {

    }

    override fun initView(rootView: View) {
    }

    override fun initData(context: Context) {
        binding.rvImageMain.layoutManager = LinearLayoutManager(activity)
        binding.rvImageMain.adapter = BaseAdapter<ItemBean>()
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

                }
            }
            .refresh()
    }

    private fun getFuncList(): MutableList<ItemBean> {
        return mutableListOf(
            // TODO 定义选项
            ItemBean(R.drawable.ic_audio_play, R.string.audio_play, PageRoute.AUDIO_PLAY_FRAGMENT),
            ItemBean(R.drawable.ic_audio_record, R.string.audio_record, PageRoute.AUDIO_RECORD_FRAGMENT)
        )
    }
}