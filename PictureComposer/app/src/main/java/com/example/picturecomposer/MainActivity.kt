package com.example.picturecomposer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class MainActivity : AppCompatActivity() {

    //initialize lateinit variables
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button
    private lateinit var progBar: ProgressBar

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var mySwitch: Switch
    private lateinit var forgotPassword: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //populate the lateinit variables
        firebaseAuth = FirebaseAuth.getInstance()

        email = findViewById(R.id.emailEditText)
        password = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpButton)
        signUpButton = findViewById(R.id.signUpButton)
        progBar = findViewById(R.id.loginProgressBar)
        mySwitch = findViewById(R.id.rememberMeSwitch)
        forgotPassword = findViewById(R.id.forgotPasswordTextView)

        //get shared preferences
        val preferences = getSharedPreferences("photo-composer", Context.MODE_PRIVATE)

        //if the user had wanted their credentials remembered, than pre-populate the editTexts, and have the switch on
        if (preferences.getBoolean("REMEMBER", false)) {
            email.setText(preferences.getString("EMAIL", ""))
            password.setText(preferences.getString("PASSWORD", ""))
            mySwitch.isChecked = true
        }

        //if the user clicks forgot password, have firebase send them a reset email
        forgotPassword.setOnClickListener {
            firebaseAuth.sendPasswordResetEmail(email.text.toString().trim())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.reset_success), Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(this, getString(R.string.reset_fail), Toast.LENGTH_LONG)
                            .show()
                    }
                }
        }

        //when login button is pressed:
        loginButton.setOnClickListener {

            //retrieve the text from the editTexts
            val inputtedUsername = email.text.toString().trim()
            val inputtedPassword = password.text.toString().trim()

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

                //if both editTexts are filled in:
                else -> {
                    //start the progress bar and disable clicks to the screen since networking is about to occur
                    progBar.visibility = View.VISIBLE
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    )

                    //use firebase to attempt to sign the user in
                    firebaseAuth.signInWithEmailAndPassword(inputtedUsername, inputtedPassword)
                        .addOnCompleteListener { task ->

                            //if the user is signed in successfully:
                            if (task.isSuccessful) {

                                //remember me switch: save credentials if checked
                                if (mySwitch.isChecked) {
                                    preferences.edit().putString("EMAIL", inputtedUsername).apply()
                                    preferences.edit().putString("PASSWORD", inputtedPassword)
                                        .apply()
                                    preferences.edit().putBoolean("REMEMBER", true).apply()
                                }

                                //remember me switch: forget credentials if unchecked
                                else {
                                    preferences.edit().putString("EMAIL", "").apply()
                                    preferences.edit().putString("PASSWORD", "").apply()
                                    preferences.edit().putBoolean("REMEMBER", false).apply()
                                }

                                //make a toast with the new user name
                                val user = firebaseAuth.currentUser
                                Toast.makeText(
                                    this,
                                    getString(R.string.logged_in_user, user!!.email),
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Go to the next Activity and get rid of progress bar and screen click disable
                                val intent = Intent(
                                    this,
                                    NavigationActivity::class.java
                                ).putExtra("NEW_USER", false)
                                startActivity(intent)
                                progBar.visibility = View.GONE
                                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                            }

                            //if the user is not signed in successfully:
                            else {
                                val exception = task.exception

                                // The user’s username is formatted incorrectly or their password doesn’t meet minimum requirements
                                //then toast and get rid of progress bar and screen click disable
                                when (exception) {
                                    is FirebaseAuthInvalidCredentialsException -> {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.invalid_cred),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        progBar.visibility = View.GONE
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                    }


                                    // A user with this email doesn't exists.  Toast and get rid of progress bar and screen click disable
                                    is FirebaseAuthInvalidUserException -> {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.not_exist),
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
                                            getString(R.string.login_error, exception),
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

        //when signup button is pressed, head to sign up activity
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}
