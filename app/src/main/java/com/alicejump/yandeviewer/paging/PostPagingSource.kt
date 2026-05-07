package com.alicejump.yandeviewer.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.alicejump.yandeviewer.model.Post
import com.alicejump.yandeviewer.network.RetrofitClient

class PostPagingSource(
    private val tags: String
) : PagingSource<Int, Post>() {
    private companion object {
        const val MAX_EMPTY_PAGE_PROBES = 3
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 1
        return try {
            var requestPage = page
            var posts = RetrofitClient.api.getPosts(tags = tags, page = requestPage)
            var emptyProbeCount = 0

            while (posts.isEmpty() && emptyProbeCount < MAX_EMPTY_PAGE_PROBES) {
                emptyProbeCount++
                requestPage++
                posts = RetrofitClient.api.getPosts(tags = tags, page = requestPage)
            }

            LoadResult.Page(
                data = posts,
                prevKey = if (requestPage == 1) null else requestPage - 1,
                nextKey = if (posts.isEmpty()) null else requestPage + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? = null
}
