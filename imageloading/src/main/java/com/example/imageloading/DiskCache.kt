package com.example.imageloading

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Disk cache implementation for persistent storage of images.
 * Uses file system to store decoded bitmaps, allowing images to persist
 * across app restarts.
 */
class DiskCache private constructor(
    private val cacheDir: File,
    private val maxSize: Int
) {
    
    companion object {
        private const val DEFAULT_MAX_SIZE = 50 * 1024 * 1024 // 50 MB
        
        /**
         * Create a default disk cache
         */
        fun createDefault(context: Context): DiskCache {
            val cacheDir = File(context.cacheDir, "image_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            return DiskCache(cacheDir, DEFAULT_MAX_SIZE)
        }
    }
    
    /**
     * Get a bitmap from disk cache
     */
    fun get(key: String): Bitmap? {
        val file = getFileForKey(key)
        if (!file.exists()) {
            return null
        }
        
        return try {
            FileInputStream(file).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Put a bitmap into disk cache
     */
    fun put(key: String, bitmap: Bitmap): Boolean {
        if (getCurrentSize() >= maxSize) {
            // Evict old files if cache is full
            evictOldest()
        }
        
        val file = getFileForKey(key)
        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Remove a specific entry from disk cache
     */
    fun remove(key: String): Boolean {
        val file = getFileForKey(key)
        return file.exists() && file.delete()
    }
    
    /**
     * Clear all entries from disk cache
     */
    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * Get the file for a given key
     */
    private fun getFileForKey(key: String): File {
        val hash = hashKey(key)
        return File(cacheDir, hash)
    }
    
    /**
     * Hash a key to a filename-safe string
     */
    private fun hashKey(key: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(key.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get current cache size
     */
    private fun getCurrentSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    /**
     * Evict oldest files when cache is full
     */
    private fun evictOldest() {
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() }
        files?.take(10)?.forEach { it.delete() } // Remove 10 oldest files
    }
}

