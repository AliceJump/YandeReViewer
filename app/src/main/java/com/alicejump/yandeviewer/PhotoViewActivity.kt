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
        val imageUrl = intent.getStringExtra("url")

        if (imageUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Image URL not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        photoView.load(imageUrl)

        // Click to exit
        photoView.setOnPhotoTapListener { _, _, _ ->
            finish()
        }
    }
}
