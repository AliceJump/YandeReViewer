package com.alicejump.yandeviewer.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.alicejump.yandeviewer.DetailActivity
import com.alicejump.yandeviewer.model.Post
import com.alicejump.yandeviewer.viewmodel.TagTypeCache

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalPagingApi::class,
)
@Composable
fun MainScreen(
    posts: LazyPagingItems<Post>,
    onSearch: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    var newTag by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var ratingS by remember { mutableStateOf(false) }
    var ratingQ by remember { mutableStateOf(false) }
    var ratingE by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val isRefreshing = posts.loadState.refresh is LoadState.Loading
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val allAvailableTags by TagTypeCache.tagTypes.collectAsState()
    val suggestedTags = remember(newTag, allAvailableTags) {
        if (newTag.isBlank()) {
            emptyList()
        } else {
            allAvailableTags.keys.filter { it.contains(newTag, ignoreCase = true) }.take(10)
        }
    }

    fun performSearch() {
        keyboardController?.hide()
        val searchTags = mutableListOf<String>()
        searchTags.addAll(selectedTags)
        if (ratingS) searchTags.add("rating:s")
        if (ratingQ) searchTags.add("rating:q")
        if (ratingE) searchTags.add("rating:e")
        onSearch(searchTags.joinToString(" "))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.yandereviewer_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_description))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Box {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = {
                            newTag = it
                            dropdownExpanded = it.isNotBlank()
                        },
                        label = { Text(stringResource(R.string.add_a_tag)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newTag.isNotBlank() && !selectedTags.contains(newTag)) {
                                selectedTags.add(newTag.trim())
                                newTag = ""
                                performSearch()
                            }
                            keyboardController?.hide()
                        })
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded && suggestedTags.isNotEmpty(),
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestedTags.forEach { tag ->
                            DropdownMenuItem(text = { Text(tag) }, onClick = {
                                if (!selectedTags.contains(tag)) {
                                    selectedTags.add(tag)
                                }
                                newTag = ""
                                dropdownExpanded = false
                                performSearch()
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    selectedTags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { /* Nothing to do on click */ },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove_tag_description),
                                    modifier = Modifier.clickable { selectedTags.remove(tag) }
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ratingS, onCheckedChange = { ratingS = it })
                    Text(stringResource(R.string.safe))
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(checked = ratingQ, onCheckedChange = { ratingQ = it })
                    Text(stringResource(R.string.questionable))
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(checked = ratingE, onCheckedChange = { ratingE = it })
                    Text(stringResource(R.string.explicit))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { performSearch() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.search))
                }
            }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { posts.refresh() })
            {
                if (posts.itemCount == 0 && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_results_found))
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        items(posts.itemCount, key = posts.itemKey { it.id }) { index ->
                            val post = posts[index]
                            if (post != null) {
                                PostItem(post) {
                                    val intent = Intent(context, DetailActivity::class.java).apply {
                                        val postList = posts.itemSnapshotList.items
                                        putParcelableArrayListExtra("posts", ArrayList(postList))
                                        putExtra("position", index)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PostItem(post: Post, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AsyncImage(
            model = post.preview_url,
            contentDescription = post.tags,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
