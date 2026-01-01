package com.example.instalgam.adapter

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
            // Save previous state in case we need to rollback
            val previousLikedState = post.likedByUser
            val previousLikeCount = post.likeCount

            post.likeCount += if (!post.likedByUser) 1 else -1
            holder.likeCount.text = post.likeCount.toString()
            holder.likeButton.setImageResource(if (!post.likedByUser) R.drawable.liked_heart else R.drawable.unliked_heart)

            fun rollback() {
                // Restore previous state
                post.likedByUser = previousLikedState
                post.likeCount = previousLikeCount

                holder.likeCount.text = post.likeCount.toString()
                holder.likeButton.setImageResource(
                    if (post.likedByUser) {
                        R.drawable.liked_heart
                    } else {
                        R.drawable.unliked_heart
                    },
                )

                Snackbar
                    .make(
                        holder.itemView,
                        "API Call failed!",
                        Snackbar.LENGTH_SHORT,
                    ).show()
                Log.e("apiStatus", "API call failed")
            }
            if (!post.likedByUser) {
                dbHelper.likePost(
                    post.postId,
                    onSuccess = {
                        Log.d("apiStatus", "Like API succeeded")
                    },
                    onFailing = {
                        rollback()
                    },
                )
            } else {
                dbHelper.dislikePost(
                    post.postId,
                    onSuccess = {
                        Log.d("apiStatus", "Dislike API succeeded")
                    },
                    onFailing = {
                        rollback()
                    },
                )
            }

            if (post.likedByUser) {
                post.likedByUser = false
            } else {
                post.likedByUser = true
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
