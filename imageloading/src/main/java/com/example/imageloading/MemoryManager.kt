package com.example.imageloading

import android.app.ActivityManager
import android.content.Context
import android.content.ComponentCallbacks2
import android.content.res.Configuration

/**
 * Memory Manager for tracking and managing memory usage.
 * Monitors system memory pressure and responds accordingly by
 * clearing caches and trimming pools when memory is low.
 */
class MemoryManager private constructor(
    private val context: Context,
    memoryCache: MemoryCache?,
    bitmapPool: BitmapPool?
) : ComponentCallbacks2 {
    
    companion object {
        /**
         * Create a default memory manager
         */
        fun createDefault(context: Context): MemoryManager {
            // This will be initialized with caches after they're created
            return MemoryManager(context, null, null)
        }
    }
    
    private var memoryCacheRef: MemoryCache? = memoryCache
    private var bitmapPoolRef: BitmapPool? = bitmapPool
    
    /**
     * Initialize with cache references (called after caches are created)
     */
    fun initialize(memoryCache: MemoryCache, bitmapPool: BitmapPool) {
        this.memoryCacheRef = memoryCache
        this.bitmapPoolRef = bitmapPool
        context.registerComponentCallbacks(this)
    }
    
    /**
     * Get available memory in MB
     */
    fun getAvailableMemoryMB(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }
    
    /**
     * Get total memory in MB
     */
    fun getTotalMemoryMB(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }
    
    /**
     * Check if device is low on memory
     */
    fun isLowMemory(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }
    
    /**
     * Handle memory pressure by clearing caches
     */
    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // Critical memory pressure - clear everything
                memoryCacheRef?.clear()
                bitmapPoolRef?.clear()
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                // Moderate pressure - trim pools
                bitmapPoolRef?.trimToSize(5)
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // UI is hidden - light trim
                bitmapPoolRef?.trimToSize(7)
            }

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                TODO()
            }

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                TODO()
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        // No action needed
    }
    
    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        // Clear all caches on low memory
        memoryCacheRef?.clear()
        bitmapPoolRef?.clear()
    }
}

