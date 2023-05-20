/*
 * Copyright 2022 The Android Open Source Project
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

package com.mustly.wellmedia.video.camera2

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mustly.wellmedia.base.BaseFragment
import com.mustly.wellmedia.base.PageRoute
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.commonlib.route.Router

/**
 * 是否启用人像滤镜(whether the portrait filter is on or not)
 */
@Route(PageRoute.CAMERA2_FILTER_FRAGMENT)
class FilterFragment : BaseFragment() {
    private lateinit var args: CameraInfo

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext())

    override fun initData(rootView: View) {
        parseArgs()

        rootView as RecyclerView
        rootView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            val modeList = enumerateModes()
            val layoutId = android.R.layout.simple_list_item_1
            adapter = GenericListAdapter(modeList, itemLayoutId = layoutId) { view, item, _ ->
                val (name, value) = item
                view.findViewById<TextView>(android.R.id.text1).text = name
                view.setOnClickListener { onSelectConfig(value) }
            }
        }
    }

    private fun parseArgs() {
        val intent = activity?.intent ?: throw IllegalArgumentException("Activity Intent is null")

        intent.apply {
            args = CameraInfo(
                "",
                getStringExtra(Constants.cameraId) ?: "",
                Size(
                    getIntExtra(Constants.width, 1920),
                    getIntExtra(Constants.height, 1080),
                ),
                getIntExtra(Constants.fps, 30),
                getBooleanExtra(Constants.previewStabilization, false),
            )
        }
    }

    private fun onSelectConfig(filterOn: Boolean) {
        // MediaCodec 硬件编码时才能开启人像滤镜
        Router.go(requireContext(), PageRoute.CAMERA2_PREVIEW_FRAGMENT) {
            putExtra(Constants.cameraId, args.cameraId)
            putExtra(Constants.width, args.size.width)
            putExtra(Constants.height, args.size.height)
            putExtra(Constants.fps, args.fps)
            putExtra(Constants.previewStabilization, args.previewStabilization)
            putExtra(Constants.filterOn, filterOn)
            putExtra(Constants.useHardware, true)
        }
    }

    private fun enumerateModes(): List<Pair<String, Boolean>> {
        val modeList: MutableList<Pair<String, Boolean>> = mutableListOf()

        modeList.add(Pair("Portrait Filter On", true))
        modeList.add(Pair("Portrait Filter Off", false))

        return modeList
    }
}
