package com.example.instalgam.model

import android.media.Image

class Post(
    val postId: Int,
    val userName: String,
    // val profilePicture: Image,
    // val postImage: Image,
    var likeCount: Int,
    var likedByUser: Boolean,
) {
    fun like() {
        likedByUser = !likedByUser
        if (likedByUser) {
            likeCount++
        } else {
            likeCount--
        }
    }
}
