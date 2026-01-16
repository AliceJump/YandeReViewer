package com.alicejump.yandeviewer

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var imagePagerAdapter: ImagePagerAdapter
    private lateinit var sourceButton: Button

    // Labels
    private lateinit var artistLabel: TextView
    private lateinit var copyrightLabel: TextView
    private lateinit var characterLabel: TextView
    private lateinit var generalLabel: TextView

    // Chip Groups
    private lateinit var artistTagsContainer: ChipGroup
    private lateinit var copyrightTagsContainer: ChipGroup
    private lateinit var characterTagsContainer: ChipGroup
    private lateinit var generalTagsContainer: ChipGroup

    private var firstVisiblePosition: Int = -1
    private var lastVisiblePosition: Int = -1

    private fun createSnapshotView(source: View): View {
        val bitmap = createBitmap(source.width, source.height)
        val canvas = Canvas(bitmap)
        source.draw(canvas)

        return ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    private fun addToOverlay(source: View): View {
        val decorView = window.decorView as ViewGroup
        val snapshot = createSnapshotView(source)

        val location = IntArray(2)
        source.getLocationOnScreen(location)

        val params = FrameLayout.LayoutParams(
            source.width,
            source.height
        ).apply {
            leftMargin = location[0]
            topMargin = location[1]
        }

        decorView.addView(snapshot, params)
        return snapshot
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        // Postpone the shared element transition.
        postponeEnterTransition()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentPosition = viewPager.currentItem
                val isOnScreen = currentPosition in firstVisiblePosition..lastVisiblePosition

                val resultIntent = Intent().apply {
                    putExtra("position", currentPosition)
                }
                setResult(RESULT_OK, resultIntent)

                if (isOnScreen) {
                    finishAfterTransition()
                    return
                }

                // ===== 非屏幕内，自定义动画 =====
                val recyclerView = viewPager.getChildAt(0) as? RecyclerView
                val viewHolder =
                    recyclerView?.findViewHolderForAdapterPosition(currentPosition)
                            as? ImagePagerAdapter.ImageViewHolder

                val imageView = viewHolder
                    ?.itemView
                    ?.findViewById<View>(R.id.pagerImageView)

                if (imageView == null) {
                    finish()
                    return
                }

                val isAbove = currentPosition < firstVisiblePosition
                val isLeft = currentPosition % 2 == 0

                resources.displayMetrics.widthPixels.toFloat()
                resources.displayMetrics.heightPixels.toFloat()

                if (isAbove) {
                    // —— 上方：缩向左上 / 右上（基于 View 自身）——
                    imageView.pivotX = if (isLeft) 0f else imageView.width.toFloat()
                    imageView.pivotY = 0f
                    @Suppress("DEPRECATION")
                    imageView.animate()
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            finish()
                            overridePendingTransition(0, 0)
                        }
                        .start()

                } else {
                    // —— 下方：使用屏幕级 Overlay 飞向左下 / 右下 ——

                    val decorView = window.decorView as ViewGroup
                    val snapshot = addToOverlay(imageView)

                    imageView.alpha = 0f   // 原图隐藏

                    val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                    val screenHeight = resources.displayMetrics.heightPixels.toFloat()

                    val targetX = if (isLeft) -screenWidth else screenWidth
                    @Suppress("DEPRECATION")
                    snapshot.animate()
                        .translationX(targetX)
                        .translationY(screenHeight)
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            decorView.removeView(snapshot)
                            finish()
                            overridePendingTransition(0, 0)
                        }
                        .start()
                }

            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewPager = findViewById(R.id.viewPager)
        sourceButton = findViewById(R.id.source_button)

        // Init Labels
        artistLabel = findViewById(R.id.artist_label)
        copyrightLabel = findViewById(R.id.copyright_label)
        characterLabel = findViewById(R.id.character_label)
        generalLabel = findViewById(R.id.general_label)

        // Init ChipGroups
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
        firstVisiblePosition = intent.getIntExtra("first_visible_position", -1)
        lastVisiblePosition = intent.getIntExtra("last_visible_position", -1)
        val transitionName = intent.getStringExtra("transition_name")

        if (posts == null) {
            Toast.makeText(this, R.string.detail_posts_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imagePagerAdapter = ImagePagerAdapter(posts)
        viewPager.adapter = imagePagerAdapter
        viewPager.transitionName = transitionName

        // This collector will automatically update the UI whenever the tag cache changes.
        lifecycleScope.launch {
            TagTypeCache.tagTypes.collectLatest { _ ->
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

        // Start the transition after the view has been laid out.
        viewPager.post { startPostponedEnterTransition() }
    }

    private fun updateUiForPosition(position: Int, posts: List<Post>) {
        val currentPost = posts[position]
        val tagsToFetch = currentPost.tags.split(" ").toSet()

        // Immediately render the page with currently cached data.
        setupTags(tagsToFetch, TagTypeCache.tagTypes.value)
        setupSourceButton(currentPost)

        // Then, request any missing tags (if any).
        if (tagsToFetch.isNotEmpty()) {
            TagTypeCache.prioritizeTags(this, tagsToFetch)
        }
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
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
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

    private fun setupTags(currentPostTags: Set<String>, allTagTypes: Map<String, Int>) {
        artistTagsContainer.removeAllViews()
        copyrightTagsContainer.removeAllViews()
        characterTagsContainer.removeAllViews()
        generalTagsContainer.removeAllViews()

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
                1 -> artistTagsContainer.addView(chip) // Artist
                3 -> copyrightTagsContainer.addView(chip) // Copyright
                4 -> characterTagsContainer.addView(chip) // Character
                else -> generalTagsContainer.addView(chip) // Includes general (0) and not-yet-fetched (-1)
            }
        }

        // Hide label and container if they are empty
        updateGroupVisibility(artistLabel, artistTagsContainer)
        updateGroupVisibility(copyrightLabel, copyrightTagsContainer)
        updateGroupVisibility(characterLabel, characterTagsContainer)
        updateGroupVisibility(generalLabel, generalTagsContainer)
    }

    private fun updateGroupVisibility(label: TextView, group: ChipGroup) {
        val visibility = if (group.children.count() > 0) View.VISIBLE else View.GONE
        label.visibility = visibility
        group.visibility = visibility
    }
    override fun onStop() {
        super.onStop()
        TagTypeCache.flush(this)
    }

}
