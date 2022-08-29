package com.mustly.wellmedia.lib.commonlib.dialog

import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.mustly.wellmedia.lib.commonlib.R

class ConfirmDialog() : BaseDialogFragment() {
    companion object {
        const val TAG = "ConfirmDialog"
    }

    private lateinit var tvTitle: TextView
    private lateinit var divider1: View
    private lateinit var tvDesc: TextView
    private lateinit var cancel: TextView
    private lateinit var confirm: TextView

    private var title = ""
    private var showTitle = true
    private var desc = ""
    private var cancelText = "取消"
    private var confirmText = "确定"
    private var cancelCallback: (() -> Unit)? = null
    private var confirmCallback: (() -> Unit)? = null

    constructor(
        title: String,
        desc: String,
        cancelText: String = "取消",
        confirmText: String = "确定",
        cancelCallback: (() -> Unit)? = null,
        confirmCallback: (() -> Unit)? = null
    ): this() {
        this.title = title
        this.desc = desc
        this.cancelText = cancelText
        this.confirmText = confirmText
        if (cancelCallback != null) {
            this.cancelCallback = cancelCallback
        }
        this.confirmCallback = confirmCallback
    }

    override fun getDialogTag(): String {
        return TAG
    }

    override fun getLayoutResId(): Int {
        return R.layout.dialog_confirm
    }

    override fun bindView(rootView: View) {
        findView(rootView)

        showTitle = title.isNullOrBlank()

        tvTitle.text = title
        val visibility = if (showTitle) {
            View.VISIBLE
        } else {
            View.GONE
        }

        tvTitle.visibility = visibility
        divider1.visibility = visibility

        tvDesc.text = desc

        cancel.text = cancelText
        cancel.setOnClickListener {
            cancelCallback?.invoke()
            if (isShowing()) {
                dismiss()
            }
        }
        confirm.text = confirmText
        confirm.setOnClickListener {
            confirmCallback?.invoke()
            if (isShowing()) {
                dismiss()
            }
        }
    }

    private fun findView(rootView: View) {
        tvTitle = rootView.findViewById(R.id.tvTitle)
        divider1 = rootView.findViewById(R.id.divider1)
        tvDesc = rootView.findViewById(R.id.tvDesc)
        cancel = rootView.findViewById(R.id.cancel)
        confirm = rootView.findViewById(R.id.confirm)
    }
}