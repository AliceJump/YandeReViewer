package com.alicejump.yandeviewer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

class PostAdapter(
    private val onPostClick: (Post, Int, ImageView) -> Unit,
    private val onSelectionChange: (Int) -> Unit
) : PagingDataAdapter<Post, PostVH>(diff) {

    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<Post>()

    /** 返回已选图片 */
    fun getSelectedItems(): List<Post> = selectedItems.toList()

    /** 是否处于多选模式 */
    fun isSelectionActive(): Boolean = selectedItems.isNotEmpty()

    /** 清空选择（返回键调用） */
    fun clearSelection() {
        val oldSelected = selectedItems.toList()
        selectedItems.clear()
        isSelectionMode = false
        // 刷新被选中的 item，让勾选框消失
        oldSelected.forEach { post ->
            val pos = snapshot().items.indexOf(post)
            if (pos != -1) notifyItemChanged(pos)
        }
        onSelectionChange(0)
    }

    /** 进入多选模式（长按调用） */
    private fun enterSelectionMode(post: Post, position: Int) {
        if (!isSelectionMode) {
            isSelectionMode = true
        }
        selectedItems.add(post)
        notifyItemChanged(position)
        onSelectionChange(selectedItems.size)
    }

    /** 切换某个 item 的选中状态 */
    private fun toggleSelection(post: Post, position: Int) {
        if (selectedItems.contains(post)) selectedItems.remove(post)
        else selectedItems.add(post)
        notifyItemChanged(position)
        onSelectionChange(selectedItems.size)
    }

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        val post = getItem(position) ?: return

        // 显示勾选框
        holder.bind(post, isSelectionMode, selectedItems.contains(post))

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(post, position)
            } else {
                onPostClick(post, position, holder.getImageView())
            }
        }

        holder.itemView.setOnLongClickListener {
            enterSelectionMode(post, position)
            true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostVH {
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
