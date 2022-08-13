package com.mustly.wellmedia.utils

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.medialib.base.PlayState
import com.mustly.wellmedia.lib.medialib.base.bean.HardwareMediaInfo
import java.util.concurrent.locks.ReentrantLock

/**
 * description:
 *
 * MediaCodec 硬解码视频
 *
 * @author   wchenzhang
 * date：    2022/8/5 19:25
 * version   1.0
 * modify by
 */
class HardwareDecoder(
    val isVideo: Boolean, // 是否解码视频
    val fileUri: Uri // 文件的路径
) {
    companion object {
        const val TAG = "HardwareDecoder"
        const val TIMEOUT = 10000L

        /**
         * MediaCodec 的各状态定义:
         *
         * 在MediaCodec的生命周期内存在三种状态：Stopped, Executing or Released
         *     ● Stopped状态包含三种子状态：Uninitialized, Configured, Error
         *     ● Executing状态包含三种子状态：Flushed, Running, End-of-Stream
         *
         * ● 创建 Codec 示例后(调用以下3个方法之一)，Codec将会处于 Uninitialized 状态
         *     ● createByCodecName
         *     ● createDecoderByType
         *     ● createEncoderByType
         * ● 调用 MediaCodec.configure 方法后，Codec 将进入 Configured 状态
         * ● 调用 MediaCodec.start 方法后，Codec 会转入 Executing 状态
         *     ● start 后 Codec 立即进入 Flushed 子状态，此时的 Codec 拥有所有的 input and output buffers，Client 无法操作这些 buffers
         *     ● 调用 MediaCodec.dequeueInputBuffer 请求得到了一个有效的input buffer index 后, Codec 立即进入到了 Running 子状态
         *     ● 当得到带有 end-of-stream 标记的 input buffer 时(queueInputBuffer(EOS))，Codec将转入 End-of-Stream 子状态。
         *       在此状态下，Codec 不再接受新的 input buffer 数据，但仍会处理之前入队列而未处理完的 input buffer 并产生 output buffer，直到
         *       end-of-stream 标记到达输出端，数据处理的过程也随即终止
         *     ●  在 Executing 状态下可以调用 MediaCodec.flush方法进入 Flushed 子状态
         *     ●  在 Executing 状态下可以调用 MediaCodec.stop 方法进入 Uninitialized 子状态,可以对 Codec 进行重新配置
         * ● 极少数情况下 Codec 会遇到错误进入 Error 状态，可以调用 MediaCodec.reset 方法使其再次可用
         * ● 当 MediaCodec 数据处理任务完成时或不再需要 MediaCodec 时，可使用  MediaCodec.release 方法释放其资源
         * */
        private enum class MediaCodecState {
            UNINITIALIZED, // 调用 reset 或者 stop 时进入
            CONFIGURED, // 调用 configure 进入
            FLUSHED, // 调用 flush 或者 start 进入
            RUNNING, // 首次 dequeue Input(Output) Buffer 成功时进入
            END_OF_STREAM,  // Output Buffer 结束时进入
            ERROR, // 遇见错误时进入
            RELEASED, // 调用 release 时进入
            PAUSED, // 自定义状态，非 MediaCodec 标准状态，用于暂停场景
        }
    }

    private var lock = ReentrantLock()
    // 解码状态
    private var state = MediaCodecState.UNINITIALIZED
        set(value) {
            lock.lock()
            field = value
            lock.unlock()
        }

    /**
     * 音视频分离器
     * */
    var mExtractor: MediaExtractor = MediaExtractor()
    // 解码器
    var decoder: MediaCodec? = null
    // 音频播放器，仅解码音频时有效
    lateinit var audioTrack: AudioTrack

    // 只有视频用得上 surface
    var surface: Surface? = null

    var mediaInfo: HardwareMediaInfo? = null

    var currentSampleTime = 0L

    var startMs = 0L

    /**
     * 使用协程同步解码视频
     *
     * 文章介绍：https://www.jianshu.com/p/b625dba0399a
     *
     * 当使用原始视频数据时，最好采用 Surface 作为输入源来替代 ByteBuffer，这样效率更高，因为 Surface 使用的更底层
     * 的视频数据，不会映射或复制到 ByteBuffer 缓冲区
     *
     * Video 需要输出到 AudioTrack
     * */
    fun decode(context: Context, targetCallback: (() -> Any?)) {
        try {
            if (!configMedia(context)) {
                return
            }

            if (isVideo) {
                this.surface = targetCallback.invoke() as Surface
            } else {
                this.audioTrack = targetCallback.invoke() as AudioTrack
            }

            createAndDecode()

            release()
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
            state = MediaCodecState.ERROR
        }
    }

    private fun configMedia(context: Context): Boolean {
        // 1. 获取 MediaExtractor
        mExtractor.setDataSource(context, fileUri, null)
        // 2. 找到音频相关信息
        mediaInfo = findMediaFormat()
        if (mediaInfo!!.trackIndex < 0 || mediaInfo!!.mediaFormat == null) {
            return false
        }
        mExtractor.selectTrack(mediaInfo!!.trackIndex)

        infoLog()

        return true
    }

    fun release() = try {
        mExtractor.release()
        decoder?.release()
        if (!isVideo) {
            audioTrack.stop()
            audioTrack.release()
        }
        state = MediaCodecState.UNINITIALIZED
    } catch (e: Exception) {
        LogUtil.e(TAG, e)
    }

    /**
     * 只有视频才传入 surface
     * */
    private fun createAndDecode() = try {
        // 3. 创建解码器
        create()
        // 4. 配置并开始解码，音频的 surface 为空
        prepare()
        start()

        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            // 将资源传递到解码器
            if (!inputDone) {
                inputDone = decoder?.inputData(mExtractor) ?: inputDone
            }
            outputDone = decoder?.outputData() ?: outputDone
        }
    } catch (e: Exception) {
        LogUtil.e(TAG, e)
        state = MediaCodecState.ERROR
    }

    fun start() {
        if (state != MediaCodecState.CONFIGURED) {
            LogUtil.e(TAG, "can not start decoder which not in configured state")
            return
        }
        decoder?.start()
        state = MediaCodecState.FLUSHED
    }

    fun prepare() {
        if (state != MediaCodecState.UNINITIALIZED) {
            LogUtil.e(TAG, "can not prepare decoder which not in uninitialized state")
            return
        }
        decoder?.configure(mediaInfo?.mediaFormat, surface, null, 0)
        state = MediaCodecState.CONFIGURED
    }

    fun create() {
        if (decoder == null) {
            decoder = MediaCodec.createDecoderByType(mediaInfo?.mimeType ?: "")
        }
        state = MediaCodecState.UNINITIALIZED
    }

    fun pause() {
        LogUtil.d(TAG, "pause decoder")
        stop()
        state = MediaCodecState.PAUSED
    }

    fun resume() {
        LogUtil.d(TAG, "resume decoder")

    }

    fun reset() = stop()

    fun stop() {
        if (state != MediaCodecState.FLUSHED &&
            state != MediaCodecState.RUNNING &&
            state != MediaCodecState.END_OF_STREAM
        ) {
            LogUtil.e(TAG, "can not stop decoder which not in flushed or running or end-of-stream state, current state: $state")
            return
        }
        decoder?.stop()
        state = MediaCodecState.UNINITIALIZED
    }

    fun flush() {
        if (state != MediaCodecState.RUNNING && state != MediaCodecState.END_OF_STREAM) {
            LogUtil.e(TAG, "can not flush decoder which not in running or end-of-stream state, current state: $state")
            return
        }
        decoder?.flush()
        state = MediaCodecState.FLUSHED
    }

    fun releaseDecoder() {
        if (state == MediaCodecState.RELEASED) {
            LogUtil.w(TAG, "decoder already released!!!")
            return
        }
        decoder?.release()
        state = MediaCodecState.RELEASED
    }

    /**
     * 原始数据写入解码器
     * */
    private fun MediaCodec.inputData(mExtractor: MediaExtractor?): Boolean? {
        if (!isPlaying()) {
            LogUtil.d(TAG, "not playing >>> play state = $state")
            return true
        }
        // 1. dequeue: 出列，拿到一个输入缓冲区的index，因为有好几个缓冲区来缓冲数据. -1表示暂时没有可用的
        val inputBufferIndex = dequeueInputBuffer(TIMEOUT)
        if (inputBufferIndex < 0) {
            LogUtil.d(TAG, "negative inputBufferIndex = $inputBufferIndex")
            return null
        }

        if (state != MediaCodecState.RUNNING) {
            state = MediaCodecState.RUNNING
        }

        // 2. 使用返回的 inputBuffer 的 index 得到一个ByteBuffer，可以放数据了
        val inputBuffer = getInputBuffer(inputBufferIndex)
        if (inputBuffer == null) {
            LogUtil.d(TAG, "input buffer is null")
            return null
        }

        // 3. 往 InputBuffer 里面写入数据。返回的是写入的实际数据量，-1 表示已全部写入
        val sampleSize = mExtractor?.readSampleData(inputBuffer, 0) ?: -1
        LogUtil.d(TAG, "readSampleData = $sampleSize")
        // 4. 数据入队
        return if (sampleSize >= 0) {
            // 填充好的数据写入 InputBuffer，分别设置 size 和 sampleTime
            // 这里 sampleTime 不一定是顺序来的，所以需要缓冲区来调节顺序
            queueInputBuffer(inputBufferIndex, 0, sampleSize, mExtractor?.sampleTime ?: 0, 0)
            // 在 MediaExtractor 执行完一次 readSampleData 方法后，
            // 需要调用 advance() 去跳到下一个 sample， 然后再次读取数据(读取下次采样视频帧)
            mExtractor?.advance()
            false
        } else {
            // 入队结束
            queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            true
        }
    }

    /**
     * 从解码器获取解码后的音频数据
     * */
    private fun MediaCodec.outputData(): Boolean? {
        if (!isPlaying()) {
            LogUtil.d(TAG, "not playing >>> play state = $state")
            return true
        }

        val bufferInfo = MediaCodec.BufferInfo()
        // 等待 10 秒
        val outputBufferIndex = dequeueOutputBuffer(bufferInfo, TIMEOUT)
        if (outputBufferIndex < 0 || bufferInfo.size <= 0) {
            LogUtil.d(TAG, "negative outputBufferIndex = $outputBufferIndex, negative bufferSize = ${bufferInfo.size}")
            return null
        }

        if (!isVideo) {
            val byteBuffer = getOutputBuffer(outputBufferIndex) ?: return null
            val pcmData = ByteArray(bufferInfo.size)

            // 读取缓存到数组
            byteBuffer.position(0)
            byteBuffer.get(pcmData, 0, bufferInfo.size)
            byteBuffer.clear()
            // audioTrack.write(pcmData, 0, audioBufferInfo.size);//用这个写法会导致少帧？
            /* TODO 待删除 LogUtil.d(TAG, "pcmData.size = ${pcmData.size}, " +
                "bufferInfo.offset = ${bufferInfo.offset}, " +
                "bufferInfo.size = ${bufferInfo.size}, " +
                "bufferInfo.offset + bufferInfo.size = ${bufferInfo.offset + bufferInfo.size}"
            )*/
            // 数据写入播放器
            audioTrack.write(pcmData, bufferInfo.offset, bufferInfo.offset + bufferInfo.size)
        }
        // 直接渲染到 Surface 时使用不到 outputBuffer
        // ByteBuffer outputBuffer = videoCodec.getOutputBuffer(outputBufferIndex);
        // 如果缓冲区里的展示时间(PTS) > 当前音频播放的进度，就休眠一下(音频解析过快，需要缓缓)
        sleep(bufferInfo)
        // 将该ByteBuffer释放掉，以供缓冲区的循环使用
        releaseOutputBuffer(outputBufferIndex, true)

        return if (bufferInfo.flags.and(MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            state = MediaCodecState.END_OF_STREAM
            true
        } else {
            false
        }
    }

    fun findMediaFormat(): HardwareMediaInfo {
        val prefix = if (isVideo) {
            "video/"
        } else {
            "audio/"
        }
        (0 until mExtractor.trackCount).forEach {
            val format = mExtractor.getTrackFormat(it)
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mimeType.startsWith(prefix)) {
                return HardwareMediaInfo(mimeType, it, format)
            }
        }

        return HardwareMediaInfo("", -1, null)
    }

    private fun sleep(mediaBufferInfo: MediaCodec.BufferInfo) {
        // videoBufferInfo.presentationTimeUs / 1000  PTS 视频的展示时间戳(相对时间)
        val ffTime = startMs + mediaBufferInfo.presentationTimeUs / 1000 - System.currentTimeMillis()
        if (ffTime > 0) {
            Thread.sleep(ffTime)
        }
    }

    private fun infoLog() = mediaInfo?.apply {
        var logStr = "mimeType = $mimeType, duration = $duration"
        if (isVideo) {
            logStr = "$logStr, width = $width, height = $height"
        } else {
            logStr = "$logStr, sampleRate = $sampleRate, channelCount = $channelCount, " +
                "sampleDepth = $sampleDepth"
        }

        LogUtil.d(TAG, "mediaInfo >>> $logStr")
    }
}