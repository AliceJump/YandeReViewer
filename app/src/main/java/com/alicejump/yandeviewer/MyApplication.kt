package com.alicejump.yandeviewer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.alicejump.yandeviewer.network.ParallelImageFetcher

class MyApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(ParallelImageFetcher.Factory())
            }
            .build()
    }
}
