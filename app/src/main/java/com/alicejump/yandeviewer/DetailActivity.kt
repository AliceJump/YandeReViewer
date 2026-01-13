package com.alicejump.yandeviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
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
    private lateinit var sourceButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val resultIntent = Intent().apply {
                    putExtra("position", viewPager.currentItem)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewPager = findViewById(R.id.viewPager)
        artistTagsContainer = findViewById(R.id.artist_tags_container)
        copyrightTagsContainer = findViewById(R.id.copyright_tags_container)
        characterTagsContainer = findViewById(R.id.character_tags_container)
        generalTagsContainer = findViewById(R.id.general_tags_container)
        sourceButton = findViewById(R.id.source_button)

        val posts = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra("posts", Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("posts")
        }
        val position = intent.getIntExtra("position", 0)

        if (posts == null) {
            Toast.makeText(this, R.string.detail_posts_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imagePagerAdapter = ImagePagerAdapter(posts)
        viewPager.adapter = imagePagerAdapter

        // This collector will automatically update the UI whenever the tag cache changes.
        lifecycleScope.launch {
            TagTypeCache.tagTypes.collectLatest { tagTypes ->
                updateUiForPosition(viewPager.currentItem, posts)
            }
        }

        // This callback handles the user swiping between pages.
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUiForPosition(position, posts)
            }
        })

        viewPager.setCurrentItem(position, false)

        // Manually trigger the setup for the initial item, as onPageSelected isn't called for it.
        updateUiForPosition(position, posts)
    }

    private fun updateUiForPosition(position: Int, posts: List<Post>) {
        val currentPost = posts[position]
        val tagsToFetch = currentPost.tags?.split(" ")?.toSet() ?: emptySet()

        // Immediately render the page with currently cached data.
        setupTags(currentPost, tagsToFetch, TagTypeCache.tagTypes.value)
        setupSourceButton(currentPost)

        // Then, request any missing tags with high priority.
        if (tagsToFetch.isNotEmpty()) {
            TagTypeCache.prioritizeTags(tagsToFetch)
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

    private fun setupSourceButton(currentPost: Post) {
        val source = currentPost.source
        if (source.isNullOrBlank()) {
            sourceButton.visibility = View.GONE
        } else {
            sourceButton.visibility = View.VISIBLE
            sourceButton.setOnClickListener {
                if (Patterns.WEB_URL.matcher(source).matches()) {
                    var url = source
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } else {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.detail_source_button_text)
                        .setMessage(source)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
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
