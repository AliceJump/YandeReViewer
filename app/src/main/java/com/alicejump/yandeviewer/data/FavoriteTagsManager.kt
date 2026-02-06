package com.alicejump.yandeviewer.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FavoriteTagsManager {

    private const val PREF_NAME = "favorite_tags"
    private const val KEY_TAGS = "favorite_tags_list"

    // 添加收藏标签
    fun addFavoriteTag(context: Context, tag: String) {
        val tags = getAllTags(context).toMutableSet()
        tags.add(tag)
        saveTags(context, tags.toList())
    }

    // 删除收藏标签
    fun removeFavoriteTag(context: Context, tag: String) {
        val tags = getAllTags(context).filter { it != tag }
        saveTags(context, tags)
    }

    // 获取所有收藏标签
    fun getAllTags(context: Context): List<String> {
        val json = prefs(context).getString(KEY_TAGS, null) ?: return emptyList()

        return try {
            Gson().fromJson(
                json,
                object : TypeToken<List<String>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 检查是否是收藏标签
    fun isFavorite(context: Context, tag: String): Boolean {
        return getAllTags(context).contains(tag)
    }

    // 清空所有收藏标签
    fun clearAll(context: Context) {
        prefs(context).edit().putString(KEY_TAGS, null).apply()
    }

    // 私有方法：保存标签
    private fun saveTags(context: Context, tags: List<String>) {
        val json = Gson().toJson(tags)
        prefs(context).edit().putString(KEY_TAGS, json).apply()
    }

    // 私有方法：获取 SharedPreferences
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
