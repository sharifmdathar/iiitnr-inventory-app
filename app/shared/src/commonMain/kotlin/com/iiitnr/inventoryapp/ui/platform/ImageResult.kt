package com.iiitnr.inventoryapp.ui.platform

data class ImageResult(
    val bytes: ByteArray,
    val filename: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageResult

        if (!bytes.contentEquals(other.bytes)) return false
        if (filename != other.filename) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + filename.hashCode()
        return result
    }
}

expect suspend fun pickImage(): ImageResult?

expect suspend fun takePhoto(): ImageResult?
