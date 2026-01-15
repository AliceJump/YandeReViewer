package com.alicejump.yandeviewer.adapter

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.load
import com.alicejump.yandeviewer.PhotoViewActivity
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post
import java.io.File

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
                allowHardware(false)
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

            // Long press to drag
            imageView.setOnLongClickListener { view ->
                val context = view.context
                val imageLoader = context.imageLoader
                val diskCache = imageLoader.diskCache

                val snapshot = diskCache?.openSnapshot(post.file_url)
                if (snapshot == null) {
                    Toast.makeText(context, "Image not cached yet, please wait for it to load.", Toast.LENGTH_SHORT).show()
                    return@setOnLongClickListener true
                }

                // Copy the cached file to a temporary file with a proper extension
                val cacheFile = snapshot.data.toFile()
                val tempFile = File(context.cacheDir, "dragged_image.jpg")
                cacheFile.copyTo(tempFile, true)
                snapshot.close()

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)

                // The item to be dragged, with an explicit MIME type.
                val mimeTypes = arrayOf("image/jpeg")
                val dragItem = ClipData.Item(uri)
                val clipData = ClipData("Image from YandeReViewer", mimeTypes, dragItem)

                val dragShadowBuilder = View.DragShadowBuilder(view)

                // Add flags to grant URI permissions across apps
                val dragFlags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
                ViewCompat.startDragAndDrop(view, clipData, dragShadowBuilder, null, dragFlags)
                true
            }
        }
    }
}
