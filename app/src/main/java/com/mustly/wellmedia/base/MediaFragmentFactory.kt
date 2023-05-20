package com.mustly.wellmedia.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.mustly.wellmedia.audio.AudioMainFragment
import com.mustly.wellmedia.image.ImageMainFragment
import com.mustly.wellmedia.lib.commonlib.route.RouteHub
import com.mustly.wellmedia.video.VideoMainFragment

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
        Class.forName(RouteHub.routeTable[this]) as Class<out Fragment>
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}