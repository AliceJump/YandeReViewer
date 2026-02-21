package com.alicejump.yandeviewer.model

/**
 * Artist 别名信息
 * 
 * 包含该 alias 的多语言名称，语言代码（jp, zh, ko, ru, el, ar, en）
 * 
 * 注：由于 data class 不可变，建议在生成时就填充所有语言字段
 */
data class ArtistAlias(
    val id: Int,            // alias 原始 id
    val jp: String? = null, // 日文名称
    val zh: String? = null, // 中文名称
    val ko: String? = null, // 韩文名称
    val ru: String? = null, // 俄文名称
    val el: String? = null, // 希腊文名称
    val ar: String? = null, // 阿拉伯文名称
    val en: String? = null  // 英文名称（默认）
) {
    /**
     * 获取所有非空的名称
     */
    fun getAllNames(): Set<String> = setOfNotNull(
        jp, zh, ko, ru, el, ar, en
    )

    /**
     * 获取优先级最高的名称（按语言优先级：jp > zh > ko > ru > el > ar > en）
     */
    fun getPrimaryName(): String? {
        return jp ?: zh ?: ko ?: ru ?: el ?: ar ?: en
    }

    /**
     * 合并多个别名对象（用于同一个 id 有多条记录）
     * 
     * 示例：
     * ```
     * alias1 = ArtistAlias(id=55474, jp="ホットビーナス")
     * alias2 = ArtistAlias(id=55474, en="hot venus")
     * merged = alias1.merge(alias2)  // ArtistAlias(id=55474, jp="...", en="...")
     * ```
     */
    fun merge(other: ArtistAlias): ArtistAlias {
        if (this.id != other.id) {
            throw IllegalArgumentException("Cannot merge aliases with different ids")
        }
        
        return copy(
            jp = this.jp ?: other.jp,
            zh = this.zh ?: other.zh,
            ko = this.ko ?: other.ko,
            ru = this.ru ?: other.ru,
            el = this.el ?: other.el,
            ar = this.ar ?: other.ar,
            en = this.en ?: other.en
        )
    }
}

/**
 * 单个 Artist 完整数据
 * 
 * 包含主名称、所有别名和关联网址
 */
data class Artist(
    val name: String,               // 主名称（alias_id = null 的那条）
    val aliases: List<ArtistAlias>, // 所有别名列表
    val urls: List<String>          // 所有去重后的关联网址
) {
    /**
     * 获取该艺术家的所有名称（包括别名）
     */
    fun getAllNames(): Set<String> {
        val names = mutableSetOf(name)
        aliases.forEach { names.addAll(it.getAllNames()) }
        return names
    }

    /**
     * 获取特定语言的别名
     */
    fun getNamesByLanguage(language: String): Set<String> {
        val names = mutableSetOf<String>()
        
        // 检查主名称是否匹配
        when (language) {
            "jp" -> names.add(name)  // 假设主名称是日文
            else -> {} // 根据实际调整
        }
        
        // 检查所有别名
        aliases.forEach { alias ->
            when (language) {
                "jp" -> alias.jp?.let { names.add(it) }
                "zh" -> alias.zh?.let { names.add(it) }
                "ko" -> alias.ko?.let { names.add(it) }
                "ru" -> alias.ru?.let { names.add(it) }
                "el" -> alias.el?.let { names.add(it) }
                "ar" -> alias.ar?.let { names.add(it) }
                "en" -> alias.en?.let { names.add(it) }
                else -> {}
            }
        }
        
        return names
    }
}

/**
 * 艺术家数据库顶层结构
 * 
 * 对应 yandere_artists_archived_by_id.json 的完整格式
 */
data class ArtistsArchive(
    val max_id: Int,                        // 当前本地数据中的最大 artist id（用于增量更新）
    val artists: Map<String, Artist>,       // 主 artist 数据表，以主 id 为 key
    val name_index: Map<String, Int>        // 名称到主 id 的索引表（支持快速查询）
) {
    /**
     * 快速查询：通过名称（主名或别名）获取 artist id
     */
    fun getArtistIdByName(name: String): Int? {
        return name_index[name]
    }

    /**
     * 快速查询：通过 id 获取 artist 完整数据
     */
    fun getArtistById(id: Int): Artist? {
        return artists[id.toString()]
    }

    /**
     * 批量查询：获取多个名称对应的 id 和数据
     */
    fun getArtistsByNames(names: Set<String>): Map<String, Artist?> {
        val result = mutableMapOf<String, Artist?>()
        names.forEach { name ->
            val id = getArtistIdByName(name)
            result[name] = if (id != null) getArtistById(id) else null
        }
        return result
    }

    /**
     * 获取所有艺术家名称（用于搜索建议、自动完成等）
     */
    fun getAllNames(): Set<String> {
        return name_index.keys
    }

    /**
     * 获取总艺术家数量
     */
    fun getArtistCount(): Int {
        return artists.size
    }
}
