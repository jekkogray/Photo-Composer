package com.example.picturecomposer.util

import com.example.picturecomposer.CameraActivity.Companion.capturedImage

class ProcImg {

    /* GET RULE OF THIRDS GRADE */
    fun thirds(left: Float, right: Float, top: Float, bottom: Float) : Int
    {
        var check = false

        val width = capturedImage!!.width
        val height = capturedImage!!.height

        // Find vertical and horizontal grid "lines"
        val v1 = width / 3
        val v2 = 2 * (width / 3)
        val h1 = height / 3
        val h2 = 2 * (height / 3)

        // Check if there's an intersection within the subject box
        // Vertical
        var vertical = false
        if (v1 >= left && v1 <= right || v2 >= left && v2 <= right) {
            vertical = true
        }

        // Horizontal
        var horizontal = false
        if (h1 >= top && h1 <= bottom || h2 >= top && h2 <= bottom) {
            horizontal = true
        }

        // If there is at least one vertical grid line and one horizontal gird line within the subject box
        if (horizontal && vertical) {
            check = true
        }

        // Return 1 or 5
        return if(check) {
            5
        } else {
            1
        }
    }

    /* GET FRAMING GRADE */
    fun framing(left: Float, right: Float, top: Float, bottom: Float) : Int
    {
        val width = capturedImage!!.width
        val height = capturedImage!!.height

        // Get subject dimensions
        val subWidth = left - right
        val subHeight = top - bottom

        // Get square pixels
        val subSq = (subWidth * subHeight).toDouble()
        val imgSq = (width * height).toDouble()

        // Get percentage of frame filled
        val pct = (subSq / imgSq) * 100

        // Assign grade
        val grade : Int

        when {
            pct < 20 -> {
                // Very far
                grade = 1
            }
            pct < 45 -> {
                // Far
                grade = 4
            }
            pct < 85 -> {
                // Good
                grade = 5
            }
            pct < 95 -> {
                // Close
                grade = 3
            }
            else -> {
                // Very close
                grade = 2
            }
        }

        return grade
    }

    /* GET EXPOSURE GRADE */
    fun exposure() : Int
    {
        return ExpHist.run(capturedImage)
    }

    /* GET BLUR GRADE */
    fun blur() : Int
    {
        val blurry = DetectBlur.run(capturedImage)
        if(!blurry) {
            return 5
        }
        return 1
    }

    /* GET LEVEL GRADE */
    fun level() : Int
    {
        val angle = CalcSkew.run(capturedImage)

        return when {
            angle < 3 -> {
                5
            }
            angle < 6 -> {
                4
            }
            angle < 12 -> {
                3
            }
            angle < 30 -> {
                2
            }
            else -> {
                1
            }
        }
    }

    /* GET FINAL GRADE */
    fun final (thirds: Int, framing: Int, exposure: Int, level: Int, blur: Int) : Int
    {
        // Calculate numerical grade from sub grades
        var final = 0
        val grades: Array<Int> = arrayOf(thirds, framing, exposure, level, blur)
        for(i in 0..4)
        {
            final += grades[i]
        }
        final /= 5

        // Convert to letter grade
        return final
    }
}