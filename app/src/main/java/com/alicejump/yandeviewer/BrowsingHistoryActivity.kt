package com.alicejump.yandeviewer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alicejump.yandeviewer.adapter.PostAdapter
import com.alicejump.yandeviewer.data.BrowsingHistoryManager
import com.alicejump.yandeviewer.data.DetailPostCache
import androidx.paging.PagingData
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BrowsingHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter

    private val detailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val position = result.data?.getIntExtra("position", -1)
                if (position != null && position != -1) {
                    recyclerView.scrollToPosition(position)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browsing_history)

        val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        val toolbar: Toolbar? = appBarLayout?.findViewById(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = getString(R.string.history)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        recyclerView = findViewById(R.id.historyRecyclerView)

        postAdapter = PostAdapter(
            onPostClick = { post, position, imageView ->
                val posts = BrowsingHistoryManager.getAll(this)
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_POSTS_CACHE_KEY, DetailPostCache.put(posts))
                    putExtra(DetailActivity.EXTRA_POST_ID, post.id)
                    putExtra("position", position)
                    putExtra("grid_span_count", 2)
                }
                val transitionName = "image_transition_${post.id}"
                imageView.transitionName = transitionName
                intent.putExtra("transition_name", transitionName)
                detailActivityLauncher.launch(intent)
            },
            onSelectionChange = { count ->
                if (count > 0) {
                    supportActionBar?.title = getString(R.string.items_selected, count)
                } else {
                    supportActionBar?.title = getString(R.string.history)
                }
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = postAdapter

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val history = BrowsingHistoryManager.getAll(this@BrowsingHistoryActivity)
            if (history.isEmpty()) {
                Toast.makeText(this@BrowsingHistoryActivity, R.string.no_history_yet, Toast.LENGTH_SHORT).show()
                postAdapter.submitData(lifecycle, PagingData.empty())
            } else {
                postAdapter.submitData(lifecycle, PagingData.from(history))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_history -> {
                showClearHistoryDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.clear_history_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                BrowsingHistoryManager.clearAll(this)
                lifecycleScope.launch {
                    postAdapter.submitData(lifecycle, PagingData.empty())
                }
                Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
