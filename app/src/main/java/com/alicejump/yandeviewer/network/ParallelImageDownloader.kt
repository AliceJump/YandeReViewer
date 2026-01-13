package com.alicejump.yandeviewer.network

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.lang.Exception

class ParallelImageFetcher(private val client: OkHttpClient, private val url: String, private val context: Context) : Fetcher {

    private val numChunks = 4

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        try {
            // 1. Send HEAD request to get content length
            val headRequest = Request.Builder().url(url).head().build()
            val headResponse = client.newCall(headRequest).execute()

            if (!headResponse.isSuccessful) return@withContext null

            val contentLength = headResponse.header("Content-Length")?.toLongOrNull()
            val acceptRanges = headResponse.header("Accept-Ranges")

            // 2. If server supports range requests and we have content length, proceed with parallel download
            if (contentLength != null && contentLength > 0 && acceptRanges == "bytes") {
                val chunkSize = contentLength / numChunks
                val downloadedBytes = ByteArray(contentLength.toInt())

                val chunks = (0 until numChunks).map { i ->
                    async {
                        val start = i * chunkSize
                        val end = if (i == numChunks - 1) contentLength - 1 else start + chunkSize - 1
                        val chunkRequest = Request.Builder()
                            .url(url)
                            .header("Range", "bytes=$start-$end")
                            .build()
                        val chunkResponse = client.newCall(chunkRequest).execute()
                        if (chunkResponse.isSuccessful) {
                            chunkResponse.body?.bytes()
                        } else {
                            null
                        }
                    }
                }

                var currentPosition = 0
                chunks.forEach { deferred ->
                    val chunkBytes = deferred.await()
                    if (chunkBytes != null) {
                        System.arraycopy(chunkBytes, 0, downloadedBytes, currentPosition, chunkBytes.size)
                        currentPosition += chunkBytes.size
                    } else {
                        return@withContext null
                    }
                }

                val buffer = Buffer().write(downloadedBytes)
                return@withContext SourceResult(source = ImageSource(buffer, context), mimeType = null, dataSource = DataSource.NETWORK)
            }

            // 5. Fallback to a normal, sequential download
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val buffer = Buffer().write(response.body!!.bytes())
                return@withContext SourceResult(source = ImageSource(buffer, context), mimeType = null, dataSource = DataSource.NETWORK)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    class Factory : Fetcher.Factory<String> {
        private val client by lazy { OkHttpClient() }

        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.startsWith("http://") || data.startsWith("https://")) {
                ParallelImageFetcher(client, data, options.context)
            } else {
                null
            }
        }
    }
}
