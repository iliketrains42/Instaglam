package com.example.instalgam.apiClient

import com.example.instalgam.model.LikeResponse
import com.example.instalgam.model.PostResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

object RetrofitApiClient {
    private const val BASE_URL = "https://dfbf9976-22e3-4bb2-ae02-286dfd0d7c42.mock.pstmn.io/user/"
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val retrofit: Retrofit by lazy {
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    val apiService: PostsApiService by lazy {
        retrofit.create(PostsApiService::class.java)
    }
}

data class LikeBody(
    val like: Boolean,
    val post_id: String,
)

interface PostsApiService {
    @GET("feed")
    fun fetchPosts(): retrofit2.Call<PostResponse>

    @POST("like")
    fun likePost(
        @Body likeBody: LikeBody,
    ): retrofit2.Call<LikeResponse>

    @DELETE("dislike")
    fun dislikePost(): retrofit2.Call<LikeResponse>
}
