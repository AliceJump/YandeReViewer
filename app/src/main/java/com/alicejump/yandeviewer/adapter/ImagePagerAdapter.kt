package com.alicejump.yandeviewer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.load
import com.alicejump.yandeviewer.PhotoViewActivity
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post
import java.io.File
import kotlin.math.abs

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

        @SuppressLint("ClickableViewAccessibility")
        fun bind(post: Post) {
            val transitionName = "image_transition_${post.id}"
            imageView.transitionName = transitionName

            imageView.load(post.file_url) {
                allowHardware(false)
                placeholderMemoryCacheKey(post.preview_url)
                error(android.R.drawable.ic_menu_close_clear_cancel)
                crossfade(true)
            }

            val handler = Handler(Looper.getMainLooper())
            var isDragging = false
            var downTime = 0L
            var startX = 0f
            var startY = 0f
            val dragTriggerDelayMillis = 1000L // 1 second

            val dragRunnable = Runnable {
                isDragging = true
                startDrag(imageView, post)
            }

            imageView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downTime = System.currentTimeMillis()
                        startX = event.x
                        startY = event.y
                        isDragging = false
                        handler.postDelayed(dragRunnable, dragTriggerDelayMillis)
                        true // Must be true to receive subsequent events
                    }

                    MotionEvent.ACTION_UP -> {
                        handler.removeCallbacks(dragRunnable)
                        if (!isDragging) {
                            val pressDuration = System.currentTimeMillis() - downTime
                            if (pressDuration < ViewConfiguration.getTapTimeout()) {
                                // CLICK
                                val context = itemView.context
                                val intent = Intent(context, PhotoViewActivity::class.java).apply {
                                    putExtra("file_url", post.file_url)
                                    putExtra("preview_url", post.preview_url)
                                    putExtra("transition_name", transitionName)
                                }
                                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity, imageView, transitionName)
                                context.startActivity(intent, options.toBundle())
                            } else {
                                // DOWNLOAD (Short Long-Press)
                                downloadImage(view.context, post)
                            }
                        }
                        isDragging = false // IMPORTANT: Reset state
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val slop = ViewConfiguration.get(view.context).scaledTouchSlop
                        if (abs(event.x - startX) > slop || abs(event.y - startY) > slop) {
                            handler.removeCallbacks(dragRunnable)
                        }
                        false // Allow ViewPager2 to scroll
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(dragRunnable)
                        isDragging = false // IMPORTANT: Reset state
                        true
                    }

                    else -> false
                }
            }
        }

        private fun downloadImage(context: Context, post: Post) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(post.file_url.toUri())
                .setTitle("Downloading Post ${post.id}")
                .setDescription(post.tags)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "yande.re_${post.id}.jpg")
            downloadManager.enqueue(request)
            Toast.makeText(context, R.string.started_downloading, Toast.LENGTH_SHORT).show()
        }

        @OptIn(ExperimentalCoilApi::class)
        private fun startDrag(view: View, post: Post) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val context = view.context
            val imageLoader = context.imageLoader
            val diskCache = imageLoader.diskCache

            val snapshot = diskCache?.openSnapshot(post.file_url) ?: run {
                Toast.makeText(context, R.string.image_not_cached, Toast.LENGTH_SHORT).show()
                return
            }

            val cacheFile = snapshot.data.toFile()
            val tempFile = File(context.cacheDir, "dragged_image.jpg")
            cacheFile.copyTo(tempFile, true)
            snapshot.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)

            val mimeTypes = arrayOf("image/jpeg")
            val dragItem = ClipData.Item(uri)
            val clipData = ClipData("Image from YandeReViewer", mimeTypes, dragItem)

            val dragShadowBuilder = View.DragShadowBuilder(view)
            val dragFlags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            ViewCompat.startDragAndDrop(view, clipData, dragShadowBuilder, null, dragFlags)
        }
    }
}
