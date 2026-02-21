package com.alicejump.yandeviewer.utils

import com.alicejump.yandeviewer.model.Artist
import java.util.Locale

fun getArtistDisplayName(artist: Artist): String {
    val systemLang = Locale.getDefault().language

    // 定义语言组（按优先顺序）
    val languageGroups = listOf(
        listOf("zh", "ja", "ko"), // 东亚语言组
        listOf("ru", "el", "ar"), // 欧洲及其他语言组
        listOf("en")              // 英语单独
    )

    // 按语言获取别名
    fun findAlias(lang: String): String? {
        for (alias in artist.aliases) {
            val name = when (lang) {
                "zh" -> alias.zh
                "ja" -> alias.jp
                "ko" -> alias.ko
                "ru" -> alias.ru
                "el" -> alias.el
                "ar" -> alias.ar
                "en" -> alias.en
                else -> null
            }
            if (!name.isNullOrEmpty()) return name
        }
        return null
    }

    // 1️⃣ 系统语言优先
    findAlias(systemLang)?.let { return it }

    // 2️⃣ 系统语言所在组优先（按顺序）
    val systemGroup = languageGroups.find { systemLang in it }
    systemGroup?.forEach { lang ->
        findAlias(lang)?.let { return it }
    }

    // 3️⃣ 其他组按顺序尝试
    languageGroups.forEach { group ->
        if (group != systemGroup) {
            group.forEach { lang ->
                findAlias(lang)?.let { return it }
            }
        }
    }

    // 4️⃣ fallback 主名称
    return artist.name
}