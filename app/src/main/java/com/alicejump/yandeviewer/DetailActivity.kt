package com.alicejump.yandeviewer

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var imageUrl: String? = null

    companion object {
        const val PERMISSION_REQUEST_WRITE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        imageView = findViewById(R.id.detailImage)
        imageUrl = intent.getStringExtra("url")

        if (imageUrl == null) {
            Toast.makeText(this, "没有图片 URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 加载图片
        Glide.with(this)
            .load(imageUrl)
            .into(imageView)

        // 长按下载
        imageView.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("下载图片")
                .setMessage("确定要下载这张图片吗？")
                .setPositiveButton("下载") { _, _ -> checkPermissionAndDownload() }
                .setNegativeButton("取消", null)
                .show()
            true
        }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 及以上不需要存储权限
            downloadImage()
        } else {
            // Android 9 及以下需要 WRITE_EXTERNAL_STORAGE 权限
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_WRITE
                )
            } else {
                downloadImage()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_WRITE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            downloadImage()
        } else {
            Toast.makeText(this, "没有权限，无法下载图片", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadImage() {
        val url = imageUrl ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = Glide.with(this@DetailActivity)
                    .asBitmap()
                    .load(url)
                    .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get()

                saveImageToGallery(bitmap, "yande_${System.currentTimeMillis()}.jpg")

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetailActivity, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetailActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
