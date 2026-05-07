package com.alicejump.yandeviewer
import com.alicejump.yandeviewer.viewmodel.ArtistCache
import com.alicejump.yandeviewer.utils.getArtistDisplayName
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.alicejump.yandeviewer.data.BlacklistManager
import com.alicejump.yandeviewer.data.BrowsingHistoryManager
import com.alicejump.yandeviewer.data.FavoritesManager
import com.alicejump.yandeviewer.data.FavoriteTagsManager
import com.alicejump.yandeviewer.model.Post
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import androidx.core.view.isNotEmpty

// 详情页 Activity，用于查看单张图片、标签、来源信息，并可进行收藏
class DetailActivity : AppCompatActivity() {

    // UI 元素
    private lateinit var viewPager: ViewPager2
    private lateinit var imagePagerAdapter: ImagePagerAdapter
    private lateinit var menuFab: FloatingActionButton
    private lateinit var internalFab: FloatingActionButton
    private lateinit var sourceFab: FloatingActionButton
    private lateinit var favoriteFab: FloatingActionButton
    private var isExpanded = false
    private var currentSource: String? = null

    private companion object {
        const val FAB_DRAWER_DISTANCE = 300f
        const val FAB_ANIMATION_DURATION = 300L
    }


    // 标签的标题
    private lateinit var artistLabel: TextView
    private lateinit var copyrightLabel: TextView
    private lateinit var characterLabel: TextView
    private lateinit var generalLabel: TextView
    private lateinit var dividerArtist: View
    private lateinit var dividerCopyright: View
    private lateinit var dividerCharacter: View

    // ChipGroup 用于显示不同类型标签
    private lateinit var artistTagsContainer: ChipGroup
    private lateinit var copyrightTagsContainer: ChipGroup
    private lateinit var characterTagsContainer: ChipGroup
    private lateinit var generalTagsContainer: ChipGroup


    private var firstVisiblePosition: Int = -1
    private var lastVisiblePosition: Int = -1

    // 保存从 MainActivity 传来的复选框状态
    private var ratingSState: Boolean = false
    private var ratingQState: Boolean = false
    private var ratingEState: Boolean = false
    private var gridSpanCount: Int = 2

    // ====== 共享元素动画辅助函数 ======

    // 创建源 View 的快照，用于动画 Overlay
    private fun createSnapshotView(source: View): View {
        val bitmap = createBitmap(source.width, source.height)
        val canvas = Canvas(bitmap)
        source.draw(canvas)

        return ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    // 将快照加入屏幕 Overlay，用于自定义动画
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
    private fun hideDrawerFab(fab: FloatingActionButton) {
        fab.animate().cancel()
        fab.translationY = 0f
        fab.alpha = 0f
        fab.visibility = View.GONE
    }

    private fun showDrawerFab(fab: FloatingActionButton, slot: Int, animate: Boolean) {
        fab.animate().cancel()
        fab.visibility = View.VISIBLE

        val targetTranslationY = -FAB_DRAWER_DISTANCE * slot
        if (animate) {
            fab.animate()
                .translationY(targetTranslationY)
                .alpha(1f)
                .setDuration(FAB_ANIMATION_DURATION)
                .withEndAction { }
                .start()
        } else {
            fab.translationY = targetTranslationY
            fab.alpha = 1f
        }
    }

    private fun collapseDrawerFab(fab: FloatingActionButton) {
        fab.animate().cancel()
        if (fab.visibility != View.VISIBLE) {
            hideDrawerFab(fab)
            return
        }

        fab.animate()
            .translationY(0f)
            .alpha(0f)
            .setDuration(FAB_ANIMATION_DURATION)
            .withEndAction { fab.visibility = View.GONE }
            .start()
    }

    private fun syncFabDrawerState() {
        if (!isExpanded) {
            hideDrawerFab(sourceFab)
            hideDrawerFab(internalFab)
            return
        }

        var slot = 1
        if (currentSource != null) {
            showDrawerFab(sourceFab, slot, animate = false)
            slot++
        } else {
            hideDrawerFab(sourceFab)
        }
        showDrawerFab(internalFab, slot, animate = false)
    }

    private fun toggleFabDrawer() {
        if (!isExpanded) {
            isExpanded = true
            var slot = 1
            if (currentSource != null) {
                showDrawerFab(sourceFab, slot, animate = true)
                slot++
            } else {
                hideDrawerFab(sourceFab)
            }
            showDrawerFab(internalFab, slot, animate = true)
        } else {
            isExpanded = false
            collapseDrawerFab(sourceFab)
            collapseDrawerFab(internalFab)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        internalFab = findViewById(R.id.fab_internal)

        // ======= 获取按钮引用 =======
        menuFab = findViewById(R.id.fab_menu)
        sourceFab = findViewById(R.id.fab_source)
        favoriteFab = findViewById(R.id.fab_favorite)
        syncFabDrawerState()

        // ======= 点击菜单按钮展开/收起 =======
        menuFab.setOnClickListener {
            toggleFabDrawer()
        }
        // 延迟共享元素过渡，等 View 加载完成
        postponeEnterTransition()

        // ===== 处理返回按钮行为，包括自定义动画 =====
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentPosition = viewPager.currentItem
                val isOnScreen = currentPosition in firstVisiblePosition..lastVisiblePosition

                val resultIntent = Intent().apply {
                    putExtra("position", currentPosition)
                }
                setResult(RESULT_OK, resultIntent)

                if (isOnScreen) {
                    // 如果图片在屏幕内，正常共享元素返回
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
                val currentColumn = currentPosition % gridSpanCount
                val isLeft = currentColumn < (gridSpanCount / 2)

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
                    // —— 下方：屏幕级 Overlay 飞出动画 ——
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

        // 显示 ActionBar 返回箭头
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ====== 初始化 UI 元素 ======
        viewPager = findViewById(R.id.viewPager)

        // 标签标题
        artistLabel = findViewById(R.id.artist_label)
        copyrightLabel = findViewById(R.id.copyright_label)
        characterLabel = findViewById(R.id.character_label)
        generalLabel = findViewById(R.id.general_label)

        // ChipGroup 容器
        artistTagsContainer = findViewById(R.id.artist_tags_container)
        copyrightTagsContainer = findViewById(R.id.copyright_tags_container)
        characterTagsContainer = findViewById(R.id.character_tags_container)
        generalTagsContainer = findViewById(R.id.general_tags_container)
        dividerArtist = findViewById(R.id.divider_artist)
        dividerCopyright = findViewById(R.id.divider_copyright)
        dividerCharacter = findViewById(R.id.divider_character)


        // 获取 Intent 数据
        val posts = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra("posts", Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("posts")
        }
        val position = intent.getIntExtra("position", 0)
        firstVisiblePosition = intent.getIntExtra("first_visible_position", -1)
        lastVisiblePosition = intent.getIntExtra("last_visible_position", -1)
        gridSpanCount = intent.getIntExtra("grid_span_count", 2).coerceAtLeast(1)
        val transitionName = intent.getStringExtra("transition_name")

        // 获取复选框状态
        ratingSState = intent.getBooleanExtra(MainActivity.EXTRA_RATING_S, false)
        ratingQState = intent.getBooleanExtra(MainActivity.EXTRA_RATING_Q, false)
        ratingEState = intent.getBooleanExtra(MainActivity.EXTRA_RATING_E, false)

        if (posts == null) {
            Toast.makeText(this, R.string.detail_posts_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (posts.isEmpty()) {
            Toast.makeText(this, R.string.detail_posts_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val maxValidPosition = posts.size - 1
        val safeInitialPosition = position.coerceIn(0, maxValidPosition)

        // ====== 初始化 ViewPager ======
        imagePagerAdapter = ImagePagerAdapter(posts)
        viewPager.adapter = imagePagerAdapter
        viewPager.orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewPager2.ORIENTATION_VERTICAL
        } else {
            ViewPager2.ORIENTATION_HORIZONTAL
        }
        viewPager.transitionName = transitionName

        // ====== 收藏按钮逻辑 ======
        favoriteFab.setOnClickListener {
            val currentPost = posts.getOrNull(viewPager.currentItem) ?: return@setOnClickListener

            if (FavoritesManager.isFavorite(this, currentPost.id)) {
                // 已收藏 -> 取消收藏
                FavoritesManager.removeFavorite(this, currentPost.id)
                Toast.makeText(this, R.string.post_unfavorited, Toast.LENGTH_SHORT).show()
            } else {
                // 未收藏 -> 添加收藏（存完整对象）
                FavoritesManager.addFavorite(this, currentPost)
                Toast.makeText(this, R.string.post_favorite, Toast.LENGTH_SHORT).show()
            }

            updateFavoriteButton(currentPost)
        }
        internalFab.setOnClickListener {
            val post = posts.getOrNull(viewPager.currentItem) ?: return@setOnClickListener
            val postId = post.id
            val url = "https://yande.re/post/show/$postId"

            // 使用浏览器打开
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
        // ====== 标签缓存更新监听 ======
        lifecycleScope.launch {
            TagTypeCache.tagTypes.collectLatest { _ ->
                updateUiForPosition(viewPager.currentItem, posts)
            }
        }

        // ====== ViewPager 页面切换监听 ======
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUiForPosition(position, posts)
            }
        })

        // 设置初始显示页面
        viewPager.setCurrentItem(safeInitialPosition, false)
        updateUiForPosition(safeInitialPosition, posts)

        // 启动共享元素过渡
        viewPager.post { startPostponedEnterTransition() }
    }

    // 更新收藏按钮状态
    private fun updateFavoriteButton(post: Post) {
        if (FavoritesManager.isFavorite(this, post.id)) {
            favoriteFab.setImageResource(R.drawable.ic_star_filled_24)
        } else {
            favoriteFab.setImageResource(R.drawable.ic_star_outline_24)
        }
    }

    // 根据当前页面更新 UI：标签、来源、收藏状态
    private fun updateUiForPosition(position: Int, posts: List<Post>) {
        if (position !in posts.indices) return
        val currentPost = posts[position]
        val tagsToFetch = currentPost.tags.split(" ").toSet()

        // 记录浏览历史
        BrowsingHistoryManager.recordView(this, currentPost)

        // 立即渲染当前缓存的标签
        setupTags(tagsToFetch, TagTypeCache.tagTypes.value)
        setupSourceButton(currentPost)
        updateFavoriteButton(currentPost)

        // 请求尚未获取的标签
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

    private fun convertPixivImageUrlToArtwork(url: String): String? {

        if (!url.contains("i.pximg.net")) return null

        val regex = Regex("""/(\d+)_p\d+""")
        val match = regex.find(url) ?: return null

        val illustId = match.groupValues[1]

        return "https://www.pixiv.net/artworks/$illustId"
    }

    private fun openSource(source: String) {
        var url = source

        // 先 Pixiv CDN → artworks
        convertPixivImageUrlToArtwork(url)?.let {
            url = it
        }

        // 没协议补 https
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        // 再判断是不是可打开链接
        if (Patterns.WEB_URL.matcher(url).matches()) {

            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)

        } else {

            // fallback 复制
            val clipboard = getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText("source", source)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                R.string.source_copied_to_clipboard,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 设置来源按钮逻辑
    private fun setupSourceButton(currentPost: Post) {
        val hadSource = currentSource != null
        currentSource = currentPost.source?.trim()?.takeIf { it.isNotEmpty() }

        sourceFab.isEnabled = currentSource != null
        sourceFab.setOnClickListener(currentSource?.let { source ->
            View.OnClickListener { openSource(source) }
        })

        if (!isExpanded || hadSource != (currentSource != null)) {
            syncFabDrawerState()
        }
    }
    // 设置标签显示逻辑
    private fun setupTags(currentPostTags: Set<String>, allTagTypes: Map<String, Int>) {
        // 清空旧标签
        artistTagsContainer.removeAllViews()
        copyrightTagsContainer.removeAllViews()
        characterTagsContainer.removeAllViews()
        generalTagsContainer.removeAllViews()

        currentPostTags.forEach { tag ->
            val type = allTagTypes[tag] ?: -1 // 未获取的标签类型为 -1

            // 如果是艺术家标签，尝试获取 Artist 对象并显示 displayName
            val displayName = if (type == 1) { // 1 = Artist
                val artistId = ArtistCache.getArtistId(tag)
                val artist = artistId?.let { ArtistCache.getArtist(it) }
                artist?.let { getArtistDisplayName(it) } ?: tag
            } else {
                tag
            }

            val isFavorited = FavoriteTagsManager.isFavorite(this, tag)
            val chip = Chip(this).apply {
                text = displayName
                isClickable = true
                isFocusable = true
                // 已收藏的标签用金色高亮显示
                if (isFavorited) {
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FBC02D"))
                    setTextColor(Color.WHITE)
                }
            }

            // 点击 -> 跳转 MainActivity 搜索该标签
            chip.setOnClickListener {
                val intent = Intent(this@DetailActivity, MainActivity::class.java).apply {
                    putExtra(MainActivity.NEW_SEARCH_TAG, tag)
                    // 传递复选框状态
                    putExtra(MainActivity.EXTRA_RATING_S, ratingSState)
                    putExtra(MainActivity.EXTRA_RATING_Q, ratingQState)
                    putExtra(MainActivity.EXTRA_RATING_E, ratingEState)
                }
                startActivity(intent)
            }

            // 长按 -> 弹出菜单
            chip.setOnLongClickListener {
                val alreadyFavorited = FavoriteTagsManager.isFavorite(this, tag)
                val favoriteLabel = if (alreadyFavorited)
                    getString(R.string.remove_favorite_tag)
                else
                    getString(R.string.favorite_tag)

                val options = arrayOf(
                    getString(R.string.copy_tag),
                    getString(R.string.add_to_blacklist),
                    favoriteLabel
                )

                AlertDialog.Builder(this)
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> {
                                val clipboard =
                                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("tag", tag)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(
                                    this,
                                    R.string.tag_copied_to_clipboard,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            1 -> {
                                BlacklistManager.add(tag)
                                Toast.makeText(
                                    this,
                                    getString(R.string.tag_added_to_blacklist, tag),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            2 -> {
                                if (alreadyFavorited) {
                                    FavoriteTagsManager.removeFavoriteTag(this, tag)
                                    Toast.makeText(
                                        this,
                                        getString(R.string.removed_from_favorites_with_tag_name, tag),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    FavoriteTagsManager.addFavoriteTag(this, tag)
                                    Toast.makeText(
                                        this,
                                        getString(R.string.tag_favorited, tag),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                // 刷新标签显示，更新收藏颜色
                                setupTags(currentPostTags, allTagTypes)
                            }
                        }
                    }
                    .show()

                true
            }

            // 根据标签类型添加到对应 ChipGroup
            when (type) {
                1 -> artistTagsContainer.addView(chip)
                3 -> copyrightTagsContainer.addView(chip)
                4 -> characterTagsContainer.addView(chip)
                else -> generalTagsContainer.addView(chip)
            }
        }

        // 更新标签组可见性
        updateGroupVisibility(artistLabel, artistTagsContainer)
        updateGroupVisibility(copyrightLabel, copyrightTagsContainer)
        updateGroupVisibility(characterLabel, characterTagsContainer)
        updateGroupVisibility(generalLabel, generalTagsContainer)
        updateDividers()
    }
    private fun updateDividers() {

        val groups = listOf(
            artistTagsContainer to dividerArtist,
            copyrightTagsContainer to dividerCopyright,
            characterTagsContainer to dividerCharacter,
            generalTagsContainer to null
        )

        for (i in groups.indices) {

            val (group, divider) = groups[i]

            if (divider == null) continue   // general 没有分隔线

            val currentVisible = group.isVisible

            // 检查下面是否还有可见分组
            val hasVisibleBelow = groups
                .drop(i + 1)
                .any { it.first.isVisible }

            divider.visibility =
                if (currentVisible && hasVisibleBelow)
                    View.VISIBLE
                else
                    View.GONE
        }
    }
    // 更新标签组和标题可见性
    private fun updateGroupVisibility(label: TextView, group: ChipGroup) {

        val visible = group.isNotEmpty()

        label.visibility = if (visible) View.VISIBLE else View.GONE
        group.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onStop() {
        super.onStop()
        // 页面关闭时刷新缓存
        TagTypeCache.flush(this)
    }

}
