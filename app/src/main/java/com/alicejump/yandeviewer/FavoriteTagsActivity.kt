package com.alicejump.yandeviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.alicejump.yandeviewer.adapter.FavoriteTagAdapter
import com.alicejump.yandeviewer.adapter.FavoriteTagItem
import com.alicejump.yandeviewer.data.FavoriteTagsManager
import com.alicejump.yandeviewer.network.RetrofitClient
import com.alicejump.yandeviewer.viewmodel.ArtistCache
import com.alicejump.yandeviewer.utils.getArtistDisplayName
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class FavoriteTagsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoriteTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_tags)

        // Toolbar
        val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        val toolbar: Toolbar? = appBarLayout?.findViewById(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = getString(R.string.favorite_tags)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        recyclerView = findViewById(R.id.favoriteTagsRecyclerView)
        adapter = FavoriteTagAdapter(
            onTagClick = { tag ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.NEW_SEARCH_TAG, tag)
                }
                startActivity(intent)
            },
            onTagLongClick = { view, tag ->
                showTagContextMenu(tag)
            },
            onPostClick = { post, posts ->
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putParcelableArrayListExtra("posts", ArrayList(posts))
                    putExtra("position", posts.indexOf(post))
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter

        loadFavoriteTags()
    }

    private fun showTagContextMenu(tagName: String) {
        val options = arrayOf(getString(R.string.copy_tag), getString(R.string.remove_favorite_tag))
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyTagToClipboard(tagName)
                    1 -> removeFavoriteTag(tagName)
                }
            }.show()
    }

    private fun copyTagToClipboard(tagName: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("tag", tagName))
        Toast.makeText(this, R.string.tag_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun removeFavoriteTag(tagName: String) {
        FavoriteTagsManager.removeFavoriteTag(this, tagName)
        Toast.makeText(this, getString(R.string.removed_from_favorites_with_tag_name, tagName), Toast.LENGTH_SHORT).show()
        loadFavoriteTags() // Refresh the list
    }

    private fun loadFavoriteTags() {
        lifecycleScope.launch {
            val favoriteTags = FavoriteTagsManager.getAllTags(this@FavoriteTagsActivity)
            if (favoriteTags.isEmpty()) {
                adapter.submitList(emptyList())
                Toast.makeText(this@FavoriteTagsActivity, R.string.no_favorite_tags_yet, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val allTagTypes = TagTypeCache.tagTypes.value

            val items = favoriteTags.map { tag ->
                async {
                    val type = allTagTypes[tag] ?: -1
                    val displayName = if (type == 1) {
                        val artistId = ArtistCache.getArtistId(tag)
                        val artist = artistId?.let { ArtistCache.getArtist(it) }
                        artist?.let { getArtistDisplayName(it) } ?: tag
                    } else {
                        tag
                    }

                    val previewPosts = try {
                        RetrofitClient.api.getPosts(tags = tag, limit = 10, page = 1)
                    } catch (e: Exception) {
                        emptyList()
                    }

                    FavoriteTagItem(tag, displayName, previewPosts)
                }
            }.awaitAll()

            adapter.submitList(items)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}