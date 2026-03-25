package com.alicejump.yandeviewer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

data class FavoriteTagItem(
    val tag: String,
    val displayName: String,
    val previewPosts: List<Post> = emptyList(),
    val isLoading: Boolean = true
)

class FavoriteTagAdapter(
    private val onTagClick: (String) -> Unit,
    private val onTagLongClick: (View, String) -> Unit,
    private val onPostClick: (Post, List<Post>) -> Unit
) : ListAdapter<FavoriteTagItem, FavoriteTagAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_tag_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagNameText: TextView = itemView.findViewById(R.id.tagNameText)
        private val previewRecyclerView: RecyclerView = itemView.findViewById(R.id.previewImagesRecyclerView)

        fun bind(item: FavoriteTagItem) {
            tagNameText.text = item.displayName
            itemView.setOnClickListener { onTagClick(item.tag) }
            itemView.setOnLongClickListener {
                onTagLongClick(itemView, item.tag)
                true
            }

            // 只有当图片列表发生变化时才重新设置 Adapter，避免滚动时闪烁
            if (previewRecyclerView.adapter == null || 
                (previewRecyclerView.adapter as? PreviewImageAdapter)?.tag != item.tag) {
                previewRecyclerView.layoutManager =
                    LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                previewRecyclerView.adapter = PreviewImageAdapter(item.tag, item.previewPosts, onPostClick)
            } else {
                (previewRecyclerView.adapter as? PreviewImageAdapter)?.updatePosts(item.previewPosts)
            }
            
            // 如果正在加载且没有图片，可以给个提示或占位，这里简单处理
            previewRecyclerView.visibility = if (item.previewPosts.isEmpty() && !item.isLoading) View.GONE else View.VISIBLE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FavoriteTagItem>() {
        override fun areItemsTheSame(oldItem: FavoriteTagItem, newItem: FavoriteTagItem) = oldItem.tag == newItem.tag
        override fun areContentsTheSame(oldItem: FavoriteTagItem, newItem: FavoriteTagItem) = oldItem == newItem
    }
}

class PreviewImageAdapter(
    val tag: String,
    private var posts: List<Post>,
    private val onPostClick: (Post, List<Post>) -> Unit
) : RecyclerView.Adapter<PreviewImageAdapter.ViewHolder>() {

    fun updatePosts(newPosts: List<Post>) {
        if (this.posts != newPosts) {
            this.posts = newPosts
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            adjustViewBounds = true
            setPadding(4, 0, 4, 0)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        (holder.itemView as ImageView).load(post.preview_url) {
            crossfade(true)
        }
        holder.itemView.setOnClickListener { onPostClick(post, posts) }
    }

    override fun getItemCount(): Int = posts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}