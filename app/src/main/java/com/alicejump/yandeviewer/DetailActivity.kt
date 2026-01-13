package com.alicejump.yandeviewer

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.alicejump.yandeviewer.adapter.ImagePagerAdapter
import com.alicejump.yandeviewer.model.Post
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class DetailActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tagsContainer: ChipGroup
    private lateinit var imagePagerAdapter: ImagePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Add back button to ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewPager = findViewById(R.id.viewPager)
        tagsContainer = findViewById(R.id.tagsContainer)

        val posts = intent.getParcelableArrayListExtra<Post>("posts")
        val position = intent.getIntExtra("position", 0)

        if (posts == null) {
            Toast.makeText(this, "Posts not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imagePagerAdapter = ImagePagerAdapter(posts)
        viewPager.adapter = imagePagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setupTags(posts[position].tags)
            }
        })

        viewPager.setCurrentItem(position, false)

        // If the initial position is the one we want, the onPageSelected callback might not be called.
        // Post a runnable to the ViewPager's message queue to run after the layout is complete.
        if (position == viewPager.currentItem) {
            viewPager.post { setupTags(posts[viewPager.currentItem].tags) }
        }
    }

    // Handle back button click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupTags(tags: String?) {
        tagsContainer.removeAllViews()
        if (tags.isNullOrBlank()) {
            return
        }

        val tagList = tags.split(" ").filter { !it.startsWith("rating:") }

        tagList.forEach { tag ->
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

            tagsContainer.addView(chip)
        }
    }
}
