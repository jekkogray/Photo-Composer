@file:Suppress("DEPRECATION")

package com.example.picturecomposer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*

object ImageUtility {

    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun byteArrayToBitmap(src: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(src, 0, src.size)
    }

    fun rotateBitmap(image: Bitmap?, rotationDegree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(rotationDegree)
        val scaledBitmap =
            image?.let { Bitmap.createScaledBitmap(it, image.width, image.height, true) }
        return scaledBitmap?.let {
            Bitmap.createBitmap(
                it,
                0,
                0,
                scaledBitmap.width,
                scaledBitmap.height,
                matrix,
                true
            )
        }
    }

    fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        // Generate unique ID
        val uniqueFileID = UUID.randomUUID().toString()
        val path =
            MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, uniqueFileID, null)

        return Uri.parse(path.toString())
    }


}