package com.alicejump.yandeviewer
import com.alicejump.yandeviewer.viewmodel.ArtistCache
import com.alicejump.yandeviewer.utils.getArtistDisplayName
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.alicejump.yandeviewer.data.FavoriteTagsManager
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import androidx.core.view.isNotEmpty

class FavoriteTagsActivity : AppCompatActivity() {

    private lateinit var artistLabel: android.widget.TextView
    private lateinit var copyrightLabel: android.widget.TextView
    private lateinit var characterLabel: android.widget.TextView
    private lateinit var generalLabel: android.widget.TextView

    private lateinit var artistTagsContainer: ChipGroup
    private lateinit var copyrightTagsContainer: ChipGroup
    private lateinit var characterTagsContainer: ChipGroup
    private lateinit var generalTagsContainer: ChipGroup

    // XML 中已经放置好的分割线
    private lateinit var dividerArtist: View
    private lateinit var dividerCopyright: View
    private lateinit var dividerCharacter: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_tags)

        // Toolbar
        val headerView = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        val toolbar: Toolbar? = headerView?.findViewById(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = getString(R.string.favorite_tags)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        // 初始化视图
        artistLabel = findViewById(R.id.artist_label)
        copyrightLabel = findViewById(R.id.copyright_label)
        characterLabel = findViewById(R.id.character_label)
        generalLabel = findViewById(R.id.general_label)

        artistTagsContainer = findViewById(R.id.artist_tags_container)
        copyrightTagsContainer = findViewById(R.id.copyright_tags_container)
        characterTagsContainer = findViewById(R.id.character_tags_container)
        generalTagsContainer = findViewById(R.id.general_tags_container)

        dividerArtist = findViewById(R.id.divider_artist)
        dividerCopyright = findViewById(R.id.divider_copyright)
        dividerCharacter = findViewById(R.id.divider_character)

        loadFavoriteTags()
    }

    private fun loadFavoriteTags() {
        lifecycleScope.launch {
            val allTagTypes = TagTypeCache.tagTypes.value
            val favoriteTags = FavoriteTagsManager.getAllTags(this@FavoriteTagsActivity)

            if (favoriteTags.isEmpty()) {
                Toast.makeText(this@FavoriteTagsActivity, "还没有收藏任何标签", Toast.LENGTH_SHORT).show()
                return@launch
            }

            displayTagsByCategory(favoriteTags.toSet(), allTagTypes)
        }
    }

    private fun displayTagsByCategory(tags: Set<String>, allTagTypes: Map<String, Int>) {
        // 清空旧标签
        artistTagsContainer.removeAllViews()
        copyrightTagsContainer.removeAllViews()
        characterTagsContainer.removeAllViews()
        generalTagsContainer.removeAllViews()

        tags.forEach { tag ->
            val type = allTagTypes[tag] ?: -1

            // ✅ 新增 displayName 逻辑
            val displayName = if (type == 1) { // 1 = Artist
                val artistId = ArtistCache.getArtistId(tag)
                val artist = artistId?.let { ArtistCache.getArtist(it) }
                artist?.let { getArtistDisplayName(it) } ?: tag
            } else {
                tag
            }

            val chip = Chip(this).apply {
                text = displayName  // 显示 displayName
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    val intent = Intent(this@FavoriteTagsActivity, MainActivity::class.java).apply {
                        putExtra(MainActivity.NEW_SEARCH_TAG, tag)
                        putExtra(MainActivity.EXTRA_RATING_S, getRatingState(MainActivity.EXTRA_RATING_S))
                        putExtra(MainActivity.EXTRA_RATING_Q, getRatingState(MainActivity.EXTRA_RATING_Q))
                        putExtra(MainActivity.EXTRA_RATING_E, getRatingState(MainActivity.EXTRA_RATING_E))
                    }
                    startActivity(intent)
                }

                setOnLongClickListener {
                    showTagContextMenu(this, tag)
                    true
                }
            }

            // 根据标签类型添加到对应 ChipGroup
            when (type) {
                1 -> artistTagsContainer.addView(chip)
                3 -> copyrightTagsContainer.addView(chip)
                4 -> characterTagsContainer.addView(chip)
                else -> generalTagsContainer.addView(chip)
            }
        }

        updateGroupVisibilityAndDividers()
    }

    private fun showTagContextMenu(chip: Chip, tagName: String) {
        val options = arrayOf(getString(R.string.copy_tag), getString(R.string.remove_favorite_tag))
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyTagToClipboard(tagName)
                    1 -> removeFavoriteTag(chip, tagName)
                }
            }.show()
    }

    private fun copyTagToClipboard(tagName: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("tag", tagName))
        Toast.makeText(this, R.string.tag_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun removeFavoriteTag(chip: Chip, tagName: String) {
        FavoriteTagsManager.removeFavoriteTag(this, tagName)
        val parent = chip.parent as? ChipGroup
        parent?.removeView(chip)
        Toast.makeText(this, "${R.string.remove_favorite_tag}：$tagName", Toast.LENGTH_SHORT).show()
        updateGroupVisibilityAndDividers()
    }

    private fun updateGroupVisibilityAndDividers() {
        // 更新每组可见性
        val artistVisible = updateGroupVisibility(artistLabel, artistTagsContainer)
        val copyrightVisible = updateGroupVisibility(copyrightLabel, copyrightTagsContainer)
        val characterVisible = updateGroupVisibility(characterLabel, characterTagsContainer)
        val generalVisible = updateGroupVisibility(generalLabel, generalTagsContainer)

        // 分割线显示条件：当前组可见且后面有其他可见组
        dividerArtist.visibility =
            if (artistVisible && (copyrightVisible || characterVisible || generalVisible)) View.VISIBLE else View.GONE

        dividerCopyright.visibility =
            if (copyrightVisible && (characterVisible || generalVisible)) View.VISIBLE else View.GONE

        dividerCharacter.visibility =
            if (characterVisible && generalVisible) View.VISIBLE else View.GONE
    }

    // 更新标签组和标题可见性，返回当前组是否可见
    private fun updateGroupVisibility(label: android.widget.TextView, group: ChipGroup): Boolean {
        val visible = group.isNotEmpty()
        label.visibility = if (visible) View.VISIBLE else View.GONE
        group.visibility = if (visible) View.VISIBLE else View.GONE
        return visible
    }

    private fun getRatingState(key: String): Boolean {
        val prefs = getSharedPreferences("rating_state", MODE_PRIVATE)
        return prefs.getBoolean(key, false)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // 使用新的回调分发器
        return true
    }

    override fun onStop() {
        super.onStop()
        TagTypeCache.flush(this)
    }
}