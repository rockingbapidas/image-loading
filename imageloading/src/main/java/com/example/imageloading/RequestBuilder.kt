package com.example.imageloading

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * Fluent API builder for image loading requests.
 * Similar to Glide's RequestBuilder, provides methods to configure
 * how images are loaded and displayed.
 */
class RequestBuilder(
    private val imageLoader: ImageLoader,
    private val engine: Engine,
    private val source: Any?
) {
    private var targetView: ImageView? = null
    private var placeholder: Int? = null
    private var error: Int? = null
    private var skipMemoryCache: Boolean = false
    private var skipDiskCache: Boolean = false
    private var overrideWidth: Int? = null
    private var overrideHeight: Int? = null
    private var transformation: Transformation? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var callback: ImageCallback? = null
    
    /**
     * Set the target ImageView to load the image into
     */
    fun into(imageView: ImageView): RequestBuilder {
        this.targetView = imageView
        return this
    }
    
    /**
     * Set a placeholder drawable resource to show while loading
     */
    fun placeholder(resourceId: Int): RequestBuilder {
        this.placeholder = resourceId
        return this
    }
    
    /**
     * Set an error drawable resource to show if loading fails
     */
    fun error(resourceId: Int): RequestBuilder {
        this.error = resourceId
        return this
    }
    
    /**
     * Skip memory cache for this request
     */
    fun skipMemoryCache(skip: Boolean = true): RequestBuilder {
        this.skipMemoryCache = skip
        return this
    }
    
    /**
     * Skip disk cache for this request
     */
    fun skipDiskCache(skip: Boolean = true): RequestBuilder {
        this.skipDiskCache = skip
        return this
    }
    
    /**
     * Override the image dimensions
     */
    fun override(width: Int, height: Int): RequestBuilder {
        this.overrideWidth = width
        this.overrideHeight = height
        return this
    }
    
    /**
     * Apply a transformation to the image
     */
    fun transform(transformation: Transformation): RequestBuilder {
        this.transformation = transformation
        return this
    }
    
    /**
     * Associate this request with a LifecycleOwner for automatic cancellation
     */
    fun lifecycle(lifecycleOwner: LifecycleOwner): RequestBuilder {
        this.lifecycleOwner = lifecycleOwner
        return this
    }
    
    /**
     * Set a callback for when the image loads or fails
     */
    fun callback(callback: ImageCallback): RequestBuilder {
        this.callback = callback
        return this
    }
    
    /**
     * Execute the request
     */
    fun submit() {
        if (source == null) {
            handleError("Source is null")
            return
        }
        
        val request = ImageRequest(
            source = source,
            targetView = targetView,
            placeholder = placeholder,
            error = error,
            skipMemoryCache = skipMemoryCache,
            skipDiskCache = skipDiskCache,
            overrideWidth = overrideWidth,
            overrideHeight = overrideHeight,
            transformation = transformation,
            lifecycleOwner = lifecycleOwner,
            callback = callback
        )
        
        engine.load(request)
    }
    
    private fun handleError(message: String) {
        targetView?.let { view ->
            error?.let { view.setImageResource(it) }
        }
        callback?.onError(Exception(message))
    }
}

/**
 * Callback interface for image loading events
 */
interface ImageCallback {
    fun onSuccess(bitmap: Bitmap)
    fun onError(exception: Exception)
}

/**
 * Transformation interface for applying transformations to bitmaps
 */
interface Transformation {
    fun transform(source: Bitmap): Bitmap
    fun key(): String
}

