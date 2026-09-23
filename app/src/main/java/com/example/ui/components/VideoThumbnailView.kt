package com.example.ui.components

import android.content.ContentUris
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.domain.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val thumbnailMemoryCache = LruCache<String, Bitmap>(64)

@Composable
fun VideoThumbnailView(
    video: Video,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var localBitmap by remember(video.id, video.uri) {
        mutableStateOf(thumbnailMemoryCache.get(video.id))
    }

    LaunchedEffect(video.id, video.uri) {
        if (localBitmap == null && !video.isOnline) {
            withContext(Dispatchers.IO) {
                try {
                    val parsedUri = Uri.parse(video.uri)
                    val bmp: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            context.contentResolver.loadThumbnail(parsedUri, Size(512, 384), null)
                        } catch (e: Exception) {
                            null
                        }
                    } else null

                    val finalBmp = bmp ?: try {
                        val retriever = MediaMetadataRetriever()
                        if (video.path.isNotBlank()) {
                            retriever.setDataSource(video.path)
                        } else {
                            retriever.setDataSource(context, parsedUri)
                        }
                        val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.frameAtTime
                        retriever.release()
                        frame
                    } catch (e: Exception) {
                        null
                    }

                    if (finalBmp != null) {
                        thumbnailMemoryCache.put(video.id, finalBmp)
                        localBitmap = finalBmp
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(Color(0xFF0F172A), Color(0xFF1E293B))
            )
        )
    ) {
        if (localBitmap != null) {
            Image(
                bitmap = localBitmap!!.asImageBitmap(),
                contentDescription = video.title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.uri)
                    .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                    .videoFrameMillis(1500)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
