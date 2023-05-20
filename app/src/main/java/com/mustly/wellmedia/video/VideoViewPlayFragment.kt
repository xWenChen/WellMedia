package com.mustly.wellmedia.video

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.MediaController
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseBindingFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentVideoViewPlayBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.utils.uriPath

/**
 * VideoView 提供的功能太过简单
 * 默认播放时不会保持屏幕常亮；
 * 图像默认位于右上角；
 * 熄屏后有内存泄漏问题；
 * 熄屏后回来，VideoView会被回收，导致黑屏；
 * MediaController 提供的功能也很简单，且位于屏幕底部；
 *
 * TODO 建议不使用
 * */
@Route(PageRoute.VIDEO_VIEW_PLAY)
class VideoViewPlayFragment : BaseBindingFragment<FragmentVideoViewPlayBinding>() {
    companion object {
        const val TAG = "VideoViewPlayFragment"
    }

    override fun initView(rootView: View) {
        binding.videoView.apply {
            /**
             * TODO 解决熄屏后 VideoView 被回收的问题
             * TODO 解决视频未居中的问题
             * TODO 解决空白背景的问题(添加模糊图像)
             * */
            // 播放 res/raw 目录下的文件
            // 田中明日香的视频
            setVideoURI(Uri.parse(R.raw.tanaka_asuka.uriPath()))
            // MediaController 自定进度条，快进，暂停等功能
            setMediaController(MediaController(requireActivity()))
            setOnPreparedListener {
                Log.d(TAG, " >>> Video prepared")
                keepScreenOn(true)
            }
            setOnCompletionListener {
                Log.d(TAG, " >>> Video play completed")
                keepScreenOn(false)
            }

            setOnErrorListener { mediaPlayer, what, extra ->
                Log.e(TAG, " >>> Video play error, what=$what, extra=$extra")
                // 返回 true, 表示错误被处理，不显示 VideoView 自定义的弹框
                keepScreenOn(false)
                true
            }
            // 开始播放
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        keepScreenOn(false)
    }

    override fun initData(rootView: View) {

    }

}