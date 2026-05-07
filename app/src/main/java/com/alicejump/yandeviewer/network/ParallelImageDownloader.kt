package com.alicejump.yandeviewer.network

import android.content.Context
import android.util.Log
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

class ParallelImageFetcher(private val client: OkHttpClient, private val url: String, private val context: Context) : Fetcher {

    private companion object {
        const val TAG = "ParallelImageFetcher"
        const val PARALLEL_DOWNLOAD_MIN_BYTES = 1_500_000L
        const val MEDIUM_FILE_CHUNK_MULTIPLIER = 3
        const val MEDIUM_FILE_CHUNK_COUNT = 2
        const val LARGE_FILE_CHUNK_COUNT = 4
    }

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        try {
            val headRequest = Request.Builder().url(url).head().build()
            val headInfo = client.newCall(headRequest).execute().use { headResponse ->
                if (!headResponse.isSuccessful) return@use null
                val contentLength = headResponse.header("Content-Length")?.toLongOrNull()
                val acceptRanges = headResponse.header("Accept-Ranges")
                Pair(contentLength, acceptRanges)
            } ?: return@withContext null

            val contentLength = headInfo.first
            val acceptRanges = headInfo.second
            val chunkCount = resolveChunkCount(contentLength)

            if (chunkCount > 1 && contentLength != null && acceptRanges == "bytes") {
                val chunkSize = contentLength / chunkCount
                val chunks = (0 until chunkCount).map { i ->
                    async {
                        val start = i * chunkSize
                        val end = if (i == chunkCount - 1) contentLength - 1 else start + chunkSize - 1
                        val chunkRequest = Request.Builder().url(url).header("Range", "bytes=$start-$end").build()
                        client.newCall(chunkRequest).execute().use { chunkResponse ->
                            if (chunkResponse.isSuccessful) chunkResponse.body?.bytes() else null
                        }
                    }
                }

                val downloadedParts = chunks.map { it.await() ?: return@withContext null }
                val totalBytes = downloadedParts.sumOf { it.size }
                val downloadedBytes = ByteArray(totalBytes)
                var offset = 0
                downloadedParts.forEach { part ->
                    System.arraycopy(part, 0, downloadedBytes, offset, part.size)
                    offset += part.size
                }

                val buffer = Buffer().write(downloadedBytes)
                return@withContext SourceResult(source = ImageSource(buffer, context), mimeType = null, dataSource = DataSource.NETWORK)
            }

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body ?: return@withContext null
                    val buffer = Buffer().write(body.bytes())
                    return@withContext SourceResult(source = ImageSource(buffer, context), mimeType = null, dataSource = DataSource.NETWORK)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Image fetch failed for $url", e)
        }
        return@withContext null
    }

    private fun resolveChunkCount(contentLength: Long?): Int {
        if (contentLength == null || contentLength < PARALLEL_DOWNLOAD_MIN_BYTES) return 1
        return if (contentLength < PARALLEL_DOWNLOAD_MIN_BYTES * MEDIUM_FILE_CHUNK_MULTIPLIER) {
            MEDIUM_FILE_CHUNK_COUNT
        } else {
            LARGE_FILE_CHUNK_COUNT
        }
    }

    class Factory : Fetcher.Factory<String> {
        private val client by lazy { NetworkClient.client }

        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.startsWith("http://") || data.startsWith("https://")) {
                ParallelImageFetcher(client, data, options.context)
            } else {
                null
            }
        }
    }
}
