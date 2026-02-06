package com.alicejump.yandeviewer

import android.os.Bundle
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alicejump.yandeviewer.data.BlacklistManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class BlacklistActivity : AppCompatActivity() {

    private lateinit var chipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blacklist)

        // 获取 header 中的 Toolbar 并设置
        val headerView = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        val toolbar: Toolbar? = headerView?.findViewById(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.title = getString(R.string.blacklist_tags)
        }

        chipGroup = findViewById(R.id.blacklist_chip_group)

        loadBlacklistTags()
    }

    private fun loadBlacklistTags() {
        chipGroup.removeAllViews()

        val blacklistTags = BlacklistManager.getAll().sorted()

        if (blacklistTags.isEmpty()) {
            Toast.makeText(this, "黑名单为空", Toast.LENGTH_SHORT).show()
            return
        }

        blacklistTags.forEach { tag ->
            val chip = createTagChip(tag)
            chipGroup.addView(chip)
        }
    }

    private fun createTagChip(tagName: String): Chip {
        return Chip(this).apply {
            text = tagName
            isClickable = true
            isLongClickable = true

            setOnLongClickListener {
                showTagContextMenu(this, tagName)
                true
            }
        }
    }

    private fun showTagContextMenu(chip: Chip, tagName: String) {
        PopupMenu(this, chip).apply {
            menu.add("删除")

            setOnMenuItemClickListener { item ->
                when (item.title) {
                    "删除" -> {
                        BlacklistManager.remove(tagName)
                        chipGroup.removeView(chip)
                        Toast.makeText(
                            this@BlacklistActivity,
                            "已从黑名单删除：$tagName",
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}


