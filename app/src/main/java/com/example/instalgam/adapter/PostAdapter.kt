package com.example.instalgam.adapter

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import com.example.instalgam.R
import com.example.instalgam.model.Post
import com.example.instalgam.room.PostDatabase
import com.example.instalgam.room.PostDatabaseHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PostAdapter(
    val context: Context,
    val elements: MutableList<Post>,
) : RecyclerView.Adapter<PostAdapter.Holder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.post_layout, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int,
    ) {
        val post = elements[position]
        holder.likeCount.text = post.likeCount.toString()

        if (post.likedByUser) {
            holder.likeButton.setImageResource(R.drawable.liked_heart)
        } else {
            holder.likeButton.setImageResource(R.drawable.unliked_heart)
        }

        holder.username.text = post.userName

        val db = PostDatabase.getInstance(context.applicationContext)
        val dbHelper = PostDatabaseHelper(db.postDao())

        holder.likeButton.setOnClickListener {
            val previousLikedState = post.likedByUser
            val previousLikeCount = post.likeCount

            post.likedByUser = !previousLikedState
            post.likeCount += if (post.likedByUser) 1 else -1

            holder.likeCount.text = post.likeCount.toString()
            holder.likeButton.setImageResource(
                if (post.likedByUser) R.drawable.liked_heart else R.drawable.unliked_heart,
            )

            fun rollbackUI() {
                post.likedByUser = previousLikedState
                post.likeCount = previousLikeCount
                holder.likeCount.text = post.likeCount.toString()
                holder.likeButton.setImageResource(
                    if (post.likedByUser) R.drawable.liked_heart else R.drawable.unliked_heart,
                )
                val rootView =
                    (holder.itemView.context as Activity)
                        .findViewById<View>(android.R.id.content)

                Snackbar.make(rootView, "Action failed. Please try again.", Snackbar.LENGTH_SHORT).show()
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val isSuccess =
                        withTimeoutOrNull(2000L) {
                            if (previousLikedState) {
                                dbHelper.dislikePost(post.postId)
                            } else {
                                dbHelper.likePost(post.postId)
                            }
                        } ?: false

                    if (isSuccess) {
                        Log.d("apiStatus", "Sync successful")
                    } else {
                        Log.e("apiStatus", "Sync failed, rolling back UI")
                        rollbackUI()
                    }
                } catch (e: Exception) {
                    Log.e("apiStatus", "Error: ${e.message}")
                    rollbackUI()
                }
            }
        }

        holder.pfpImage.load(post.profilePicture) {
            transformations(CircleCropTransformation())
        }
        holder.postImage.load(post.postImage)
    }

    override fun getItemCount(): Int = elements.size

    inner class Holder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.usernameText)
        val likeButton: ImageView = view.findViewById(R.id.likeButton)
        val likeCount: TextView = view.findViewById(R.id.likeCountText)
        val postImage: ImageView = view.findViewById(R.id.postPicture)
        val pfpImage: ImageView = view.findViewById(R.id.profilePictureImage)
    }
}
