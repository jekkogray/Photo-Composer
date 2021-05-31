package com.example.picturecomposer

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.example.picturecomposer.CameraActivity.Companion.capturedImage
import com.example.picturecomposer.model.PCSubjectDetected
import com.example.picturecomposer.util.ImageUtility
import com.example.picturecomposer.util.ProcImg
import com.example.picturecomposer.util.SubjectBoundingBoxView
import com.example.picturecomposer.util.SubjectDotView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.photocomposer.subjectdetection.model.SubjectDetected
import com.photocomposer.subjectdetection.subjectdetector.SubjectDetectorLocalizerAPI
import org.opencv.android.OpenCVLoader
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
class ResultsActivity : AppCompatActivity() {
    companion object {
        val TAG = ResultsActivity::class.java.simpleName
    }

    // UI Components
    private lateinit var categoryTextView: TextView
    private lateinit var viewGradesButton: Button
    private lateinit var revealSubjectsButton: ToggleButton
    private lateinit var backButton: FloatingActionButton
    private lateinit var helpButton: FloatingActionButton
    private lateinit var inputImageView: ImageView
    private var captureMode: Boolean = false
    private var uploadMode: Boolean = false

    private lateinit var storage: FirebaseStorage
    private lateinit var firebaseDatabase: FirebaseDatabase

    private lateinit var subjectOverlayViewContainer: ViewGroup
    private var dotViewSize: Int = 0

    private val pickPhotoCode = 1046

    private var selectedSubjectLabel = "Unknown"
    private var selectedSubjectBoundingBox: RectF = RectF()
    private var selectedSubjectConfidenceScore: Float = 0.0f

    private val subjectDetector by lazy { SubjectDetectorLocalizerAPI.createDefault(assets) }
    private lateinit var sharedPreferences: SharedPreferences

    // Grading
    private var calcThirds = 0
    private var calcFraming = 0
    private var calcExposure = 0
    private var calcLevel = 0
    private var calcBlur = 0
    private var calcFinal = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        // Initialize UI components
        viewGradesButton = findViewById(R.id.view_grades_button)
        backButton = findViewById(R.id.back_button)
        inputImageView = findViewById(R.id.input_image_view)
        categoryTextView = findViewById(R.id.category_text_view)
        revealSubjectsButton = findViewById(R.id.reveal_subjects_button)
        helpButton = findViewById(R.id.help_button)

        // Initialize subject detection UI components
        subjectOverlayViewContainer = findViewById(R.id.subject_overlay_view_container)
        dotViewSize = resources.getDimensionPixelOffset(R.dimen.subject_image_dot_size)

        firebaseDatabase = FirebaseDatabase.getInstance()

        // Determine the source activity
        captureMode = intent.getBooleanExtra("CAPTURE_MODE", false)
        uploadMode = intent.getBooleanExtra("UPLOAD_MODE", false)

        // Initialize appropriate intents for corresponding mode
        if (captureMode) {
            backButton.setOnClickListener {
                val intent = Intent(this.applicationContext, CameraActivity::class.java)
                capturedImage?.recycle()
                startActivity(intent)
            }
        }

        if (uploadMode) {
            backButton.setOnClickListener {
                val intent = Intent(this.applicationContext, NavigationActivity::class.java)
                intent.putExtra("CAPTURE_MODE", false).putExtra("UPLOAD_MODE", true)
                capturedImage?.recycle()
                startActivity(intent)
            }
        }

        //get shared preferences
        sharedPreferences = getSharedPreferences("photo-composer", Context.MODE_PRIVATE)
        // Check OpenCV setup
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCVLoader.initDebug(), not working.")
            exitProcess(0)
        } else {
            Log.d(TAG, "OpenCVLoader.initDebug(), working.")
        }
        runSubjectDetection()
        viewGradesButtonInitializeListener()
        helpButtonInitializeListener()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && requestCode == pickPhotoCode) {
            //get photo Uri first
            val photoUri = data.data

            //Uri to bitmap conversion
            var selectedImage: Bitmap? = null
            try {
                // check version of Android on device
                selectedImage = if (Build.VERSION.SDK_INT >= 29) { //changed from 28
                    // on newer versions of Android, use the new decodeBitmap method
                    val source = ImageDecoder.createSource(this.contentResolver, photoUri!!)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    // support older versions of Android by using getBitmap
                    MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            //store this photo in global variable
            capturedImage = selectedImage

            //head to results activity to display selected photo
            val intent = Intent(
                applicationContext,
                ResultsActivity::class.java
            ).putExtra("CAPTURE_MODE", false).putExtra("UPLOAD_MODE", true)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        if (captureMode) {
            val intent = Intent(this.applicationContext, CameraActivity::class.java)
            capturedImage?.recycle()
            startActivity(intent)
        }

        if (uploadMode) {
            val intent = Intent(this.applicationContext, NavigationActivity::class.java)
            capturedImage?.recycle()
            startActivity(intent)
        }
    }

    private fun helpButtonInitializeListener() {
        helpButton.setOnClickListener {
            val helpDialog = AlertDialog.Builder(this)
            helpDialog.setMessage("Reveal Subjects Button reveals the selectable subjects in the image.\n\n Tap on the dot in the image to select your subject.\n\n Tap and hold on the dot to reveal related labels.")
                .setPositiveButton("Ok"
                ) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
            helpDialog.create().show()
        }
    }


    /**
     * Confirms the image for upload, and sends metadata.
     */
    private fun viewGradesButtonInitializeListener() {
        viewGradesButton.setOnClickListener {

            // Process img
            calcThirds = ProcImg().thirds(
                selectedSubjectBoundingBox.left,
                selectedSubjectBoundingBox.right,
                selectedSubjectBoundingBox.top,
                selectedSubjectBoundingBox.bottom
            )
            calcFraming = ProcImg().framing(
                selectedSubjectBoundingBox.left,
                selectedSubjectBoundingBox.right,
                selectedSubjectBoundingBox.top,
                selectedSubjectBoundingBox.bottom
            )
            calcExposure = ProcImg().exposure()
            calcBlur = ProcImg().blur()
            calcLevel = ProcImg().level()
            calcFinal = ProcImg().final(
                calcThirds,
                calcFraming,
                calcExposure,
                calcLevel,
                calcBlur
            )

            Toast.makeText(applicationContext, "Uploading image...", Toast.LENGTH_LONG).show()
            storage = Firebase.storage

            // Create a storage reference from our app
            val storageRef = storage.reference

            // Create a reference to image file in a directory named as the UID
            val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
            val imageRef = storageRef.child(
                "$currentUserID/${
                ImageUtility.getImageUriFromBitmap(
                    applicationContext,
                    capturedImage!!
                ).lastPathSegment
                }"
            )

            // Create task to upload image Uri
            val uploadTask =
                imageRef.putFile(ImageUtility.getImageUriFromBitmap(applicationContext, capturedImage!!))
            // If the photo successfully uploads to cloud storage and a downloadUrl was generated for it
            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener {
                    // Toast for the successful upload and URL retrieval
                    Toast.makeText(
                        applicationContext,
                        "Photo uploaded successfully!",
                        Toast.LENGTH_LONG
                    )
                        .show()

                    // Find where to put the photo in the db
                    val currUserID = FirebaseAuth.getInstance().currentUser!!.uid
                    val currPhotoKeyRef =
                        firebaseDatabase.getReference("UID/$currUserID/currPhotoKey")
                    val idReference = firebaseDatabase.getReference("UID/$currUserID/email")
                    val photoListReference =
                        firebaseDatabase.getReference("UID/$currUserID/photoList")

                    // Create a photo object with all of the photo attributes
                    val newPhoto = Photo(
                        it.toString(),
                        selectedSubjectLabel,
                        selectedSubjectBoundingBox.left,
                        selectedSubjectBoundingBox.top,
                        selectedSubjectBoundingBox.right,
                        selectedSubjectBoundingBox.bottom,
                        calcThirds,
                        calcFraming,
                        calcExposure,
                        calcLevel,
                        calcBlur,
                        calcFinal,
                        "tempKey"
                    )

                    // If this is the user's first photo, initialize their email in the db
                    idReference.setValue(FirebaseAuth.getInstance().currentUser!!.email!!)

                    // Push the new photo with all of its metadata to the appropriate spot in the db
                    val key = photoListReference.push().key
                    firebaseDatabase.getReference("UID/$currUserID/photoList/$key")
                        .setValue(newPhoto)

                    // have currPhotoKey point to this photo so we know it's the one to display on grading screen
                    firebaseDatabase.getReference("UID/$currUserID/photoList/$key/key")
                        .setValue(key)
                    currPhotoKeyRef.setValue(key)

                    //head to the grading screen now and pass along the subject word
                    val intent = Intent(applicationContext, GradeActivity::class.java)
                    startActivity(intent)
                }
                    .addOnFailureListener {
                        // Since the image failed to upload we want to enable the button again.
                        Toast.makeText(
                            applicationContext,
                            "Unable to upload image please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }


        categoryTextView.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (categoryTextView.text.toString() != ""){
                    viewGradesButton.isEnabled = true
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (categoryTextView.text.toString() == ""){
                    viewGradesButton.isEnabled = false
                }
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (categoryTextView.text.toString() != ""){
                    viewGradesButton.isEnabled = true
                }
            }
        })
    }


    /**
     * Runs subject detection on the provided image from CameraActivity
     */
    private fun runSubjectDetection() {
        capturedImage?.let {
            // Required format for images is ARGB_8888, otherwise will throw Config#Hardware getPixels error.
            val image = it.copy(
                Bitmap.Config.ARGB_8888,
                true
            )
            inputImageView.setImageBitmap(image)

            val detectedSubjectsList =
                image.let { inputImage -> subjectDetector.detectSubjects(inputImage) }

            selectedSubjectBoundingBox =
                detectedSubjectsList.maxBy { detectedSubject -> (detectedSubject.subjectBoundingBox.width() * detectedSubject.subjectBoundingBox.height()) }?.subjectBoundingBox
                    ?: RectF()

            // Convert to MLKIT InputImage
            val inputImage = InputImage.fromBitmap(image, 0)

            // Wait for imageView to initialize parameters before running.
            // Otherwise, width and height will return 0.
            inputImageView.post {
                run {
                    if (detectedSubjectsList.isNotEmpty()) {
                        if (detectedSubjectsList.size == 1) {
                            revealSubjectsButton.isEnabled = false
                        }
                        onSubjectsDetected(detectedSubjectsList, inputImage)
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "No subjects detected",
                            Toast.LENGTH_LONG
                        ).show()
                        revealSubjectsButton.isEnabled = false
                        viewGradesButton.isEnabled = false
                        revealSubjectsButton.visibility = View.INVISIBLE
                        selectedSubjectLabel = ""
                        updateCategoryTextView()

                        // Redirect users back to upload/capture
                        val alertDialog = AlertDialog.Builder(this)
                        alertDialog.setMessage("No subject detected, try uploading or capturing again.")
                            .setPositiveButton("Ok"
                            ) { dialogInterface, _ ->
                                run {
                                    // Return the user to either capture or upload
                                    captureMode = intent.getBooleanExtra("CAPTURE_MODE", false)
                                    uploadMode = intent.getBooleanExtra("UPLOAD_MODE", false)
                                    if (captureMode) {
                                        val intent = Intent(
                                            this.applicationContext,
                                            CameraActivity::class.java
                                        )
                                        capturedImage?.recycle()
                                        startActivity(intent)
                                    }

                                    if (uploadMode) {
                                        val intent = Intent(
                                            this.applicationContext,
                                            NavigationActivity::class.java
                                        )
                                        intent.putExtra("CAPTURE_MODE", false)
                                            .putExtra("UPLOAD_MODE", true)
                                        capturedImage?.recycle()
                                        startActivity(intent)
                                    }
                                    dialogInterface.dismiss()
                                }
                            }
                        alertDialog.create().show()

                    }
                }
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    private fun onSubjectsDetected(
        subjectsDetectedList: List<SubjectDetected>,
        inputImage: InputImage
    ) {
        subjectOverlayViewContainer.removeAllViews()

        // Select the most prominent subject as the main subject
        val mostProminentSubject =
            subjectsDetectedList.maxBy { subject -> (subject.subjectBoundingBox.width() * subject.subjectBoundingBox.height()) }

        // Create a Tap-able dot view for every subject on the picture.
        for (subjectDetected in subjectsDetectedList) {
            val pcSubjectDetected =
                PCSubjectDetected(subjectDetected, subjectDetected.subjectBoundingBox, inputImage)
            val subjectDotView = createSubjectDotView(pcSubjectDetected, true)
            if (subjectDetected == mostProminentSubject) {
                val boxView =
                    SubjectBoundingBoxView(
                        this,
                        subjectDotView.boxInViewBoundingBox
                    )
                boxView.tag = "Selected Bounding Box"
                subjectOverlayViewContainer.addView(boxView)
                selectedSubjectBoundingBox = subjectDetected.subjectBoundingBox
                runLabeler(pcSubjectDetected)
                subjectDotView.visibility = View.VISIBLE
                animateBoxView(boxView)
                subjectDotView.tag = "Selected Subject"
            } else {
                subjectDotView.visibility = View.INVISIBLE
            }

            subjectDotView.setOnClickListener {
                runLabeler(pcSubjectDetected)
                for (view in subjectOverlayViewContainer.children) {

                    // Remove the previously selected boxView and update this as the new bounding box view
                    if (view.tag == "Selected Bounding Box") {
                        subjectOverlayViewContainer.removeView(view)
                        val boxView =
                            SubjectBoundingBoxView(
                                this,
                                subjectDotView.boxInViewBoundingBox
                            )
                        boxView.tag = "Selected Bounding Box"
                        subjectOverlayViewContainer.addView(boxView)
                        animateBoxView(boxView)
                    }

                    if (view.tag == "Selected Subject") {
                        view.tag = ""
                        subjectDotView.tag = "Selected Subject"
                    }
                }
                animateDotView(subjectDotView)

            }

            subjectDotView.setOnLongClickListener {
                runLabelerLongClick(pcSubjectDetected)
                for (view in subjectOverlayViewContainer.children) {

                    // Remove the previously selected boxView and update this as the new bounding box view
                    if (view.tag == "Selected Bounding Box") {
                        subjectOverlayViewContainer.removeView(view)
                        val boxView =
                            SubjectBoundingBoxView(
                                this,
                                subjectDotView.boxInViewBoundingBox
                            )
                        boxView.tag = "Selected Bounding Box"
                        subjectOverlayViewContainer.addView(boxView)
                        animateBoxView(boxView)
                    }

                    if (view.tag == "Selected Subject") {
                        view.tag = ""
                        subjectDotView.tag = "Selected Subject"
                        animateDotView(subjectDotView)
                    }
                }
                return@setOnLongClickListener true
            }

            subjectOverlayViewContainer.addView(subjectDotView)
        }

        revealSubjectsButton.setOnCheckedChangeListener { _, isChecked ->
            // Subjects revealed
            if (isChecked) {

                for (view in subjectOverlayViewContainer.children) {
                    if (view.visibility == View.INVISIBLE && view.tag != "Selected Subject" && view.tag != "Selected Bounding Box") {
                        view.visibility = View.VISIBLE
                    }
                }

                if (sharedPreferences.getBoolean("DISPLAYDIALOG", true)) {
                    val alertDialog = AlertDialog.Builder(this)
                    val rememberLabel = arrayOf("Don't show again.")
                    var remember = false
                    alertDialog.setMessage("Tap on the dot in the image to select your subject.")
                        .setSingleChoiceItems(
                            rememberLabel,
                            0
                        ) { _: DialogInterface, _: Int ->
                            remember = !remember
                        }
                        .setPositiveButton("Ok"
                        ) { dialogInterface, _ ->
                            run {
                                if (remember) {
                                    sharedPreferences.edit().putBoolean("DISPLAYDIALOG", false)
                                }
                                dialogInterface.dismiss()
                            }
                        }
                    alertDialog.create().show()
                }
            }
            // Subjects hidden
            else {
                for (view in subjectOverlayViewContainer.children) {
                    if (view.visibility == View.VISIBLE && view.tag != "Selected Subject" && view.tag != "Selected Bounding Box") {
                        view.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    private fun animateDotView(targetView: View) {
        val animatorSet =
            AnimatorInflater.loadAnimator(
                this,
                R.animator.subject_dot_enter
            ) as AnimatorSet
        animatorSet.setTarget(targetView)
        animatorSet.start()
    }

    private fun animateBoxView(targetView: View) {
        val animatorSet =
            AnimatorInflater.loadAnimator(
                this,
                R.animator.subject_bounding_box_enter
            ) as AnimatorSet
        animatorSet.setTarget(targetView)
        animatorSet.start()
    }

    private fun runLabelerLongClick(pcSubjectDetected: PCSubjectDetected) {
        val croppedInputImage =
            InputImage.fromBitmap(
                pcSubjectDetected.imageData?.let { it1 ->
                    ImageUtility.byteArrayToBitmap(
                        it1
                    )
                }, 0
            )
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
        val faceDetector = FaceDetection.getClient(highAccuracyOpts)

        val arrayAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.select_dialog_singlechoice
        )
        faceDetector.process(croppedInputImage)
            .addOnSuccessListener { faces ->
                if (faces.size != 0) {
                    selectedSubjectLabel = "Portrait"
                    categoryTextView.text = selectedSubjectLabel.toUpperCase(Locale.ROOT)
                    arrayAdapter.insert(selectedSubjectLabel, 0)
                }

            }
        val labeler =
            ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(croppedInputImage)
            .addOnSuccessListener { subjectLabelList ->
                if (subjectLabelList.size != 0) {
                    for (i in 0 until subjectLabelList.size) {
                        if (i < 5) {
                            arrayAdapter.add(subjectLabelList[i].text)
                        }
                    }

                    val imageView = ImageView(applicationContext)
                    imageView.maxHeight = 50
                    imageView.setImageBitmap(croppedInputImage.bitmapInternal)
                    AlertDialog.Builder(this)
                        .setTitle("Choose Subject Label")
                        .setView(imageView)
                        .setAdapter(
                            arrayAdapter
                        ) { _, which ->
                            selectedSubjectBoundingBox =
                                pcSubjectDetected.subjectBoundingBox
                            selectedSubjectLabel =
                                arrayAdapter.getItem(which)
                                    ?.toUpperCase(Locale.ROOT).toString()
                            categoryTextView.text =
                                arrayAdapter.getItem(which)
                                    ?.toUpperCase(Locale.ROOT)
                            updateCategoryTextView()
                        }.setNegativeButton(
                            "Cancel"
                        ) { dialog, _ -> dialog?.dismiss() }.show()

                } else {
                    Toast.makeText(
                        applicationContext,
                        "Cannot identify subject, try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun runLabeler(pcSubjectDetected: PCSubjectDetected) {
        selectedSubjectBoundingBox = pcSubjectDetected.subjectBoundingBox
        val croppedInputImage =
            InputImage.fromBitmap(
                pcSubjectDetected.imageData?.let { it1 ->
                    ImageUtility.byteArrayToBitmap(
                        it1
                    )
                }, 0
            )
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        val faceDetector = FaceDetection.getClient(highAccuracyOpts)

        faceDetector.process(croppedInputImage)
            .addOnSuccessListener { faces ->
                if (faces.size != 0) {
                    selectedSubjectLabel = "Portrait"
                    categoryTextView.text = selectedSubjectLabel.toUpperCase(Locale.ROOT)
                    updateCategoryTextView()

                    for (face in faces) {
                    }
                } else {
                    val labeler =
                        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

                    labeler.process(croppedInputImage)
                        .addOnSuccessListener { subjectLabelList ->
                            if (subjectLabelList.size != 0) {
                                selectedSubjectLabel =
                                    subjectLabelList.maxBy { subjectDetected -> subjectDetected.confidence }!!.text
                                selectedSubjectConfidenceScore =
                                    subjectLabelList.maxBy { subjectDetected -> subjectDetected.confidence }!!.confidence
                                updateCategoryTextView()
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Cannot identify subject, try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                applicationContext,
                                "Cannot identify subject, try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener {
            }
    }

    private fun updateCategoryTextView() {
        categoryTextView.text = selectedSubjectLabel.toUpperCase(Locale.ROOT)
        categoryTextView.invalidate()
        categoryTextView.forceLayout()
        // categoryTextView.requestLayout() // this requires the whole screen to be redrawn causing slow reload
        val animatorSet =
            AnimatorInflater.loadAnimator(
                this,
                R.animator.subject_bounding_box_enter
            ) as AnimatorSet
        animatorSet.setTarget(categoryTextView)
        animatorSet.start()
    }

    override fun onResume() {
        super.onResume()
        val intentFilterSelectedSubjectLabelUpdate =
            IntentFilter("SELECTED_SUBJECT_LABEL_UPDATE")
        val intentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                categoryTextView.text = selectedSubjectLabel.toUpperCase(Locale.ROOT)
                categoryTextView.invalidate()
                categoryTextView.requestLayout()
            }
        }
        this.registerReceiver(intentReceiver, intentFilterSelectedSubjectLabelUpdate)
    }

    /**
     * Creates a dot view for every subject in the picture.
     */
    private fun createSubjectDotView(
        subject: PCSubjectDetected,
        selected: Boolean
    ): SubjectDotView {
        val viewCoordinateScale: Float
        val horizontalGap: Float
        val verticalGap: Float
        val inputImageView = inputImageView
        val inputBitmap = capturedImage ?: throw NullPointerException()
        val inputImageViewRatio = inputImageView.width.toFloat() / inputImageView.height
        val inputBitmapRatio = inputBitmap.width.toFloat() / inputBitmap.height

        if (inputBitmapRatio <= inputImageViewRatio) { // Image content fills height
            viewCoordinateScale = inputImageView.height.toFloat() / inputBitmap.height
            horizontalGap = (inputImageView.width - inputBitmap.width * viewCoordinateScale) / 2
            verticalGap = 0f
        } else { // Image content fills width
            viewCoordinateScale = inputImageView.width.toFloat() / inputBitmap.width
            horizontalGap = 0f
            verticalGap = (inputImageView.height - inputBitmap.height * viewCoordinateScale) / 2
        }


        val subjectBoundingBox = subject.subjectBoundingBox
        // Calculate corresponding coordinate.
        val boxInViewCoordinate = RectF(
            subjectBoundingBox.left * viewCoordinateScale + horizontalGap,
            subjectBoundingBox.top * viewCoordinateScale + verticalGap,
            subjectBoundingBox.right * viewCoordinateScale + horizontalGap,
            subjectBoundingBox.bottom * viewCoordinateScale + verticalGap
        )

        val dotView = SubjectDotView(this, selected, boxInViewCoordinate)
        val layoutParams = FrameLayout.LayoutParams(dotViewSize, dotViewSize)
        val dotCenter = PointF(
            (boxInViewCoordinate.right + boxInViewCoordinate.left) / 2,
            (boxInViewCoordinate.bottom + boxInViewCoordinate.top) / 2
        )
        layoutParams.setMargins(
            (dotCenter.x - dotViewSize / 2f).toInt(),
            (dotCenter.y - dotViewSize / 2f).toInt(),
            0,
            0
        )

        dotView.layoutParams = layoutParams
        return dotView
    }
}