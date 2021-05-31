package com.example.picturecomposer.model

import android.graphics.Bitmap
import android.graphics.Bitmap.*
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.photocomposer.subjectdetection.model.SubjectDetected
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.roundToInt


/**
 * A wrapper class for com.photocomposer.subjectdetection.model.SubjectDetected
 * This wrapper class provides thumbnail for
 */

class PCSubjectDetected(
    @Suppress("UNUSED_PARAMETER") subjectDetected: SubjectDetected,
    private val boundingBox: RectF,
    private val image: InputImage
) {
    private var bitmap: Bitmap? = null
    private var jpegBytes: ByteArray? = null

    val subjectBoundingBox: RectF = boundingBox

    // When this variable is referenced it calls the getter function.
    val imageData: ByteArray?
        @Synchronized get() {
            // imageData not stored yet.
            if (jpegBytes == null) {
                try {
                    // get the byteArray from stream.
                    ByteArrayOutputStream().use { outputStream ->
                        // We want a JPEG consumes less space.
                        getCroppedBitmap().compress(CompressFormat.JPEG, 100, outputStream)
                        // Store the array of bytes stored in outputStream to jpegBytes.
                        jpegBytes = outputStream.toByteArray()
                    }
                } catch (e: IOException) { }
            }
            return jpegBytes
        }

    @Synchronized
    fun getCroppedBitmap(): Bitmap {
        // ensure bitmap exists before performing function
        return bitmap ?: let {

            val createdBitmap = createBitmap(
                image.bitmapInternal!!,
                // Uncomment this if the bitmap do not look right.
               if (subjectBoundingBox.left < 0 ||(subjectBoundingBox.left + subjectBoundingBox.width() > image.width) ) 0 else subjectBoundingBox.left.roundToInt(),
                if (subjectBoundingBox.top < 0 || (subjectBoundingBox.top + subjectBoundingBox.height() > image.height)) 0 else boundingBox.top.roundToInt(),
                subjectBoundingBox.width().roundToInt(),
                subjectBoundingBox.height().roundToInt()
            )

            // Scale thumbnail to the maximum size of the thumbnail preview
            if (createdBitmap.width > MAX_THUMBNAIL_WIDTH) {
                // The scale factor
                val dstHeight =
                    (MAX_THUMBNAIL_WIDTH.toFloat() / createdBitmap.width * createdBitmap.height).toInt()
                bitmap =
                    createScaledBitmap(createdBitmap, MAX_THUMBNAIL_WIDTH, dstHeight, false)
            }
            createdBitmap
        }!!
    }

    companion object {
        private const val MAX_THUMBNAIL_WIDTH = 640
    }
}