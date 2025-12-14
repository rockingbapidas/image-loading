# Image Loading Library

A high-performance Android image loading library inspired by Glide, emphasizing essential mobile concepts like **Caching**, **Memory Management**, and **Bitmap Pooling**.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Key Features](#key-features)
- [Getting Started](#getting-started)
- [Core Components](#core-components)
- [Image Loading Flow](#image-loading-flow)
- [Memory Management](#memory-management)
- [Caching Strategy](#caching-strategy)
- [Best Practices](#best-practices)
- [API Reference](#api-reference)

## 🎯 Overview

This library provides a fluent API for loading images from various sources (network, resources, files) with intelligent caching, memory management, and bitmap reuse. It's designed to be memory-efficient and prevent OutOfMemoryErrors while maintaining excellent performance.

### Key Highlights

- ✅ **Dual Caching**: Memory cache (LRU) + Disk cache (persistent)
- ✅ **Bitmap Pooling**: Reuses bitmap memory to reduce GC pressure
- ✅ **Memory Management**: Automatic response to system memory pressure
- ✅ **Lifecycle Aware**: Automatic request cancellation
- ✅ **Multiple Sources**: Network URLs, Android resources, local files
- ✅ **Transformations**: Support for custom bitmap transformations

## 🏗️ Architecture

The library follows a modular architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      ImageLoader (Singleton)                 │
│                  Main Entry Point & API                      │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ RequestBuilder│ │    Engine    │ │ MemoryManager│
│  Fluent API   │ │  Orchestrator│ │  Memory Watch│
└───────┬───────┘ └──────┬───────┘ └──────┬───────┘
        │                 │                 │
        │                 │                 │
        ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────┐
│                    Core Components                           │
├──────────────┬──────────────┬──────────────┬─────────────────┤
│ MemoryCache  │  DiskCache  │ BitmapPool  │    Loaders       │
│   (LRU)      │  (File)     │  (Reuse)    │ Network/Resource │
└──────────────┴──────────────┴──────────────┴─────────────────┘
```

## ✨ Key Features

### 1. **Dual-Layer Caching**

#### Memory Cache (LRU)
- Fast in-memory storage for decoded bitmaps
- Automatically evicts least recently used items
- Size: 1/8 of available memory by default
- Provides hit/miss statistics

#### Disk Cache
- Persistent file-based storage
- Survives app restarts
- Size: 50MB by default
- MD5 hashing for cache keys

### 2. **Bitmap Pooling**

- Reuses bitmap memory instead of allocating new ones
- Reduces garbage collection pressure
- Improves performance for frequent image loads
- Automatically manages pool size based on memory pressure

### 3. **Memory Management**

- Monitors system memory pressure via `ComponentCallbacks2`
- Automatically clears caches when memory is low
- Trims bitmap pool based on memory state
- Prevents OutOfMemoryErrors

### 4. **Lifecycle Awareness**

- Automatically cancels requests when activities are destroyed
- Prevents memory leaks from pending requests
- Integrates with Android Lifecycle components

## 🚀 Getting Started

### 1. Initialize in Application

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ImageLoader.init(this)
    }
}
```

### 2. Basic Usage

```kotlin
// Load from network
ImageLoader.get()
    .load("https://example.com/image.jpg")
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .lifecycle(this)
    .into(imageView)
    .submit()

// Load from resources
ImageLoader.get()
    .load(R.drawable.my_image)
    .override(200, 200)
    .into(imageView)
    .submit()

// Load with callback
ImageLoader.get()
    .load("https://example.com/image.jpg")
    .callback(object : ImageCallback {
        override fun onSuccess(bitmap: Bitmap) {
            // Handle success
        }
        override fun onError(exception: Exception) {
            // Handle error
        }
    })
    .into(imageView)
    .submit()
```

### 3. Jetpack Compose Usage

```kotlin
@Composable
fun ImageViewWithLoader(imageUrl: String) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            ImageLoader.get()
                .load(imageUrl)
                .lifecycle(context as LifecycleOwner)
                .into(imageView)
                .submit()
        }
    )
}
```

## 🔧 Core Components

### ImageLoader

**Purpose**: Singleton entry point for the library

**Responsibilities**:
- Initialize and manage core components
- Provide fluent API via `RequestBuilder`
- Expose cache management methods

**Key Methods**:
```kotlin
ImageLoader.init(context)        // Initialize
ImageLoader.get()                 // Get instance
ImageLoader.get().load(source)    // Start request
ImageLoader.get().clearMemoryCache()
ImageLoader.get().clearDiskCache()
```

### RequestBuilder

**Purpose**: Fluent API for building image requests

**Features**:
- Method chaining for configuration
- Placeholder and error handling
- Size overrides
- Transformations
- Lifecycle integration

**Example**:
```kotlin
ImageLoader.get()
    .load(url)
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .override(300, 300)
    .transform(CircleTransformation())
    .skipMemoryCache(false)
    .skipDiskCache(false)
    .lifecycle(this)
    .callback(callback)
    .into(imageView)
    .submit()
```

### Engine

**Purpose**: Orchestrates image loading process

**Responsibilities**:
- Manage request lifecycle
- Coordinate caching layers
- Handle loaders (Network, Resource, File)
- Apply transformations
- Deliver results to UI thread

**Flow**:
1. Check memory cache
2. Check disk cache
3. Load from source if needed
4. Apply transformations
5. Cache results
6. Deliver to UI

### MemoryCache

**Purpose**: Fast in-memory storage using LRU strategy

**Implementation**:
- Uses Android's `LruCache`
- Size: 1/8 of available memory
- Automatically evicts least recently used items
- Returns evicted bitmaps to BitmapPool

**Key Features**:
- Hit/miss statistics
- Automatic eviction
- Bitmap pooling integration

### DiskCache

**Purpose**: Persistent file-based storage

**Implementation**:
- Stores bitmaps as PNG files
- MD5 hashing for cache keys
- Automatic eviction when full
- Survives app restarts

**Key Features**:
- 50MB default size
- LRU-based eviction
- File system based

### BitmapPool

**Purpose**: Reuse bitmap memory to reduce allocations

**Implementation**:
- Thread-safe queue of reusable bitmaps
- Matches bitmaps by dimensions and config
- Automatic trimming on memory pressure
- Returns bitmaps to pool when evicted from cache

**Key Features**:
- Reduces GC pressure
- Improves performance
- Automatic memory management

### MemoryManager

**Purpose**: Monitor and respond to system memory pressure

**Implementation**:
- Implements `ComponentCallbacks2`
- Receives system memory callbacks
- Automatically clears/trims caches
- Provides memory statistics

**Memory Pressure Levels**:
- **Critical**: Clear all caches
- **Moderate**: Trim bitmap pool
- **UI Hidden**: Light trim

### Loaders

**Purpose**: Load images from different sources

#### NetworkLoader
- Loads images from HTTP/HTTPS URLs
- Handles connection timeouts
- Supports size overrides with sampling
- Uses bitmap pool for decoding

#### ResourceLoader
- Loads Android drawable resources
- Supports size overrides
- Efficient resource decoding
- Bitmap pool integration

#### FileLoader
- Loads images from local files
- File existence validation
- Bitmap pool support

## 📊 Image Loading Flow

```
User Request
    │
    ▼
RequestBuilder
    │
    ▼
Engine.load()
    │
    ├─► Check Memory Cache ──► Hit? ──► Deliver to UI
    │                              │
    │                              └─► Miss
    │
    ├─► Check Disk Cache ────► Hit? ──► Put in Memory Cache ──► Deliver to UI
    │                              │
    │                              └─► Miss
    │
    └─► Load from Source
            │
            ├─► NetworkLoader
            ├─► ResourceLoader
            └─► FileLoader
            │
            ├─► Try BitmapPool (inBitmap)
            ├─► Decode Image
            ├─► Apply Transformation
            ├─► Cache Results
            └─► Deliver to UI
```

## 🧠 Memory Management

### Memory Hierarchy

```
System Memory
    │
    ├─► MemoryCache (1/8 of available)
    │       │
    │       └─► Evicted Bitmaps ──► BitmapPool
    │
    ├─► BitmapPool (10 bitmaps max)
    │       │
    │       └─► Reused for new decodes
    │
    └─► Active Bitmaps (in use)
```

### Memory Pressure Response

```
System: onTrimMemory(TRIM_MEMORY_RUNNING_CRITICAL)
    │
    ▼
MemoryManager.onTrimMemory()
    │
    ├─► Clear MemoryCache
    └─► Clear BitmapPool

System: onTrimMemory(TRIM_MEMORY_MODERATE)
    │
    ▼
MemoryManager.onTrimMemory()
    │
    └─► Trim BitmapPool to 5 items

System: onTrimMemory(TRIM_MEMORY_UI_HIDDEN)
    │
    ▼
MemoryManager.onTrimMemory()
    │
    └─► Trim BitmapPool to 7 items
```

### Bitmap Pool Integration

1. **When decoding**: Try to get bitmap from pool
2. **If found**: Use `inBitmap` in `BitmapFactory.Options`
3. **If fails**: Fall back to new allocation
4. **When evicted from cache**: Return to pool
5. **When memory pressure**: Trim pool size

## 💾 Caching Strategy

### Cache Key Generation

```kotlin
cacheKey = sourceKey + sizeKey + transformKey

Examples:
- "https://example.com/image.jpg"
- "https://example.com/image.jpg300x300"
- "https://example.com/image.jpg300x300circle"
```

### Cache Lookup Order

1. **Memory Cache** (fastest, ~0ms)
   - Check if bitmap exists in memory
   - Return immediately if found

2. **Disk Cache** (fast, ~10-50ms)
   - Check if cached file exists
   - Decode from disk
   - Put in memory cache for next time

3. **Source** (slowest, ~100-1000ms)
   - Load from network/resource/file
   - Decode bitmap
   - Cache in both memory and disk

### Cache Eviction

- **Memory Cache**: LRU eviction when size limit reached
- **Disk Cache**: Evict oldest files when size limit reached
- **Bitmap Pool**: Evict oldest when pool is full

## 📝 Best Practices

### 1. Always Initialize in Application

```kotlin
// ✅ Good
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ImageLoader.init(this)
    }
}

// ❌ Bad - Don't initialize in Activity
```

### 2. Use Lifecycle Awareness

```kotlin
// ✅ Good - Automatic cancellation
ImageLoader.get()
    .load(url)
    .lifecycle(this)  // Cancels when activity destroyed
    .into(imageView)
    .submit()

// ❌ Bad - May leak memory
ImageLoader.get()
    .load(url)
    .into(imageView)
    .submit()
```

### 3. Provide Placeholders and Errors

```kotlin
// ✅ Good
ImageLoader.get()
    .load(url)
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .into(imageView)
    .submit()
```

### 4. Use Size Overrides for Large Images

```kotlin
// ✅ Good - Reduces memory usage
ImageLoader.get()
    .load(url)
    .override(300, 300)  // Load smaller version
    .into(imageView)
    .submit()

// ❌ Bad - May cause OOM with large images
ImageLoader.get()
    .load(url)
    .into(imageView)
    .submit()
```

### 5. Clear Caches Appropriately

```kotlin
// On logout or when user changes
ImageLoader.get().clearMemoryCache()
ImageLoader.get().clearDiskCache()
```

## 📚 API Reference

### ImageLoader

| Method | Description |
|--------|-------------|
| `init(context)` | Initialize the library |
| `get()` | Get singleton instance |
| `load(source)` | Start building a request |
| `clearMemoryCache()` | Clear memory cache |
| `clearDiskCache()` | Clear disk cache |
| `pauseRequests()` | Pause all requests |
| `resumeRequests()` | Resume all requests |
| `getMemoryCacheStats()` | Get cache statistics |
| `getBitmapPoolStats()` | Get pool statistics |

### RequestBuilder

| Method | Description |
|--------|-------------|
| `into(imageView)` | Set target ImageView |
| `placeholder(resId)` | Set placeholder drawable |
| `error(resId)` | Set error drawable |
| `override(width, height)` | Override image dimensions |
| `transform(transformation)` | Apply transformation |
| `skipMemoryCache(skip)` | Skip memory cache |
| `skipDiskCache(skip)` | Skip disk cache |
| `lifecycle(owner)` | Associate with lifecycle |
| `callback(callback)` | Set loading callback |
| `submit()` | Execute the request |

### Transformations

Create custom transformations by implementing `Transformation`:

```kotlin
class MyTransformation : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        // Apply transformation
        return transformedBitmap
    }
    
    override fun key(): String {
        return "my_transformation"
    }
}
```

## 🔍 Troubleshooting

### Images Not Loading

1. **Check Internet Permission**
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

2. **Check Logcat** for error messages
3. **Verify URL** is accessible
4. **Check Memory** - may be low on memory

### OutOfMemoryError

1. Use size overrides: `.override(width, height)`
2. Clear caches: `ImageLoader.get().clearMemoryCache()`
3. Check memory statistics: `ImageLoader.get().getMemoryCacheStats()`

### "Image decoding logging dropped!"

This occurs when bitmap decoding fails. The library now handles this automatically by:
- Falling back to new allocation if pool bitmap fails
- Logging errors for debugging
- Ensuring decoding always succeeds

## 📦 Module Structure

```
imageloading/
├── src/main/java/com/example/imageloading/
│   ├── ImageLoader.kt           # Main entry point
│   ├── RequestBuilder.kt         # Fluent API
│   ├── Engine.kt                 # Request orchestrator
│   ├── ImageRequest.kt           # Request model
│   ├── MemoryCache.kt            # LRU memory cache
│   ├── DiskCache.kt              # File-based cache
│   ├── BitmapPool.kt             # Bitmap reuse pool
│   ├── MemoryManager.kt          # Memory pressure handler
│   ├── Loader.kt                 # Source loaders
│   └── transformations/
│       └── CircleTransformation.kt
```

## 🎓 Learning Resources

### Key Concepts

1. **LRU Cache**: Least Recently Used eviction strategy
2. **Bitmap Pooling**: Reusing bitmap memory
3. **Memory Pressure**: System callbacks for low memory
4. **Lifecycle Awareness**: Automatic cleanup
5. **BitmapFactory.Options**: Efficient bitmap decoding

### Android Documentation

- [Managing Bitmap Memory](https://developer.android.com/topic/performance/graphics/manage-memory)
- [Caching Bitmaps](https://developer.android.com/topic/performance/graphics/cache-bitmap)
- [ComponentCallbacks2](https://developer.android.com/reference/android/content/ComponentCallbacks2)

## 📄 License

This project is for educational purposes, demonstrating image loading library architecture and best practices.

## 🤝 Contributing

This is an educational project. Feel free to use it as a reference for building your own image loading library.

---

**Built with ❤️ to demonstrate essential Android image loading concepts**
