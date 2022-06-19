package com.mustly.wellmedia.video

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentVideoViewPlayBinding
import com.mustly.wellmedia.lib.annotation.Route

@Route(PageRoute.VIDEO_VIEW_PLAY)
class VideoViewPlayFragment : BaseFragment<FragmentVideoViewPlayBinding>(R.layout.fragment_video_view_play) {
    companion object {
        const val TAG = "VideoViewPlayFragment"
    }

    override fun initArguments(savedInstanceState: Bundle?) {

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
            setVideoURI(Uri.parse("android.resource://${requireActivity().packageName}/${R.raw.tanaka_asuka}"))
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

    override fun initData(context: Context) {

    }

}