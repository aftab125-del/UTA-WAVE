package com.uta.wave.data.canvas

data class CanvasArtwork(
    val name: String? = null,
    val artist: String? = null,
    val albumName: String? = null,
    val videoUrl: String? = null,
    val animated: String? = null
) {
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl
}
