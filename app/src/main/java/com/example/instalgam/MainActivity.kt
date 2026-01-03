package com.example.instalgam

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {
    val username: String = "admin"
    val password: String = "password"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sp: SharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_file_name), Context.MODE_PRIVATE)
        val username: String? = sp.getString(getString(R.string.logged_in_user), null)

        if (username != null) {
            val intent = Intent(this@MainActivity, PostFeedActivity::class.java)
            // intent.putExtra("USER_USERNAME", username)
            startActivity(intent)
        }

        setContentView(R.layout.landing_page)
        val login: Button = findViewById(R.id.loginButton)
        val signin: Button = findViewById(R.id.signInButton)
        login.setOnClickListener {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        }
        signin.setOnClickListener {
            val intent = Intent(this@MainActivity, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}
