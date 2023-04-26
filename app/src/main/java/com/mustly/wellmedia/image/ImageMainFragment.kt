package com.mustly.wellmedia.image

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.FragmentImageMainBinding
import com.mustly.wellmedia.lib.annotation.Route

@Route(PageRoute.IMAGE_MAIN_FRAGMENT)
class ImageMainFragment : BaseFragment<FragmentImageMainBinding>() {

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
                    startFunctionActivity(data.route)
                }
            }
            .refresh()
    }

    private fun getFuncList(): MutableList<ItemBean> {
        return mutableListOf(
            // TODO 定义选项
            ItemBean(
                R.drawable.ic_number_1,
                R.string.camera2_take_photo,
                PageRoute.CAMERA2_TAKE_PHOTO
            ),
        )
    }
}