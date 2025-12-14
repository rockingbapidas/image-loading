package com.example.imageloading

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

/**
 * Interface for loading images from different sources
 */
interface Loader {
    fun canHandle(source: Any): Boolean
    fun load(request: ImageRequest, callback: (Result<Bitmap>) -> Unit)
}

/**
 * Loader for network URLs
 */
class NetworkLoader(
    private val context: Context,
    private val bitmapPool: BitmapPool?
) : Loader {

    override fun canHandle(source: Any): Boolean {
        return source is String && (source.startsWith("http://") || source.startsWith("https://"))
    }

    override fun load(request: ImageRequest, callback: (Result<Bitmap>) -> Unit) {
        val urlString = request.source as String
        val url = URL(urlString)

        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { inputStream ->
                    val bitmap = decodeBitmap(
                        inputStream,
                        request.overrideWidth,
                        request.overrideHeight
                    )

                    if (bitmap != null) {
                        callback(Result.success(bitmap))
                    } else {
                        callback(Result.failure(Exception("Failed to decode bitmap from URL: $urlString")))
                    }
                }
            } else {
                callback(Result.failure(Exception("HTTP error: ${connection.responseCode} for URL: $urlString")))
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkLoader", "Error loading image from $urlString", e)
            callback(Result.failure(e))
        } finally {
            // Ensure connection is disconnected
            try {
                (url.openConnection() as? HttpURLConnection)?.disconnect()
            } catch (e: Exception) {
                // Ignore disconnect errors
            }
        }
    }

    private fun decodeBitmap(
        inputStream: InputStream,
        width: Int?,
        height: Int?
    ): Bitmap? {
        return try {
            if (width != null && height != null) {
                // Read stream into byte array to support multiple passes
                val bytes = inputStream.readBytes()

                // First pass: get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(options, width, height)
                options.inJustDecodeBounds = false
                
                // Calculate target dimensions after sampling
                val targetWidth = options.outWidth / options.inSampleSize
                val targetHeight = options.outHeight / options.inSampleSize
                
                // Try to get a bitmap from the pool to reuse (must match exactly)
                val pooledBitmap = bitmapPool?.get(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                options.inBitmap = pooledBitmap
                options.inMutable = pooledBitmap == null
                
                // Second pass: decode with sample size
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                
                // If decoding with inBitmap failed, try without it
                if (bitmap == null && pooledBitmap != null) {
                    // Return the bitmap to pool and try again without inBitmap
                    bitmapPool.put(pooledBitmap)
                    options.inBitmap = null
                    options.inMutable = true
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                }
                
                bitmap
            } else {
                // Read stream into byte array
                val bytes = inputStream.readBytes()
                
                // First pass: get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                
                // Try to reuse bitmap from pool (must match exactly)
                val pooledBitmap = bitmapPool?.get(
                    options.outWidth,
                    options.outHeight,
                    Bitmap.Config.ARGB_8888
                )
                options.inBitmap = pooledBitmap
                options.inJustDecodeBounds = false
                options.inMutable = pooledBitmap == null
                
                // Decode
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                
                // If decoding with inBitmap failed, try without it
                if (bitmap == null && pooledBitmap != null) {
                    // Return the bitmap to pool and try again without inBitmap
                    bitmapPool.put(pooledBitmap)
                    options.inBitmap = null
                    options.inMutable = true
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                }
                
                bitmap
            }
        } catch (e: Exception) {
            // Log error and return null
            android.util.Log.e("NetworkLoader", "Failed to decode bitmap: ${e.message}", e)
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

/**
 * Loader for Android resource IDs
 */
class ResourceLoader(
    private val context: Context,
    private val bitmapPool: BitmapPool?
) : Loader {

    override fun canHandle(source: Any): Boolean {
        return source is Int
    }

    override fun load(request: ImageRequest, callback: (Result<Bitmap>) -> Unit) {
        val resourceId = request.source as Int

        try {
            val bitmap = if (request.overrideWidth != null && request.overrideHeight != null) {
                // First get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(context.resources, resourceId, options)

                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(
                    options,
                    request.overrideWidth,
                    request.overrideHeight
                )
                options.inJustDecodeBounds = false
                
                // Try to get a bitmap from the pool to reuse
                val targetWidth = options.outWidth / options.inSampleSize
                val targetHeight = options.outHeight / options.inSampleSize
                options.inBitmap = bitmapPool?.get(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                options.inMutable = options.inBitmap == null

                // Decode with sample size
                val decodedBitmap = BitmapFactory.decodeResource(
                    context.resources,
                    resourceId,
                    options
                )

                // Scale to exact size if needed
                if (decodedBitmap != null &&
                    (decodedBitmap.width != request.overrideWidth ||
                            decodedBitmap.height != request.overrideHeight)
                ) {
                    val bitmapConfig = decodedBitmap.config ?: Bitmap.Config.ARGB_8888
                    // Try to get a bitmap from pool for scaling
                    val scaledBitmap = bitmapPool?.get(
                        request.overrideWidth,
                        request.overrideHeight,
                        bitmapConfig
                    ) ?: createBitmap(request.overrideWidth, request.overrideHeight, bitmapConfig)
                    
                    val canvas = android.graphics.Canvas(scaledBitmap)
                    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                    val srcRect = android.graphics.Rect(0, 0, decodedBitmap.width, decodedBitmap.height)
                    val dstRect = android.graphics.Rect(0, 0, request.overrideWidth, request.overrideHeight)
                    canvas.drawBitmap(decodedBitmap, srcRect, dstRect, paint)
                    
                    // Return original to pool if it was mutable and different from scaled
                    if (decodedBitmap.isMutable && decodedBitmap != scaledBitmap) {
                        bitmapPool?.put(decodedBitmap) ?: decodedBitmap.recycle()
                    }
                    
                    scaledBitmap
                } else {
                    decodedBitmap
                }
            } else {
                // Try to get bitmap from pool
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(context.resources, resourceId, options)
                
                options.inBitmap = bitmapPool?.get(
                    options.outWidth,
                    options.outHeight,
                    Bitmap.Config.ARGB_8888
                )
                options.inJustDecodeBounds = false
                options.inMutable = options.inBitmap == null
                
                BitmapFactory.decodeResource(context.resources, resourceId, options)
            }

            if (bitmap != null) {
                callback(Result.success(bitmap))
            } else {
                callback(Result.failure(Exception("Failed to decode resource")))
            }
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

/**
 * Loader for local files
 */
class FileLoader(
    private val context: Context,
    private val bitmapPool: BitmapPool?
) : Loader {

    override fun canHandle(source: Any): Boolean {
        return source is File || (source is String && !source.startsWith("http"))
    }

    override fun load(request: ImageRequest, callback: (Result<Bitmap>) -> Unit) {
        val file = when (request.source) {
            is File -> request.source
            is String -> File(request.source)
            else -> return callback(Result.failure(Exception("Invalid file source")))
        }

        if (!file.exists()) {
            callback(Result.failure(Exception("File does not exist: ${file.absolutePath}")))
            return
        }

        try {
            FileInputStream(file).use { inputStream ->
                // First get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                val bytes = inputStream.readBytes()
                BitmapFactory.decodeByteArray(/* data = */ bytes, /* offset = */
                    0, /* length = */
                    bytes.size, /* opts = */
                    options)
                
                // Try to get bitmap from pool
                options.inBitmap = bitmapPool?.get(
                    width = options.outWidth,
                    height = options.outHeight,
                    config = Bitmap.Config.ARGB_8888
                )
                options.inJustDecodeBounds = false
                options.inMutable = options.inBitmap == null
                
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (bitmap != null) {
                    callback(Result.success(bitmap))
                } else {
                    callback(Result.failure(Exception("Failed to decode file")))
                }
            }
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }
}

