package com.alicejump.yandeviewer.adapter

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.alicejump.yandeviewer.PhotoViewActivity
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post

class ImagePagerAdapter(private val posts: List<Post>) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_pager, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.pagerImageView)

        fun bind(post: Post) {
            imageView.load(post.file_url) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_close_clear_cancel)
            }

            // Click to view full screen
            imageView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, PhotoViewActivity::class.java).apply {
                    putExtra("url", post.file_url)
                }
                context.startActivity(intent)
            }

            // Long press to save
            imageView.setOnLongClickListener {
                val context = itemView.context
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(Uri.parse(post.file_url))
                    .setTitle("Downloading Post ${post.id}")
                    .setDescription(post.tags)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "yande.re_${post.id}.jpg")
                downloadManager.enqueue(request)
                Toast.makeText(context, "Started downloading...", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }
}
