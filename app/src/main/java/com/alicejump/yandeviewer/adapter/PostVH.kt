package com.alicejump.yandeviewer.adapter

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

class PostVH(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.postImage)
    private val checkbox: CheckBox = view.findViewById(R.id.checkbox)

    fun bind(post: Post, isSelectionMode: Boolean, isSelected: Boolean) {
        imageView.load(post.preview_url)
        checkbox.isVisible = isSelectionMode
        checkbox.isChecked = isSelected
    }

    fun getImageView(): ImageView {
        return imageView
    }
}
