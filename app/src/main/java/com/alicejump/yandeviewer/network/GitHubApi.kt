package com.alicejump.yandeviewer.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Data class representing a release from the GitHub API.
 */
data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset>
)

/**
 * Data class for an asset within a GitHub release.
 */
data class GitHubAsset(
    @SerializedName("browser_download_url")
    val downloadUrl: String
)

/**
 * Retrofit interface for the GitHub API.
 */
interface GitHubApiService {
    @GET("/repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRelease
}

/**
 * Singleton object to provide a Retrofit instance for the GitHub API.
 */
object GitHubApiClient {
    private const val BASE_URL = "https://api.github.com/"

    val api: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }
}
