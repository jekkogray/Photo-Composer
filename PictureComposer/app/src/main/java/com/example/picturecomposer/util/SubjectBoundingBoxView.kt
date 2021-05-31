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

class SubjectBoundingBoxView @JvmOverloads constructor(
    context: Context,
    private val boxInViewBoundingBox: RectF = RectF()
) : View(context) {
    private val paint: Paint = Paint().apply {
        style = Paint.Style.STROKE
    }
    private val unselectedDotRadius: Int =
        context.resources.getDimensionPixelOffset(R.dimen.subject_dot_radius_unselected)
    private val radiusOffsetRange: Int

    private var currentRadiusOffset: Float = 0.toFloat()

    init {
        val selectedDotRadius =
            context.resources.getDimensionPixelOffset(R.dimen.subject_dot_radius_selected)
        radiusOffsetRange = selectedDotRadius - unselectedDotRadius
        currentRadiusOffset = radiusOffsetRange.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = Color.parseColor("#D81C61")


        paint.strokeWidth = (unselectedDotRadius + currentRadiusOffset) / 2
        canvas.drawRoundRect(boxInViewBoundingBox, 25f, 25f, paint)
    }

}
