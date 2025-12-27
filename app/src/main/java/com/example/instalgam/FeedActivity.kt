package com.example.instalgam

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instalgam.adapter.PostAdapter
import com.example.instalgam.apiClient.RetrofitApiClient
import com.example.instalgam.model.Post
import com.example.instalgam.model.PostResponse
import com.example.instalgam.room.DatabasePost
import com.example.instalgam.room.PostDatabase
import com.example.instalgam.room.PostDatabaseHelper
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class FeedActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var signOutButton: Button
    private lateinit var postAdapter: PostAdapter
    private val posts: MutableList<Post> = mutableListOf()

    private lateinit var dbHelper: PostDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.post_feed)

        val data = intent.extras
        if (data != null) {
            val username: String? = data.getString("USER_USERNAME")
            Toast.makeText(this, "$username has logged in!", Toast.LENGTH_SHORT).show()
        }

        val db = PostDatabase.getInstance(applicationContext)
        dbHelper = PostDatabaseHelper(db.postDao())

        recyclerView = findViewById(R.id.recyclerView)
        postAdapter = PostAdapter(this, posts)
        recyclerView.adapter = postAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        signOutButton = findViewById(R.id.signOutButton)
        signOutButton.setOnClickListener {
            val sp =
                getSharedPreferences(
                    getString(R.string.shared_preferences_file_name),
                    Context.MODE_PRIVATE,
                )

            val username = sp.getString(getString(R.string.logged_in_user), null)

            with(sp.edit()) {
                putString(getString(R.string.logged_in_user), null)
                apply()
            }

            startActivity(Intent(this, MainActivity::class.java))
            Toast.makeText(this, "Signing out of $username", Toast.LENGTH_SHORT).show()
        }

        checkConnectivityStatus()
    }

    private fun checkConnectivityStatus() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        if (connectivityManager.activeNetwork == null) {
            Log.d("networkStatus", "Network is not available on startup")
            Toast
                .makeText(
                    this@FeedActivity,
                    "Device is not connected to a network. Loading posts from Room database",
                    Toast.LENGTH_SHORT,
                ).show()
            fetchPostsOffline()
        } else {
            Log.d("networkStatus", "Network is available")
            fetchPostsOnline()
        }

//        connectivityManager.registerDefaultNetworkCallback(
//            object : ConnectivityManager.NetworkCallback() {
//                override fun onAvailable(network: Network) {
//                    Log.d("networkStatus", "Network is available")
//                    fetchPostsOnline()
//                }
//
//                override fun onUnavailable() {
//                    Log.d("networkStatus", "Network is not available")
//                    fetchPostsOffline()
//                }
//            },
//        )
    }

    private fun fetchPostsOnline() {
        RetrofitApiClient.apiService.fetchPosts().enqueue(
            object : retrofit2.Callback<PostResponse> {
                override fun onResponse(
                    call: Call<PostResponse>,
                    response: Response<PostResponse>,
                ) {
                    if (response.isSuccessful) {
                        val apiPosts = response.body()?.posts?.filterNotNull() ?: emptyList()

                        posts.clear()
                        posts.addAll(apiPosts)
                        postAdapter.notifyDataSetChanged()

                        lifecycleScope.launch {
                            val dbPosts =
                                apiPosts.map {
                                    DatabasePost(
                                        it.postId,
                                        it.userName,
                                        it.profilePicture,
                                        it.postImage,
                                        it.likeCount,
                                        it.likedByUser,
                                    )
                                }
                            dbHelper.savePosts(dbPosts)
                            Log.d("dbStatus", "Pushed ${dbPosts.size} posts into database")
                        }
                    } else {
                        Toast.makeText(this@FeedActivity, "Failed to load posts", Toast.LENGTH_SHORT).show()
                        fetchPostsOffline() // Fallback to offline if API fails
                    }
                }

                override fun onFailure(
                    call: Call<PostResponse>,
                    t: Throwable,
                ) {
                    Log.e("apiCall", t.message.toString())
                    Toast.makeText(this@FeedActivity, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                    fetchPostsOffline() // Fallback to offline on failure
                }
            },
        )
    }

    private fun fetchPostsOffline() {
        lifecycleScope.launch {
            val dbPosts = dbHelper.getPosts()
            Log.d("dbStatus", "Loaded ${dbPosts.size} posts from database")
            posts.clear()
            posts.addAll(
                dbPosts.map {
                    Post(
                        it.postId,
                        it.userName,
                        it.profilePicture,
                        it.postImage,
                        it.likeCount,
                        it.likedByUser,
                    )
                },
            )
            postAdapter.notifyDataSetChanged()
        }
    }
}
