package com.alicejump.yandeviewer

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@OptIn(ExperimentalPagingApi::class)
class MainActivity : AppCompatActivity() {

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

        // 状态栏高度 padding
        ViewCompat.setOnApplyWindowInsetsListener(searchBox) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBarHeight)
            insets
        }

        swipeRefreshLayout.setOnRefreshListener {
            postAdapter.refresh() // Paging3 触发刷新
        }
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

    private fun setupSearch() {
        searchBtn.setOnClickListener { performSearch() }

        searchBox.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                performSearch()
                true
            } else false
        }

        tagCompletionAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        searchBox.setAdapter(tagCompletionAdapter)

        // 1. 初始化 Adapter
        tagCompletionAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        searchBox.setAdapter(tagCompletionAdapter)
        searchBox.threshold = 1 // 只要输入一个字符就开始过滤

        // 2. TextWatcher
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                val cursorPos = searchBox.selectionStart.coerceAtLeast(0)
                val lastSpaceIndex = text.lastIndexOf(' ', cursorPos - 1)
                val currentWord = if (lastSpaceIndex < 0) text.take(cursorPos)
                else text.substring(lastSpaceIndex + 1, cursorPos)

                // 更新 adapter 数据
                tagCompletionAdapter.clear()
                tagCompletionAdapter.addAll(allAvailableTags)
                tagCompletionAdapter.notifyDataSetChanged()

                // 强制过滤当前单词
                tagCompletionAdapter.filter.filter(currentWord)

                // 强制显示下拉
                if (currentWord.isNotEmpty()) {
                    searchBox.showDropDown()
                }
            }
        })

        searchBox.setOnItemClickListener { _, _, _, _ ->

        }

        handleIntent(intent)
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
        // 隐藏键盘
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        searchBox.post {
            imm.hideSoftInputFromWindow(searchBox.windowToken, 0)
            recyclerView.scrollToPosition(0)
        }

        val tags = mutableListOf<String>()
        if (searchBox.text.isNotEmpty()) tags += searchBox.text.toString()
        if (ratingSCheckbox.isChecked) tags += "rating:s"
        if (ratingQCheckbox.isChecked) tags += "rating:q"
        if (ratingECheckbox.isChecked) tags += "rating:e"

        postViewModel.search(tags.joinToString(" "))
        postAdapter.refresh()
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
        intent?.getStringExtra(NEW_SEARCH_TAG)?.let {
            searchBox.setText(it)
            ratingSCheckbox.isChecked = false
            ratingQCheckbox.isChecked = false
            ratingECheckbox.isChecked = false
            performSearch()
        }
    }
}
