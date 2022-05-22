package com.mustly.wellmedia.lib.medialib.audio.base.bean

import com.mustly.wellmedia.lib.medialib.base.MediaType
import com.mustly.wellmedia.lib.medialib.base.bean.MediaTrackInfo

class AudioTrackInfo : MediaTrackInfo() {
    override var mediaType: MediaType = MediaType.AUDIO
        get() = MediaType.AUDIO
        set(value) {field = MediaType.AUDIO}
}