package com.example.imageloading

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner

/**
 * Represents a request to load an image
 */
data class ImageRequest(
    val source: Any,
    val targetView: ImageView?,
    val placeholder: Int?,
    val error: Int?,
    val skipMemoryCache: Boolean,
    val skipDiskCache: Boolean,
    val overrideWidth: Int?,
    val overrideHeight: Int?,
    val transformation: Transformation?,
    val lifecycleOwner: LifecycleOwner?,
    val callback: ImageCallback?
) {
    /**
     * Generate a unique cache key for this request
     */
    fun getCacheKey(): String {
        val sourceKey = when (source) {
            is String -> source
            is Int -> "res:$source"
            is java.io.File -> "file:${source.absolutePath}"
            else -> source.toString()
        }
        
        val sizeKey = if (overrideWidth != null && overrideHeight != null) {
            "${overrideWidth}x${overrideHeight}"
        } else {
            ""
        }
        
        val transformKey = transformation?.key() ?: ""
        
        return "$sourceKey$sizeKey$transformKey"
    }
}

