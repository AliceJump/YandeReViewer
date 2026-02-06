package com.alicejump.yandeviewer.data

object BlacklistManager {

    private val blacklist = mutableSetOf<String>()

    fun add(tag: String) {
        blacklist.add(tag)
    }

    fun remove(tag: String) {
        blacklist.remove(tag)
    }

    fun getAll(): Set<String> {
        return blacklist
    }

    fun contains(tag: String): Boolean {
        return blacklist.contains(tag)
    }
}
