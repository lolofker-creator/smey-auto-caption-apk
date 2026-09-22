package com.smey.autocaption

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.DownloadListener

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

        webView.setDownloadListener(
            DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->

                val request = DownloadManager.Request(Uri.parse(url))

                request.setMimeType(mimeType)

                val cookies = CookieManager
                    .getInstance()
                    .getCookie(url)

                if (cookies != null) {
                    request.addRequestHeader(
                        "Cookie",
                        cookies
                    )
                }

                request.addRequestHeader(
                    "User-Agent",
                    userAgent
                )

                request.setTitle(
                    "Smey Auto Caption"
                )

                request.setDescription(
                    "កំពុងទាញយកវីដេអូ..."
                )

                request.setNotificationVisibility(
                    DownloadManager.Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "smey_auto_caption.mp4"
                )

                val manager =
                    getSystemService(
                        Context.DOWNLOAD_SERVICE
                    ) as DownloadManager

                manager.enqueue(request)
            }
        )

        webView.loadUrl(
            "https://khmer-auto-caption-vip1.streamlit.app/"
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
