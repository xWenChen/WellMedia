package com.mustly.wellmedia.utils

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import com.mustly.wellmedia.base.getFragmentClass
import java.lang.IllegalArgumentException

fun FragmentTransaction.showFragment(fragment: Fragment) {
    this.show(fragment)
    this.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
}

fun FragmentTransaction.hideFragment(fragment: Fragment) {
    this.hide(fragment)
    this.setMaxLifecycle(fragment, Lifecycle.State.STARTED)
}

fun FragmentTransaction.addFragment(containerId: Int, route: String, args: (Bundle.() -> Unit)? = null) {
    val bundle: Bundle?
    if(args == null) {
        bundle = null
    } else {
        bundle = Bundle()
        args.invoke(bundle)
    }

    route.getFragmentClass()?.also {
        this.add(containerId, it, bundle, route)
    } ?: Log.e("FragmentTransaction", "addFragment", IllegalArgumentException("the class of $route is null"))
}

fun FragmentTransaction.replaceFragment(containerId: Int, route: String, args: (Bundle.() -> Unit)? = null) {
    val bundle: Bundle?
    if(args == null) {
        bundle = null
    } else {
        bundle = Bundle()
        args.invoke(bundle)
    }

    route.getFragmentClass()?.also {
        this.replace(containerId, it, bundle, route)
    } ?: Log.e("FragmentTransaction", "replaceFragment", IllegalArgumentException("the class of $route is null"))
}

fun FragmentManager.commitTransaction(allowStateLoss: Boolean = true, commitNow: Boolean = false, task: (FragmentTransaction.() -> Unit)? = null) {
    val temp = this.beginTransaction().apply {
        task?.invoke(this)
    }
    if (allowStateLoss && commitNow) {
        temp.commitNowAllowingStateLoss()
    } else if (allowStateLoss) {
        temp.commitAllowingStateLoss()
    } else if (commitNow) {
        temp.commitNow()
    } else {
        temp.commit()
    }
}