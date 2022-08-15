package com.mustly.wellmedia.video

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseAdapter
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.ItemBean
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentVideoMainBinding
import com.mustly.wellmedia.lib.annotation.Route

@Route(PageRoute.VIDEO_MAIN_FRAGMENT)
class VideoMainFragment : BaseFragment<FragmentVideoMainBinding>() {

    override fun initView(rootView: View) {

    }

    override fun initData(context: Context) {
        binding.rvVideoMain.layoutManager = LinearLayoutManager(activity)
        binding.rvVideoMain.adapter = BaseAdapter<ItemBean>()
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
            ItemBean(
                R.drawable.ic_number_1,
                R.string.video_view_play,
                PageRoute.VIDEO_VIEW_PLAY
            ),
            ItemBean(
                R.drawable.ic_number_2,
                R.string.media_player_play_video,
                PageRoute.MEDIA_PLAYER_PLAY_VIDEO
            ),
            ItemBean(
                R.drawable.ic_number_3,
                R.string.media_codec_play_video,
                PageRoute.MEDIA_CODEC_PLAY_VIDEO
            ),
        )
    }
}