package com.example.instalgam

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.instalgam.adapter.ReelAdapter
import com.example.instalgam.apiClient.RetrofitApiClient
import com.example.instalgam.connectivity.NetworkObserver
import com.example.instalgam.model.Reel
import com.example.instalgam.model.ReelResponse
import com.example.instalgam.room.DatabaseReel
import com.example.instalgam.room.PendingReelLikeDatabase
import com.example.instalgam.room.PendingReelLikeDatabaseHelper
import com.example.instalgam.room.ReelDatabase
import com.example.instalgam.room.ReelDatabaseHelper
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class ReelsFeedActivity : AppCompatActivity() {
    private val reels: MutableList<Reel> = mutableListOf()
    private lateinit var dbHelper: ReelDatabaseHelper
    private lateinit var pendingReelLikeDbHelper: PendingReelLikeDatabaseHelper
    private lateinit var vp: ViewPager2
    private lateinit var signOutButton: Button
    private lateinit var postsButton: Button
    private lateinit var reelAdapter: ReelAdapter
    private lateinit var networkObserver: NetworkObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reel_feed)

        val db = ReelDatabase.getInstance(applicationContext)
        dbHelper = ReelDatabaseHelper(db.reelDao())

        val pendingReelLikeDb = PendingReelLikeDatabase.getInstance(applicationContext)
        pendingReelLikeDbHelper = PendingReelLikeDatabaseHelper(pendingReelLikeDb.pendingReelLikesDao())

        vp = findViewById(R.id.viewPager)
        reelAdapter = ReelAdapter(this, reels, pendingReelLikeDbHelper)
        vp.adapter = reelAdapter

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

        postsButton = findViewById(R.id.posts)
        postsButton.setOnClickListener {
            val intent = Intent(this@ReelsFeedActivity, PostFeedActivity::class.java)
            startActivity(intent)
        }
        lifecycleScope.launch {
            fetchReelsOffline()
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
                    if (!status) hasDisconnected = true
                    if (status and hasDisconnected) {
                        reelAdapter.likeUnsyncedPosts()
                        Toast
                            .makeText(
                                this@ReelsFeedActivity,
                                "Network is available, syncing posts with database",
                                Toast.LENGTH_SHORT,
                            ).show()

                        Log.d("networkStatus", "Network is available")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        reelAdapter.pausePlayers()
    }

    override fun onResume() {
        super.onResume()
        reelAdapter.resumePlayers()
    }

    private fun checkConnectivityStatus() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        if (connectivityManager.activeNetwork == null) {
            Log.d("networkStatus", "Network is not available on startup")
            Toast
                .makeText(
                    this@ReelsFeedActivity,
                    "Device is not connected to a network. Loading reels from Room database",
                    Toast.LENGTH_SHORT,
                ).show()
        } else {
            Log.d("networkStatus", "Network is available")
            fetchReelsOnline()
        }
    }

    private fun fetchReelsOnline() {
        RetrofitApiClient.reelsApiService.fetchReels().enqueue(
            object : retrofit2.Callback<ReelResponse> {
                override fun onResponse(
                    call: Call<ReelResponse>,
                    response: Response<ReelResponse>,
                ) {
                    if (response.isSuccessful) {
                        val apiReels = response.body()?.posts?.filterNotNull() ?: emptyList()

                        reels.clear()
                        reels.addAll(apiReels)
                        reelAdapter.notifyDataSetChanged()

                        lifecycleScope.launch {
                            val dbReels =
                                apiReels.map {
                                    DatabaseReel(
                                        it.reelId,
                                        it.userName,
                                        it.profilePicture,
                                        it.reelVideo,
                                        it.likeCount,
                                        it.likedByUser,
                                    )
                                }
                            dbHelper.saveReels(dbReels)
                            Log.d("dbStatus", "Pushed ${dbReels.size} reels into database")
                        }
                    } else {
                        Toast.makeText(this@ReelsFeedActivity, "Failed to load reels", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<ReelResponse>,
                    t: Throwable,
                ) {
                    Log.e("apiCall", t.message.toString())
                    Toast.makeText(this@ReelsFeedActivity, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    private suspend fun fetchReelsOffline() {
        val dbReels = dbHelper.getReels()
        Log.d("dbStatus", "Loaded ${dbReels.size} reels from database")

        reels.clear()
        reels.addAll(
            dbReels.map {
                Reel(
                    it.reelId,
                    it.userName,
                    it.profilePicture,
                    it.reelVideo,
                    it.likeCount,
                    it.likedByUser,
                )
            },
        )
        reelAdapter.notifyDataSetChanged()
    }
}
