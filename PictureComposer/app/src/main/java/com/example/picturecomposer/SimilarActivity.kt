package com.example.picturecomposer

import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import org.jetbrains.anko.doAsync
import android.view.View
import androidx.cardview.widget.CardView

class SimilarActivity : AppCompatActivity() {

    //initialize lateinit variables
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var img1: ImageView
    private lateinit var backButton: FloatingActionButton
    private lateinit var doneButton: Button
    private lateinit var homeIcon: ImageView
    private lateinit var rightButton: ImageButton
    private lateinit var leftButton: ImageButton

    private lateinit var rotRec: TextView
    private lateinit var framingRec: TextView
    private lateinit var expRec: TextView
    private lateinit var blurRec: TextView
    private lateinit var levelRec: TextView

    private lateinit var thirds: CardView
    private lateinit var framing: CardView
    private lateinit var exposure: CardView
    private lateinit var blur: CardView
    private lateinit var level: CardView

    private lateinit var urls: MutableList<RefPhoto>

    var picNum: Int = 0
    var noRec: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_similar)

        //populate lateinit vars
        firebaseDatabase = FirebaseDatabase.getInstance()
        img1 = findViewById(R.id.img1)
        backButton = findViewById(R.id.gradeBackFAB2)
        doneButton = findViewById(R.id.doneButton)
        homeIcon = findViewById(R.id.gradeLogo2)
        rightButton = findViewById(R.id.right_button)
        leftButton = findViewById(R.id.left_button)

        rotRec = findViewById(R.id.rot_rec)
        framingRec = findViewById(R.id.fr_rec)
        expRec = findViewById(R.id.exp_rec)
        blurRec = findViewById(R.id.blur_rec)
        levelRec = findViewById(R.id.level_rec)

        thirds = findViewById(R.id.rot_card)
        framing = findViewById(R.id.fr_card)
        exposure = findViewById(R.id.exp_card)
        blur = findViewById(R.id.blur_card)
        level = findViewById(R.id.level_card)

        //tap logout button to log out
        doneButton.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        //get the firebase user's ID and establish the reference we'll need
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val currPhotoKeyRef = firebaseDatabase.getReference("UID/$currentUserID/currPhotoKey")

        //retrieve photo link saved as "most recent" in db
        currPhotoKeyRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@SimilarActivity,
                    getString(R.string.photo_ref_error),
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val pulledKey = dataSnapshot.getValue(String::class.java)

                //since we know which photo to refer to - pull that photo's metadata now
                val photoRef =
                    firebaseDatabase.getReference("UID/$currentUserID/photoList/$pulledKey")
                photoRef.addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(
                            this@SimilarActivity,
                            getString(R.string.image_data_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //get the curr photo
                        val currPhoto: Photo? = dataSnapshot.getValue(Photo::class.java)

                        //if it's not null, show similar images
                        if (currPhoto != null) {
                            // update recommendations

                            // rot
                            if (currPhoto.thirds == 5) {
                                // set card to gone
                                thirds.visibility = View.GONE
                            } else {
                                noRec = false
                                rotRec.text = getString(R.string.thirds_rec)
                            }

                            // framing
                            if (currPhoto.framing == 5) {
                                // set card to gone
                                framing.visibility = View.GONE
                            } else if (currPhoto.framing == 1 || currPhoto.framing == 4) {
                                noRec = false
                                framingRec.text = getString(R.string.framing_rec_1)
                            } else {
                                noRec = false
                                framingRec.text = getString(R.string.framing_rec_2)
                            }

                            // exposure
                            if (currPhoto.exposure == 5) {
                                // set card to gone
                                exposure.visibility = View.GONE
                            } else if (currPhoto.exposure == 2 || currPhoto.exposure == 4) {
                                noRec = false
                                expRec.text = getString(R.string.exposure_rec_1)
                            } else {
                                noRec = false
                                expRec.text = getString(R.string.exposure_rec_2)
                            }

                            // blur
                            if (currPhoto.blur == 5) {
                                // set card to gone
                                blur.visibility = View.GONE
                            } else {
                                noRec = false
                                blurRec.text = getString(R.string.blur_rec)
                            }

                            // level
                            if (currPhoto.level == 5) {
                                // show congrats msg if photo has no recs (all 5 stars)
                                if (noRec) {
                                    levelRec.text = getString(R.string.perfect)
                                } else {
                                    // set card to gone
                                    level.visibility = View.GONE
                                }
                            } else {
                                noRec = false
                                levelRec.text = getString(R.string.level_rec)
                            }

                            // do reference photo work
                            populateRefPhotos(currPhoto.subject)
                        }
                    }
                })
            }
        })

        // PC logo takes the user home
        homeIcon.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        //back button takes the user back to grading screen
        backButton.setOnClickListener {
            val intent = Intent(this, GradeActivity::class.java)
            startActivity(intent)
        }

        /*Photo Gallery*/

        // Tap right button to show next photo
        rightButton.setOnClickListener {
            // Increment photo
            picNum += 1

            // Enable left (back button)
            leftButton.isEnabled = true
            leftButton.setColorFilter(Color.argb(255, 67, 112, 213))

            // disable right button if on last photo
            if (picNum == 2) {
                rightButton.isEnabled = false
                rightButton.setColorFilter(Color.WHITE)
            }

            Picasso.get()
                .load(urls[picNum].photoUrl)
                .into(img1)

            // Set the click listeners to route to the pixabay post
            img1.setOnClickListener {
                // Create an Intent to open an external web browser to the photo's post on Pixabay
                val uri: Uri = Uri.parse(urls[picNum].postUrl)
                val webIntent = Intent(Intent.ACTION_VIEW, uri)
                // Execute the Intent - launch the user’s browser app
                startActivity(webIntent)
            }
        }

        // tap right button to show next photo
        leftButton.setOnClickListener {
            // decrement photo
            picNum -= 1

            // Enable right (next button)
            rightButton.isEnabled = true
            rightButton.setColorFilter(Color.argb(255, 67, 112, 213))

            // disable left button if on first photo
            if (picNum == 0) {
                leftButton.isEnabled = false
                leftButton.setColorFilter(Color.WHITE)
            }

            Picasso.get()
                .load(urls[picNum].photoUrl)
                .into(img1)

            //set the click listeners to route to the pixabay post
            img1.setOnClickListener {
                // Create an Intent to open an external web browser to the photo's post on Pixabay
                val uri: Uri = Uri.parse(urls[picNum].postUrl)
                val webIntent = Intent(Intent.ACTION_VIEW, uri)
                // Execute the Intent - launch the user’s browser app
                startActivity(webIntent)
            }
        }

        /* click to composition principle wiki page */

        rotRec.setOnClickListener {
            // Create an Intent to open an external web browser to the rot wiki page
            val uri: Uri = Uri.parse("https://en.wikipedia.org/wiki/Rule_of_thirds")
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            // Execute the Intent - launch the user’s browser app
            startActivity(webIntent)
        }

        framingRec.setOnClickListener {
            // Create an Intent to open an external web browser to the framing wiki page
            val uri: Uri = Uri.parse("https://en.wikipedia.org/wiki/Framing_(visual_arts)")
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            // Execute the Intent - launch the user’s browser app
            startActivity(webIntent)
        }

        expRec.setOnClickListener {
            // Create an Intent to open an external web browser to the exposure wiki page
            val uri: Uri = Uri.parse("https://en.wikipedia.org/wiki/Exposure_(photography)")
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            // Execute the Intent - launch the user’s browser app
            startActivity(webIntent)
        }

        blurRec.setOnClickListener {
            // Create an Intent to open an external web browser to the blur wiki page
            val uri: Uri = Uri.parse("https://en.wikipedia.org/wiki/Motion_blur")
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            // Execute the Intent - launch the user’s browser app
            startActivity(webIntent)
        }

        levelRec.setOnClickListener {
            if (!noRec) {
                // Create an Intent to open an external web browser to the perspective wiki page
                val uri: Uri = Uri.parse("https://en.wikipedia.org/wiki/Perspective_control")
                val webIntent = Intent(Intent.ACTION_VIEW, uri)
                // Execute the Intent - launch the user’s browser app
                startActivity(webIntent)
            }
        }
    }

    fun populateRefPhotos(subject: String) {
        //do a safety check that a subject was provided
        if (subject != "") {

            //do networking call to Pixabay
            doAsync {
                val pixabayManager = PixabayManager()
                urls =
                    pixabayManager.retrieveImages(subject, getString(R.string.pixabay_api_key))

                runOnUiThread {
                    // Show first image
                    Picasso.get()
                        .load(urls[0].photoUrl)
                        .into(img1)

                    //set the click listeners to route to the pixabay post
                    img1.setOnClickListener {
                        // Create an Intent to open an external web browser to the photo's post on Pixabay
                        val uri: Uri = Uri.parse(urls[0].postUrl)
                        val webIntent = Intent(Intent.ACTION_VIEW, uri)
                        // Execute the Intent - launch the user’s browser app
                        startActivity(webIntent)
                    }

                    // disable left (previous picture) button to start
                    leftButton.isEnabled = false
                    leftButton.setColorFilter(Color.WHITE)
                }
            }
        }
    }
}