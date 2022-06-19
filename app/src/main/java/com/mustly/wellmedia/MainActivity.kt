package com.mustly.wellmedia

import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.mustly.wellmedia.base.*
import com.mustly.wellmedia.databinding.ActivityMainBinding
import com.mustly.wellmedia.utils.addFragment
import com.mustly.wellmedia.utils.commitTransaction
import com.mustly.wellmedia.utils.replaceFragment

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun preParseData() {

    }

    override fun initView() {
        binding.titleBar.setTextColor(R.color.white.colorRes())
        binding.titleBar.setTextSize(17)
        binding.titleBar.setTitle(R.string.app_title)
        binding.titleBar.setOnIconClickListener {
            binding.drawerLayout.apply {
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
        binding.drawerLayout.addDrawerListener(drawerListener)

        binding.navigationView.setNavigationItemSelectedListener {
            switchFragment(getRouteById(it.itemId))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        binding.navigationView.itemIconTintList = null
    }

    override fun initData() {
        initFragment()
    }

    private fun setTitleIcon(resId: Int) {
        if(binding.titleBar.getIconRes() == resId) {
            return
        }
        binding.titleBar.setIcon(resId)
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
    private fun switchFragment(route: String) {
        supportFragmentManager.commitTransaction {
            replaceFragment(getFragmentContainerId(), route)
        }
    }

    private fun getFragmentContainerId(): Int {
        return R.id.fragmentContainerView
    }
}