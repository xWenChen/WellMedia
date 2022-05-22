package com.mustly.wellmedia.lib.commonlib.route

import android.content.Context
import android.content.Intent

interface RouteAnnotationHandler {
    fun register(url: String, target: String)
    fun startUrl(context: Context, url :String)
    fun getIntent(context: Context, className: String): Intent
}