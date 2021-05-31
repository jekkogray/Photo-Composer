package com.example.picturecomposer

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class PortfolioAdapter(private val photoList: MutableList<Photo>, private val editMode: Boolean) :
    RecyclerView.Adapter<PortfolioAdapter.ViewHolder>() {

    //initialize lateinit vars
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //populate the grid layout with the desired cell layout
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.cell_portfolio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //load the image into each cell
        Picasso.get().load(photoList[position].url).into(holder.portImage)

        //make each image clickable for users to view their full photo grade again
        holder.portImage.setOnClickListener {
            //push the photo key to the db as the user's currPhotoKey
            firebaseDatabase = FirebaseDatabase.getInstance()
            val currUserID = FirebaseAuth.getInstance().currentUser!!.uid
            val currPhotoKeyRef = firebaseDatabase.getReference("UID/$currUserID/currPhotoKey")
            currPhotoKeyRef.setValue(photoList[position].key)

            // push this to the user's currPhotoKey photoList[position].key
            val intent = Intent(it.context, GradeActivity::class.java)
            it.context.startActivity(intent)
        }

        //if the user clicks the edit button, then let them delete a given photo
        if (editMode) {
            //make red exes show up in the corner of each photo during edit mode
            holder.exButton.visibility = VISIBLE

            //when the user clicks a photo's ex button, show a dialog confirming they want to delete
            holder.exButton.setOnClickListener {
                val deleteDialog = AlertDialog.Builder(it.context)

                //if the user does want to delte, remove photo reference from database. If not, just close dialog box
                deleteDialog.setMessage(it.context.getString(R.string.delete_dialog))
                    .setPositiveButton(
                        it.context.getString(R.string.delete_yes)
                    ) { dialogInterface, _ ->
                        //find the photo reference so we can remove it from firebase db
                        firebaseDatabase = FirebaseDatabase.getInstance()
                        val currUserID = FirebaseAuth.getInstance().currentUser!!.uid
                        val currPhotoKeyRef =
                            firebaseDatabase.getReference("UID/$currUserID/photoList/${photoList[position].key}")
                        currPhotoKeyRef.removeValue()
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton(
                        it.context.getString(R.string.delete_no)
                    ) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }

                deleteDialog.create().show()
            }
        }
    }

    override fun getItemCount(): Int {
        return photoList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val portImage: ImageView = itemView.findViewById(R.id.portImage)
        val exButton: ImageButton = itemView.findViewById(R.id.exButton)
    }
}