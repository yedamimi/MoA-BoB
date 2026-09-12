package com.eatda.app.ui.shop

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight

class ShopWebViewActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShopWebViewScreen(onClose = { finish() })
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ShopWebViewScreen(onClose: () -> Unit) {
    var progress by remember { mutableStateOf(0) }
    val isLoading = progress < 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F3F8)),
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2B6FED))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(
                "🛒 싱싱마켓",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF2EC57E))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("냉장고 연동", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // 로딩 프로그레스
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2EC57E),
                trackColor = Color(0xFF2B6FED).copy(alpha = 0.3f),
            )
        }

        // ✅ 핵심 수정: fillMaxSize() → fillMaxWidth() + weight(1f)
        // fillMaxSize()를 Column 안에서 쓰면 WebView가 높이를 올바르게 받지 못해 빈 화면이 됩니다.
        // weight(1f)로 해야 헤더 이후 남은 공간을 올바르게 채웁니다.
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled                = true
                        domStorageEnabled                = true
                        allowFileAccessFromFileURLs      = true
                        allowUniversalAccessFromFileURLs = true
                        mixedContentMode =
                            android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            // eatda://close → 앱으로 복귀
                            if (request?.url?.scheme == "eatda") {
                                onClose()   // finish()는 Composable 안에서 직접 호출 불가 → onClose 콜백 사용
                                return true
                            }
                            return false
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    loadUrl("file:///android_asset/shop.html")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)          
                .navigationBarsPadding(),
        )
    }
}
