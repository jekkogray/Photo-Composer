package com.example.picturecomposer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException

@Suppress("DEPRECATION")
class NavigationActivity : AppCompatActivity() {
    companion object {
        const val PERMISSION_REQUEST = 101
    }

    //initialize lateinit variables
    private lateinit var portfolioButton: Button
    private lateinit var uploadButton: Button
    private lateinit var cameraButton: Button
    private lateinit var logoutButton: Button
    private lateinit var welcomeMessage: TextView

    private val pickPhotoCode = 1046

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        //check to see if a user is logged in
        FirebaseAuth.getInstance().addAuthStateListener {
            if (it.currentUser == null) {
                // User is not signed in, go to login
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

        //populate the lateinit variables
        portfolioButton = findViewById(R.id.portfolioButton)
        uploadButton = findViewById(R.id.uploadButton)
        cameraButton = findViewById(R.id.cameraButton)
        logoutButton = findViewById(R.id.logoutButton)
        welcomeMessage = findViewById(R.id.welcomeBackTextView)

        if (intent.getBooleanExtra("NEW_USER", false))
            welcomeMessage.text = getString(R.string.welcome)
        else
            welcomeMessage.text = getString(R.string.welcome_back)

        //tap portfolio button to view portfolio
        portfolioButton.setOnClickListener {
            val intent = Intent(this, PortfolioActivity::class.java)
            startActivity(intent)
        }
        if (intent.getBooleanExtra("UPLOAD_MODE", false)) {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

            // if we pass the safety check, then head to photo gallery
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, pickPhotoCode)
            }
        }

        //tap upload button to upload a photo for grading
        uploadButton.setOnClickListener {
            // Trigger gallery selection for a photo by creating intent for picking a photo from the gallery
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

            // if we pass the safety check, then head to photo gallery
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, pickPhotoCode)
            }
        }

        //tap camera button to capture a photo for grading
        cameraButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        //tap logout button to log out
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        //don't allow captures or uploads until we have permission
        uploadButton.isEnabled = false
        cameraButton.isEnabled = false
        checkPermissions()
    }

    private fun checkPermissions() {
        //check camera permissions
        val cameraPermission =

            this.checkSelfPermission(Manifest.permission.CAMERA)

        //check storage writing permissions
        val writePermission =
            this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //if user has already granted permission to camera and storage, we can allow for captures and uploads
        val grantCameraActivity =
            (cameraPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED)
        if (grantCameraActivity) {
            uploadButton.isEnabled = true
            cameraButton.isEnabled = true
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            NavigationActivity.PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() || grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            requestPermissions()
        } else {
            uploadButton.isEnabled = true
            cameraButton.isEnabled = true
        }
    }

    //if upload photo was tapped and the user selected a photo, then this will run
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
            CameraActivity.capturedImage = selectedImage

            //head to results activity to display selected photo
            val intent = Intent(
                applicationContext,
                ResultsActivity::class.java
            ).putExtra("CAPTURE_MODE", false).putExtra("UPLOAD_MODE", true)
            startActivity(intent)
        }
    }
}
