package com.alicejump.yandeviewer

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alicejump.yandeviewer.adapter.PostAdapter
import com.alicejump.yandeviewer.model.Post
import com.alicejump.yandeviewer.network.GitHubRelease
import com.alicejump.yandeviewer.viewmodel.PostViewModel
import com.alicejump.yandeviewer.viewmodel.UpdateCheckState
import com.alicejump.yandeviewer.viewmodel.UpdateViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalPagingApi::class)
class MainActivity : AppCompatActivity() {

    private val postViewModel by viewModels<PostViewModel>()
    private val updateViewModel by viewModels<UpdateViewModel>()
    private lateinit var adapter: PostAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBox: EditText
    private lateinit var searchBtn: Button
    private lateinit var ratingSCheckbox: CheckBox
    private lateinit var ratingQCheckbox: CheckBox
    private lateinit var ratingECheckbox: CheckBox

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

    companion object {
        const val NEW_SEARCH_TAG = "NEW_SEARCH_TAG"
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.action_mode_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val selectedItems = adapter.getSelectedItems()
            return when (item.itemId) {
                R.id.action_download -> {
                    val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    selectedItems.forEach { post ->
                        val request = DownloadManager.Request(Uri.parse(post.file_url))
                            .setTitle("Downloading Post ${post.id}")
                            .setDescription(post.tags)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "yande.re_${post.id}.jpg")
                        downloadManager.enqueue(request)
                    }
                    Toast.makeText(this@MainActivity, "Started downloading ${selectedItems.size} items", Toast.LENGTH_SHORT).show()
                    mode.finish()
                    true
                }
                R.id.action_copy_links -> {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val links = selectedItems.joinToString("\n") { it.file_url }
                    val clip = ClipData.newPlainText("Yande.re Links", links)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@MainActivity, "Links copied to clipboard", Toast.LENGTH_SHORT).show()
                    mode.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            adapter.exitSelectionMode()
            actionMode = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ContextCompat.registerReceiver(this, onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_NOT_EXPORTED)

        setupViews()
        setupRecyclerView()
        setupSearch()
        observeViewModels()

        // Check for updates on startup
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

        ViewCompat.setOnApplyWindowInsetsListener(searchBox) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBarHeight + view.paddingTop)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(
            onPostClick = { post, position ->
                val intent = Intent(this, DetailActivity::class.java).apply {
                    val posts = adapter.snapshot().items.filterNotNull()
                    putParcelableArrayListExtra("posts", ArrayList(posts))
                    putExtra("position", position)
                }
                startActivity(intent)
            },
            onSelectionChange = { count ->
                if (count > 0) {
                    if (actionMode == null) {
                        actionMode = startSupportActionMode(actionModeCallback)
                    }
                    actionMode?.title = "$count selected"
                } else {
                    actionMode?.finish()
                }
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchBtn.setOnClickListener {
            search()
        }
        handleIntent(intent)
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            postViewModel.posts.collectLatest {
                adapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            updateViewModel.updateState.collect { state ->
                when (state) {
                    is UpdateCheckState.UpdateAvailable -> showUpdateDialog(state.release)
                    is UpdateCheckState.Error -> Toast.makeText(this@MainActivity, "Update check failed: ${state.message}", Toast.LENGTH_LONG).show()
                    else -> { /* Idle, Checking, or NoUpdate states - do nothing */ }
                }
            }
        }
    }

    private fun showUpdateDialog(release: GitHubRelease) {
        AlertDialog.Builder(this)
            .setTitle("New Version Available: ${release.name}")
            .setMessage(release.body) // The release notes
            .setPositiveButton("Update Now") { dialog, _ ->
                val apkAsset = release.assets.firstOrNull { it.downloadUrl.endsWith(".apk") }
                if (apkAsset != null) {
                    startDownload(apkAsset.downloadUrl, release.tagName)
                } else {
                    Toast.makeText(this, "No APK found in the release.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Ignore this version") { dialog, _ ->
                updateViewModel.ignoreThisVersion(this, release.tagName.removePrefix("v"))
                dialog.dismiss()
            }
            .setNeutralButton("Remind me in 7 days") { dialog, _ ->
                updateViewModel.snoozeUpdate(this, 7)
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun startDownload(url: String, version: String) {
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("YandeReViewer Update")
            .setDescription("Downloading version $version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "YandeReViewer-$version.apk")

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
            search()
        }
    }

    private fun search() {
        val tags = StringBuilder(searchBox.text.toString())
        if (ratingSCheckbox.isChecked) {
            tags.append(" rating:s")
        }
        if (ratingQCheckbox.isChecked) {
            tags.append(" rating:q")
        }
        if (ratingECheckbox.isChecked) {
            tags.append(" rating:e")
        }
        postViewModel.search(tags.toString().trim())
        recyclerView.scrollToPosition(0)
    }
}
