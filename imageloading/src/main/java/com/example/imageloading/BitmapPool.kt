package com.example.imageloading

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Bitmap Pool for efficient memory management.
 * Reuses bitmap memory instead of allocating new bitmaps,
 * reducing GC pressure and improving performance.
 * 
 * This is a critical component for memory efficiency in image loading.
 */
class BitmapPool private constructor(
    private val maxSize: Int
) {
    
    companion object {
        private const val TAG = "BitmapPool"
        private const val DEFAULT_MAX_SIZE = 10 // Maximum number of bitmaps to pool
        
        /**
         * Create a default bitmap pool
         */
        fun createDefault(): BitmapPool {
            return BitmapPool(DEFAULT_MAX_SIZE)
        }
    }
    
    private val pool = ConcurrentLinkedQueue<Bitmap>()
    
    /**
     * Get a bitmap from the pool that matches the given dimensions and config.
     * Returns null if no suitable bitmap is available.
     */
    fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val bitmap = iterator.next()
            if (bitmap.isRecycled) {
                iterator.remove()
                continue
            }
            
            if (bitmap.width == width && 
                bitmap.height == height && 
                bitmap.config == config) {
                iterator.remove()
                // Reuse the bitmap by erasing its contents
                bitmap.eraseColor(0)
                return bitmap
            }
        }
        return null
    }
    
    /**
     * Put a bitmap into the pool for reuse.
     * Only mutable bitmaps can be pooled.
     */
    fun put(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || !bitmap.isMutable) {
            return false
        }
        
        if (pool.size >= maxSize) {
            // Pool is full, recycle the oldest bitmap
            pool.poll()?.recycle()
        }
        
        return pool.offer(bitmap)
    }
    
    /**
     * Clear the pool and recycle all bitmaps
     */
    fun clear() {
        pool.forEach { it.recycle() }
        pool.clear()
    }
    
    /**
     * Trim the pool to a smaller size, recycling excess bitmaps
     */
    fun trimToSize(size: Int) {
        while (pool.size > size) {
            pool.poll()?.recycle()
        }
    }
    
    /**
     * Get pool statistics
     */
    fun getStats(): Stats {
        val totalMemory = pool.sumOf { it.byteCount }
        return Stats(
            size = pool.size,
            maxSize = maxSize,
            totalMemoryBytes = totalMemory,
            totalMemoryMB = totalMemory / (1024.0 * 1024.0)
        )
    }
    
    /**
     * Pool statistics
     */
    data class Stats(
        val size: Int,
        val maxSize: Int,
        val totalMemoryBytes: Int,
        val totalMemoryMB: Double
    )
}

