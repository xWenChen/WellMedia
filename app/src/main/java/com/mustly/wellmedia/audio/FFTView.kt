package com.mustly.wellmedia.audio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot


/**
 * FFT(快速傅里叶变换图)频率图布局
 *
 * @author wchenzhang
 * @date 2025/01/18 17:51:35
 */
class FFTView : View {
    var magnitudes = FloatArray(0) // 振幅数据
    var phases = FloatArray(0) // 频率图段数
    private val mRect = Rect()
    private val mPaint = Paint()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, null, 0) {
        init()
    }

    fun init() {
        mPaint.strokeWidth = 5.0f
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.setColor(Color.parseColor("#99E64D")) // 青草绿
    }

    // 1、通过FFT的数组格式，获取到每个频率点的实部和虚部。
    fun update(fft: ByteArray?, samplingRate: Int) {
        val n = fft!!.size
        magnitudes = FloatArray(n / 2 + 1) // 振幅数据
        phases = FloatArray(n / 2 + 1) // 频率图段数

        // 每个傅里叶数据由实数和虚数构成，数组0是实数，1是虚数
        magnitudes[0] = abs(fft[0].toInt()).toFloat() // DC 实部
        magnitudes[n / 2] = abs(fft[1].toInt()).toFloat() // Nyquist 虚部

        phases[0] = 0f

        for (k in 1 until n / 2) {
            val i = k * 2
            //取频率点实部与虚部的模
            magnitudes[k] = hypot(fft[i].toDouble(), fft[i + 1].toDouble()).toFloat()
        }

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // 2、拿到数据magnitudes之后在View中进行绘制。将每个点以条形状的形式画出：
        if (magnitudes.isEmpty() || phases.isEmpty()) {
            return
        }
        val mSpectrumCount = (phases.size - 1)
        mRect.set(0, 0, width, height)
        for (i in phases.indices) {
            val x = mRect.width() * i / mSpectrumCount.toFloat()
            // todo 完善图形
            /*canvas?.drawLine(x, mRect.height() / 2f,
                mRect.width() * i / mSpectrumCount,
                2 + mRect.height() / 2 - mRawAudioBytes[i],
                mPaint
            )*/
        }
    }
}