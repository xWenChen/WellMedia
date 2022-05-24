package com.mustly.wellmedia.lib.commonlib.route

import kotlin.reflect.KClass

interface RouteRegister {
    fun register(map: Map<String, KClass<*>>)
}