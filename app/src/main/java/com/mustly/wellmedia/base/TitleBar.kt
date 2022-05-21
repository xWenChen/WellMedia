package com.mustly.wellmedia.base

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mustly.wellmedia.R

/**
 * 自定义标题栏
 * */
class TitleBar : LinearLayout{
    lateinit var root: View
    lateinit var iconView: ImageView
    lateinit var titleView: TextView
    private var iconRes = 0
    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        initView()
    }

    private fun initView() {
        root = LayoutInflater.from(context).inflate(R.layout.layout_title_bar, this)
        iconView = rootView.findViewById(R.id.iv_icon)
        iconView.setImageResource(R.drawable.ic_unfold_menu)
        titleView = rootView.findViewById(R.id.tv_title)
    }

    fun setIcon(@DrawableRes id: Int) {
        iconRes = id
        iconView.setImageResource(id)
    }

    @DrawableRes
    fun getIconRes(): Int {
        return iconRes
    }

    fun setTextSize(spSize: Int) {
        titleView.textSize = spSize.toFloat()
    }

    fun setTextColor(color: Int) {
        titleView.setTextColor(color)
    }

    fun setTitle(@StringRes titleId: Int) {
        titleView.setText(titleId)
    }

    fun setOnIconClickListener(listener: ((View) -> Unit)) {
        iconView.setOnClickListener{
            listener.invoke(it)
        }
    }
}