package com.mustly.wellmedia.audio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import com.mustly.wellmedia.R
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.utils.colorRes

class AudioRecordView : View {
    companion object {
        const val TAG = "AudioRecordView"
    }

    // 当前需要绘制的弧线数量
    var arcCount = 1

    var playAnim = false

    var startTimeMs = 0L
    var curFpsCount = 0

    lateinit var paint: Paint

    // 每帧动画绘制时的回调
    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        // 输出应用帧率
        checkAppFps(frameTimeNanos)
        drawAndListenNextFrame()
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, null, 0) {
        init(context)
    }

    fun init(context: Context) {
        paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = R.color.main_blue.colorRes
            strokeWidth = 3f
        }
    }

    fun playAnim(isPlay: Boolean) {
        playAnim = isPlay
        reset()
        drawAndListenNextFrame()
    }

    /**
     * 检测应用帧率
     *
     * @param frameTimeNanos 绘制当前帧的时间，时间单位：纳秒。1秒 =1_000_000_000纳秒，1毫秒=1_000_000纳秒
     * */
    private fun checkAppFps(frameTimeNanos: Long) {
        val curMs = frameTimeNanos / 1_000_000L // 当前的毫秒数
        if(curMs - startTimeMs > 1000L) { // 大于1秒，输出帧率
            LogUtil.d(TAG, "应用当前的帧率为：$curFpsCount")
            reset()
            return
        }
        curFpsCount++
    }

    // 监听下一帧的动作
    private fun drawAndListenNextFrame() {
        if (!playAnim) {
            return
        }
        invalidate()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun reset() {
        startTimeMs = System.currentTimeMillis()
        curFpsCount = 0
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        if (!playAnim) {
            return
        }

        // todo 先画个圆点，再画指定数量的弧线。
    }
}