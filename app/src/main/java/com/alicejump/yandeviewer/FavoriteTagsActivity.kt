package com.alicejump.yandeviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alicejump.yandeviewer.data.FavoriteTagsManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class FavoriteTagsActivity : AppCompatActivity() {

    private lateinit var chipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_tags)

        // 获取 header 中的 Toolbar 并设置
        val headerView = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        val toolbar: Toolbar? = headerView?.findViewById(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.title = "收藏标签"
        }

        chipGroup = findViewById(R.id.favorite_tags_chip_group)

        loadFavoriteTags()
    }

    private fun loadFavoriteTags() {
        chipGroup.removeAllViews()

        val favoriteTags = FavoriteTagsManager.getAllTags(this).sorted()

        if (favoriteTags.isEmpty()) {
            Toast.makeText(this, "还没有收藏任何标签", Toast.LENGTH_SHORT).show()
            return
        }

        favoriteTags.forEach { tag ->
            val chip = createTagChip(tag)
            chipGroup.addView(chip)
        }
    }

    private fun createTagChip(tagName: String): Chip {
        return Chip(this).apply {
            text = tagName
            isClickable = true
            isFocusable = true
            isLongClickable = true

            // 点击 -> 跳转搜索该标签（启动新的 MainActivity，并传递复选框状态）
            setOnClickListener {
                val intent = Intent(this@FavoriteTagsActivity, MainActivity::class.java).apply {
                    putExtra(MainActivity.NEW_SEARCH_TAG, tagName)
                    // 传递三个复选框的状态（从 SharedPreferences 获取）
                    putExtra(MainActivity.EXTRA_RATING_S, getRatingSState())
                    putExtra(MainActivity.EXTRA_RATING_Q, getRatingQState())
                    putExtra(MainActivity.EXTRA_RATING_E, getRatingEState())
                }
                startActivity(intent)
            }

            // 长按 -> 删除或复制
            setOnLongClickListener {
                showTagContextMenu(this, tagName)
                true
            }
        }
    }

    private fun showTagContextMenu(chip: Chip, tagName: String) {
        PopupMenu(this, chip).apply {
            menu.add("删除")
            menu.add("复制")

            setOnMenuItemClickListener { item ->
                when (item.title) {
                    "删除" -> {
                        FavoriteTagsManager.removeFavoriteTag(this@FavoriteTagsActivity, tagName)
                        chipGroup.removeView(chip)
                        Toast.makeText(
                            this@FavoriteTagsActivity,
                            "已从收藏删除：$tagName",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    "复制" -> {
                        val clipboard = this@FavoriteTagsActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("tag", tagName)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            this@FavoriteTagsActivity,
                            "已复制到剪贴板",
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

    // 从 SharedPreferences 中获取复选框状态
    private fun getRatingSState(): Boolean {
        val prefs = getSharedPreferences("rating_state", MODE_PRIVATE)
        return prefs.getBoolean(MainActivity.EXTRA_RATING_S, false)
    }

    private fun getRatingQState(): Boolean {
        val prefs = getSharedPreferences("rating_state", MODE_PRIVATE)
        return prefs.getBoolean(MainActivity.EXTRA_RATING_Q, false)
    }

    private fun getRatingEState(): Boolean {
        val prefs = getSharedPreferences("rating_state", MODE_PRIVATE)
        return prefs.getBoolean(MainActivity.EXTRA_RATING_E, false)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

