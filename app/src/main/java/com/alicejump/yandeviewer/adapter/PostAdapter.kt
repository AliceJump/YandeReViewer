package com.alicejump.yandeviewer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

class PostAdapter(private val click: (Post) -> Unit) :
    PagingDataAdapter<Post, PostVH>(diff) {

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        val post = getItem(position) ?: return
        holder.bind(post)
        holder.itemView.setOnClickListener { click(post) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PostVH(LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false))

    companion object {
        val diff = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(a: Post, b: Post) = a.id == b.id
            override fun areContentsTheSame(a: Post, b: Post) = a == b
        }
    }
}
