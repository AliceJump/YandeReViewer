package com.alicejump.yandeviewer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.github.chrisbanes.photoview.PhotoView

class PhotoViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)

        val photoView = findViewById<PhotoView>(R.id.photo_view)
        val fileUrl = intent.getStringExtra("file_url")
        val previewUrl = intent.getStringExtra("preview_url")

        if (fileUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Image URL not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        photoView.load(fileUrl) {
            // Use the preview image from DetailActivity's cache as a placeholder
            if (!previewUrl.isNullOrEmpty()) {
                placeholderMemoryCacheKey(previewUrl)
            }
            error(android.R.drawable.ic_menu_close_clear_cancel)
        }

        // Click to exit
        photoView.setOnPhotoTapListener { _, _, _ ->
            finish()
        }
    }
}
