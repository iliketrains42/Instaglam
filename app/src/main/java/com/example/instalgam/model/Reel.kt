package com.example.instalgam.model

import com.squareup.moshi.Json

data class Reel(
    @Json(name = "reel_id")
    val reelId: String,
    @Json(name = "user_name")
    val userName: String,
    @Json(name = "user_image")
    val profilePicture: String,
    @Json(name = "reel_video")
    val reelVideo: String,
    @Json(name = "like_count")
    var likeCount: Int,
    @Json(name = "liked_by_user")
    var likedByUser: Boolean,
)

data class ReelResponse(
    @Json(name = "reels")
    val posts: MutableList<Reel>,
)
