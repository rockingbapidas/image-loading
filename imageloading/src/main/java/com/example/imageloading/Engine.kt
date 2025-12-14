package com.example.imageloading

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Engine that manages image loading requests.
 * Handles caching, loading from different sources, and request lifecycle.
 */
class Engine(
    context: android.content.Context,
    private val memoryCache: MemoryCache,
    private val diskCache: DiskCache,
    private val bitmapPool: BitmapPool,
    private val memoryManager: MemoryManager
) {
    
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeRequests = mutableMapOf<String, Future<*>>()
    private var paused = false
    
    private val loaders = listOf(
        NetworkLoader(context, bitmapPool),
        ResourceLoader(context, bitmapPool),
        FileLoader(context, bitmapPool)
    )
    
    /**
     * Load an image request
     */
    fun load(request: ImageRequest) {
        val cacheKey = request.getCacheKey()
        
        // Check if memory is critically low - skip memory cache if so
        val isLowMemory = memoryManager.isLowMemory()
        val shouldSkipMemoryCache = request.skipMemoryCache || isLowMemory
        
        // Show placeholder
        request.targetView?.let { view ->
            request.placeholder?.let { view.setImageResource(it) }
        }
        
        // Check memory cache first (unless memory is low)
        if (!shouldSkipMemoryCache) {
            memoryCache.get(cacheKey)?.let { bitmap ->
                deliverBitmap(request, bitmap)
                return
            }
        }
        
        // Check disk cache
        if (!request.skipDiskCache) {
            diskCache.get(cacheKey)?.let { bitmap ->
                // Put in memory cache for next time (only if memory is not low)
                if (!shouldSkipMemoryCache) {
                    memoryCache.put(cacheKey, bitmap)
                }
                deliverBitmap(request, bitmap)
                return
            }
        }
        
        // Load from source
        val future = executor.submit {
            try {
                val loader = loaders.firstOrNull { it.canHandle(request.source) }
                    ?: throw Exception("No loader found for source: ${request.source}")
                
                loader.load(request) { result ->
                    result.onSuccess { bitmap ->
                        // Apply transformation if needed
                        val finalBitmap = request.transformation?.transform(bitmap) ?: bitmap
                        
                        // If transformation created a new bitmap, return original to pool
                        if (finalBitmap != bitmap && bitmap.isMutable) {
                            bitmapPool.put(bitmap)
                        }
                        
                        // Cache the bitmap (skip memory cache if memory is low)
                        val shouldSkipMemoryCache = request.skipMemoryCache || memoryManager.isLowMemory()
                        if (!shouldSkipMemoryCache) {
                            memoryCache.put(cacheKey, finalBitmap)
                        }
                        if (!request.skipDiskCache) {
                            diskCache.put(cacheKey, finalBitmap)
                        }
                        
                        deliverBitmap(request, finalBitmap)
                        activeRequests.remove(cacheKey)
                    }.onFailure { exception ->
                        deliverError(request, exception as Exception)
                        activeRequests.remove(cacheKey)
                    }
                }
            } catch (e: Exception) {
                deliverError(request, e)
                activeRequests.remove(cacheKey)
            }
        }
        
        activeRequests[cacheKey] = future
        
        // Register lifecycle observer if provided
        request.lifecycleOwner?.let { owner ->
            owner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    cancelRequest(cacheKey)
                }
            })
        }
    }
    
    /**
     * Deliver bitmap to the target view on main thread
     */
    private fun deliverBitmap(request: ImageRequest, bitmap: Bitmap) {
        mainHandler.post {
            request.targetView?.setImageBitmap(bitmap)
            request.callback?.onSuccess(bitmap)
        }
    }
    
    /**
     * Deliver error to the target view on main thread
     */
    private fun deliverError(request: ImageRequest, exception: Exception) {
        mainHandler.post {
            request.targetView?.let { view ->
                request.error?.let { view.setImageResource(it) }
            }
            request.callback?.onError(exception)
        }
    }
    
    /**
     * Cancel a specific request
     */
    fun cancelRequest(cacheKey: String) {
        activeRequests.remove(cacheKey)?.cancel(true)
    }
    
    /**
     * Pause all pending requests
     */
    fun pauseRequests() {
        paused = true
    }
    
    /**
     * Resume all pending requests
     */
    fun resumeRequests() {
        paused = false
    }
    
    /**
     * Shutdown the engine
     */
    fun shutdown() {
        executor.shutdown()
        activeRequests.clear()
    }
}

