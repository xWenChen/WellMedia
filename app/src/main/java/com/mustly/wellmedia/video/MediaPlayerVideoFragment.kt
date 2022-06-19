package com.mustly.wellmedia.video

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.mustly.wellmedia.R
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.databinding.FragmentMediaPlayerVideoBinding
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.runResult
import com.mustly.wellmedia.uriPath

/**
 * MediaPlayer 状态说明：
 * 当您创建新的 MediaPlayer 时，它处于“Idle”状态
 * 此时，您应该通过调用 setDataSource() 初始化该类，使其处于“Initialized”状态
 * 然后，您必须使用 prepare() 或 prepareAsync() 方法完成准备工作
 * 当 MediaPlayer 准备就绪后，它便会进入“Prepared”状态，这也意味着您可以通过调用 start() 使其播放媒体内容
 * 此时，如图所示，您可以通过调用 start()、pause() 和 seekTo() 等方法在“Started”、“Paused”和“PlaybackCompleted”状态之间切换
 * 不过请注意，当您调用 stop() 时，除非您再次准备 MediaPlayer，否则将无法再次调用 start()，需要重新走 Prepare 流程
 *
 * MediaPlayer 状态图：https://developer.android.com/images/mediaplayer_state_diagram.gif?hl=zh-cn
 * */
@Route(PageRoute.MEDIA_PLAYER_PLAY_VIDEO)
class MediaPlayerVideoFragment : BaseFragment<FragmentMediaPlayerVideoBinding>(R.layout.fragment_media_player_video) {
    companion object {
        const val TAG = "MediaPlayerVideo"
    }

    val mediaPlayer = MediaPlayer()

    override fun initView(rootView: View) {
        binding.svVideo.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )
                mediaPlayer.setDisplay(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {}

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {}
        })
    }

    override fun initData(context: Context) {
        mediaPlayer.apply {
            mediaPlayer.reset()
            setDataSource(context, Uri.parse(R.raw.tanaka_asuka.uriPath()))
            lifecycleScope.runResult(
                doOnIo = {
                    prepare()
                },
                doOnSuccess = {
                    start()
                }
            )
            /*setOnPreparedListener {

            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "视频解析异常", IllegalStateException("parse error: what=$what, extra=$extra"))
                true
            }
            setOnCompletionListener {
                // OnErrorListener 返回 false 时，会调用这个接口
            }
            prepareAsync()*/
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }
}