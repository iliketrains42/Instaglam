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
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.instalgam.adapter.PostAdapter
import com.example.instalgam.apiClient.RetrofitApiClient
import com.example.instalgam.connectivity.NetworkObserver
import com.example.instalgam.model.Post
import com.example.instalgam.model.PostResponse
import com.example.instalgam.room.DatabasePost
import com.example.instalgam.room.PendingLikeDatabase
import com.example.instalgam.room.PendingLikeDatabaseHelper
import com.example.instalgam.room.PostDatabase
import com.example.instalgam.room.PostDatabaseHelper
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class PostFeedActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var signOutButton: Button
    private lateinit var postAdapter: PostAdapter
    private lateinit var reelsButton: Button
    private val posts: MutableList<Post> = mutableListOf()
    private lateinit var dbHelper: PostDatabaseHelper
    private lateinit var pendingLikeDbHelper: PendingLikeDatabaseHelper
    private lateinit var networkObserver: NetworkObserver

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

        val pendingLikeDb = PendingLikeDatabase.getInstance(applicationContext)
        pendingLikeDbHelper = PendingLikeDatabaseHelper(pendingLikeDb.pendingLikesDao())

        recyclerView = findViewById(R.id.recyclerView)
        postAdapter = PostAdapter(this, posts, pendingLikeDbHelper)
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

            sp.edit {
                putString(getString(R.string.logged_in_user), null)
            }

            startActivity(Intent(this, MainActivity::class.java))
            Toast.makeText(this, "Signing out of $username", Toast.LENGTH_SHORT).show()
        }

        reelsButton = findViewById(R.id.reels)
        reelsButton.setOnClickListener {
            val intent = Intent(this@PostFeedActivity, ReelsFeedActivity::class.java)
            startActivity(intent)
        }
        // Always fetch posts from the DB first
        lifecycleScope.launch {
            fetchPostsOffline()
        }
        checkConnectivityStatus()
        networkObserver = NetworkObserver(applicationContext)
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var hasDisconnected = false
                networkObserver.observe().collect { status ->
                    if (!status) {
                        hasDisconnected = true
                    }
                    if (status && hasDisconnected) {
                        postAdapter.likeUnsyncedPosts()
                        Toast
                            .makeText(
                                this@PostFeedActivity,
                                "Network is available, syncing posts with database",
                                Toast.LENGTH_SHORT,
                            ).show()
                        Log.d("networkStatus", "Network reconnected, syncing")
                    }
                }
            }
        }
    }

    private fun checkConnectivityStatus() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        if (connectivityManager.activeNetwork == null) {
            Log.d("networkStatus", "Network is not available on startup")
            Toast
                .makeText(
                    this@PostFeedActivity,
                    "Device is not connected to a network. Loading posts from Room database",
                    Toast.LENGTH_SHORT,
                ).show()
//            fetchPostsOffline()
        } else {
            Log.d("networkStatus", "Network is available")
            fetchPostsOnline()
        }
    }

    private fun fetchPostsOnline() {
        RetrofitApiClient.postsApiService.fetchPosts().enqueue(
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
                            Log.d("dbStatus", "Pushed ${dbPosts.size} posts into database")
                            dbHelper.savePosts(dbPosts)
                        }
                    } else {
                        Toast.makeText(this@PostFeedActivity, "Failed to load posts", Toast.LENGTH_SHORT).show()
//                        fetchPostsOffline()
                    }
                }

                override fun onFailure(
                    call: Call<PostResponse>,
                    t: Throwable,
                ) {
                    Toast.makeText(this@PostFeedActivity, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
//                    fetchPostsOffline()
                    Log.e("apiStatus", t.message.toString())
                }
            },
        )
    }

    private suspend fun fetchPostsOffline() {
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
