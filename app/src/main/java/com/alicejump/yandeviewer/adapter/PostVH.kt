package com.alicejump.yandeviewer.adapter

import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.load
import coil.memory.MemoryCache
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

class PostVH(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.postImage)
    private val selectionOverlay: View = view.findViewById(R.id.selectionOverlay)

    fun bind(post: Post, isSelectionMode: Boolean, isSelected: Boolean) {
        // Set the aspect ratio to prevent image jumping
        val layoutParams = imageView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.dimensionRatio = "${post.width}:${post.height}"
        imageView.layoutParams = layoutParams

        val hasOriginalInMemoryCache =
            imageView.context.imageLoader.memoryCache?.get(MemoryCache.Key(post.file_url)) != null

        val imageUrl = if (hasOriginalInMemoryCache) post.file_url else post.preview_url

        imageView.load(imageUrl) {
            allowHardware(false)
        }

        // Show overlay when selected, hide when not
        selectionOverlay.isVisible = isSelected
    }

    fun getImageView(): ImageView {
        return imageView
    }
}
