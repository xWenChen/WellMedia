package com.mustly.wellmedia.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.mustly.wellmedia.audio.AudioMainFragment
import com.mustly.wellmedia.audio.AudioPlayFragment
import com.mustly.wellmedia.image.ImageMainFragment
import com.mustly.wellmedia.video.VideoMainFragment
import java.lang.IllegalArgumentException

/**
 * 参考链接：https://blog.csdn.net/vitaviva/article/details/111774009
 * */
object MediaFragmentFactory : FragmentFactory() {
    /**
     * 项目中已定义的 Fragment 的集合，所有 Fragment 必须在此处注册
     * */
    private var usefulFragments = HashMap<String, Class<out BaseFragment>>().apply {
        this[FragmentConstant.Tag.TAG_AUDIO_MAIN_FRAGMENT] = AudioMainFragment::class.java
        this[FragmentConstant.Tag.TAG_AUDIO_PLAY_FRAGMENT] = AudioPlayFragment::class.java

        this[FragmentConstant.Tag.TAG_VIDEO_MAIN_FRAGMENT] = VideoMainFragment::class.java
        this[FragmentConstant.Tag.TAG_IMAGE_MAIN_FRAGMENT] = ImageMainFragment::class.java
    }

    fun findClassByTag(tag: String): Class<out BaseFragment> {
        return if(usefulFragments.containsKey(tag)) {
            usefulFragments[tag]!!
        } else {
            throw IllegalArgumentException("Fragment not registered, tag = $tag")
        }
    }

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

object FragmentConstant {
    annotation class Tag {
        companion object {
            const val TAG_AUDIO_MAIN_FRAGMENT = "tag_audio_main_fragment"
            const val TAG_AUDIO_PLAY_FRAGMENT = "tag_audio_play_fragment"
            const val TAG_AUDIO_RECORD_FRAGMENT = "tag_audio_record_fragment"

            const val TAG_VIDEO_MAIN_FRAGMENT = "tag_video_main_fragment"
            const val TAG_IMAGE_MAIN_FRAGMENT = "tag_image_main_fragment"
        }
    }

    annotation class Key {
        companion object {
            const val KEY_FRAGMENT_TAG = "key_fragment_tag"
        }
    }
}

fun getFragmentClassByTag(tag: String): Class<out BaseFragment> {
    return MediaFragmentFactory.findClassByTag(tag)
}