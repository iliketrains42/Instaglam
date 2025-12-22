package com.example.instalgam

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instalgam.adapter.PostAdapter
import com.example.instalgam.model.Post

class FeedActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var posts: MutableList<Post>
    private lateinit var signOutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.feed)
        val data = intent.extras

        if (data != null) {
            val username: String? = data.getString("USER_USERNAME")
            Toast.makeText(this, "$username has logged in!", Toast.LENGTH_SHORT).show()
        }
        signOutButton = findViewById(R.id.signOutButton)
        posts = getPosts(10)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.adapter = PostAdapter(this, posts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        signOutButton.setOnClickListener {
            val sp: SharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_file_name), Context.MODE_PRIVATE)
            val username: String? = sp.getString(getString(R.string.logged_in_user), null)
            with(sp.edit()) {
                putString(getString(R.string.logged_in_user), null)
                apply()
            }
            val intent = Intent(this@FeedActivity, MainActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Signing out of $username", Toast.LENGTH_SHORT).show()
        }
    }

    // Use Retrofit(?) to fetch all the posts
    private fun getPosts(size: Int): MutableList<Post> {
        var posts = mutableListOf<Post>()

        var cond = true
        for (i in 0 until size) {
            // posts.add(Post(i, "u$i", R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground, i * i, cond))
            posts.add(Post(i, "u$i", i * i + 1, cond))
            cond = !cond
        }
        return posts
    }
}
