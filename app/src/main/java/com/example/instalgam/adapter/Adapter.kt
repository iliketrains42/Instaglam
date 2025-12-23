package com.example.instalgam.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.instalgam.R
import com.example.instalgam.model.Post
import com.squareup.picasso.Picasso
import java.net.URL

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
            post.like()
            holder.likeCount.text = post.likeCount.toString()
            if (post.likedByUser) {
                holder.likeButton.setImageResource(R.drawable.liked_heart)
            } else {
                holder.likeButton.setImageResource(R.drawable.unliked_heart)
            }
        }

        // var url = URL(post.profilePicture)
        // var bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        // holder.pfpImage.setImageBitmap(bmp)
        // url = URL(post.postImage)
        // bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        // holder.postImage.setImageBitmap(bmp)
//        holder.postImage.setImageResource(R.drawable.liked_heart)
//        holder.pfpImage.setImageResource(R.drawable.unliked_heart)
        Picasso.get().load(post.profilePicture).into(holder.pfpImage)
        Picasso.get().load(post.postImage).into(holder.postImage)
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
