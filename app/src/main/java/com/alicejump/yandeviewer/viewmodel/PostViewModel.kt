package com.alicejump.yandeviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.alicejump.yandeviewer.paging.PostPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModel : ViewModel() {

    private val _query = MutableStateFlow("")

    val posts = _query.flatMapLatest { query ->
        Pager(PagingConfig(pageSize = 20)) {
            PostPagingSource(query)
        }.flow
    }.cachedIn(viewModelScope)

    fun search(query: String) {
        _query.value = query
    }
}
