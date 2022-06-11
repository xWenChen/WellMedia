package com.mustly.wellmedia

import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.mustly.wellmedia.base.*

class MainActivity : BaseActivity() {
    var drawerLayout: DrawerLayout? = null
    var navigationView: NavigationView? = null
    var toolbar: TitleBar? = null

    override fun onBackPressed() {
        if(drawerLayout == null) {
            super.onBackPressed()
            return
        }
        if (drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
            drawerLayout!!.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun preParseData(savedInstanceState: Bundle?) {

    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_main
    }

    override fun initView() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.titleBar)

        toolbar?.setTextColor(R.color.white.colorRes())
        toolbar?.setTextSize(17)
        toolbar?.setTitle(R.string.app_title)
        toolbar?.setOnIconClickListener {
            drawerLayout?.apply {
                // 打开则关闭，关闭则打开
                if(isDrawerOpen(GravityCompat.START)) {
                    this.closeDrawer(GravityCompat.START)
                    setTitleIcon(R.drawable.ic_unfold_menu)
                } else {
                    this.openDrawer(GravityCompat.START)
                    setTitleIcon(R.drawable.ic_fold_menu)
                }
            }
        }

        val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                setTitleIcon(R.drawable.ic_fold_menu)
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                setTitleIcon(R.drawable.ic_unfold_menu)
            }
        }
        drawerLayout?.addDrawerListener(drawerListener)

        navigationView?.setNavigationItemSelectedListener {
            switchFragment(getRouteById(it.itemId))
            drawerLayout?.closeDrawer(GravityCompat.START)
            true
        }
        navigationView?.itemIconTintList = null
    }

    override fun initData() {
        initFragment()
    }

    private fun setTitleIcon(resId: Int) {
        if(toolbar?.getIconRes() == resId) {
            return
        }
            toolbar?.setIcon(resId)
    }

    private fun getRouteById(itemId: Int): String {
        return when(itemId) {
            R.id.menu_item_video -> PageRoute.VIDEO_MAIN_FRAGMENT
            R.id.menu_item_image -> PageRoute.IMAGE_MAIN_FRAGMENT
            else -> PageRoute.AUDIO_MAIN_FRAGMENT
        }
    }

    private fun initFragment() {
        // 默认显示音频的 Fragment
        supportFragmentManager.commitTransaction {
            addFragment(getFragmentContainerId(), PageRoute.AUDIO_MAIN_FRAGMENT)
        }
    }

    /**
     * 根据 TAG 切换 Fragment
     * */
    private fun switchFragment(tag: String) {
        supportFragmentManager.commitTransaction {
            replaceFragment(getFragmentContainerId(), tag)
        }
    }

    private fun getFragmentContainerId(): Int {
        return R.id.fragmentContainerView
    }
}