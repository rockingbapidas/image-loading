package com.example.imageloading.transformations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.example.imageloading.Transformation
import androidx.core.graphics.createBitmap

/**
 * Example transformation that creates a circular bitmap.
 * Demonstrates how to implement custom transformations.
 */
class CircleTransformation : Transformation {
    
    override fun transform(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        
        val paint = Paint().apply {
            isAntiAlias = true
        }
        
        val rect = Rect(0, 0, size, size)
        val radius = size / 2f
        
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, rect, rect, paint)
        
        return output
    }
    
    override fun key(): String {
        return "circle"
    }
}

