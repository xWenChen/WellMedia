package com.mustly.wellmedia.base

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class ItemBean(@DrawableRes var iconResId: Int, @StringRes var textRes: Int, @FragmentConstant.Tag var tag: String)