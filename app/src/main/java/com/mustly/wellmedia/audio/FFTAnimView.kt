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
import kotlin.math.min


/**
 * FFT(快速傅里叶变换图)频率图布局
 *
 * @author wchenzhang
 * @date 2025/01/18 17:51:35
 */
class FFTAnimView : View {
    companion object {
        private const val TAG = "FFTAnimView"
    }

    var magnitudes = FloatArray(0) // 振幅数据(分贝数据)
    // var phases = FloatArray(0) // 频率图段数
    private val mRect = Rect(0, 0, 24, 150)
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
    // 2、取 Visualizer.getMaxCaptureRate() / 2 是波形是对称的，前半段和后半段是对称的。
    // 3、原生 API 的数据是有缺陷的，音量为 0 时，应该是拿不到的数据的。
    fun update(fft: ByteArray?) {
        fft ?: return
        val n = fft.size
        magnitudes = FloatArray(n / 2 + 1)
        // 只有第一个点和第二个点比较特殊代表直流(DC)和Nyquist频率(最大频率的一半)，其余点都是实部+虚部
        magnitudes[0] = abs(fft[0].toInt()).toFloat() // 直流(DC)
        magnitudes[n / 2] = abs(fft[1].toInt()).toFloat() // Nyquist频率(最大频率的一半)
        /*phases = FloatArray(n / 2 + 1)
        phases[0] = 0f
        phases[n / 2] = 0f*/

        for (k in 1 until n / 2) {
            val i = k * 2
            magnitudes[k] = hypot(fft[i].toDouble(), fft[i + 1].toDouble()).toFloat() // 结合实部和虚部，得到振幅
            //LogUtil.d(TAG, "update, magnitudes[$k]=${magnitudes[k]}")
            //phases[k] = atan2(fft[i + 1].toDouble(), fft[i].toDouble()).toFloat() // 反正切数据
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // 2、拿到数据magnitudes之后在View中进行绘制。将每个点以条形状的形式画出：
        if (magnitudes.isEmpty()) {
            return
        }
        // 显示 25 个波形，skipCount的取值根据具体效果优化，1024 / 2 + 1 = 513 / 25 = 20
        val skipCount = 20
        mRect.set(0, 0, width, height)
        for (i in 0 until 25) {
            val j = min(i * skipCount, magnitudes.size - 1)
            //LogUtil.d(TAG, "draw info: ｛left: ${27f * i}, top: ${mRect.height() - magnitudes[i] * 3f}, right: ${27f * i + 24}, height: ${mRect.height().toFloat()}｝")
            // 宽为 24，高为 振幅 * 10，保留4dp的基础高度。根据实际效果调整。
            canvas?.drawRect(
                27f * i,
                min(mRect.height() - magnitudes[j] * 10f, mRect.height() - 12f),
                27f * i + 24,
                mRect.height().toFloat(),
                mPaint
            )
        }
    }
}