package com.alicejump.yandeviewer.network

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.github.chrisbanes.photoview.BuildConfig
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    @GET("/repos/{owner}/{repo}/releases")
    suspend fun getAllReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<GitHubRelease>
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

    /**
     * 获取最新版本 release
     */
    suspend fun getLatestRelease(owner: String, repo: String): GitHubRelease =
        api.getLatestRelease(owner, repo)

    /**
     * 获取所有 release
     */
    suspend fun getAllReleases(owner: String, repo: String): List<GitHubRelease> =
        api.getAllReleases(owner, repo)


    /**
     * 简单版本号比较函数
     * 返回 true 表示 version > current
     */
    private fun isVersionNewer(version: String, current: String): Boolean {
        val v1 = version.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val v2 = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(v1.size, v2.size)) {
            val n1 = v1.getOrElse(i) { 0 }
            val n2 = v2.getOrElse(i) { 0 }
            if (n1 > n2) return true
            if (n1 < n2) return false
        }
        return false
    }
}

/**
 * 占位类，可忽略或删除
 */
class GitHubApi(context: kotlinx.coroutines.CoroutineDispatcher, block: Any)
