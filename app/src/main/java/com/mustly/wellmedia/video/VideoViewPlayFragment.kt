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
            // 播放 res/raw 目录下的文件
            setVideoURI(Uri.parse("android.resource://${requireActivity().packageName}/${R.raw.}"))

            setMediaController(MediaController(requireActivity()))

            setOnPreparedListener {
                Log.d(TAG, " >>> Video prepared")
            }
            setOnCompletionListener {
                Log.d(TAG, " >>> Video play completed")
            }

            setOnErrorListener { mediaPlayer, what, extra ->
                Log.e(TAG, " >>> Video play error, what=$what, extra=$extra")
                // 返回 true, 表示错误被处理，不显示 VideoView 自定义的弹框
                true
            }
            // 开始播放
            start()
        }
    }

    override fun initData(context: Context) {

    }

}