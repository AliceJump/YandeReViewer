package com.alicejump.yandeviewer

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.alicejump.yandeviewer.adapter.PostAdapter
import com.alicejump.yandeviewer.data.BlacklistManager
import com.alicejump.yandeviewer.data.FavoritesManager
import com.alicejump.yandeviewer.model.Post
import com.alicejump.yandeviewer.network.GitHubApiClient
import com.alicejump.yandeviewer.network.GitHubRelease
import com.alicejump.yandeviewer.sync.ArtistSyncer
import com.alicejump.yandeviewer.tool.downloadImage
import com.alicejump.yandeviewer.utils.getArtistDisplayName
import com.alicejump.yandeviewer.viewmodel.ArtistCache
import com.alicejump.yandeviewer.viewmodel.PostViewModel
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import com.alicejump.yandeviewer.viewmodel.UpdateCheckState
import com.alicejump.yandeviewer.viewmodel.UpdateViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalPagingApi::class)
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var tagChipGroup: ChipGroup
    private lateinit var drawerLayout: DrawerLayout
    private var favoriteSource: List<Post> = emptyList()
    private val selectedTags = linkedSetOf<String>()

    private val postViewModel by viewModels<PostViewModel>()
    private val updateViewModel by viewModels<UpdateViewModel>()
    private lateinit var postAdapter: PostAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBox: AutoCompleteTextView

    private lateinit var ratingSCheckbox: MaterialButton
    private lateinit var ratingQCheckbox: MaterialButton
    private lateinit var ratingECheckbox: MaterialButton

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabScrollToTop: FloatingActionButton
    private lateinit var fabRefresh: FloatingActionButton

    private lateinit var tagCompletionAdapter: ArrayAdapter<String>
    private var allAvailableTags: List<String> = emptyList()
    private var downloadId: Long = 0

    enum class FeedMode {
        NORMAL,
        FAVORITES
    }

    private var currentMode = FeedMode.NORMAL

    private fun isBlacklisted(post: Post): Boolean {
        val blacklist = BlacklistManager.getAll()
        if (blacklist.isEmpty()) return false
        val postTags = post.tags.split(" ")
        return postTags.any { it in blacklist }
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                val uri = downloadManager.getUriForDownloadedFile(id)
                installApk(uri)
            }
        }
    }

    private val detailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val position = result.data?.getIntExtra("position", -1)
                if (position != -1 && position != null) {
                    recyclerView.scrollToPosition(position)
                }
            }
        }

    companion object {
        const val NEW_SEARCH_TAG = "NEW_SEARCH_TAG"
        const val EXTRA_RATING_S = "extra_rating_s"
        const val EXTRA_RATING_Q = "extra_rating_q"
        const val EXTRA_RATING_E = "extra_rating_e"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        ContextCompat.registerReceiver(
            this,
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setupViews()
        setupRecyclerView()
        setupSearch()
        setupBackPressedHandler()

        lifecycleScope.launch {
            ArtistCache.initialize(this@MainActivity)
            ArtistSyncer.launchSync(this@MainActivity)
            TagTypeCache.tagTypes.collect { tagMap ->
                val newTags = tagMap.keys.toMutableList()
                newTags.addAll(ArtistCache.getAllArtistNames())
                allAvailableTags = newTags.distinct()
            }
        }

        observeViewModels()

        lifecycleScope.launch {
            postAdapter.loadStateFlow.collect { loadState ->
                swipeRefreshLayout.isRefreshing =
                    loadState.refresh is androidx.paging.LoadState.Loading
            }
        }

        updateViewModel.checkForUpdate(this, "AliceJump", "YandeReViewer")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        searchBox = findViewById(R.id.searchBox)
        ratingSCheckbox = findViewById(R.id.rating_s_checkbox)
        ratingQCheckbox = findViewById(R.id.rating_q_checkbox)
        ratingECheckbox = findViewById(R.id.rating_e_checkbox)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        tagChipGroup = findViewById(R.id.tagChipGroup)
        fabScrollToTop = findViewById(R.id.fab_scroll_to_top)
        fabRefresh = findViewById(R.id.fab_refresh)

        swipeRefreshLayout.setOnRefreshListener {
            when (currentMode) {
                FeedMode.NORMAL -> {
                    performSearch()
                    postAdapter.refresh()
                }
                FeedMode.FAVORITES -> {
                    switchToFavorites()
                }
            }
        }

        fabScrollToTop.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }

        fabRefresh.setOnClickListener {
            when (currentMode) {
                FeedMode.NORMAL -> {
                    performSearch()
                    postAdapter.refresh()
                }
                FeedMode.FAVORITES -> {
                    switchToFavorites()
                }
            }
        }
    }

    private fun setupSearch() {

        tagCompletionAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        searchBox.setAdapter(tagCompletionAdapter)
        searchBox.threshold = 1

        searchBox.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                val text = searchBox.text.toString().trim()
                if (text.isNotEmpty()) {
                    addTag(text)
                    searchBox.setText("")
                }
                performSearch()
                true
            } else false
        }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val word = s?.toString()?.trim() ?: return
                tagCompletionAdapter.clear()
                tagCompletionAdapter.addAll(allAvailableTags)
                tagCompletionAdapter.filter.filter(word)
                if (word.isNotEmpty()) searchBox.showDropDown()
            }
        })

        searchBox.setOnItemClickListener { parent, _, position, _ ->
            val tag = parent.getItemAtPosition(position) as String
            addTag(tag)
            searchBox.setText("")
            performSearch()
        }

        handleIntent(intent)
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onPostClick = { post, position, imageView ->
                val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager
                val firstVisiblePositions = IntArray(layoutManager.spanCount)
                layoutManager.findFirstVisibleItemPositions(firstVisiblePositions)
                val firstVisible = firstVisiblePositions.minOrNull() ?: 0

                val lastVisiblePositions = IntArray(layoutManager.spanCount)
                layoutManager.findLastVisibleItemPositions(lastVisiblePositions)
                val lastVisible = lastVisiblePositions.maxOrNull() ?: 0

                val intent = Intent(this, DetailActivity::class.java).apply {
                    val posts = postAdapter.snapshot().items
                    putParcelableArrayListExtra("posts", ArrayList(posts))
                    putExtra("position", position)
                    putExtra("first_visible_position", firstVisible)
                    putExtra("last_visible_position", lastVisible)
                    putExtra(EXTRA_RATING_S, ratingSCheckbox.isChecked)
                    putExtra(EXTRA_RATING_Q, ratingQCheckbox.isChecked)
                    putExtra(EXTRA_RATING_E, ratingECheckbox.isChecked)
                }

                val transitionName = "image_transition_${'$'}{post.id}"
                imageView.transitionName = transitionName
                intent.putExtra("transition_name", transitionName)

                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, imageView, transitionName
                )
                detailActivityLauncher.launch(intent, options)
            },
            onSelectionChange = { count ->
                if (count > 0) showSelectionMenu(count) else hideSelectionMenu()
            }
        )

        recyclerView.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = postAdapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager
                val firstVisiblePositions = IntArray(layoutManager.spanCount)
                layoutManager.findFirstVisibleItemPositions(firstVisiblePositions)
                val firstVisible = firstVisiblePositions.minOrNull() ?: 0
                if (firstVisible > 0) {
                    fabScrollToTop.show()
                    fabRefresh.show()
                } else {
                    fabScrollToTop.hide()
                    fabRefresh.hide()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.action_download)?.isVisible = false
        menu.findItem(R.id.action_copy_links)?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val selectedPosts = postAdapter.getSelectedItems()
        return when (item.itemId) {
            R.id.action_download -> {
                if (selectedPosts.isEmpty()) {
                    Toast.makeText(this, R.string.no_items_selected, Toast.LENGTH_SHORT).show()
                    return true
                }
                selectedPosts.forEach { post ->
                    downloadImage(this, post, false)
                }
                Toast.makeText(
                    this,
                    getString(
                        R.string.started_downloading_multiple_items,
                        selectedPosts.size
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                postAdapter.clearSelection()
                hideSelectionMenu()
                true
            }
            R.id.action_copy_links -> {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val links = selectedPosts.joinToString("\n\n") { it.file_url }
                val clip = ClipData.newPlainText("Yande.re Links", links)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.links_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                postAdapter.clearSelection()
                hideSelectionMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSelectionMenu(count: Int) {
        supportActionBar?.title = getString(R.string.items_selected, count)
        val menu = findViewById<Toolbar>(R.id.toolbar).menu
        menu.findItem(R.id.action_download)?.isVisible = true
        menu.findItem(R.id.action_copy_links)?.isVisible = true
    }

    private fun hideSelectionMenu() {
        supportActionBar?.title = getString(R.string.app_name)
        val menu = findViewById<Toolbar>(R.id.toolbar).menu
        menu.findItem(R.id.action_download)?.isVisible = false
        menu.findItem(R.id.action_copy_links)?.isVisible = false
    }

    private fun addTag(tag: String) {
        val artistId = ArtistCache.getArtistId(tag)
        val artist = artistId?.let { ArtistCache.getArtist(it) }

        val originalName: String
        val displayName: String

        if (artist != null) {
            originalName = artist.name
            displayName = getArtistDisplayName(artist)
        } else {
            originalName = tag
            displayName = tag
        }

        if (selectedTags.contains(originalName)) return
        selectedTags.add(originalName)

        val chip = Chip(this).apply {
            text = displayName
            isCloseIconVisible = true

            val typeNum = if (artist != null) 1 else TagTypeCache.tagTypes.value[tag]

            val color = when {
                tag.startsWith("rating:s") -> "#4CAF50".toColorInt()
                tag.startsWith("rating:q") -> "#FFC107".toColorInt()
                tag.startsWith("rating:e") -> "#F44336".toColorInt()
                typeNum == 1 -> "#F06292".toColorInt() // Artist
                typeNum == 3 -> "#BA68C8".toColorInt() // Copyright
                typeNum == 4 -> "#7986CB".toColorInt() // Character
                typeNum == 5 -> "#4DB6AC".toColorInt() // Style
                typeNum == 0 -> "#90A4AE".toColorInt() // General
                else -> "#BDBDBD".toColorInt()
            }

            chipBackgroundColor = ColorStateList.valueOf(color)
            setTextColor(Color.WHITE)

            setOnCloseIconClickListener {
                selectedTags.remove(originalName)
                tagChipGroup.removeView(this)
                performSearch()
            }

            setOnLongClickListener {
                val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Tag", originalName)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, getString(R.string.tag_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                true
            }
        }
        tagChipGroup.addView(chip)
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            postViewModel.posts.collectLatest { pagingData ->
                postAdapter.submitData(
                    pagingData.filter { post ->
                        !isBlacklisted(post)
                    }
                )
            }
        }

        lifecycleScope.launch {
            updateViewModel.updateState.collect { state ->
                when (state) {
                    is UpdateCheckState.UpdateAvailable -> {
                        showUpdateDialog(state.release)
                    }
                    is UpdateCheckState.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.update_check_failed, state.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {}
                }
            }
        }

    }

    private fun filterFavorites(
        source: List<Post>,
        tags: List<String>
    ): List<Post> {
        if (tags.isEmpty()) return source
        return source.filter { post ->
            tags.all { tag ->
                when {
                    tag.startsWith("rating:") -> {
                        val r = tag.removePrefix("rating:")
                        post.rating.equals(r, ignoreCase = true)
                    }
                    else -> {
                        val postTags = post.tags.split(" ")
                        postTags.contains(tag)
                    }
                }
            }
        }
    }

    private fun performSearch() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchBox.windowToken, 0)
        saveCheckboxStates()
        val queryTags = mutableListOf<String>()
        queryTags += selectedTags
        if (ratingSCheckbox.isChecked) queryTags += "rating:s"
        if (ratingQCheckbox.isChecked) queryTags += "rating:q"
        if (ratingECheckbox.isChecked) queryTags += "rating:e"

        when (currentMode) {
            FeedMode.NORMAL -> {
                postViewModel.search(queryTags.joinToString(" "))
                postViewModel.forceRefresh()
            }
            FeedMode.FAVORITES -> {
                lifecycleScope.launch {
                    val all = FavoritesManager.getAll(this@MainActivity)
                    val filtered = filterFavorites(all, queryTags)
                    val data = if (filtered.isEmpty()) {
                        PagingData.empty()
                    } else {
                        PagingData.from(filtered)
                    }
                    postAdapter.submitData(lifecycle, data)
                    recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    private fun showUpdateDialog(latestRelease: GitHubRelease) {
        lifecycleScope.launch {
            val allReleases = withContext(Dispatchers.IO) {
                GitHubApiClient.api.getAllReleases("AliceJump", "YandeReViewer")
            }
            val currentVersion = try {
                val pInfo = packageManager.getPackageInfo(packageName, 0)
                pInfo.versionName ?: "0.0"
            } catch (_: Exception) {
                "0.0"
            }
            val newerReleases = allReleases
                .filter { isVersionNewer(it.tagName, currentVersion) }
                .sortedBy { it.tagName }

            val changelog = newerReleases.joinToString("\n\n") { getString(R.string.version_text, it.tagName, it.body) }

            // 4️⃣ 弹出 Dialog
            AlertDialog.Builder(this@MainActivity)
                .setTitle(getString(R.string.new_version_available, latestRelease.name))
                .setMessage(changelog.ifEmpty { getString(R.string.no_changelog_available) })
                .setPositiveButton(R.string.update_now) { dialog, _ ->
                    val apkAsset = latestRelease.assets.firstOrNull { it.downloadUrl.endsWith(".apk") }
                    if (apkAsset != null) {
                        startDownload(apkAsset.downloadUrl, latestRelease.tagName)
                    } else {
                        Toast.makeText(this@MainActivity, R.string.no_apk_found, Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.ignore_this_version) { dialog, _ ->
                    updateViewModel.ignoreThisVersion(this@MainActivity, latestRelease.tagName.removePrefix("v"))
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.remind_me_in_7_days) { dialog, _ ->
                    updateViewModel.snoozeUpdate(this@MainActivity, 7)
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun isVersionNewer(version: String, current: String): Boolean {
        val v1 = version.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val v2 = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(v1.size, v2.size)) {
            val n1 = v1.getOrElse(i) { 0 }
            val n2 = v2.getOrElse(i) { 0 }
            if (n1 > n2) return true
            if (n1 < n2) return false
        }
        return false
    }

    private fun startDownload(url: String, version: String) {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(url.toUri()).setTitle(getString(R.string.yandereviewer_update))
            .setDescription(getString(R.string.downloading_version, version))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                this, Environment.DIRECTORY_DOWNLOADS, "YandeReViewer-$version.apk"
            )
        downloadId = downloadManager.enqueue(request)
        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()
    }

    private fun installApk(uri: Uri?) {
        if (uri == null) {
            Toast.makeText(this, R.string.install_update_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.hasExtra(EXTRA_RATING_S) == true) {
            ratingSCheckbox.isChecked = intent.getBooleanExtra(EXTRA_RATING_S, false)
            ratingQCheckbox.isChecked = intent.getBooleanExtra(EXTRA_RATING_Q, false)
            ratingECheckbox.isChecked = intent.getBooleanExtra(EXTRA_RATING_E, false)
        }
        intent?.getStringExtra(NEW_SEARCH_TAG)?.let { tag ->
            selectedTags.clear()
            tagChipGroup.removeAllViews()
            addTag(tag)
            performSearch()
        }
    }

    private fun saveCheckboxStates() {
        val prefs = getSharedPreferences("rating_state", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(EXTRA_RATING_S, ratingSCheckbox.isChecked)
            putBoolean(EXTRA_RATING_Q, ratingQCheckbox.isChecked)
            putBoolean(EXTRA_RATING_E, ratingECheckbox.isChecked)
            apply()
        }
    }

    override fun onStop() {
        super.onStop()
        saveCheckboxStates()
        TagTypeCache.flush(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        TagTypeCache.flush(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_favorite_tags -> {
                startActivity(Intent(this, FavoriteTagsActivity::class.java))
            }
            R.id.nav_favorite_images -> {
                switchToFavorites()
            }
            R.id.nav_blacklist_tags -> {
                startActivity(Intent(this, BlacklistActivity::class.java))
            }
            R.id.nav_history -> Toast.makeText(this, R.string.history_clicked, Toast.LENGTH_SHORT).show()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun switchToNormalMode() {
        currentMode = FeedMode.NORMAL
        supportActionBar?.title = getString(R.string.app_name)
        performSearch()
    }

    private fun switchToFavorites() {
        currentMode = FeedMode.FAVORITES
        supportActionBar?.title = getString(R.string.my_favorites)
        lifecycleScope.launch {
            val allFavorites = FavoritesManager.getAll(this@MainActivity)
            if (allFavorites.isEmpty()) {
                postAdapter.submitData(PagingData.empty())
                return@launch
            }
            val sorted = allFavorites.sortedByDescending { it.favoriteAt }
            favoriteSource = sorted
            postAdapter.submitData(PagingData.from(sorted))
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return
                }
                if (postAdapter.isSelectionActive()) {
                    postAdapter.clearSelection()
                    hideSelectionMenu()
                    return
                }
                if (currentMode == FeedMode.FAVORITES) {
                    switchToNormalMode()
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }
}