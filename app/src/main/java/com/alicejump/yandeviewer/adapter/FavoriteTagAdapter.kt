package com.alicejump.yandeviewer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

data class FavoriteTagItem(
    val tag: String,
    val displayName: String,
    val previewPosts: List<Post> = emptyList()
)

class FavoriteTagAdapter(
    private val onTagClick: (String) -> Unit,
    private val onTagLongClick: (View, String) -> Unit,
    private val onPostClick: (Post, List<Post>) -> Unit
) : RecyclerView.Adapter<FavoriteTagAdapter.ViewHolder>() {

    private var items: List<FavoriteTagItem> = emptyList()

    fun submitList(newList: List<FavoriteTagItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_tag_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

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

            previewRecyclerView.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            previewRecyclerView.adapter = PreviewImageAdapter(item.previewPosts) { post ->
                onPostClick(post, item.previewPosts)
            }
        }
    }
}

class PreviewImageAdapter(
    private val posts: List<Post>,
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<PreviewImageAdapter.ViewHolder>() {

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
        holder.itemView.setOnClickListener { onPostClick(post) }
    }

    override fun getItemCount(): Int = posts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}