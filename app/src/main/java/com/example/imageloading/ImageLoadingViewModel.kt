package com.example.imageloading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing image loading state and cache statistics
 */
class ImageLoadingViewModel : ViewModel() {
    
    private val _cacheStats = MutableStateFlow<CacheStats?>(null)
    val cacheStats: StateFlow<CacheStats?> = _cacheStats.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        updateStats()
    }
    
    /**
     * Update cache statistics
     */
    fun updateStats() {
        viewModelScope.launch {
            try {
                val memoryStats = ImageLoader.get().getMemoryCacheStats()
                val poolStats = ImageLoader.get().getBitmapPoolStats()
                
                _cacheStats.value = CacheStats(
                    memoryMaxSize = memoryStats.maxSize,
                    memoryCurrentSize = memoryStats.currentSize,
                    memoryHitRate = memoryStats.hitRate,
                    memoryHits = memoryStats.hitCount,
                    memoryMisses = memoryStats.missCount,
                    poolSize = poolStats.size,
                    poolMaxSize = poolStats.maxSize,
                    poolMemoryMB = poolStats.totalMemoryMB
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to get stats: ${e.message}"
            }
        }
    }
    
    /**
     * Load network image
     */
    fun loadNetworkImage(imageUrl: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // Image loading is handled by the composable
                // This just updates the loading state
                updateStats()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load image: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load resource image
     */
    fun loadResourceImage(resourceId: Int) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                updateStats()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load resource: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear all caches
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                ImageLoader.get().clearMemoryCache()
                ImageLoader.get().clearDiskCache()
                updateStats()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear cache: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Data class for cache statistics
     */
    data class CacheStats(
        val memoryMaxSize: Int,
        val memoryCurrentSize: Int,
        val memoryHitRate: Double,
        val memoryHits: Int,
        val memoryMisses: Int,
        val poolSize: Int,
        val poolMaxSize: Int,
        val poolMemoryMB: Double
    )
}

