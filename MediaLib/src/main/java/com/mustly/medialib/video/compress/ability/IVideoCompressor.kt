package com.mustly.medialib.video.compress.ability

interface IVideoCompressor {
    /**
     * 压缩视频
     *
     * @param sourcePath 待压缩的视频的路径
     * @param targetPath 压缩后的视频的路径
     * */
    fun startCompress(sourcePath: String, targetPath: String)
}