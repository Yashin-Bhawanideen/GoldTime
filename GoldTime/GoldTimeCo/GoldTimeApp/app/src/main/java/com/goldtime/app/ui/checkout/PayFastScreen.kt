package com.goldtime.app.ui.checkout

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.goldtime.app.data.PayFastForm
import com.goldtime.app.data.PaymentNavigation
import com.goldtime.app.ui.theme.GoldColors
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PayFastScreen(form: PayFastForm, alreadyOpened: Boolean, onOpened: () -> Unit, onClose: () -> Unit) {
    val startPayment = remember { !alreadyOpened }
    var closed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val close = { if (!closed) { closed = true; onClose() } }
    BackHandler(onBack = close)

    //checks the saved order after recreation instead of posting the payment form again
    if (!startPayment) {
        LaunchedEffect(Unit) { close() }
        return
    }

    Column(Modifier.fillMaxSize().background(GoldColors.Background).systemBarsPadding()) {
        Text("PayFast sandbox — no real money", color = GoldColors.Gold, modifier = Modifier.padding(12.dp))
        TextButton(onClick = close) { Text("Close and check order") }
        error?.let { Text(it, color = GoldColors.Error, modifier = Modifier.padding(12.dp)) }
        AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { context ->
            WebView(context).apply {
                //Payfast needs JavaScript; local files, mixed content and native bridges are not used
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.setSupportMultipleWindows(false)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        return when (form.navigation(request.url.toString())) {
                            PaymentNavigation.RETURN, PaymentNavigation.CANCEL -> { close(); true }
                            PaymentNavigation.BLOCK -> { error = "This payment link could not be opened."; true }
                            PaymentNavigation.PAYFAST -> false
                        }
                    }

                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        if (request.isForMainFrame && form.navigation(request.url.toString()) == PaymentNavigation.BLOCK)
                            return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(),
                                ByteArrayInputStream("Payment navigation blocked.".toByteArray()))
                        return null
                    }

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        when (form.navigation(url)) {
                            PaymentNavigation.RETURN, PaymentNavigation.CANCEL -> { view.stopLoading(); close() }
                            PaymentNavigation.BLOCK -> { view.stopLoading(); error = "This payment link could not be opened." }
                            else -> Unit
                        }
                    }

                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, sslError: SslError) {
                        handler.cancel()
                        error = "A secure connection could not be verified. Close this page and check your order."
                    }

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                        if (request.isForMainFrame) error = "The payment page could not load. Close it and check your order."
                    }

                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                        if (request.isForMainFrame) error = "The payment service returned an error. Close it and check your order."
                    }
                }
                onOpened()
                //posts the signed fields directly to Payfast without exposing a Firebase token (Android Developers, n.d.)
                postUrl(form.actionUrl, form.postData())
            }
        }, onReset = null, onRelease = { view -> view.stopLoading(); view.destroy() })
    }
}

/* REFERENCE LIST
Android Developers. n.d. WebView: postUrl. [Online]. Available at:
https://developer.android.com/reference/android/webkit/WebView [Accessed 1 October 2026].
Android Developers. n.d. Using Views in Compose. [Online]. Available at:
https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose
[Accessed 1 October 2026].
Android Developers. n.d. Webviews - Unsafe URI Loading. [Online]. Available at:
https://developer.android.com/privacy-and-security/risks/unsafe-uri-loading [Accessed 1 October 2026].
*/
