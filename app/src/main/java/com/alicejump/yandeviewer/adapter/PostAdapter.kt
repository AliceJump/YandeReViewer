package com.alicejump.yandeviewer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

class PostAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onSelectionChange: (Int) -> Unit
) : PagingDataAdapter<Post, PostVH>(diff) {

    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<Post>()

    fun getSelectedItems(): List<Post> {
        return selectedItems.toList()
    }

    fun enterSelectionMode() {
        isSelectionMode = true
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        val post = getItem(position) ?: return
        holder.bind(post, isSelectionMode, selectedItems.contains(post))

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                if (selectedItems.contains(post)) {
                    selectedItems.remove(post)
                } else {
                    selectedItems.add(post)
                }
                onSelectionChange(selectedItems.size)
                notifyItemChanged(position)
            } else {
                onPostClick(post)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                enterSelectionMode()
                selectedItems.add(post)
                onSelectionChange(selectedItems.size)
                notifyItemChanged(position)
            }
            true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PostVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostVH(view)
    }

    companion object {
        val diff = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(a: Post, b: Post) = a.id == b.id
            override fun areContentsTheSame(a: Post, b: Post) = a == b
        }
    }
}
