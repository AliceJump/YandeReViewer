package com.alicejump.yandeviewer.adapter

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

class PostVH(view: View) : RecyclerView.ViewHolder(view) {
    private val image: ImageView = view.findViewById(R.id.postImage)

    fun bind(post: Post) {
        image.load(post.preview_url)
    }
}
