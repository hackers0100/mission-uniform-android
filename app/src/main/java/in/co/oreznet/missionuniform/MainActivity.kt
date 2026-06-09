package `in`.co.oreznet.missionuniform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView


class MainActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            // State tracking if internet is connected and if there's a loading error
            var isConnected by remember { mutableStateOf(isNetworkAvailable(context)) }
            var hasWebError by remember { mutableStateOf(false) }
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

            // UI Conditional Check
            if (!isConnected || hasWebError) {
                // Better UI stating: internet disconnected
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connection Lost",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please check your internet settings.",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        // Check status again on click
                        isConnected = isNetworkAvailable(context)
                        if (isConnected) {
                            hasWebError = false
                            webView?.reload() // Refresh web page
                        }
                    }) {
                        Text("Try Again")
                    }
                }
            } else {
                // Render the WebView in full screen
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                                    if (request.url == null) return false

                                    // Retrieve the hit test result from the WebView to see how the link was opened
                                    val hitTestResult = view?.hitTestResult

                                    // Check if the link was meant to open in a new window/tab (target="_blank")
                                    if (hitTestResult?.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                                        hitTestResult?.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

                                        // If you want all standard clicked links to stay in-app except target="_blank",
                                        // WebView doesn't natively expose the target attribute here easily.
                                        // To force *external domains* or *new window intents* out:

                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))
                                        context.startActivity(intent)
                                        return true // Tells the app: "I handled it, don't open it inside the WebView"
                                    }

                                    return false // Let the WebView load standard links normally
                                }
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    hasWebError = true
                                }
                            }

                            // Enable essential settings for modern websites
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.supportMultipleWindows() // Allows the webview to recognize window creation requests

                            // Capture JavaScript window.open() triggers
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: android.os.Message?
                                ): Boolean {
                                    // Pull the URL out of the window creation request
                                    val href = view?.handler?.let {
                                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                                        transport?.webView?.url
                                    }

                                    // Alternatively, get the target URL directly from the message data if available
                                    val data = resultMsg?.data
                                    val url = data?.getString("url") ?: view?.hitTestResult?.extra

                                    if (!url.isNullOrEmpty()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                        return true
                                    }
                                    return false
                                }
                            }

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
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
