package com.alicejump.yandeviewer.sync

import android.content.Context
import android.util.Log
import com.alicejump.yandeviewer.model.Artist
import com.alicejump.yandeviewer.model.ArtistAlias
import com.alicejump.yandeviewer.model.ArtistsArchive
import com.alicejump.yandeviewer.network.RetrofitClient
import com.alicejump.yandeviewer.viewmodel.ArtistCache
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "ArtistSyncer"

/**
 * 来自 yande.re API 的艺术家 DTO
 * 
 * 对应 /artist.json 的响应格式
 */
data class ArtistDto(
    val id: Int,
    val name: String,
    @SerializedName("alias_id")
    val aliasId: Int? = null,
    val urls: List<String> = emptyList()
)

/**
 * 艺术家数据增量同步器
 * 
 * 职责：
 * - 从 yande.re API 增量拉取艺术家数据
 * - 解析为本地 ArtistsArchive 格式
 * - 合并到本地 ArtistCache
 * - 跟踪最后同步 ID
 * 
 * 核心流程：
 * 1. 获取本地 max_id
 * 2. 从 API 拉取全量数据（order=date）
 * 3. 过滤出 id > max_id 的记录
 * 4. 按语言检测填充别名
 * 5. 构建 name_index
 * 6. 合并到本地
 * 
 * 调用点：
 * - MyApplication.onCreate() - 应用启动时
 * - (可选) 定期任务 - WorkManager 或 AlarmManager
 */
object ArtistSyncer {

    private val isSyncing = AtomicBoolean(false)

    /**
     * 启动艺术家数据同步
     * 
     * @param context Android Context
     */
    fun launchSync(context: Context) {
        // 防止并发同步
        if (isSyncing.getAndSet(true)) {
            Log.d(TAG, "Sync already in progress")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                performSync(context)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
            } finally {
                isSyncing.set(false)
            }
        }
    }

    /**
     * 执行实际的同步操作
     */
    private suspend fun performSync(context: Context) = withContext(Dispatchers.IO) {
        // 1. 获取本地状态
        val currentMaxId = ArtistCache.getMaxId() ?: 0
        Log.d(TAG, "Current max_id = $currentMaxId")

        // 2. 从 API 拉取数据
        val newArtistsData = try {
            RetrofitClient.api.getArtistsByPage(page = 1, limit = 2000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch artists from API", e)
            return@withContext
        }

        if (newArtistsData.isEmpty()) {
            Log.d(TAG, "No new data from API")
            return@withContext
        }

        // 3. 处理增量数据
        val (processedArtists, newMaxId, newCount) = processArtistData(
            newArtistsData,
            currentMaxId,
            ArtistCache.getMaxId() ?: 0
        )

        if (processedArtists.isEmpty()) {
            Log.d(TAG, "No updates needed")
            return@withContext
        }

        // 4. 构建新的 ArtistsArchive
        val newArchive = ArtistsArchive(
            max_id = newMaxId,
            artists = processedArtists,
            name_index = buildNameIndex(processedArtists)
        )

        // 5. 合并到本地
        ArtistCache.mergeArchive(context, newArchive)
        ArtistCache.updateLastSyncedId(context, newMaxId)

        Log.d(TAG, "Sync completed: $newCount new records, new max_id = $newMaxId")
    }

    /**
     * 处理来自 API 的艺术家数据
     * 
     * 流程：
     * 1. 构建 id_map（快速查找）
     * 2. 遍历 API 数据，过滤 id > currentMaxId
     * 3. 根据 alias_id 判断主记录或别名
     * 4. 语言检测别名
     * 5. 去重 URL
     */
    private fun processArtistData(
        apiData: List<ArtistDto>,
        currentMaxId: Int,
        localMaxId: Int
    ): Triple<Map<String, Artist>, Int, Int> {
        val idMap = mutableMapOf<Int, MutableMap<String, Any?>>()
        var newCount = 0
        var highestId = currentMaxId

        for (item in apiData) {
            val currentId = item.id

            // 只处理比本地 max_id 大的
            if (currentId <= currentMaxId) {
                continue
            }

            val mainId = item.aliasId ?: currentId

            // 如果主记录不存在则创建
            if (mainId !in idMap) {
                idMap[mainId] = mutableMapOf(
                    "name" to (if (item.aliasId == null) item.name else null),
                    "aliases" to mutableListOf<ArtistAlias>(),
                    "urls" to mutableSetOf<String>()
                )
            }

            val record = idMap[mainId]!!

            // 如果是主记录 (alias_id == null)
            if (item.aliasId == null) {
                record["name"] = item.name
            }
            // 如果是别名 (alias_id != null)
            else {
                val lang = detectLanguage(item.name)
                val aliases = record["aliases"] as MutableList<ArtistAlias>

                // 检查是否已存在此别名
                val existingAlias = aliases.find { it.id == currentId }
                if (existingAlias != null) {
                    // 合并多个语言到同一别名
                    val newAlias = createAliasFromLanguage(currentId, lang, item.name)
                    val mergedAlias = existingAlias.merge(newAlias)
                    val indexToReplace = aliases.indexOfFirst { it.id == currentId }
                    if (indexToReplace >= 0) {
                        aliases[indexToReplace] = mergedAlias
                    }
                } else {
                    // 创建新别名
                    aliases.add(createAliasFromLanguage(currentId, lang, item.name))
                }
            }

            // URLs 去重
            @Suppress("UNCHECKED_CAST")
            val urls = record["urls"] as MutableSet<String>
            urls.addAll(item.urls)

            newCount++
            highestId = maxOf(highestId, currentId)
        }

        // 转换为最终格式
        val artists = mutableMapOf<String, Artist>()
        for ((id, record) in idMap) {
            val name = record["name"] as? String
            if (name != null) {
                @Suppress("UNCHECKED_CAST")
                val aliases = (record["aliases"] as List<ArtistAlias>)
                @Suppress("UNCHECKED_CAST")
                val urls = (record["urls"] as Set<String>).toList()

                artists[id.toString()] = Artist(
                    name = name,
                    aliases = aliases,
                    urls = urls
                )
            }
        }

        return Triple(artists, highestId, newCount)
    }

    /**
     * 检测文本的语言
     * 
     * 返回值：jp, zh, ko, ru, el, ar, en（默认）
     */
    private fun detectLanguage(text: String): String {
        return when {
            text.matches(Regex(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u31F0-\\u31FF].*")) -> "jp"
            text.matches(Regex(".*[\\u4e00-\\u9fff].*")) -> "zh"
            text.matches(Regex(".*[\\uAC00-\\uD7AF].*")) -> "ko"
            text.matches(Regex(".*[\\u0400-\\u04FF].*")) -> "ru"
            text.matches(Regex(".*[\\u0370-\\u03FF].*")) -> "el"
            text.matches(Regex(".*[\\u0600-\\u06FF].*")) -> "ar"
            else -> "en"
        }
    }

    /**
     * 根据语言创建别名对象
     */
    private fun createAliasFromLanguage(id: Int, language: String, name: String): ArtistAlias {
        return ArtistAlias(
            id = id,
            jp = if (language == "jp") name else null,
            zh = if (language == "zh") name else null,
            ko = if (language == "ko") name else null,
            ru = if (language == "ru") name else null,
            el = if (language == "el") name else null,
            ar = if (language == "ar") name else null,
            en = if (language == "en") name else null
        )
    }

    /**
     * 构建 name_index：所有名称（主名称和别名）到 artist id 的映射
     */
    private fun buildNameIndex(artists: Map<String, Artist>): Map<String, Int> {
        val index = mutableMapOf<String, Int>()

        for ((idStr, artist) in artists) {
            val id = idStr.toIntOrNull() ?: continue

            // 添加主名称
            index[artist.name] = id

            // 添加所有别名
            for (alias in artist.aliases) {
                alias.getAllNames().forEach { name ->
                    if (name.isNotBlank()) {
                        index[name] = id
                    }
                }
            }
        }

        return index
    }

    /**
     * 强制完全更新（跳过增量检查）
     * 
     * 用于恢复或重新初始化艺术家数据
     */
    fun launchFullSync(context: Context) {
        ArtistCache.clearCache(context)
        launchSync(context)
    }

    /**
     * 检查是否正在同步中
     */
    fun isSyncing(): Boolean {
        return isSyncing.get()
    }
}
