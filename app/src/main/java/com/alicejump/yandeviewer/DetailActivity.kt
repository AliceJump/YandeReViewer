package com.alicejump.yandeviewer

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.load
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.alicejump.yandeviewer.MainActivity.Companion.NEW_SEARCH_TAG
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var tagsContainer: ChipGroup
    private var imageUrl: String? = null

    companion object {
        private const val PERMISSION_REQUEST_WRITE_STORAGE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        imageView = findViewById(R.id.detailImage)
        tagsContainer = findViewById(R.id.tagsContainer)

        imageUrl = intent.getStringExtra("url")
        val previewUrl = intent.getStringExtra("preview_url")
        val tags = intent.getStringExtra("tags")

        if (imageUrl == null) {
            Toast.makeText(this, "Image URL not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load image using Coil with placeholder from memory cache
        imageView.load(imageUrl) {
            if (previewUrl != null) {
                placeholderMemoryCacheKey(MemoryCache.Key(previewUrl))
            }
            error(android.R.drawable.ic_menu_close_clear_cancel)
        }

        // Click to view full screen
        imageView.setOnClickListener {
            val intent = Intent(this, PhotoViewActivity::class.java).apply {
                putExtra("url", imageUrl)
            }
            startActivity(intent)
        }

        // Setup tags
        setupTags(tags)

        // Long press to download
        imageView.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Download Image")
                .setMessage("Do you want to download this image?")
                .setPositiveButton("Download") { _, _ -> checkPermissionAndDownload() }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    private fun setupTags(tags: String?) {
        if (tags.isNullOrBlank()) {
            return
        }

        val tagList = tags.split(" ").filter { !it.startsWith("rating:") }

        tagList.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isClickable = true
                isFocusable = true
            }

            chip.setOnClickListener {
                val intent = Intent(this@DetailActivity, MainActivity::class.java).apply {
                    putExtra(NEW_SEARCH_TAG, tag)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }

            tagsContainer.addView(chip)
        }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadImage()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_WRITE_STORAGE)
            } else {
                downloadImage()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_WRITE_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadImage()
        } else {
            Toast.makeText(this, "Permission denied, cannot download image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadImage() {
        val urlToDownload = imageUrl ?: return
        lifecycleScope.launch {
            try {
                val request = ImageRequest.Builder(this@DetailActivity)
                    .data(urlToDownload)
                    .allowHardware(false) // Important for saving bitmap
                    .build()
                val result = (this@DetailActivity.imageLoader.execute(request) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap

                saveImageToGallery(bitmap, "yande_${System.currentTimeMillis()}.jpg")
                Toast.makeText(this@DetailActivity, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@DetailActivity, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap, filename: String) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/YandeViewer")
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let {
            resolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        }
    }
}
