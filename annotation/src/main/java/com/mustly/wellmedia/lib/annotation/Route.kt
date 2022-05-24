package com.mustly.wellmedia.lib.annotation

/**
 * 指定页面路由
 *
 * @param url 页面的路由地址
 * */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Route(val url: String = "")