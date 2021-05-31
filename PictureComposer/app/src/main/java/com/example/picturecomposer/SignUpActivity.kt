package com.example.picturecomposer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class SignUpActivity : AppCompatActivity() {

    //initialize lateinit variables
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var password2: EditText
    private lateinit var signUpButton: Button
    private lateinit var progBar: ProgressBar

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var mySwitch: Switch
    private lateinit var backButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //populate the lateinit variables
        firebaseAuth = FirebaseAuth.getInstance()

        email = findViewById(R.id.newEmailEditText)
        password = findViewById(R.id.newPasswordEditText)
        password2 = findViewById(R.id.newPasswordEditText2)
        signUpButton = findViewById(R.id.newSignUpButton)
        progBar = findViewById(R.id.signupProgressBar)
        mySwitch = findViewById(R.id.rememberMeSignup)
        backButton = findViewById(R.id.signupBackFAB)

        //get shared preferences
        val preferences = getSharedPreferences("photo-composer", Context.MODE_PRIVATE)

        //when sign up button is pressed:
        signUpButton.setOnClickListener {

            //retrieve the text from the editTexts
            val inputtedUsername: String = email.text.toString().trim()
            val inputtedPassword: String = password.text.toString().trim()
            val inputtedPassword2: String = password2.text.toString().trim()

            //make sure email editText is not empty
            when {
                inputtedUsername == "" -> {
                    Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_LONG).show()
                }

                //make sure password editText is not empty
                inputtedPassword == "" -> {
                    Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_LONG)
                        .show()
                }

                //make sure second password editText is not empty
                inputtedPassword2 == "" -> {
                    Toast.makeText(this, getString(R.string.reenter_password), Toast.LENGTH_LONG)
                        .show()
                }

                //make sure the two passwords match
                inputtedPassword != inputtedPassword2 -> {
                    Toast.makeText(this, getString(R.string.match_password), Toast.LENGTH_LONG)
                        .show()
                }

                //if all editTexts are filled in and passwords match:
                else -> {
                    //start the progress bar and disable clicks to the screen since networking is about to occur
                    progBar.visibility = View.VISIBLE
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    )

                    //use firebase to attempt to create the user
                    firebaseAuth.createUserWithEmailAndPassword(inputtedUsername, inputtedPassword)
                        .addOnCompleteListener { task ->

                            //if the user is created successfully:
                            if (task.isSuccessful) {

                                //make a toast with the new user name
                                val user = firebaseAuth.currentUser
                                Toast.makeText(
                                    this,
                                    getString(R.string.created_user, user!!.email),
                                    Toast.LENGTH_SHORT
                                ).show()

                                //remember sign in credentials if user flipped switch
                                if (mySwitch.isChecked) {
                                    preferences.edit().putString("EMAIL", inputtedUsername).apply()
                                    preferences.edit().putString("PASSWORD", inputtedPassword)
                                        .apply()
                                    preferences.edit().putBoolean("REMEMBER", true).apply()
                                }

                                // Go to the next Activity and get rid of progress bar and screen click disable
                                val intent = Intent(
                                    this,
                                    NavigationActivity::class.java
                                ).putExtra("CAPTURE_MODE", false).putExtra("NEW_USER", true)
                                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                startActivity(intent)
                                progBar.visibility = View.GONE
                            }

                            //if the user is not created successfully:
                            else {
                                // The user’s username is formatted incorrectly or their password doesn’t meet minimum requirements
                                // then toast and get rid of progress bar and screen click disable
                                when (val exception = task.exception) {
                                    is FirebaseAuthInvalidCredentialsException -> {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.invalid_cred),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        progBar.visibility = View.GONE
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                    }

                                    // A user with this email already exists. Toast and get rid of progress bar and screen click disable
                                    is FirebaseAuthUserCollisionException -> {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.already_exists),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        progBar.visibility = View.GONE
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                    }

                                    // A networking error occurred.  Toast and get rid of progress bar and screen click disable
                                    is FirebaseNetworkException -> {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.network_error),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        progBar.visibility = View.GONE
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                    }

                                    // Some other error occurred.  Toast with exception and get rid of progress bar and screen click disable
                                    else -> {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.signup_error, exception),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        progBar.visibility = View.GONE
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                    }
                                }
                            }
                        }
                }
            }
        }

        //if the user taps the back button, return to login screen
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
