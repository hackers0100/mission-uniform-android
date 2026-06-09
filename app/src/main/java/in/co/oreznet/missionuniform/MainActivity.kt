package `in`.co.oreznet.missionuniform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import `in`.co.oreznet.missionuniform.ui.theme.MissionUniformTheme


class MainActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current

            var isConnected by remember { mutableStateOf(isNetworkAvailable(context)) }
            var hasWebError by remember { mutableStateOf(false) }
            var isLoading by remember { mutableStateOf(true) }
            var webView: WebView? = remember { null }

            // Handle the Android back button inside the web app
            BackHandler(enabled = true) {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                } else {
                    finish() // Closes the app if there is no web history
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isConnected || hasWebError) {
                    // Better UI stating: internet disconnected
                    MissionUniformErrorView {
                        isConnected = isNetworkAvailable(context)
                        if (isConnected) {
                            hasWebError = false
                            isLoading = true // Show splash while trying to reload
                            webView?.reload()
                        }}
                } else {
                    // Render the WebView in full screen
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest
                                    ): Boolean {
                                        if (request.url == null) return false

                                        // Retrieve the hit test result from the WebView to see how the link was opened
                                        val hitTestResult = view?.hitTestResult

                                        // Check if the link was meant to open in a new window/tab (target="_blank")
                                        if (hitTestResult?.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                                            hitTestResult?.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                                        ) {

                                            // If you want all standard clicked links to stay in-app except target="_blank",
                                            // WebView doesn't natively expose the target attribute here easily.
                                            // To force *external domains* or *new window intents* out:

                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                request.url.toString().toUri()
                                            )
                                            context.startActivity(intent)
                                            return true // Tells the app: "I handled it, don't open it inside the WebView"
                                        }

                                        return false // Let the WebView load standard links normally
                                    }
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // Dismiss Splash screen when page is fully built
                                        if (!hasWebError) {
                                            isLoading = false
                                        }
                                    }
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        hasWebError = true
                                        isLoading = false
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
                                            val transport =
                                                resultMsg?.obj as? WebView.WebViewTransport
                                            transport?.webView?.url
                                        }

                                        // Alternatively, get the target URL directly from the message data if available
                                        val data = resultMsg?.data
                                        val url =
                                            data?.getString("url") ?: view?.hitTestResult?.extra

                                        if (!url.isNullOrEmpty()) {
                                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
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

                AnimatedVisibility(
                    visible = isLoading && isConnected && !hasWebError,
                    exit = fadeOut()
                ) {
                    SplashView()
                }
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


@Composable
fun SplashView() {
    MissionUniformTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background), // Deep slate theme color
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )
                    ) {
                        append("Mission")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.surface,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )
                    ) {
                        append(" Uniform")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PRACTICE • DISCIPLINE • SELECTION",
                color = MaterialTheme.colorScheme.secondary, // Stealth gray motto text
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(48.dp))// Minimalist loader that aligns perfectly with the brand aesthetic
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MissionUniformErrorView(onTryAgainClick: () -> Unit = {}) {
    MissionUniformTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚡", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SIGNAL LOST",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Discipline requires connection. Please verify your network state to continue your training.",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onTryAgainClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text(
                    text = "Retry",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "MISSION UNIFORM",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
        }
    }
}

@Preview
@Composable
fun PreviewSplashScreen() {
    AnimatedVisibility(
        visible = true,
        exit = fadeOut(),
    ) {
        SplashView()
    }
}
@Preview
@Composable
fun PreviewErrorScreen() {
    MissionUniformErrorView()
}