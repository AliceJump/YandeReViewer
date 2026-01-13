package com.alicejump.yandeviewer.adapter

import android.app.Activity
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
import androidx.core.app.ActivityOptionsCompat
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
            val transitionName = "image_transition_${post.id}"
            imageView.transitionName = transitionName

            // Temporarily disable the click listener while the high-res image is loading.
            imageView.isClickable = false

            imageView.load(post.file_url) {
                placeholderMemoryCacheKey(post.preview_url)
                error(android.R.drawable.ic_menu_close_clear_cancel)
                crossfade(true)
                listener(
                    onSuccess = { _, _ ->
                        // Re-enable clicks once the high-res image is successfully loaded.
                        imageView.isClickable = true
                    },
                    onError = { _, _ ->
                        // Also re-enable on error, so the user can at least interact with it.
                        imageView.isClickable = true
                    }
                )
            }

            // Click to view full screen
            imageView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, PhotoViewActivity::class.java).apply {
                    putExtra("file_url", post.file_url)
                    putExtra("preview_url", post.preview_url)
                    putExtra("transition_name", transitionName)
                }

                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity, imageView, transitionName)
                context.startActivity(intent, options.toBundle())
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
