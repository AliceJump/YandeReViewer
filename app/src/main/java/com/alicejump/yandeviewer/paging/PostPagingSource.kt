package com.alicejump.yandeviewer.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.alicejump.yandeviewer.model.Post
import com.alicejump.yandeviewer.network.RetrofitClient
import com.alicejump.yandeviewer.viewmodel.TagTypeCache

class PostPagingSource(
    private val tags: String
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 1
        return try {
            val posts = RetrofitClient.api.getPosts(tags = tags, page = page)

            // Extract all unique tags from the loaded posts and add them to the background queue.
            val allTags = posts.flatMap { it.tags?.split(" ") ?: emptyList() }.toSet()
            TagTypeCache.queueTagsForBackgroundFetching(allTags)

            LoadResult.Page(
                data = posts,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (posts.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? = null
}
