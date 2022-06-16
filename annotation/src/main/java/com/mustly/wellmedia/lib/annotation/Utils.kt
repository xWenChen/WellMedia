package com.mustly.wellmedia.lib.annotation

import java.util.*

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2022/5/25 19:18
 * version   1.0
 * modify by
 */
object Utils {
    fun parseModuleName(srcModuleName: String?): String {
        if (srcModuleName.isNullOrEmpty()) {
            return Constants.DEFAULT_MODULE_NAME
        }

        return srcModuleName
            .replace(".", "_")
            .replace("-", "_")
            .replace(" ", "_")
            .capitalize()
    }

    fun getRegisterClassName(moduleName: String?, needParse: Boolean = true): String {
        var prefix = ""

        if (needParse) {
            prefix = parseModuleName(moduleName)
        }

        if (prefix.isEmpty()) {
            throw RuntimeException("module name parse error, can not get route table info.")
        }

        return "$prefix${Constants.ROUTE_REGISTER}"
    }
}