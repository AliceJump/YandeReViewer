package com.alicejump.yandeviewer.network

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {
    private val cpuCoreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    private val maxRequests = (cpuCoreCount * 3).coerceIn(8, 24)
    private val maxRequestsPerHost = (maxRequests / 2).coerceIn(4, 8)

    private val dispatcher = Dispatcher().apply {
        this.maxRequests = this@NetworkClient.maxRequests
        this.maxRequestsPerHost = this@NetworkClient.maxRequestsPerHost
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}
