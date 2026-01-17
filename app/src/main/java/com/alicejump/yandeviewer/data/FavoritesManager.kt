package com.alicejump.yandeviewer.data

import android.content.Context
import com.alicejump.yandeviewer.model.Post
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FavoritesManager {

    private const val PREF_NAME = "favorites"
    private const val KEY_LIST = "favorite_posts"

    // ───────── 基础接口 ─────────

    fun addFavorite(context: Context, post: Post) {
        val list = getAll(context).toMutableList()

        // 已存在就删旧的，避免重复
        list.removeAll { it.id == post.id }

        // 用“当前时间”作为新的收藏时间
        val newPost = post.copy(
            favoriteAt = System.currentTimeMillis()
        )

        list.add(newPost)

        saveAll(context, list)
    }

    fun removeFavorite(context: Context, postId: Long) {
        val list = getAll(context)
            .filterNot { it.id == postId }

        saveAll(context, list)
    }

    fun isFavorite(context: Context, postId: Long): Boolean {
        return getAll(context).any { it.id == postId }
    }

    fun getAll(context: Context): List<Post> {
        val json = prefs(context).getString(KEY_LIST, null)
            ?: return emptyList()

        return try {
            Gson().fromJson(
                json,
                object : TypeToken<List<Post>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ───────── 你的“收藏模式搜索核心” ─────────

    fun filterFavorites(
        context: Context,
        queryTags: List<String>
    ): List<Post> {

        return getAll(context)

            // 👍 核心：按收藏时间倒序
            .sortedByDescending { it.favoriteAt }

            .filter { post ->
                queryTags.all { q ->

                    when {
                        // rating:s / rating:q / rating:e
                        q.startsWith("rating:") -> {
                            val r = q.removePrefix("rating:")
                            post.rating == r
                        }

                        // 普通 tag
                        else -> {
                            val postTags = post.tags.split(" ")
                            postTags.contains(q)
                        }
                    }
                }
            }
    }

    // ───────── 工具 ─────────

    private fun saveAll(context: Context, list: List<Post>) {
        val json = Gson().toJson(list)

        prefs(context)
            .edit()
            .putString(KEY_LIST, json)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
