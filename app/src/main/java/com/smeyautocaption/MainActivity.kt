package com.smeyautocaption

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(
                webView: WebView?,
                filePath: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                this@MainActivity.filePathCallback?.onReceiveValue(null)

                this@MainActivity.filePathCallback = filePath

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "video/*"
                }

                startActivityForResult(intent, 100)

                return true
            }
        }

        webView.loadUrl(
            "https://smey-auto-caption-vip.streamlit.app/"
        )

        setContentView(webView)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 100) {

            val result: Array<Uri>? =
                if (
                    resultCode == RESULT_OK &&
                    data?.data != null
                ) {
                    arrayOf(data.data!!)
                } else {
                    null
                }

            filePathCallback?.onReceiveValue(result)

            filePathCallback = null
        }
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null

        super.onDestroy()
    }
}
