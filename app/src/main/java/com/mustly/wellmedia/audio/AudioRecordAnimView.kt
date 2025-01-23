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
import com.mustly.wellmedia.utils.dimenRes

/**
 * 音频录制动画布局，每 300 毫秒刷新一次，共绘制6段+1个圆点。同时可检测帧率
 * */
class AudioRecordAnimView : View {
    companion object {
        const val TAG = "AudioRecordView"
        const val MIN_ARC_COUNT = 1 // 最小的弧线数量
        const val MAX_ARC_COUNT = 6 // 最大可绘制的弧线数量
        const val DURATION = 1800 // 动画的执行时长
        const val ONE_S = 1_000_000_000L // 1秒的纳秒数
        // 弧线举例圆的距离
        const val SPACE = 46f
        // 弧线的大小
        const val ARC_SPACE = 50f
    }

    // 当前是否播放动画的标识
    private var playAnim = false

    // 当前需要绘制的弧线数量
    private var curArcCount = MIN_ARC_COUNT
    // 上次绘制的时间，单位纳秒
    private var lastDrawTime = 0L
    // 绘制时的时间间隔，单位纳秒，300 毫秒刷新一次
    private val drawTimeInterval = DURATION / MAX_ARC_COUNT * 1_000_000L

    // 计算帧率用到的信息
    private var fpsStartTimeNs = 0L
    private var curFpsCount = 0

    lateinit var paint: Paint

    private val radius = R.dimen.dp1.dimenRes.toFloat()

    // 每帧动画绘制时的回调
    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        if (!playAnim) {
            return@FrameCallback
        }
        // 输出应用帧率
        checkAppFps(frameTimeNanos)
        // 检测是否触发绘制
        checkRefresh(frameTimeNanos)
        listenNextFrame()
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

    private fun init(context: Context) {
        paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = R.color.main_blue.colorRes
            strokeWidth = radius * 2
        }
    }

    fun playAnim(isPlay: Boolean) {
        if (playAnim == isPlay) {
            return
        }
        LogUtil.d(TAG, "是否播放动画？$isPlay")
        playAnim = isPlay
        resetFps()
        resetDrawInfo()
        listenNextFrame()
    }

    /**
     * 检测应用帧率
     *
     * @param frameTimeNanos 绘制当前帧的时间，时间单位：纳秒。1秒 =1_000_000_000纳秒，1毫秒=1_000_000纳秒
     * */
    private fun checkAppFps(frameTimeNanos: Long) {
        if(frameTimeNanos - fpsStartTimeNs > ONE_S) { // 大于1秒，输出帧率
            LogUtil.d(TAG, "frameTimeNanos=$frameTimeNanos, 当前时间戳为 ${frameTimeNanos / ONE_S} 秒, 应用的帧率为：$curFpsCount")
            resetFps()
            return
        }
        curFpsCount++
    }

    /**
     * 检查是否触发重绘，在动画播放时长(DURATION)内，只触发 MAX_ARC_COUNT 次。
     *
     * @param frameTimeNanos 当前帧的纳秒时间
     * */
    private fun checkRefresh(frameTimeNanos: Long) {
        if (frameTimeNanos - lastDrawTime < drawTimeInterval) {
            return
        }
        // 触发刷新
        invalidate()
        lastDrawTime = frameTimeNanos
    }

    private fun resetDrawInfo() {
        lastDrawTime = 0L
        curArcCount = MIN_ARC_COUNT
    }

    // 监听下一帧的动作
    private fun listenNextFrame() {
        if (!playAnim) {
            return
        }
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun resetFps() {
        fpsStartTimeNs = System.nanoTime()
        curFpsCount = 0
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        if (!playAnim) {
            return
        }

        // 先画个圆点，再画指定数量的弧线。
        // 图形生成在底部
        canvas.drawCircle(width / 2f, height - 49f, radius, paint)
        val centerX = width / 2

        // 圆弧所在矩形为 宽:高=2:1
        for (i in 1..curArcCount) {
            val span = ARC_SPACE * i
            canvas.drawArc(
                centerX - span,
                height - SPACE - span,
                centerX + span,
                height - SPACE,
                -30f, // 0度为水平向右，负数表示逆时针，从30度开始，到150度结束。扫过120度。
                -120f, // 负数表示逆时针
                false,
                paint
            )
        }
        // 检查弧线数量
        if (curArcCount == MAX_ARC_COUNT) {
            curArcCount = MIN_ARC_COUNT
        } else {
            curArcCount++
        }
    }
}