@file:androidx.media3.common.util.UnstableApi

package com.example.instalgam.adapter

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import com.example.instalgam.R
import com.example.instalgam.apiClient.LikeReelBody
import com.example.instalgam.apiClient.RetrofitApiClient
import com.example.instalgam.cache.ExoPlayerCache
import com.example.instalgam.model.Reel
import com.example.instalgam.room.PendingReelLikeDatabaseHelper
import com.example.instalgam.room.ReelDatabase
import com.example.instalgam.room.ReelDatabaseHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReelAdapter(
    val context: Context,
    val elements: MutableList<Reel>,
    private val pendingReelLikeDbHelper: PendingReelLikeDatabaseHelper,
) : RecyclerView.Adapter<ReelAdapter.Holder>() {
    private var currentHolder: Holder? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.reel_layout, parent, false)

        return Holder(view)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int,
    ) {
        val reel = elements[position]
        var isMuted: Boolean = false
        holder.player?.release()
        holder.likeCount.text = reel.likeCount.toString()
        if (reel.likedByUser) {
            holder.likeButton.setImageResource(R.drawable.liked_heart)
        } else {
            holder.likeButton.setImageResource(R.drawable.unliked_heart)
        }

        holder.muteButton.load(R.drawable.unmuted) {
            transformations(CircleCropTransformation())
        }

        holder.username.text = reel.userName
        val db = ReelDatabase.getInstance(context.applicationContext)
        val dbHelper = ReelDatabaseHelper(db.reelDao())
        holder.likeButton.setOnClickListener {
            val previousLikedState = reel.likedByUser
            val previousLikeCount = reel.likeCount

            reel.likedByUser = !previousLikedState
            reel.likeCount += if (reel.likedByUser) 1 else -1

            holder.likeCount.text = reel.likeCount.toString()
            holder.likeButton.setImageResource(
                if (reel.likedByUser) R.drawable.liked_heart else R.drawable.unliked_heart,
            )

            fun snackbarDisplay() {
//                reel.likedByUser = previousLikedState
//                reel.likeCount = previousLikeCount
//                holder.likeCount.text = reel.likeCount.toString()
//                holder.likeButton.setImageResource(
//                    if (reel.likedByUser) R.drawable.liked_heart else R.drawable.unliked_heart,
//                )
                val rootView =
                    (holder.itemView.context as Activity)
                        .findViewById<View>(android.R.id.content)

                Snackbar.make(rootView, "API call failed!", Snackbar.LENGTH_SHORT).show()
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val isSuccess =
                        withTimeoutOrNull(2000L) {
                            if (previousLikedState) {
                                dbHelper.dislikeReel(reel.reelId)
                            } else {
                                dbHelper.likeReel(reel.reelId)
                            }
                        } ?: false

                    if (isSuccess) {
                        Log.d("apiStatus", "Sync successful")
                    } else {
                        snackbarDisplay()
                        Log.e("apiStatus", "Sync failed, adding to pending likes")
                        val existingPendingLike = pendingReelLikeDbHelper.getPendingLike(reel.reelId)
                        if (existingPendingLike == null) {
                            pendingReelLikeDbHelper.addPendingLike(reel.reelId, reel.likedByUser)
                        } else {
                            pendingReelLikeDbHelper.removePendingLike(reel.reelId)
                        }
                    }
                } catch (e: Exception) {
                    snackbarDisplay()
                    Log.e("apiStatus", "Error: ${e.message}")
                    val existingPendingLike = pendingReelLikeDbHelper.getPendingLike(reel.reelId)
                    if (existingPendingLike == null) {
                        pendingReelLikeDbHelper.addPendingLike(reel.reelId, reel.likedByUser)
                    } else {
                        pendingReelLikeDbHelper.removePendingLike(reel.reelId)
                    }
                }
            }
        }

        holder.pfpImage.load(reel.profilePicture) {
            transformations(CircleCropTransformation())
        }

        val upstreamFactory = DefaultDataSource.Factory(context)
        val cacheDataSourceFactory =
            CacheDataSource
                .Factory()
                .setCache(
                    ExoPlayerCache.get(context),
                ).setUpstreamDataSourceFactory(upstreamFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory)
        holder.player =
            ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()

        holder.reelVideo.player = holder.player
        val uri = reel.reelVideo.toUri()
        val mediaItem = MediaItem.fromUri(uri)
        holder.player?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 1f
        }

        holder.reelVideo.setOnClickListener {
            if (true == holder.player?.isPlaying) {
                holder.player?.pause()
            } else {
                holder.player?.play()
            }
        }

        holder.muteButton.setOnClickListener {
            if (isMuted) {
                holder.muteButton.load(R.drawable.unmuted) {
                    transformations(CircleCropTransformation())
                }

                holder.player?.volume = 1f
                isMuted = false
            } else {
                holder.muteButton.load(R.drawable.muted) {
                    transformations(CircleCropTransformation())
                }

                holder.player?.volume = 0f
                isMuted = true
            }
        }
    }

    override fun getItemCount(): Int = elements.size

    fun likeUnsyncedPosts() {
        CoroutineScope(Dispatchers.IO).launch {
            val pendingLikes = pendingReelLikeDbHelper.getAllPendingLikes()
            Log.d("pendingLikes", "Size of pending reel likes: ${pendingLikes.size}")

            for (pending in pendingLikes) {
                Log.d("apiStatus", "Trying like for: ${pending.reelId}")
                try {
                    val response =
                        if (pending.liked) {
                            RetrofitApiClient.reelsApiService.likeReel(
                                LikeReelBody(true, pending.reelId),
                            )
                        } else {
                            RetrofitApiClient.reelsApiService.dislikeReel()
                        }

                    if (response.isSuccessful) {
                        pendingReelLikeDbHelper.removePendingLike(pending.reelId)
                        Log.d("apiStatus", "Synced like: ${pending.reelId}")
                    }
                } catch (e: Exception) {
                    Log.e("apiStatus", "Sync failed", e)
                }
            }
        }
    }

    override fun onViewRecycled(holder: Holder) {
        super.onViewRecycled(holder)
        holder.player?.apply {
            release()
        }
        holder.player = null
    }

    override fun onViewAttachedToWindow(holder: Holder) {
        super.onViewAttachedToWindow(holder)
        currentHolder = holder
        holder.player?.prepare()
        holder.player?.play()

        holder.muteButton.load(R.drawable.unmuted) {
            transformations(CircleCropTransformation())
        }

        holder.player?.volume = 1f
    }

    override fun onViewDetachedFromWindow(holder: Holder) {
        super.onViewDetachedFromWindow(holder)
        if (holder == currentHolder) {
            currentHolder = null
        }
        holder.player?.stop()

        holder.muteButton.load(R.drawable.muted) {
            transformations(CircleCropTransformation())
        }

        holder.player?.volume = 0f
    }

    fun pausePlayers() {
        currentHolder?.player?.pause()
    }

    fun resumePlayers() {
        currentHolder?.player?.play()
    }

    inner class Holder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.usernameText)
        val likeButton: ImageView = view.findViewById(R.id.likeButton)
        val likeCount: TextView = view.findViewById(R.id.likeCountText)
        val pfpImage: ImageView = view.findViewById(R.id.profilePictureImage)
        val reelVideo: PlayerView = view.findViewById(R.id.reelVideo)
        var player: ExoPlayer? = null
        val muteButton: ImageView = view.findViewById(R.id.muteButton)
    }
}
