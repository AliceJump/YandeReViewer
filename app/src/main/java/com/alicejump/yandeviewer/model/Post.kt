package com.alicejump.yandeviewer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Post(
    val id: Int,
    val preview_url: String,
    val file_url: String,
    val tags: String,
    val source: String?
) : Parcelable
