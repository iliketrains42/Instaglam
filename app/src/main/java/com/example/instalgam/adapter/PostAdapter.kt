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
import com.example.instalgam.apiClient.LikeBody
import com.example.instalgam.apiClient.RetrofitApiClient
import com.example.instalgam.model.LikeResponse
import com.example.instalgam.model.Post
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Response

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

        holder.likeButton.setOnClickListener {
            // Save previous state in case we need to rollback
            val previousLikedState = post.likedByUser
            val previousLikeCount = post.likeCount

            // UI updates first
            if (post.likedByUser) {
                post.likeCount--
                post.likedByUser = false
                holder.likeButton.setImageResource(R.drawable.unliked_heart)
            } else {
                post.likeCount++
                post.likedByUser = true
                holder.likeButton.setImageResource(R.drawable.liked_heart)
            }

            holder.likeCount.text = post.likeCount.toString()

            val call =
                if (post.likedByUser) {
                    val likeBody = LikeBody(true, post.postId)
                    RetrofitApiClient.apiService.likePost(likeBody)
                } else {
                    RetrofitApiClient.apiService.dislikePost()
                }
            // API call occurs later
            call.enqueue(
                object : retrofit2.Callback<LikeResponse> {
                    override fun onResponse(
                        call: Call<LikeResponse>,
                        response: Response<LikeResponse>,
                    ) {
                        // UI is reset if the call is unsuccessful
                        if (!response.isSuccessful) {
                            rollback()
                            showSnackbar()
                        } else {
                            Log.d("apiStatus", "Success: ${response.body()}")
                        }
                    }

                    override fun onFailure(
                        call: Call<LikeResponse>,
                        t: Throwable,
                    ) {
                        Log.e("apiStatus", "Failed: ${t.message}")
                        showSnackbar()
                        rollback()
                    }

                    // Function to rollback UI changes in the case of API call failure
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
                    }

                    fun showSnackbar() {
                        Snackbar
                            .make(
                                holder.itemView,
                                "API Call failed!",
                                Snackbar.LENGTH_SHORT,
                            ).show()
                    }
                },
            )
        }

//        Picasso.get().load(post.profilePicture).into(holder.pfpImage)
//        Picasso.get().load(post.postImage).into(holder.postImage)
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
