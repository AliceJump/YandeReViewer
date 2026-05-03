package com.alicejump.yandeviewer.data

import android.content.Context
import com.alicejump.yandeviewer.model.Post
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object BrowsingHistoryManager {

    private const val PREF_NAME = "browsing_history"
    private const val KEY_LIST = "history_posts"
    private const val MAX_HISTORY = 200

    fun recordView(context: Context, post: Post) {
        val list = getAll(context).toMutableList()
        list.removeAll { it.id == post.id }
        // favoriteAt is reused here as the "viewed at" timestamp since Post is a shared Parcelable
        val newPost = post.copy(favoriteAt = System.currentTimeMillis())
        list.add(0, newPost)
        if (list.size > MAX_HISTORY) {
            saveAll(context, list.subList(0, MAX_HISTORY))
        } else {
            saveAll(context, list)
        }
    }

    fun getAll(context: Context): List<Post> {
        val json = prefs(context).getString(KEY_LIST, null) ?: return emptyList()
        return try {
            Gson().fromJson(json, object : TypeToken<List<Post>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_LIST).apply()
    }

    private fun saveAll(context: Context, list: List<Post>) {
        prefs(context).edit().putString(KEY_LIST, Gson().toJson(list)).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
