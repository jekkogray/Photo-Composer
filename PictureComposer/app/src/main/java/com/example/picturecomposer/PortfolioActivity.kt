package com.example.picturecomposer

import android.content.Intent
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class PortfolioActivity : AppCompatActivity() {

    //initialize lateinit variables
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var editButton: Button
    private lateinit var doneEditButton: Button
    private lateinit var backButton: FloatingActionButton
    private lateinit var homeIcon: ImageView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portfolio)

        //populate lateinit variables
        firebaseDatabase = FirebaseDatabase.getInstance()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this@PortfolioActivity, 3)

        editButton = findViewById(R.id.editButton)
        doneEditButton = findViewById(R.id.doneEditButton)
        backButton = findViewById(R.id.portBackFAB)
        homeIcon = findViewById(R.id.portfolioLogo)
        emptyText = findViewById(R.id.emptyPortTextView)

        // Get a reference to the photos
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val photoListRef = firebaseDatabase.getReference("UID/$currentUserID/photoList")

        //check if this is an empty portfolio
        photoListRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                //if the portfolio is empty, show a message and hide edit buttons
                if (p0.value == null) {
                    emptyText.visibility = VISIBLE
                    editButton.visibility = INVISIBLE
                    doneEditButton.visibility = INVISIBLE
                }
            }
        }
        )

        //create helpful variable for photo displaying
        val photoList = mutableListOf<Photo>()

        // Attach a listener to read the data at our photos reference
        photoListRef.addChildEventListener(object : ChildEventListener {
            //go through each photo that has been added
            override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                val newPhoto = dataSnapshot.getValue(Photo::class.java)
                if (newPhoto != null) {
                    photoList.add(newPhoto)

                    //since portfolio is not empty, hide message and show edit button
                    editButton.visibility = VISIBLE
                    emptyText.visibility = INVISIBLE

                    //now send results to adapter and put them in the recycler view
                    val adapter = PortfolioAdapter(photoList, false)
                    recyclerView.adapter = adapter
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, prevChildKey: String?) {
            }

            //respond to a photo deletion
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val newPhoto = dataSnapshot.getValue(Photo::class.java)
                if (newPhoto != null) {
                    photoList.remove(newPhoto)

                    //now send results to adapter and put them in the recycler view
                    val adapter = PortfolioAdapter(photoList, true)
                    recyclerView.adapter = adapter
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, prevChildKey: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })

        //if user taps edit, show red exes on each image and change button to "done editing"
        editButton.setOnClickListener {
            val adapter = PortfolioAdapter(photoList, true)
            editButton.visibility = INVISIBLE
            doneEditButton.visibility = VISIBLE
            recyclerView.adapter = adapter
        }

        //if user taps done editing, hide red exes on each image and change button to "edit"
        doneEditButton.setOnClickListener {
            val adapter = PortfolioAdapter(photoList, false)
            doneEditButton.visibility = INVISIBLE
            editButton.visibility = VISIBLE
            recyclerView.adapter = adapter
        }

        //tapping photo composer logo takes the user home
        homeIcon.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        //tapping back button takes the user home
        backButton.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }
    }
}