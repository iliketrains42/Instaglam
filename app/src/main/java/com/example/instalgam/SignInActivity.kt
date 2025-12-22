package com.example.instalgam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signin_page)
        val usernameField: EditText= findViewById(R.id.usernameEntry)
        val passwordField: EditText= findViewById(R.id.passwordEntry)
        val reenterPasswordField: EditText= findViewById(R.id.reenter_password)
        val enterButton: Button= findViewById(R.id.signInEntry)
        enterButton.setOnClickListener {
            val username = usernameField.text.toString()
            val pwd1 = passwordField.text.toString()
            val pwd2 = reenterPasswordField.text.toString()

            if(pwd1!=pwd2){
                Toast.makeText(this, "The passwords do not match!", Toast.LENGTH_SHORT).show()
                usernameField.setText(null)
                passwordField.setText(null)
                reenterPasswordField.setText(null)
            }
            else if(username.isEmpty()){
                Toast.makeText(this, "Empty username", Toast.LENGTH_SHORT).show()
                usernameField.setText(null)
                passwordField.setText(null)
                reenterPasswordField.setText(null)
            }
            else if(username.isNotEmpty() && pwd1.isNotEmpty() && pwd2.isNotEmpty()){
                Toast.makeText(this, "Successfully registered!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@SignInActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }
}