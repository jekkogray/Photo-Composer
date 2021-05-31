package com.photocomposer.subjectdetection.subjectdetector

import android.graphics.Bitmap
import com.photocomposer.subjectdetection.model.SubjectDetected
import java.util.*

interface Detector {
    /**
     * @return a list of subjects detected
     *
     */
    fun detectSubjects(img: Bitmap): List<SubjectDetected>

    /**
     * Close SubjectDetectorInterpreter after use
     */
    fun close()

}