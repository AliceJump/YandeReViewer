package com.alicejump.yandeviewer

import android.os.Bundle
import android.transition.Transition
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import coil.load
import com.github.chrisbanes.photoview.PhotoView

class PhotoViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)
        postponeEnterTransition()

        val photoView = findViewById<PhotoView>(R.id.photo_view)
        val fileUrl = intent.getStringExtra("file_url")
        val previewUrl = intent.getStringExtra("preview_url")
        val transitionName = intent.getStringExtra("transition_name")

        if (fileUrl.isNullOrEmpty()) {
            Toast.makeText(this, R.string.image_url_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ViewCompat.setTransitionName(photoView, transitionName)

        // Load the preview image first.
        photoView.load(previewUrl) {
            error(android.R.drawable.ic_menu_close_clear_cancel)
            listener(onSuccess = { _, _ -> startPostponedEnterTransition() })
        }

        window.sharedElementEnterTransition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition) {
                // The transition has ended. Now, load the high-resolution image.
                photoView.load(fileUrl) {
                    placeholderMemoryCacheKey(previewUrl) // Keep showing the preview while loading
                    error(android.R.drawable.ic_menu_close_clear_cancel)
                    crossfade(true)
                }
                // Clean up the listener
                transition.removeListener(this)
            }

            override fun onTransitionStart(transition: Transition) {}
            override fun onTransitionCancel(transition: Transition) {
                transition.removeListener(this)
            }
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionResume(transition: Transition) {}
        })

        // Click to exit
        photoView.setOnPhotoTapListener { _, _, _ ->
            supportFinishAfterTransition()
        }
    }
}
