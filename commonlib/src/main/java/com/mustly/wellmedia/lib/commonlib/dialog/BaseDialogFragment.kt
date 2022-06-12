package com.mustly.wellmedia.lib.commonlib.dialog

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.LayoutRes
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

abstract class BaseDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "BaseDialogFragment"
    }
    /**
     * 是否现实系统自带的标题栏
     * */
    var hideSystemTitle = true

    /**
     * dialog 在屏幕中的位置，取值请参考: [Gravity]
     * */
    var gravity = Gravity.CENTER

    /**
     * dialog 的宽度
     * */
    var width = WindowManager.LayoutParams.MATCH_PARENT
    /**
     * dialog 的高度
     * */
    var height = WindowManager.LayoutParams.WRAP_CONTENT
    /**
     * 点击 dialog 外部，dialog 是否消失的标志
     * */
    var touchOutsideDismiss = false

    @LayoutRes
    abstract fun getLayoutResId(): Int

    abstract fun bindView(rootView: View)

    abstract fun getDialogTag(): String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        config()

        val view = inflater.inflate(getLayoutResId(), container)

        bindView(view)

        return view
    }

    fun show(manager: FragmentManager) {
        show(manager, getDialogTag())
    }

    private fun config() {
        if (dialog == null) {
            Log.e(TAG, "can not config the dialog, dialog is null")
            return
        }
        if (hideSystemTitle) {
            dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

        val mWindow = dialog!!.window

        if (mWindow == null) {
            Log.e(TAG, "can not config the dialog, the window of dialog is null")
            return
        }

        // 设置背景透明，必要步骤
        mWindow.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent)

        //必要，设置 padding，这一步也是必须的，内容不能填充全部宽度和高度
        mWindow.decorView.setPadding(0)

        mWindow.attributes?.also { windowLayoutParams ->
            windowLayoutParams.gravity = gravity
            windowLayoutParams.width = width
            windowLayoutParams.height = height

            mWindow.attributes = windowLayoutParams
        }

        if (touchOutsideDismiss) {
            mWindow.decorView.setOnTouchListener { view, event ->
                view.performClick()

                GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent?): Boolean {
                        if (isShowing()) {
                            dismiss()
                        }
                        return true
                    }
                }).onTouchEvent(event)
            }
        }
    }
}

fun DialogFragment?.isShowing(): Boolean = let { df ->
    return df?.dialog?.isShowing ?: false
}