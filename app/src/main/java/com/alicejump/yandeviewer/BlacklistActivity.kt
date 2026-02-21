package com.alicejump.yandeviewer

import android.os.Bundle
import com.alicejump.yandeviewer.viewmodel.ArtistCache
import com.alicejump.yandeviewer.utils.getArtistDisplayName
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.activity.OnBackPressedCallback
import com.alicejump.yandeviewer.data.BlacklistManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class BlacklistActivity : AppCompatActivity() {

    private lateinit var chipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blacklist)

        // Toolbar
        val headerView = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        val toolbar: Toolbar? = headerView?.findViewById(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = getString(R.string.blacklist_tags)
        }

        chipGroup = findViewById(R.id.blacklist_chip_group)
        loadBlacklistTags()

        // 使用 OnBackPressedDispatcher 替代 onBackPressed()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 原来的逻辑
                finish()
            }
        })
    }

    private fun loadBlacklistTags() {
        chipGroup.removeAllViews()

        val blacklistTags = BlacklistManager.getAll().sorted()

        if (blacklistTags.isEmpty()) {
            Toast.makeText(this, R.string.blacklist_is_empty, Toast.LENGTH_SHORT).show()
            return
        }

        blacklistTags.forEach { tag ->
            val chip = createTagChip(tag)
            chipGroup.addView(chip)
        }
    }

    private fun createTagChip(tagName: String): Chip {
        val displayName = ArtistCache.getArtistId(tagName)?.let { artistId ->
            ArtistCache.getArtist(artistId)?.let { getArtistDisplayName(it) }
        } ?: tagName

        return Chip(this).apply {
            text = displayName
            isClickable = true
            isLongClickable = true

            setOnLongClickListener {
                showTagContextMenu(this, tagName) // 操作仍用原始 tagName
                true
            }
        }
    }

    private fun showTagContextMenu(chip: Chip, tagName: String) {
        PopupMenu(this, chip).apply {
            menu.add(R.string.delete)

            setOnMenuItemClickListener { item ->
                when (item.title) {
                    getString(R.string.delete) -> {
                        BlacklistManager.remove(tagName)
                        chipGroup.removeView(chip)
                        Toast.makeText(
                            this@BlacklistActivity,
                            getString(R.string.removed_from_blacklist, tagName),
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }
}