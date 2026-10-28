package com.eatda.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

// PC 스캔 스크립트(scan.py)가 보내는 MJPEG 스트림을 실시간으로 표시
//
// 연결 전이거나 끊기면 placeholder를 보여주고, 1초 뒤 자동으로 다시 연결
@Composable
fun MjpegView(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable BoxScope.() -> Unit,
) {
    var frame by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            while (isActive) {
                try {
                    readMjpeg(url) { bitmap -> frame = bitmap }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 연결 실패 또는 끊김 → 대기 화면으로 돌아가고 재시도
                }
                frame = null
                delay(1_000)
            }
        }
    }

    Box(modifier = modifier) {
        val current = frame
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = "웹캠 실시간 화면",
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            placeholder()
        }
    }
}

// multipart/x-mixed-replace 스트림에서 Content-Length 만큼씩 JPEG를 읽어 디코딩
private suspend fun readMjpeg(url: String, onFrame: (ImageBitmap) -> Unit) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 3_000
    conn.readTimeout = 5_000   // 카메라가 꺼져 프레임이 안 오면 끊고 대기 화면으로

    // 화면을 닫으면 블로킹 중인 read를 끊기 위해 연결을 닫음
    val handle = coroutineContext.job.invokeOnCompletion { conn.disconnect() }
    try {
        val input = DataInputStream(BufferedInputStream(conn.inputStream))
        var contentLength = -1

        while (coroutineContext.isActive) {
            val line = input.readAsciiLine() ?: break

            when {
                line.startsWith("Content-Length:", ignoreCase = true) ->
                    contentLength = line.substringAfter(':').trim().toIntOrNull() ?: -1

                // 파트 헤더가 끝나면 JPEG 본문
                line.isEmpty() && contentLength > 0 -> {
                    val bytes = ByteArray(contentLength)
                    input.readFully(bytes)
                    contentLength = -1
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?.let { onFrame(it.asImageBitmap()) }
                }
            }
        }
    } finally {
        handle.dispose()
        conn.disconnect()
    }
}

private fun InputStream.readAsciiLine(): String? {
    val sb = StringBuilder()
    while (true) {
        val c = read()
        if (c == -1) return if (sb.isEmpty()) null else sb.toString()
        if (c == '\n'.code) return sb.toString().trimEnd('\r')
        sb.append(c.toChar())
    }
}
