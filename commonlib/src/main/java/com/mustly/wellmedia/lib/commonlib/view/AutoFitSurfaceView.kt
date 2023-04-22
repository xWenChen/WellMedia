/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mustly.wellmedia.lib.commonlib.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import kotlin.math.roundToInt

/**
 * 可以指定宽高比并对输入帧执行 center-crop 变换的 [SurfaceView]
 */
class AutoFitSurfaceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle) {


    private var aspectRatio = 0f

    fun setAspectRatio(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            LogUtil.w(TAG, "width(value=$width) or height(value=$height) cannot be negative")
        }
        aspectRatio = width.toFloat() / height
        holder.setFixedSize(width, height)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (aspectRatio == 0f) {
            setMeasuredDimension(width, height)
            return
        }

        // 执行 center-crop 变换
        val newWidth: Int
        val newHeight: Int
        val actualRatio = if (width > height) aspectRatio else 1f / aspectRatio
        if (width < height * actualRatio) {
            newHeight = height
            newWidth = (height * actualRatio).roundToInt()
        } else {
            newWidth = width
            newHeight = (width / actualRatio).roundToInt()
        }

        Log.d(TAG, "Measured dimensions set: $newWidth x $newHeight")
        setMeasuredDimension(newWidth, newHeight)
    }

    companion object {
        private val TAG = "AutoFitSurfaceView"
    }
}
