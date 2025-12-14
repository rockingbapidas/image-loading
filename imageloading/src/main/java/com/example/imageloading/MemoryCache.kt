package com.example.imageloading

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache

/**
 * Memory cache implementation using LRU (Least Recently Used) strategy.
 * Stores decoded bitmaps in memory for fast access.
 * Automatically evicts least recently used items when memory limit is reached.
 */
class MemoryCache private constructor(
    private val cache: LruCache<String, Bitmap>,
    private val bitmapPool: BitmapPool?
) {
    
    companion object {
        /**
         * Create a default memory cache with size based on available memory
         */
        fun createDefault(context: Context, bitmapPool: BitmapPool? = null): MemoryCache {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            // Use 1/8 of available memory for cache
            val cacheSize = maxMemory / 8
            
            val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
                override fun sizeOf(key: String, bitmap: Bitmap): Int {
                    // Return the size in kilobytes
                    return bitmap.byteCount / 1024
                }
                
                override fun entryRemoved(
                    evicted: Boolean,
                    key: String,
                    oldValue: Bitmap?,
                    newValue: Bitmap?
                ) {
                    // When bitmap is removed (evicted or manually), return it to the pool for reuse
                    if (oldValue != null && oldValue.isMutable && !oldValue.isRecycled) {
                        bitmapPool?.put(oldValue)
                    }
                }
            }
            
            return MemoryCache(lruCache, bitmapPool)
        }
    }
    
    /**
     * Get a bitmap from cache
     */
    fun get(key: String): Bitmap? {
        return cache.get(key)
    }
    
    /**
     * Put a bitmap into cache
     */
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
    
    /**
     * Remove a specific entry from cache.
     * The removed bitmap will be returned to the bitmap pool if it's mutable.
     */
    fun remove(key: String): Bitmap? {
        // cache.remove() will trigger entryRemoved callback which handles pooling
        return cache.remove(key)
    }
    
    /**
     * Clear all entries from cache.
     * All bitmaps will be returned to the bitmap pool if they're mutable.
     */
    fun clear() {
        // evictAll() will trigger entryRemoved for each entry, which handles pooling
        cache.evictAll()
    }
    
    /**
     * Get cache statistics
     */
    fun getStats(): Stats {
        return Stats(
            maxSize = cache.maxSize(),
            currentSize = cache.size(),
            hitCount = cache.hitCount(),
            missCount = cache.missCount(),
            createCount = cache.createCount(),
            putCount = cache.putCount(),
            evictionCount = cache.evictionCount()
        )
    }
    
    /**
     * Cache statistics
     */
    data class Stats(
        val maxSize: Int,
        val currentSize: Int,
        val hitCount: Int,
        val missCount: Int,
        val createCount: Int,
        val putCount: Int,
        val evictionCount: Int
    ) {
        val hitRate: Double
            get() = if (hitCount + missCount > 0) {
                hitCount.toDouble() / (hitCount + missCount)
            } else {
                0.0
            }
    }
}

