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
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.alicejump.yandeviewer.adapter.PostAdapter
import com.alicejump.yandeviewer.network.GitHubRelease
import com.alicejump.yandeviewer.viewmodel.PostViewModel
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import com.alicejump.yandeviewer.viewmodel.UpdateCheckState
import com.alicejump.yandeviewer.viewmodel.UpdateViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagingApi::class)
class MainActivity : AppCompatActivity() {
    private lateinit var tagChipGroup: ChipGroup

    // 真正的标签数据源
    private val selectedTags = linkedSetOf<String>()

    private val postViewModel by viewModels<PostViewModel>()
    private val updateViewModel by viewModels<UpdateViewModel>()
    private lateinit var postAdapter: PostAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBox: AutoCompleteTextView
    private lateinit var searchBtn: Button
    private lateinit var ratingSCheckbox: CheckBox
    private lateinit var ratingQCheckbox: CheckBox
    private lateinit var ratingECheckbox: CheckBox
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var tagCompletionAdapter: ArrayAdapter<String>
    private var allAvailableTags: List<String> = emptyList()

    private var actionMode: ActionMode? = null
    private var downloadId: Long = 0
    private var tagTypeMap: Map<String, Int> = emptyMap()
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
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.action_mode_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val selectedItems = postAdapter.getSelectedItems()
            return when (item.itemId) {
                R.id.action_download -> {
                    val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    selectedItems.forEach { post ->
                        val request = DownloadManager.Request(post.file_url.toUri())
                            .setTitle("Downloading Post ${post.id}").setDescription(post.tags)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS, "yande.re_${post.id}.jpg"
                            )
                        downloadManager.enqueue(request)
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Started downloading ${selectedItems.size} items",
                        Toast.LENGTH_SHORT
                    ).show()
                    mode.finish()
                    true
                }

                R.id.action_copy_links -> {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val links = selectedItems.joinToString("\n") { it.file_url }
                    val clip = ClipData.newPlainText("Yande.re Links", links)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        this@MainActivity, "Links copied to clipboard", Toast.LENGTH_SHORT
                    ).show()
                    mode.finish()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            postAdapter.exitSelectionMode()
            actionMode = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 注册下载完成广播
        ContextCompat.registerReceiver(
            this,
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setupViews()
        setupRecyclerView()
        setupSearch()
        observeViewModels()

        // Paging3 + SwipeRefreshLayout 联动
        lifecycleScope.launch {
            postAdapter.loadStateFlow.collect { loadState ->
                swipeRefreshLayout.isRefreshing =
                    loadState.refresh is androidx.paging.LoadState.Loading
            }
        }

        // 启动检查更新
        updateViewModel.checkForUpdate(this, "AliceJump", "YandeReViewer")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    private fun setupViews() {

        recyclerView = findViewById(R.id.recyclerView)
        searchBox = findViewById(R.id.searchBox)
        searchBtn = findViewById(R.id.searchBtn)
        ratingSCheckbox = findViewById(R.id.rating_s_checkbox)
        ratingQCheckbox = findViewById(R.id.rating_q_checkbox)
        ratingECheckbox = findViewById(R.id.rating_e_checkbox)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        tagChipGroup = findViewById(R.id.tagChipGroup)


        swipeRefreshLayout.setOnRefreshListener {
            performSearch()
        }
    }
    private fun setupSearch() {

        searchBtn.setOnClickListener { performSearch() }

        // ===== 补全 =====
        tagCompletionAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())

        searchBox.setAdapter(tagCompletionAdapter)
        searchBox.threshold = 1
        searchBox.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                // 如果输入框里还有手打的 tag，也要加进去
                val text = searchBox.text.toString().trim()
                if (text.isNotEmpty()) {
                    addTag(text)
                    searchBox.setText("")
                }

                performSearch()
                true
            } else {
                false
            }
        }

        // 输入过滤
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val word = s?.toString()?.trim() ?: return

                tagCompletionAdapter.clear()
                tagCompletionAdapter.addAll(allAvailableTags)
                tagCompletionAdapter.filter.filter(word)

                if (word.isNotEmpty()) {
                    searchBox.showDropDown()
                }
            }
        })

        // 选中补全
        searchBox.setOnItemClickListener { parent, _, position, _ ->
            val tag = parent.getItemAtPosition(position) as String

            addTag(tag)

            searchBox.setText("")

            performSearch()
        }

        handleIntent(intent)
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(onPostClick = { post, position, imageView ->
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
            }
            val transitionName = "image_transition_${post.id}"
            imageView.transitionName = transitionName
            intent.putExtra("transition_name", transitionName)

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, imageView, transitionName
            )
            detailActivityLauncher.launch(intent, options)
        }, onSelectionChange = { count ->
            if (count > 0) {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(actionModeCallback)
                }
                actionMode?.title = "$count selected"
            } else {
                actionMode?.finish()
            }
        })

        recyclerView.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = postAdapter
    }

    private fun addTag(tag: String) {
        if (selectedTags.contains(tag)) return

        selectedTags.add(tag)
        val chip = Chip(this).apply {
            text = tag
            isCloseIconVisible = true

            // ===== 重点：颜色逻辑 =====


            val typeNum = TagTypeCache.tagTypes.value[tag]   // ← 直接取当前值

            val color = when {
                // rating 特殊规则
                tag.startsWith("rating:s") -> "#4CAF50".toColorInt()
                tag.startsWith("rating:q") -> "#FFC107".toColorInt()
                tag.startsWith("rating:e") -> "#F44336".toColorInt()

                // typenum 规则
                typeNum == 1 -> "#F06292".toColorInt() // artist
                typeNum == 3 -> "#BA68C8".toColorInt() // copyright
                typeNum == 4 -> "#7986CB".toColorInt() // character
                typeNum == 5 -> "#4DB6AC".toColorInt() // circle
                typeNum == 0 -> "#90A4AE".toColorInt() // general

                else -> "#BDBDBD".toColorInt()
            }

            chipBackgroundColor = ColorStateList.valueOf(color)
            setTextColor(Color.WHITE)

            // 删除逻辑（你原来的）
            setOnCloseIconClickListener {
                selectedTags.remove(tag)
                tagChipGroup.removeView(this)
                performSearch()
            }

            setOnLongClickListener {
                val typeNum = tagTypeMap[tag]

                val typeName = when (typeNum) {
                    1 -> "artist"
                    3 -> "copyright"
                    4 -> "character"
                    5 -> "circle"
                    0 -> "general"
                    else -> "unknown"
                }

                Toast.makeText(
                    context,
                    "$tag\n$typeName ($typeNum)",
                    Toast.LENGTH_SHORT
                ).show()

                true
            }

        }

        tagChipGroup.addView(chip)
    }



    private fun observeViewModels() {
        lifecycleScope.launch {
            postViewModel.posts.collectLatest {
                postAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            updateViewModel.updateState.collect { state ->
                when (state) {
                    is UpdateCheckState.UpdateAvailable -> showUpdateDialog(state.release)
                    is UpdateCheckState.Error -> Toast.makeText(
                        this@MainActivity,
                        "Update check failed: ${state.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            TagTypeCache.tagTypes.collect {
                allAvailableTags = it.keys.toList()
            }
        }
    }

    private fun performSearch() {

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchBox.windowToken, 0)

        val tags = mutableListOf<String>()

        // 已选标签
        tags += selectedTags

        // rating
        if (ratingSCheckbox.isChecked) tags += "rating:s"
        if (ratingQCheckbox.isChecked) tags += "rating:q"
        if (ratingECheckbox.isChecked) tags += "rating:e"

        postViewModel.search(tags.joinToString(" "))

        recyclerView.scrollToPosition(0)
    }


    private fun showUpdateDialog(release: GitHubRelease) {
        AlertDialog.Builder(this).setTitle("New Version Available: ${release.name}")
            .setMessage(release.body).setPositiveButton("Update Now") { dialog, _ ->
                val apkAsset = release.assets.firstOrNull { it.downloadUrl.endsWith(".apk") }
                if (apkAsset != null) startDownload(apkAsset.downloadUrl, release.tagName)
                else Toast.makeText(this, "No APK found in the release.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }.setNegativeButton("Ignore this version") { dialog, _ ->
                updateViewModel.ignoreThisVersion(this, release.tagName.removePrefix("v"))
                dialog.dismiss()
            }.setNeutralButton("Remind me in 7 days") { dialog, _ ->
                updateViewModel.snoozeUpdate(this, 7)
                dialog.dismiss()
            }.setCancelable(false).show()
    }

    private fun startDownload(url: String, version: String) {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(url.toUri()).setTitle("YandeReViewer Update")
            .setDescription("Downloading version $version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                this, Environment.DIRECTORY_DOWNLOADS, "YandeReViewer-$version.apk"
            )
        downloadId = downloadManager.enqueue(request)
        Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
    }

    private fun installApk(uri: Uri?) {
        if (uri == null) {
            Toast.makeText(this, "Failed to install update: URI is null", Toast.LENGTH_SHORT).show()
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
        intent?.getStringExtra(NEW_SEARCH_TAG)?.let { tag ->

            // 1. 清空旧状态
            selectedTags.clear()
            tagChipGroup.removeAllViews()

            // 2. 走新体系
            addTag(tag)


            performSearch()
        }
    }
    override fun onStop() {
        super.onStop()
        TagTypeCache.flush(this)
    }

}
