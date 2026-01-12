package com.alicejump.yandeviewer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        searchBox = findViewById(R.id.searchBox)
        searchBtn = findViewById(R.id.searchBtn)

        ViewCompat.setOnApplyWindowInsetsListener(searchBox) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBarHeight + view.paddingTop)
            insets
        }
        adapter = PostAdapter {
            startActivity(Intent(this, DetailActivity::class.java).putExtra("url", it.file_url))
        }

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            vm.posts.collectLatest {
                adapter.submitData(it)
            }
        }

        searchBtn.setOnClickListener {
            vm.search(searchBox.text.toString())
            recyclerView.scrollToPosition(0)
        }
    }
}
