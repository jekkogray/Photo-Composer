package com.photocomposer.subjectdetection.util

import android.graphics.Bitmap
import android.graphics.RectF
import com.photocomposer.subjectdetection.subjectdetector.SubjectDetectorAPI
import com.photocomposer.subjectdetection.subjectdetector.SubjectDetectorLocalizerAPI
import javax.security.auth.Subject

/**
 * Accepts a bitmap as an argument
 * @return resized bitmap
 * @receiver bitmap image
 */
fun Bitmap.resize(): Bitmap = Bitmap.createScaledBitmap(this,
    SubjectDetectorAPI.DEFAULT_INPUT_SIZE,
    SubjectDetectorAPI.DEFAULT_INPUT_SIZE,
    false)

/**
 * Translate the subjectDetected bounding box to match the original bounding box
 * Manipulates the subjectDetected object
 * Mutator function
 * @param subjectDetected object location based on the input size
 * @receiver original bitmap image
 */
fun Bitmap.translate(subjectDetected: RectF) {
    val originalX = width
    val originalY = height
    val ratioX = originalX / SubjectDetectorAPI.DEFAULT_INPUT_SIZE.toFloat()
    val ratioY = originalY / SubjectDetectorAPI.DEFAULT_INPUT_SIZE.toFloat()
    subjectDetected.apply {
        top *= ratioY
        bottom *= ratioY
        left *= ratioX
        right *= ratioX
    }
}


fun Bitmap.resize2(): Bitmap = Bitmap.createScaledBitmap(this,
    SubjectDetectorLocalizerAPI.DEFAULT_INPUT_SIZE,
    SubjectDetectorLocalizerAPI.DEFAULT_INPUT_SIZE,
    false)

/**
 * Translate the subjectDetected bounding box to match the original bounding box
 * Manipulates the subjectDetected object
 * Mutator function
 * @param subjectDetected object location based on the input size
 * @receiver original bitmap image
 */
fun Bitmap.translate2(subjectDetected: RectF) {
    val originalX = width
    val originalY = height
    val ratioX = originalX / SubjectDetectorLocalizerAPI.DEFAULT_INPUT_SIZE.toFloat()
    val ratioY = originalY / SubjectDetectorLocalizerAPI.DEFAULT_INPUT_SIZE.toFloat()
    subjectDetected.apply {
        top *= ratioY
        bottom *= ratioY
        left *= ratioX
        right *= ratioX
    }
}