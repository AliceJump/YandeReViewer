package com.alicejump.yandeviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.alicejump.yandeviewer.adapter.FavoriteTagAdapter
import com.alicejump.yandeviewer.adapter.FavoriteTagItem
import com.alicejump.yandeviewer.data.FavoriteTagsManager
import com.alicejump.yandeviewer.data.PostTransferStore
import com.alicejump.yandeviewer.network.RetrofitClient
import com.alicejump.yandeviewer.viewmodel.ArtistCache
import com.alicejump.yandeviewer.utils.getArtistDisplayName
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class FavoriteTagsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoriteTagAdapter
    private lateinit var filterChipGroup: ChipGroup

    private var currentItems = mutableListOf<FavoriteTagItem>()
    // 当前筛选类型：-1=全部，1=Artist，3=Copyright，4=Character
    private var currentFilterType: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_tags)

        val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        val toolbar: Toolbar? = appBarLayout?.findViewById(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = getString(R.string.favorite_tags)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        filterChipGroup = findViewById(R.id.filterChipGroup)
        setupFilterChips()

        recyclerView = findViewById(R.id.favoriteTagsRecyclerView)
        recyclerView.setHasFixedSize(true)

        adapter = FavoriteTagAdapter(
            onTagClick = { tag ->
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.NEW_SEARCH_TAG, tag)
                })
            },
            onTagLongClick = { _, tag -> showTagContextMenu(tag) },
            onPostClick = { post, posts ->
                startActivity(Intent(this, DetailActivity::class.java).apply {
                    putExtra(PostTransferStore.EXTRA_POSTS_TRANSFER_KEY, PostTransferStore.put(posts))
                    putExtra("position", posts.indexOf(post))
                })
            }
        )
        recyclerView.adapter = adapter

        loadFavoriteTags()
    }

    private fun setupFilterChips() {
        val filters = listOf(
            getString(R.string.filter_all) to -1,
            getString(R.string.filter_artist) to 1,
            getString(R.string.filter_copyright) to 3,
            getString(R.string.filter_character) to 4
        )

        filters.forEachIndexed { index, (label, typeCode) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = (index == 0)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        currentFilterType = typeCode
                        applyFilter()
                    }
                }
            }
            filterChipGroup.addView(chip)
        }
    }

    private fun applyFilter() {
        val allTagTypes = TagTypeCache.tagTypes.value
        val filtered = if (currentFilterType == -1) {
            currentItems.toList()
        } else {
            currentItems.filter { item ->
                val type = allTagTypes[item.tag] ?: -1
                type == currentFilterType
            }
        }
        adapter.submitList(filtered)
    }

    private fun loadFavoriteTags() {
        lifecycleScope.launch {
            // 1. 极其轻量级的初始化：直接读取标签字符串，不做任何转换
            val favoriteTags = withContext(Dispatchers.IO) {
                FavoriteTagsManager.getAllTags(this@FavoriteTagsActivity)
            }

            if (favoriteTags.isEmpty()) {
                adapter.submitList(emptyList())
                Toast.makeText(this@FavoriteTagsActivity, R.string.no_favorite_tags_yet, Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 2. 瞬间渲染基础框架（displayName 直接用 tag 名）
            currentItems = favoriteTags.map {
                FavoriteTagItem(it, it, emptyList(), isLoading = true)
            }.toMutableList()
            applyFilter()

            // 3. 在后台逐个"增强"信息（如查询艺术家实名）并加载图片
            startAsyncEnhancementAndLoading()
        }
    }

    private fun startAsyncEnhancementAndLoading() {
        lifecycleScope.launch {
            val updatedIndices = ConcurrentHashMap.newKeySet<Int>()
            val allTagTypes = TagTypeCache.tagTypes.value

            currentItems.forEachIndexed { index, item ->
                launch(Dispatchers.Default) {
                    // A. 增强信息解析（实名等）
                    val type = allTagTypes[item.tag] ?: -1
                    val displayName = if (type == 1) {
                        val artistId = ArtistCache.getArtistId(item.tag)
                        val artist = artistId?.let { ArtistCache.getArtist(it) }
                        artist?.let { getArtistDisplayName(it) } ?: item.tag
                    } else {
                        item.tag
                    }

                    // B. 加载图片
                    val posts = try {
                        RetrofitClient.api.getPosts(tags = item.tag, limit = 10, page = 1)
                    } catch (e: Exception) {
                        emptyList()
                    }

                    synchronized(currentItems) {
                        currentItems[index] = currentItems[index].copy(
                            displayName = displayName,
                            previewPosts = posts,
                            isLoading = false
                        )
                    }
                    updatedIndices.add(index)
                }
            }

            // 4. 批量刷新 UI 循环
            while (true) {
                if (updatedIndices.isNotEmpty()) {
                    updatedIndices.clear()
                    withContext(Dispatchers.Main) {
                        applyFilter()
                    }
                }
                if (currentItems.none { it.isLoading } && updatedIndices.isEmpty()) break
                delay(300)
            }
        }
    }

    private fun showTagContextMenu(tagName: String) {
        val options = arrayOf(getString(R.string.copy_tag), getString(R.string.remove_favorite_tag))
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("tag", tagName))
                        Toast.makeText(this, R.string.tag_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        FavoriteTagsManager.removeFavoriteTag(this, tagName)
                        loadFavoriteTags()
                    }
                }
            }.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
