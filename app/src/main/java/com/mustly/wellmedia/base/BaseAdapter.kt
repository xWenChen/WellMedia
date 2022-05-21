package com.mustly.wellmedia.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.lang.UnsupportedOperationException

class BaseAdapter<DATA> : RecyclerView.Adapter<BaseViewHolder>() {
    private var data: List<DATA>? = null
    /**
     * callback 的参数表示 viewType, 返回值为布局的资源 id
     * */
    private var layoutResCallback: ((Int) -> Int)? = null
    /**
     * callback 的参数表示根布局，position, 数据
     * */
    private var bindViewCallback: ((View, Int, DATA) -> Unit)? = null

    fun setData(data: MutableList<DATA>): BaseAdapter<DATA> {
        this.data = data
        return this
    }

    fun refresh(): BaseAdapter<DATA> {
        notifyDataSetChanged()
        return this
    }

    /**
     * callback 的参数表示 viewType, 返回值为布局的资源 id
     * */
    fun setLayoutRes(layoutRes: ((Int) -> Int)): BaseAdapter<DATA> {
        this.layoutResCallback = layoutRes
        return this
    }
    /**
     * callback 的参数表示根布局，position, 数据
     * */
    fun bindView(callback: ((View, Int, DATA) -> Unit)): BaseAdapter<DATA> {
        this.bindViewCallback = callback
        return this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        if(layoutResCallback == null) {
            throw UnsupportedOperationException("The callback is null which using to get the layout res")
        }
        return BaseViewHolder(LayoutInflater.from(parent.context)
            .inflate(layoutResCallback!!.invoke(viewType), null))
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        var item: DATA = getItem(position) ?: return
        if(bindViewCallback == null) {
            throw UnsupportedOperationException("The callback is null which bind the view")
        }
        bindViewCallback!!.invoke(holder.rootView, position, item)
    }

    override fun getItemCount(): Int {
        return data?.size?:0
    }

    fun getItem(pos: Int): DATA? {
        if(data.isNullOrEmpty()) {
            return null
        }
        if(pos < 0 || pos >= data!!.size) {
            return null
        }
        return data!!.get(pos)
    }
}

class BaseViewHolder(var rootView: View) : RecyclerView.ViewHolder(rootView) {
}