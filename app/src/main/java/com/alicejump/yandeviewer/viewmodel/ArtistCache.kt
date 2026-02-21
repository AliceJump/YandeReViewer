package com.alicejump.yandeviewer.viewmodel

import android.content.Context
import com.alicejump.yandeviewer.model.Artist
import com.alicejump.yandeviewer.model.ArtistsArchive
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 艺术家数据缓存管理器
 * 
 * 职责：
 * - 从 assets 或本地文件加载 yandere_artists_archived_by_id.json
 * - 在内存中维护艺术家数据的高效索引（name_index）
 * - 提供快速的名称查询和别名解析
 * - 支持增量更新（通过 max_id 追踪）
 * 
 * 数据结构：
 * ```
 * {
 *   "max_id": 55473,
 *   "artists": {
 *     "55473": { "name": "hotvenus", "aliases": [...], "urls": [...] }
 *   },
 *   "name_index": {
 *     "hotvenus": 55473,
 *     "ホットビーナス": 55473
 *   }
 * }
 * ```
 */
object ArtistCache {

    // ===== 常量 =====
    const val ARTISTS_ARCHIVE_FILE = "yandere_artists_archived_by_id.json"
    const val ARTISTS_LAST_ID_FILE = "artists_last_id.txt"

    // ===== 内存缓存 =====
    private var cachedArchive: ArtistsArchive? = null
    private val gson = Gson()

    // ===== 初始化 =====

    /**
     * 初始化：从 assets 或本地文件加载艺术家数据
     * 
     * 流程：
     * 1. 检查本地文件是否存在
     * 2. 如不存在，从 assets 复制
     * 3. 加载到内存缓存
     */
    suspend fun initialize(context: Context) {
        withContext(Dispatchers.IO) {
            val targetFile = File(context.filesDir, ARTISTS_ARCHIVE_FILE)

            // 首次启动：从 assets 复制
            if (!targetFile.exists()) {
                ensureAssetCopied(context)
            }

            // 加载到内存
            loadArchive(context)
        }
    }

    /**
     * 从 assets 复制初始数据文件
     */
    private fun ensureAssetCopied(context: Context) {
        try {
            val targetFile = File(context.filesDir, ARTISTS_ARCHIVE_FILE)
            context.assets.open(ARTISTS_ARCHIVE_FILE).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            // 文件可能不存在于 assets，继续执行
            e.printStackTrace()
        }
    }

    /**
     * 从文件加载艺术家数据到内存
     */
    private fun loadArchive(context: Context) {
        try {
            val file = File(context.filesDir, ARTISTS_ARCHIVE_FILE)
            if (file.exists()) {
                val json = file.readText()
                cachedArchive = gson.fromJson(json, ArtistsArchive::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cachedArchive = null
        }
    }

    // ===== 查询接口 =====

    /**
     * 通过艺术家名称（包括别名）查询 ID
     * 
     * 示例：
     * ```
     * getArtistId("hotvenus")      // → 55473
     * getArtistId("ホットビーナス") // → 55473 (别名)
     * ```
     */
    fun getArtistId(name: String): Int? {
        return cachedArchive?.name_index?.get(name)
    }

    /**
     * 通过 ID 查询完整艺术家数据
     * 
     * 示例：
     * ```
     * getArtist(55473) // → Artist(name="hotvenus", aliases=[...], urls=[...])
     * ```
     */
    fun getArtist(id: Int): Artist? {
        return cachedArchive?.artists?.get(id.toString())
    }

    /**
     * 批量查询艺术家名称对应的 ID
     * 
     * 示例：
     * ```
     * getArtistIds(setOf("hotvenus", "unknown")) 
     * // → {"hotvenus": 55473}
     * ```
     */
    fun getArtistIds(names: Set<String>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        names.forEach { name ->
            getArtistId(name)?.let { id ->
                result[name] = id
            }
        }
        return result
    }

    /**
     * 批量查询艺术家名称对应的完整数据
     * 
     * 示例：
     * ```
     * getArtists(setOf("hotvenus", "unknown"))
     * // → {"hotvenus": Artist(...), "unknown": null}
     * ```
     */
    fun getArtists(names: Set<String>): Map<String, Artist?> {
        val result = mutableMapOf<String, Artist?>()
        names.forEach { name ->
            val id = getArtistId(name)
            result[name] = if (id != null) getArtist(id) else null
        }
        return result
    }

    /**
     * 获取所有艺术家名称集合（用于自动完成、搜索建议）
     * 
     * 返回所有主名称和别名
     */
    fun getAllArtistNames(): Set<String> {
        return cachedArchive?.name_index?.keys ?: emptySet()
    }

    /**
     * 获取当前最大 artist ID（用于增量更新）
     */
    fun getMaxId(): Int? {
        return cachedArchive?.max_id
    }

    /**
     * 获取总艺术家数量
     */
    fun getArtistCount(): Int {
        return cachedArchive?.getArtistCount() ?: 0
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return cachedArchive != null
    }

    // ===== 更新接口 =====

    /**
     * 更新艺术家数据（新增或合并）
     * 
     * 用于增量更新流程
     */
    suspend fun updateArchive(context: Context, newArchive: ArtistsArchive) {
        withContext(Dispatchers.IO) {
            cachedArchive = newArchive
            persistArchive(context, newArchive)
        }
    }

    /**
     * 合并新数据到现有数据
     * 
     * 示例：更新后的艺术家将覆盖旧数据
     */
    suspend fun mergeArchive(context: Context, newArchive: ArtistsArchive) {
        withContext(Dispatchers.IO) {
            if (cachedArchive == null) {
                cachedArchive = newArchive
            } else {
                // 合并 artists
                val mergedArtists = cachedArchive!!.artists.toMutableMap()
                mergedArtists.putAll(newArchive.artists)

                // 合并 name_index
                val mergedIndex = cachedArchive!!.name_index.toMutableMap()
                mergedIndex.putAll(newArchive.name_index)

                // 更新 max_id
                val newMaxId = maxOf(cachedArchive!!.max_id, newArchive.max_id)

                val merged = ArtistsArchive(
                    max_id = newMaxId,
                    artists = mergedArtists,
                    name_index = mergedIndex
                )

                cachedArchive = merged
                persistArchive(context, merged)
            }
        }
    }

    /**
     * 保存艺术家数据到本地文件
     */
    private fun persistArchive(context: Context, archive: ArtistsArchive) {
        try {
            val file = File(context.filesDir, ARTISTS_ARCHIVE_FILE)
            file.writeText(gson.toJson(archive))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ===== 同步追踪 =====

    /**
     * 获取最后同步的 ID
     * 
     * 用于增量更新：只请求 id > lastSyncedId 的数据
     */
    suspend fun getLastSyncedId(context: Context): Int {
        return withContext(Dispatchers.IO) {
            File(context.filesDir, ARTISTS_LAST_ID_FILE).let {
                if (it.exists()) it.readText().toIntOrNull() ?: 0 else 0
            }
        }
    }

    /**
     * 更新最后同步 ID
     */
    suspend fun updateLastSyncedId(context: Context, newId: Int) {
        withContext(Dispatchers.IO) {
            File(context.filesDir, ARTISTS_LAST_ID_FILE).writeText(newId.toString())
        }
    }

    // ===== 调试/重置 =====

    /**
     * 清空所有缓存数据（调试用）
     */
    fun clearCache(context: Context) {
        cachedArchive = null
        File(context.filesDir, ARTISTS_ARCHIVE_FILE).delete()
        File(context.filesDir, ARTISTS_LAST_ID_FILE).delete()
    }

    /**
     * 获取缓存统计信息
     */
    fun getStats(): String {
        return if (cachedArchive != null) {
            "Artists: ${cachedArchive!!.getArtistCount()}, " +
                    "Names: ${cachedArchive!!.getAllNames().size}, " +
                    "MaxId: ${cachedArchive!!.max_id}"
        } else {
            "Not initialized"
        }
    }
}
