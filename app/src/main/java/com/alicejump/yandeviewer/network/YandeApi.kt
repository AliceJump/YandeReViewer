package com.alicejump.yandeviewer.network

import com.alicejump.yandeviewer.model.Post
import com.alicejump.yandeviewer.model.TagInfo
import com.alicejump.yandeviewer.sync.ArtistDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YandeApi {
    @GET("post.json")
    suspend fun getPosts(
        @Query("tags") tags: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int
    ): List<Post>

    @GET("tag.json")
    suspend fun getTags(
        @Query("name") name: String
    ): List<TagInfo>

    @GET("tag.json")
    suspend fun getTagsByPage(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 2000, // As per your logic
        @Query("order") order: String = "date"
    ): List<TagInfo>

    @GET("post.json?tags=id:{id}")
    suspend fun getPost(@Path("id") id: String): Response<List<Post>>

    // ===== 艺术家数据接口 =====
    
    /**
     * 获取艺术家数据（分页）
     * 
     * API: GET /artist.json?order=date&page=1&limit=2000
     * 
     * 返回：[{"id": 55473, "name": "hotvenus", "alias_id": null, "urls": [...]}, ...]
     * 
     * @param page 页码
     * @param limit 单页数量（通常 2000）
     * @param order 排序方式（通常 "date"）
     */
    @GET("artist.json")
    suspend fun getArtistsByPage(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 2000,
        @Query("order") order: String = "date"
    ): List<ArtistDto>
}
