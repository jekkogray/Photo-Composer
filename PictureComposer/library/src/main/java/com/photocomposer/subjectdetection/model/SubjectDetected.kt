package com.photocomposer.subjectdetection.model

import android.graphics.RectF

/**
 * Model class for results from the subject detector api
 */
data class SubjectDetected(
    val id: Int,
    val subjectLabel: String,
    val confidenceScore: Float,
    val subjectBoundingBox: RectF
)