package com.project.salbabida.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.project.salbabida.data.database.entities.MarkerCategory

/**
 * Caches marker icon bitmaps by category to avoid re-creating Bitmaps on every
 * recomposition. There are only 4 categories + 1 home icon, keeping memory minimal.
 */
object MarkerIconCache {

    private val cache = mutableMapOf<String, Drawable>()

    fun getMarkerIcon(context: Context, category: MarkerCategory, isSelected: Boolean): Drawable {
        val key = "${category.name}_${isSelected}"
        return cache.getOrPut(key) {
            val color = when (category) {
                MarkerCategory.EVACUATION_CENTER -> android.graphics.Color.parseColor("#1976D2")
                MarkerCategory.FLOOD_ZONE -> android.graphics.Color.parseColor("#D32F2F")
                MarkerCategory.SAFE_AREA -> android.graphics.Color.parseColor("#2E7D32")
                MarkerCategory.RESOURCE_CENTER -> android.graphics.Color.parseColor("#F9A825")
            }
            createPinDrawable(context, color, if (isSelected) 1.2f else 1f)
        }
    }

    fun getHomeIcon(context: Context): Drawable {
        return cache.getOrPut("HOME") {
            createPinDrawable(context, android.graphics.Color.parseColor("#3F51B5"), 1.1f)
        }
    }

    fun clear() {
        cache.clear()
    }

    private fun createPinDrawable(context: Context, color: Int, scale: Float): Drawable {
        val pinWidth = (48 * scale).toInt()
        val pinHeight = (64 * scale).toInt()
        val bitmap = Bitmap.createBitmap(pinWidth, pinHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val path = Path()
        val centerX = pinWidth / 2f
        val circleRadius = pinWidth / 2.5f
        val circleY = circleRadius + 4
        path.addCircle(centerX, circleY, circleRadius, Path.Direction.CW)

        val pointPath = Path()
        pointPath.moveTo(centerX - circleRadius * 0.6f, circleY + circleRadius * 0.5f)
        pointPath.lineTo(centerX, pinHeight.toFloat() - 4)
        pointPath.lineTo(centerX + circleRadius * 0.6f, circleY + circleRadius * 0.5f)
        pointPath.close()

        // Shadow
        paint.color = android.graphics.Color.argb(80, 0, 0, 0)
        canvas.save()
        canvas.translate(2f, 3f)
        canvas.drawPath(path, paint)
        canvas.drawPath(pointPath, paint)
        canvas.restore()

        // Body
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        canvas.drawPath(pointPath, paint)

        // Border
        paint.color = android.graphics.Color.argb(100, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawPath(path, paint)
        canvas.drawPath(pointPath, paint)

        // Dot
        paint.color = android.graphics.Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, circleY, circleRadius * 0.35f, paint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
