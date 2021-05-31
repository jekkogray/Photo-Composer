package com.example.picturecomposer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class GradeActivity : AppCompatActivity() {

    //initialize lateinit variables
    private lateinit var firebaseDatabase: FirebaseDatabase

    private lateinit var gradedPhoto: ImageView

    private lateinit var backButton: FloatingActionButton
    private lateinit var nextButton: Button
    private lateinit var homeIcon: ImageView

    private lateinit var grade: RatingBar
    private lateinit var thirds: RatingBar
    private lateinit var framing: RatingBar
    private lateinit var exposure: RatingBar
    private lateinit var blur: RatingBar
    private lateinit var level: RatingBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grade)

        //populate lateinit vars
        firebaseDatabase = FirebaseDatabase.getInstance()
        gradedPhoto = findViewById(R.id.sourceNext)
        backButton = findViewById(R.id.gradeBackFAB)
        nextButton = findViewById(R.id.next_button)

        grade = findViewById(R.id.grade_rating)
        thirds = findViewById(R.id.rot_rating)
        framing = findViewById(R.id.frame_rating)
        exposure = findViewById(R.id.exp_rating)
        blur = findViewById(R.id.blur_rating)
        level = findViewById(R.id.level_rating)
        homeIcon = findViewById(R.id.gradeLogo)

        //get the firebase user's ID and establish the reference we'll need
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val currPhotoKeyRef = firebaseDatabase.getReference("UID/$currentUserID/currPhotoKey")

        //retrieve photo link saved as "most recent" in db
        currPhotoKeyRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@GradeActivity,
                    getString(R.string.photo_ref_error),
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val pulledKey = dataSnapshot.getValue(String::class.java)

                //now we know which photo to refer to - so pull that photos metadata now
                val photoRef =
                    firebaseDatabase.getReference("UID/$currentUserID/photoList/$pulledKey")
                photoRef.addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(
                            this@GradeActivity,
                            getString(R.string.image_data_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //get the curr photo
                        val currPhoto: Photo? = dataSnapshot.getValue(Photo::class.java)

                        //if it's not null, populate the grading screen with its info
                        if (currPhoto != null) {
                            Picasso.get().load(currPhoto.url).into(gradedPhoto)

                            // Populate grades
                            grade.rating = currPhoto.grade.toFloat()
                            thirds.rating = currPhoto.thirds.toFloat()
                            framing.rating = currPhoto.framing.toFloat()
                            exposure.rating = currPhoto.exposure.toFloat()
                            blur.rating = currPhoto.blur.toFloat()
                            level.rating = currPhoto.level.toFloat()
                        }
                    }
                })
            }
        })

        //in place of the back button, we have a grid button that goes to the portfolio
        backButton.setOnClickListener {
            val intent = Intent(this, PortfolioActivity::class.java)
            startActivity(intent)
        }

        //tapping the photo composer logo will take the user home
        homeIcon.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        // next button goes to similar activity (recs and imgs)
        nextButton.setOnClickListener {
            val altIntent = Intent(this, SimilarActivity::class.java)
            startActivity(altIntent)
        }
    }
}