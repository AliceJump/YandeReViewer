package com.alicejump.yandeviewer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TagInfo(
    val id: Int,
    val name: String,
    val type: Int,
    val count: Int,
    val ambiguous: Boolean
) : Parcelable
