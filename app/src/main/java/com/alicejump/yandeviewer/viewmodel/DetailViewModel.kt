package com.alicejump.yandeviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alicejump.yandeviewer.model.TagInfo
import com.alicejump.yandeviewer.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {

    private val _tagsInfo = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tagsInfo = _tagsInfo.asStateFlow()

    fun fetchTagsInfo(tags: String) {
        viewModelScope.launch {
            val tagList = tags.split(" ").filter { !it.startsWith("rating:") }
            val newTagsInfo = mutableMapOf<String, Int>()
            tagList.forEach { tag ->
                try {
                    val tagInfos = RetrofitClient.api.getTags(tag)
                    if (tagInfos.isNotEmpty()) {
                        newTagsInfo[tag] = tagInfos[0].type
                    } else {
                        newTagsInfo[tag] = 0 // Default to general if not found
                    }
                } catch (e: Exception) {
                    newTagsInfo[tag] = 0 // Default on error
                }
            }
            _tagsInfo.value = newTagsInfo
        }
    }
}
