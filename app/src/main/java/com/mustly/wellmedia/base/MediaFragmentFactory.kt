package com.mustly.wellmedia.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.mustly.wellmedia.audio.AudioMainFragment
import com.mustly.wellmedia.audio.AudioPlayFragment
import com.mustly.wellmedia.image.ImageMainFragment
import com.mustly.wellmedia.video.VideoMainFragment
import com.mustly.wellmedia.video.VideoViewPlayFragment
import java.lang.IllegalArgumentException

/**
 * 参考链接：https://blog.csdn.net/vitaviva/article/details/111774009
 * */
object MediaFragmentFactory : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        var clazz = loadFragmentClass(classLoader, className)
        return when(clazz) {
            AudioMainFragment::class.java -> AudioMainFragment()
            VideoMainFragment::class.java -> VideoMainFragment()
            ImageMainFragment::class.java -> ImageMainFragment()
            else -> super.instantiate(classLoader, className)
        }
    }
}

fun String.getFragmentClass(): Class<out Fragment>? {
    return try {
        Class.forName(this) as Class<out Fragment>
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

object PageRoute {
    const val AUDIO_MAIN_FRAGMENT = "audio_main_fragment"
    const val AUDIO_PLAY_FRAGMENT = "audio_play_fragment"
    const val AUDIO_RECORD_FRAGMENT = "audio_record_fragment"

    const val VIDEO_MAIN_FRAGMENT = "video_main_fragment"
    // VideoView 播放视频
    const val VIDEO_VIEW_PLAY = "video_view_play"

    const val IMAGE_MAIN_FRAGMENT = "image_main_fragment"

    object Param {
        const val KEY_FRAGMENT_TAG = "key_fragment_tag"
    }
}