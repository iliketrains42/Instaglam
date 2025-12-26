package com.example.instalgam.model

import com.squareup.moshi.Json

data class Post(
    @Json(name = "post_id")
    val postId: String,
    @Json(name = "user_name")
    val userName: String,
    @Json(name = "user_image")
    val profilePicture: String,
    @Json(name = "post_image")
    val postImage: String,
    @Json(name = "like_count")
    var likeCount: Int,
    @Json(name = "liked_by_user")
    var likedByUser: Boolean,
)

data class PostResponse(
    @Json(name = "feed")
    val posts: MutableList<Post>,
)
