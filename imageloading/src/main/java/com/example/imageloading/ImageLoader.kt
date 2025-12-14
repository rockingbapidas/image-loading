package com.example.imageloading

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.io.File

/**
 * Main entry point for the Image Loading Library.
 * Similar to Glide, provides a fluent API for loading images with caching,
 * memory management, and bitmap pooling.
 */
class ImageLoader private constructor(
    private val context: Context,
    private val memoryCache: MemoryCache,
    private val diskCache: DiskCache,
    private val bitmapPool: BitmapPool,
    private val memoryManager: MemoryManager,
    private val engine: Engine
) {
    
    companion object {
        @Volatile
        private var INSTANCE: ImageLoader? = null
        
        /**
         * Initialize the ImageLoader with default configuration.
         * Should be called in Application.onCreate()
         */
        fun init(context: Context): ImageLoader {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val bitmapPool = BitmapPool.createDefault()
                val memoryCache = MemoryCache.createDefault(appContext, bitmapPool)
                val diskCache = DiskCache.createDefault(appContext)
                val memoryManager = MemoryManager.createDefault(appContext)
                // Initialize memory manager with cache references
                memoryManager.initialize(memoryCache, bitmapPool)
                val engine = Engine(appContext, memoryCache, diskCache, bitmapPool, memoryManager)
                
                INSTANCE ?: ImageLoader(
                    appContext,
                    memoryCache,
                    diskCache,
                    bitmapPool,
                    memoryManager,
                    engine
                ).also { INSTANCE = it }
            }
        }
        
        /**
         * Get the singleton instance. Must call init() first.
         */
        fun get(): ImageLoader {
            return INSTANCE ?: throw IllegalStateException(
                "ImageLoader not initialized. Call ImageLoader.init(context) first."
            )
        }
    }
    
    /**
     * Start building a request to load an image.
     */
    fun load(source: Any?): RequestBuilder {
        return RequestBuilder(this, engine, source)
    }
    
    /**
     * Clear memory cache
     */
    fun clearMemoryCache() {
        memoryCache.clear()
    }
    
    /**
     * Clear disk cache
     */
    fun clearDiskCache() {
        diskCache.clear()
    }
    
    /**
     * Pause all pending requests
     */
    fun pauseRequests() {
        engine.pauseRequests()
    }
    
    /**
     * Resume all pending requests
     */
    fun resumeRequests() {
        engine.resumeRequests()
    }
    
    /**
     * Get memory cache statistics
     */
    fun getMemoryCacheStats(): MemoryCache.Stats {
        return memoryCache.getStats()
    }
    
    /**
     * Get bitmap pool statistics
     */
    fun getBitmapPoolStats(): BitmapPool.Stats {
        return bitmapPool.getStats()
    }
}

