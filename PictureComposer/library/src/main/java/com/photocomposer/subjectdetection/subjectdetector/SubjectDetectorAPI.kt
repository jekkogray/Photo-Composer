package com.photocomposer.subjectdetection.subjectdetector

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import com.photocomposer.subjectdetection.model.SubjectDetected
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.util.*
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import com.photocomposer.subjectdetection.util.resize
import com.photocomposer.subjectdetection.util.translate

/**
 * Wrapper class for Classifier Interface
 */
class SubjectDetectorAPI : Detector {
    companion object {
        private const val TAG = "SubjectDetector"

        // DEFAULT VALUES
        // private const val DEFAULT_SUBJECT_DETECTION_MODEL_FILE =
        //     "subject_localizer.tflite" // Default file
        private const val DEFAULT_SUBJECT_DETECTION_MODEL_FILE =
            "default_subject_detection_model.tflite" // Default file
        private const val DEFAULT_SUBJECT_DETECTION_LABELS =
            "file:///android_asset/default_labels.txt"
        private const val DEFAULT_NUM_THREADS = 4

        private const val DEFAULT_NUM_BYTES_PER_CHANNEL = 1
        private const val DEFAULT_MAX_NUM_DETECTIONS = 10
        private const val DEFAULT_MIN_CONFIDENCE = 0.5f

        var DEFAULT_INPUT_SIZE = 300
        // var DEFAULT_INPUT_SIZE = 192

        /**
         * Loads default tflite model into memory provided by tensorflow model.
         * @param assetManager assetManager from the context
         * @return mapped byte buffer
         */
        @Throws(IOException::class)
        private fun loadDefaultModelFile(assetManager: AssetManager): MappedByteBuffer {
            val fileDescriptor = assetManager.openFd(DEFAULT_SUBJECT_DETECTION_MODEL_FILE)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }

        /**
         * Creates an instance of SubjectDetection
         * Load default labels in the model
         */
        @Throws(IOException::class)
        fun createDefault(assetManager: AssetManager): Detector {
            // Create an instance of this class
            val subjectDetector = SubjectDetectorAPI()
            // Open subject detection label
            val labelFileName =
                DEFAULT_SUBJECT_DETECTION_LABELS.split("file:///android_asset/".toRegex())
                    .toTypedArray()[1]
            val labelsFile = assetManager.open(labelFileName)
            // Read each label in the labelsFile
            BufferedReader(InputStreamReader(labelsFile)).use { BufferedReader ->
                var label: String?
                while (BufferedReader.readLine().also { label = it } != null)
                    subjectDetector.labels.add(label)
            }
            // set the properties of the tflite interpreter
            try {
                val interpreterOptions = Interpreter.Options().setNumThreads(DEFAULT_NUM_THREADS)
                subjectDetector.subjectDetectorInterpreter =
                    Interpreter(loadDefaultModelFile(assetManager), interpreterOptions)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            return subjectDetector.apply {
                //Initialize remaining lateinit var
                // Quantize model
                imgData =
                    ByteBuffer.allocateDirect(DEFAULT_INPUT_SIZE * DEFAULT_INPUT_SIZE * 3 * DEFAULT_NUM_BYTES_PER_CHANNEL)
                imgData.order(ByteOrder.nativeOrder())
                intValues = IntArray(DEFAULT_INPUT_SIZE * DEFAULT_INPUT_SIZE)
            }
        }
    }

    // Available labels for output
    private val labels = Vector<String>()
    private lateinit var subjectDetectorInterpreter: Interpreter
    private lateinit var imgData: ByteBuffer
    private lateinit var intValues: IntArray
    private lateinit var outputSubjectBoundingBox: Array<Array<FloatArray>>
    private lateinit var outputLabels: Array<FloatArray>
    private lateinit var outputConfidenceScores: Array<FloatArray>
    private lateinit var numDetections: FloatArray


    override fun detectSubjects(img: Bitmap): List<SubjectDetected> {
        val bitmapImage = img.resize()
        // Preprocess the image data form 0-255 int
        bitmapImage.getPixels(
            intValues,
            0,
            bitmapImage.width,
            0,
            0,
            bitmapImage.width,
            bitmapImage.height
        )

        imgData.rewind()
        for (i in 0 until DEFAULT_INPUT_SIZE) {
            for (j in 0 until DEFAULT_INPUT_SIZE) {
                // Traverse each pixel in the image
                // Set the value for each pixel in the image
                val pixelValue = intValues[i * DEFAULT_INPUT_SIZE + j]
                // Quantized model
                imgData.apply {
                    put((pixelValue shr 16 and 0xFF).toByte())
                    put((pixelValue shr 8 and 0xFF).toByte())
                    put((pixelValue and 0xFF).toByte())
                }
            }
        }

        // Outputs
        outputSubjectBoundingBox = Array(1) { Array(DEFAULT_MAX_NUM_DETECTIONS) { FloatArray(4) } }
        outputLabels = Array(1) { FloatArray(DEFAULT_MAX_NUM_DETECTIONS) }
        outputConfidenceScores = Array(1) { FloatArray(DEFAULT_MAX_NUM_DETECTIONS) }
        numDetections = FloatArray(1)

        val inputArray = arrayOf<Any>(imgData)
        val outputMap = mapOf(
            0 to outputSubjectBoundingBox,
            1 to outputLabels,
            2 to outputConfidenceScores,
            3 to numDetections
        )

        subjectDetectorInterpreter.runForMultipleInputsOutputs(inputArray, outputMap)
        val subjectsDetected = ArrayList<SubjectDetected>(DEFAULT_MAX_NUM_DETECTIONS)
        for (i in 0 until DEFAULT_MAX_NUM_DETECTIONS) {
            var subjectBoundingBox = RectF(
                outputSubjectBoundingBox[0][i][1] * DEFAULT_INPUT_SIZE,
                outputSubjectBoundingBox[0][i][0] * DEFAULT_INPUT_SIZE,
                outputSubjectBoundingBox[0][i][3] * DEFAULT_INPUT_SIZE,
                outputSubjectBoundingBox[0][i][2] * DEFAULT_INPUT_SIZE
            )
            // Convert the output subject bounding box according to the size of the actual image
            img.translate(subjectBoundingBox)
            val labelOffset = 1
            // The corresponding label for the output
            val labelIndex = outputLabels[0][i].toInt() + labelOffset
            val confidenceScore = outputConfidenceScores[0][i]
            val subjectLabel = labels[labelIndex]

            if (confidenceScore >= DEFAULT_MIN_CONFIDENCE) {
                subjectsDetected.add(
                    SubjectDetected(
                        id = i,
                        subjectLabel = subjectLabel,
                        confidenceScore = confidenceScore,
                        subjectBoundingBox = subjectBoundingBox
                    )
                )
            }
        }
        return subjectsDetected
    }

    override fun close() {
        subjectDetectorInterpreter.close()
    }
}