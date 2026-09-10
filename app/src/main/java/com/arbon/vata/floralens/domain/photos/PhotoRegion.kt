package com.arbon.vata.floralens.domain.photos

/** Pixel bounds in the upright photo. Only the offline model uses this region. */
data class PhotoRegion(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun clampedTo(width: Int, height: Int): PhotoRegion {
        require(width > 0 && height > 0)
        val x = left.coerceIn(0, width - 1)
        val y = top.coerceIn(0, height - 1)
        return PhotoRegion(x, y, right.coerceIn(x + 1, width), bottom.coerceIn(y + 1, height))
    }
}
