package `in`.co.oreznet.missionuniform

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Store a reference to the WebView instance
            var webView: WebView? = remember { null }

            // Handle the Android back button inside the web app
            BackHandler(enabled = true) {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                } else {
                    finish() // Closes the app if there is no web history
                }
            }

            // Render the WebView in full screen
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()

                        // Enable essential settings for modern websites
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        loadUrl("https://mission-uniform.oreznet.co.in/")
                        webView = this
                    }
                },
                update = {
                    webView = it
                }
            )
        }
    }
}
