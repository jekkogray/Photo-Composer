
package com.example.picturecomposer.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.example.picturecomposer.R

/**
 * Draws a dot on the center of the subject's bounding box
 */

class SubjectDotView @JvmOverloads constructor(
    context: Context,
    selected: Boolean = false,
    val boxInViewBoundingBox: RectF = RectF()
) : View(context) {
    private val paint: Paint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val unselectedDotRadius: Int =
        context.resources.getDimensionPixelOffset(R.dimen.subject_dot_radius_unselected)
    private val radiusOffsetRange: Int

    private var currentRadiusOffset: Float = 0.toFloat()

    init {
        val selectedDotRadius =
            context.resources.getDimensionPixelOffset(R.dimen.subject_dot_radius_selected)
        radiusOffsetRange = selectedDotRadius - unselectedDotRadius
        currentRadiusOffset = (if (selected) radiusOffsetRange else 0).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, unselectedDotRadius + currentRadiusOffset, paint)
    }
}
