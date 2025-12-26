package com.example.instalgam.model

import com.squareup.moshi.Json

data class LikeResponse(
    @Json(name = "status")
    val status: String,
    @Json(name = "message")
    val message: String,
    @Json(name = "timestamp")
    val timestamp: String,
)
