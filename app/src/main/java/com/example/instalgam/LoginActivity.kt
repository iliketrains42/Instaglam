package com.example.instalgam

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val username: String = "admin"
        val password: String = "password"
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_page)
        val usernameField: EditText = findViewById(R.id.usernameEntry)
        val passwordField: EditText = findViewById(R.id.passwordEntry)
        val sp: SharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_file_name), Context.MODE_PRIVATE)

        val enterButton: Button = findViewById(R.id.logInEntry)
        enterButton.setOnClickListener {
            val uname: String = usernameField.text.toString()
            val pwd: String = passwordField.text.toString()

            if (uname == username && pwd == password) {
                with(sp.edit()) {
                    putString(getString(R.string.logged_in_user), uname)
                    apply()
                }
                val intent = Intent(this@LoginActivity, PostFeedActivity::class.java)
                intent.putExtra("USER_USERNAME", uname)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_SHORT).show()
                usernameField.setText(null)
                passwordField.setText(null)
            }
        }
    }
}
