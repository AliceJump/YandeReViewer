package com.alicejump.yandeviewer

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.alicejump.yandeviewer.adapter.ImagePagerAdapter
import com.alicejump.yandeviewer.model.Post
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var artistTagsContainer: ChipGroup
    private lateinit var copyrightTagsContainer: ChipGroup
    private lateinit var characterTagsContainer: ChipGroup
    private lateinit var generalTagsContainer: ChipGroup
    private lateinit var imagePagerAdapter: ImagePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewPager = findViewById(R.id.viewPager)
        artistTagsContainer = findViewById(R.id.artist_tags_container)
        copyrightTagsContainer = findViewById(R.id.copyright_tags_container)
        characterTagsContainer = findViewById(R.id.character_tags_container)
        generalTagsContainer = findViewById(R.id.general_tags_container)

        val posts = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra("posts", Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("posts")
        }
        val position = intent.getIntExtra("position", 0)

        if (posts == null) {
            Toast.makeText(this, "Posts not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imagePagerAdapter = ImagePagerAdapter(posts)
        viewPager.adapter = imagePagerAdapter

        // This collector will automatically update the UI whenever the tag cache changes.
        lifecycleScope.launch {
            TagTypeCache.tagTypes.collectLatest { tagTypes ->
                val currentPost = posts[viewPager.currentItem]
                val currentPostTags = currentPost.tags?.split(" ")?.toSet() ?: emptySet()
                setupTags(currentPost, currentPostTags, tagTypes)
            }
        }

        // This callback handles the user swiping between pages.
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentPost = posts[position]
                val tagsToFetch = currentPost.tags?.split(" ")?.toSet() ?: emptySet()

                // Immediately render the page with currently cached data.
                setupTags(currentPost, tagsToFetch, TagTypeCache.tagTypes.value)

                // Then, request any missing tags with high priority.
                if (tagsToFetch.isNotEmpty()) {
                    TagTypeCache.prioritizeTags(tagsToFetch)
                }
            }
        })

        viewPager.setCurrentItem(position, false)

        // Manually trigger the setup for the initial item, as onPageSelected isn't called for it.
        val initialPost = posts[position]
        val initialTags = initialPost.tags?.split(" ")?.toSet() ?: emptySet()
        setupTags(initialPost, initialTags, TagTypeCache.tagTypes.value)
        if (initialTags.isNotEmpty()) {
            TagTypeCache.prioritizeTags(initialTags)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TagTypeCache.detailViewClosed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupTags(currentPost: Post, currentPostTags: Set<String>, allTagTypes: Map<String, Int>) {
        artistTagsContainer.removeAllViews()
        copyrightTagsContainer.removeAllViews()
        characterTagsContainer.removeAllViews()
        generalTagsContainer.removeAllViews()

        // Directly add the author
        val authorChip = Chip(this).apply {
            text = currentPost.author
            isClickable = true
            isFocusable = true
        }
        authorChip.setOnClickListener {
            val intent = Intent(this@DetailActivity, MainActivity::class.java).apply {
                putExtra(MainActivity.NEW_SEARCH_TAG, currentPost.author)
            }
            startActivity(intent)
        }
        artistTagsContainer.addView(authorChip)

        currentPostTags.forEach { tag ->
            val type = allTagTypes[tag] ?: -1 // Use -1 for tags not yet fetched

            val chip = Chip(this).apply {
                text = tag
                isClickable = true
                isFocusable = true
            }

            chip.setOnClickListener {
                val intent = Intent(this@DetailActivity, MainActivity::class.java).apply {
                    putExtra(MainActivity.NEW_SEARCH_TAG, tag)
                }
                startActivity(intent)
            }

            when (type) {
                3 -> copyrightTagsContainer.addView(chip)
                4 -> characterTagsContainer.addView(chip)
                else -> generalTagsContainer.addView(chip) // Includes general (0) and not-yet-fetched (-1)
            }
        }
    }
}
