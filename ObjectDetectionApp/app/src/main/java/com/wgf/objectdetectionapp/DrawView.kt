package com.wgf.objectdetectionapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.defaults.PredefinedCategory

/**
 * DrawingView class:
 *    onDraw() function implements drawing
 *     - boundingBox
 *     - Category
 *     - Confidence ( if Category is not CATEGORY_UNKNOWN )
 */
class DrawingView(context: Context, var visionObjects: List<DetectedObject>) : View(context) {

    companion object {
        // mapping table for category to strings: drawing strings
        val categoryNames: Map<String, String> = mapOf(
            PredefinedCategory.HOME_GOOD to "Home Goods",
            PredefinedCategory.FASHION_GOOD to "Fashion Goods",
            PredefinedCategory.FOOD to "Food",
            PredefinedCategory.PLACE to "Place",
            PredefinedCategory.PLANT to "Plant"
        )
    }

    val MAX_FONT_SIZE = 96F

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        for (item in visionObjects) {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = item.boundingBox
            canvas.drawRect(box, pen)

            val trackingId = item.trackingId

            for(label in item.labels) {
                val text = label.text

                // Draw result category, and confidence
                val tags: MutableList<String> = mutableListOf()
                tags.add("Category: ${categoryNames[text]}")

                if (text != null) {
                    tags.add("Confidence: ${label.confidence!!.times(100).toInt()}%")
                }

                var tagSize = Rect(0, 0, 0, 0)
                var maxLen = 0
                var index: Int = -1

                for ((idx, tag) in tags.withIndex()) {
                    if (maxLen < tag.length) {
                        maxLen = tag.length
                        index = idx
                    }
                }

                // calculate the right font size
                pen.style = Paint.Style.FILL_AND_STROKE
                pen.color = Color.YELLOW
                pen.strokeWidth = 2F

                pen.textSize = MAX_FONT_SIZE
                pen.getTextBounds(tags[index], 0, tags[index].length, tagSize)
                val fontSize: Float = pen.textSize * box.width() / tagSize.width()

                // adjust the font size so texts are inside the bounding box
                if (fontSize < pen.textSize) pen.textSize = fontSize

                var margin = (box.width() - tagSize.width()) / 2.0F
                if (margin < 0F) margin = 0F

                // draw tags onto bitmap (bmp is in upside down format)
                for ((idx, txt) in tags.withIndex()) {
                    canvas.drawText(
                        txt, box.left + margin,
                        box.top + tagSize.height().times(idx + 1.0F), pen
                    )
                }
            }
        }
    }
}
