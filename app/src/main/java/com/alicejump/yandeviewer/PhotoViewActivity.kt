package com.alicejump.yandeviewer

import android.os.Bundle
import android.transition.Transition
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import coil.load
import com.github.chrisbanes.photoview.PhotoView

// 查看单张图片的 Activity，支持共享元素过渡和高分辨率图片加载
class PhotoViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)

        // 延迟共享元素过渡，等图片加载完成再动画
        postponeEnterTransition()

        val photoView = findViewById<PhotoView>(R.id.photo_view)
        val fileUrl = intent.getStringExtra("file_url")       // 高分辨率图片 URL
        val previewUrl = intent.getStringExtra("preview_url") // 低分辨率预览 URL
        val transitionName = intent.getStringExtra("transition_name") // 共享元素名称

        // URL 检查
        if (fileUrl.isNullOrEmpty()) {
            Toast.makeText(this, R.string.image_url_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置共享元素名称
        ViewCompat.setTransitionName(photoView, transitionName)

        // ===== Step 1: 先加载预览图 =====
        photoView.load(previewUrl) {
            error(android.R.drawable.ic_menu_close_clear_cancel) // 加载失败显示默认图
            listener(onSuccess = { _, _ ->
                // 预览图加载成功后开始过渡动画
                startPostponedEnterTransition()
            })
        }

        // ===== Step 2: 监听共享元素过渡结束事件，加载高分辨率图片 =====
        window.sharedElementEnterTransition.addListener(object : Transition.TransitionListener {

            override fun onTransitionEnd(transition: Transition) {
                // 动画结束后加载高清图
                photoView.load(fileUrl) {
                    placeholderMemoryCacheKey(previewUrl) // 保持显示预览图直到高清图加载完成
                    error(android.R.drawable.ic_menu_close_clear_cancel)
                    crossfade(true) // 淡入效果
                }
                // 移除监听器，避免重复触发
                transition.removeListener(this)
            }

            override fun onTransitionStart(transition: Transition) {}
            override fun onTransitionCancel(transition: Transition) {
                transition.removeListener(this)
            }
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionResume(transition: Transition) {}
        })

        // ===== Step 3: 点击图片退出页面 =====
        photoView.setOnPhotoTapListener { _, _, _ ->
            // 使用共享元素动画返回
            supportFinishAfterTransition()
        }
    }
}

