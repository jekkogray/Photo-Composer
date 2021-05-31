package com.example.picturecomposer
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.picturecomposer.util.ImageUtility
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class CameraActivity : AppCompatActivity() {
    companion object {
        val TAG = CameraActivity::class.java.simpleName
        const val PERMISSION_REQUEST = 101
        var capturedImage: Bitmap? = null
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var backButton: ImageView
    private lateinit var flashButton: ImageView
    private lateinit var cameraButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var cameraExecutor: ExecutorService
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    var torchState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Initialize UI components
        cameraPreviewView = findViewById(R.id.camera_preview_view)
        cameraPreviewView.preferredImplementationMode = PreviewView.ImplementationMode.SURFACE_VIEW
        flashButton = findViewById(R.id.flash_button)
        cameraButton = findViewById(R.id.camera_button)
        backButton = findViewById(R.id.back_button)
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
        checkPermissions()

        // Cleanup
        capturedImage = null
        backButton.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }
        // Initialize a thread for camera
        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    override fun onBackPressed() {
        capturedImage = null
        val intent = Intent(this, NavigationActivity::class.java)
        startActivity(intent)
    }

    // Check if permission to use camera already exists
    private fun checkPermissions() {
        val cameraPermission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                this.checkSelfPermission(Manifest.permission.CAMERA)
            } else {
                -1
            }

        val writePermission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                -1
            }

        val grantCameraActivity =
            (cameraPermission == PackageManager.PERMISSION_GRANTED || writePermission == PackageManager.PERMISSION_GRANTED)

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                this,
                "Permissions insufficient.",
                Toast.LENGTH_LONG
            ).show()
            requestPermissions()
        } else {
        }

        if (grantCameraActivity) {
            startCameraActivity()
        }
    }

    private fun startCameraActivity() {
        cameraPreviewView.scaleType = (PreviewView.ScaleType.FIT_CENTER)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this@CameraActivity)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this@CameraActivity))
    }


    private fun requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() || grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
        } else {
            startCameraActivity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up
        cameraExecutor.shutdown()
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview =
            Preview.Builder()
                .setTargetRotation(Surface.ROTATION_90)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
        preview.setSurfaceProvider(cameraPreviewView.createSurfaceProvider())

        val cameraSelector: CameraSelector =
            CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val imageCapture =
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        // Clean up previous use cases
        cameraProvider.unbindAll()

        try {
            // Create camera instance
            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageCapture,
                preview
            )

            if (camera.cameraInfo.hasFlashUnit()) {
                flashButton.setOnClickListener {
                    torchState = !torchState
                    if (torchState) {
                        flashButton.setImageResource(R.drawable.ic_flash_button_on)
                    } else {
                        flashButton.setImageResource(R.drawable.ic_flash_button_off)
                    }
                    camera.cameraControl.enableTorch(torchState)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }

        cameraButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), object :
                ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    capturedImage =
                        ImageUtility.rotateBitmap(ImageUtility.imageProxyToBitmap(image), 90f)
                    capturedImage = capturedImage?.copy(Bitmap.Config.ARGB_8888, true)
                    val intent = Intent(
                        applicationContext,
                        ResultsActivity::class.java
                    ).putExtra("CAPTURE_MODE", true).putExtra("UPLOAD_MODE", false)

                    // Clean up
                    image.close()
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Toast.makeText(
                        applicationContext,
                        "Oops! Image capture failed, try again.", Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

}