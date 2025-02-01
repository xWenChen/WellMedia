package com.mustly.wellmedia.utils

/**
 *
 * */
class MP3LameEncoder {
    companion object {
        const val TAG = "MP3LameEncoder"
    }

    init {
        // 不要 lib 前缀和 .so 后缀，这是规则。
        System.loadLibrary("mp3lame")
    }

    // native 层 lame client 的指针。
    private var lameClientPtr: Long = createLame()

    fun getLibVersion(): String = getLameMp3Version()

    fun destroy(): Int {
        val ret = close(lameClientPtr)
        lameClientPtr = 0
        return ret
    }

    /**
     * 单通道输入，两个声道的数据可以来自不同的文件。
     * */
    fun encode(
        leftBuff: ByteArray,
        rightBuff: ByteArray?,
        numSamples: Int,
        resultBuff: ByteArray
    ): Int {
        return encode(lameClientPtr, leftBuff, rightBuff, numSamples, resultBuff)
    }
    /**
     * 多通道混合输入，左右左右的PCM数据。
     *
     * @param numSamples 这里的num_samples指的是每个通道的采样点数,而不是所有通道的采样点数之和。例如当AudioFormat为16bit、双通道输入的时候, 一个采样点大小为2byte, 则nsamples = buffer_l有效数据长度(byte) / 2(16bite为2byte) / 2(通道数为2)
     * */
    fun nativeEncodeInterleaved(
        pcmBuffer: ByteArray,
        numSamples: Int,
        resultBuff: ByteArray
    ): Int {
        return nativeEncodeInterleaved(lameClientPtr, pcmBuffer, numSamples, resultBuff)
    }

    private fun createLame(): Long {
        val client = createLameMp3Client()
        setInSampleRate(client, 44100)
        setOutSampleRate(client, 44100)
        setNumChannels(client, 2)
        setBitRate(client, 128) // 128k 采样率
        setQuality(client, 2) // 编码质量 0 到 9,9最低，通常采用2。
        initParams(client)

        return client
    }

    private external fun getLameMp3Version(): String
    // 初始化，创建 client。
    private external fun createLameMp3Client(): Long
    // 释放 lame client。
    private external fun close(client: Long): Int
    // 设置编码质量 0 到 9, 0 最高，9最低，通常采用2。
    private external fun setQuality(client: Long, quality: Int): Int
    // 设置输入采样率，通常为 44100
    private external fun setInSampleRate(client: Long, sampleRate: Int): Int
    // 设置输出采样率，通常等于输入采样率
    private external fun setOutSampleRate(client: Long, sampleRate: Int): Int
    // 设置码率，比如 128，代表 128k 分辨率。
    private external fun setBitRate(client: Long, bitRate: Int): Int
    // 设置声道数量。
    private external fun setNumChannels(client: Long, numChannels: Int): Int
    // 初始化参数
    private external fun initParams(client: Long): Int
    // 单通道输入，两个声道的数据可以来自不同的文件。
    private external fun encode(
        client: Long,
        leftBuff: ByteArray,
        rightBuff: ByteArray?,
        numSamples: Int,
        resultBuff: ByteArray
    ): Int
    // 多通道混合输入，左右左右的PCM数据。
    private external fun nativeEncodeInterleaved(
        client: Long,
        pcmBuff: ByteArray,
        numSamples: Int,
        resultBuff: ByteArray
    ): Int
}