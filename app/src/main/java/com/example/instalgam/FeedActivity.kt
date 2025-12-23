package com.example.instalgam

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instalgam.adapter.PostAdapter
import com.example.instalgam.apiClient.RetrofitApiClient
import com.example.instalgam.model.Post
import com.example.instalgam.model.PostResponse
import retrofit2.Call
import retrofit2.Response

class FeedActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var signOutButton: Button
    private lateinit var postAdapter: PostAdapter
    private var posts: MutableList<Post> = mutableListOf()

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

        recyclerView = findViewById(R.id.recyclerView)
        postAdapter = PostAdapter(this, posts)
        recyclerView.adapter = postAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        fetchPosts()

        signOutButton.setOnClickListener {
            val sp: SharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_file_name), Context.MODE_PRIVATE)
            val username: String? = sp.getString(getString(R.string.logged_in_user), null)
            with(sp.edit()) {
                putString(getString(R.string.logged_in_user), null)
                // remove(R.string.logged_in_user.toString())
                apply()
            }
            val intent = Intent(this@FeedActivity, MainActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Signing out of $username", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchPosts() {
        val client = RetrofitApiClient.apiService.fetchPosts()
        client.enqueue(
            object : retrofit2.Callback<PostResponse> {
                override fun onResponse(
                    call: Call<PostResponse>,
                    response: Response<PostResponse>,
                ) {
                    if (response.isSuccessful) {
                        Log.d("posts", response.body().toString())
                        response.body()?.posts?.let {
                            posts.clear()
                            posts.addAll(it.filterNotNull()) // Filter out any null posts
                        }
                    } else {
                        Toast.makeText(this@FeedActivity, "Failed to load posts", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<PostResponse>,
                    t: Throwable,
                ) {
                    Log.e("apiCallFailed", t.message.toString())
                    Toast.makeText(this@FeedActivity, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}
