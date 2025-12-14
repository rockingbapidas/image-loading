package com.example.imageloading

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imageloading.ui.theme.ImageLoadingTheme

/**
 * Main Activity using Jetpack Compose with ViewModel
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ImageLoadingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImageLoadingScreen(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Main Compose screen
 */
@Composable
fun ImageLoadingScreen(viewModel: ImageLoadingViewModel) {
    val cacheStats by viewModel.cacheStats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Image Loading Library Demo",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Image Views
        ImageViewWithLoader(
            imageUrl = "https://picsum.photos/400/300",
            placeholder = android.R.drawable.ic_menu_gallery,
            error = android.R.drawable.ic_dialog_alert,
            onLoadStart = { viewModel.loadNetworkImage("https://picsum.photos/400/300") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        ImageViewWithLoader(
            imageUrl = null,
            resourceId = android.R.drawable.ic_menu_camera,
            overrideSize = Pair(200, 200),
            onLoadStart = { viewModel.loadResourceImage(android.R.drawable.ic_menu_camera) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        ImageViewWithLoader(
            imageUrl = "https://picsum.photos/300/300",
            placeholder = android.R.drawable.ic_menu_gallery,
            error = android.R.drawable.ic_dialog_alert,
            onLoadStart = { viewModel.loadNetworkImage("https://picsum.photos/300/300") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.loadNetworkImage("https://picsum.photos/400/300")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Load Network")
            }

            Button(
                onClick = {
                    viewModel.loadResourceImage(android.R.drawable.ic_menu_camera)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Load Resource")
            }
        }

        Button(
            onClick = { viewModel.clearCache() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Clear Cache")
        }

        // Statistics
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Cache Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                cacheStats?.let { stats ->
                    Text(
                        text = buildStatsText(stats),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                } ?: Text(
                    text = "Loading statistics...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Composable that wraps AndroidView to use ImageLoader
 */
@Composable
fun ImageViewWithLoader(
    imageUrl: String? = null,
    resourceId: Int? = null,
    placeholder: Int? = null,
    error: Int? = null,
    overrideSize: Pair<Int, Int>? = null,
    onLoadStart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(imageUrl, resourceId) {
        onLoadStart()
    }

    AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier,
        update = { imageView ->
            // Load image using ImageLoader
            val requestBuilder = ImageLoader.get().load(
                imageUrl ?: resourceId
            )

            placeholder?.let { requestBuilder.placeholder(it) }
            error?.let { requestBuilder.error(it) }
            overrideSize?.let { (width, height) ->
                requestBuilder.override(width, height)
            }

            requestBuilder
                .lifecycle(context as androidx.lifecycle.LifecycleOwner)
                .into(imageView)
                .submit()

            imageKey++ // Force recomposition on change
        }
    )
}

/**
 * Build statistics text
 */
private fun buildStatsText(stats: ImageLoadingViewModel.CacheStats): String {
    return """
        Memory Cache Stats:
        - Max Size: ${stats.memoryMaxSize} KB
        - Current Size: ${stats.memoryCurrentSize} KB
        - Hit Rate: ${String.format("%.2f", stats.memoryHitRate * 100)}%
        - Hits: ${stats.memoryHits}
        - Misses: ${stats.memoryMisses}
        
        Bitmap Pool Stats:
        - Size: ${stats.poolSize}/${stats.poolMaxSize}
        - Memory: ${String.format("%.2f", stats.poolMemoryMB)} MB
    """.trimIndent()
}
