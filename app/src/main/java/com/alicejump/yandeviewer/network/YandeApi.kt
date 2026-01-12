package com.alicejump.yandeviewer.network

import com.alicejump.yandeviewer.model.Post
import retrofit2.http.GET
import retrofit2.http.Query

interface YandeApi {
    @GET("post.json")
    suspend fun getPosts(
        @Query("tags") tags: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int
    ): List<Post>
}
