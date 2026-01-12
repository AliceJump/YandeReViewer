package com.alicejump.yandeviewer

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alicejump.yandeviewer.adapter.PostAdapter
import com.alicejump.yandeviewer.viewmodel.PostViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagingApi::class)
class MainActivity : AppCompatActivity() {

    private val vm by viewModels<PostViewModel>()
    private lateinit var adapter: PostAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBox: EditText
    private lateinit var searchBtn: Button
    private lateinit var ratingSCheckbox: CheckBox
    private lateinit var ratingQCheckbox: CheckBox
    private lateinit var ratingECheckbox: CheckBox

    private var actionMode: ActionMode? = null

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

        adapter = PostAdapter(
            onPostClick = { post ->
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putExtra("url", post.file_url)
                    putExtra("preview_url", post.preview_url)
                    putExtra("tags", post.tags)
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

        lifecycleScope.launch {
            vm.posts.collectLatest {
                adapter.submitData(it)
            }
        }

        searchBtn.setOnClickListener {
            search()
        }

        handleIntent(intent)
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
        vm.search(tags.toString().trim())
        recyclerView.scrollToPosition(0)
    }
}
