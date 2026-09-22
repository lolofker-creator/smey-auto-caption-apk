package com.smey.autocaption

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.util.Base64

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

        webView.addJavascriptInterface(
            DownloadBridge(),
            "AndroidDownload"
        )

        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(
                webView: WebView?,
                filePath: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                this@MainActivity.filePathCallback
                    ?.onReceiveValue(null)

                this@MainActivity.filePathCallback = filePath

                val intent =
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "video/*"
                    }

                startActivityForResult(intent, 100)

                return true
            }
        }

        webView.loadUrl(
            "https://khmer-auto-caption-vip1.streamlit.app/"
        )

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                val script = """
                    javascript:(function() {
                        if (window.smeyDownloadInstalled) return;
                        window.smeyDownloadInstalled = true;

                        document.addEventListener('click', function(e) {
                            var el = e.target;

                            while (el && el.tagName !== 'A') {
                                el = el.parentElement;
                            }

                            if (!el) return;

                            var href = el.href || '';

                            if (
                                href.startsWith('blob:') ||
                                href.startsWith('data:')
                            ) {
                                e.preventDefault();

                                fetch(href)
                                    .then(function(r) {
                                        return r.blob();
                                    })
                                    .then(function(blob) {
                                        var reader = new FileReader();

                                        reader.onloadend = function() {
                                            var result =
                                                reader.result || '';

                                            var comma =
                                                result.indexOf(',');

                                            var base64 =
                                                comma >= 0
                                                ? result.substring(comma + 1)
                                                : result;

                                            AndroidDownload.saveFile(
                                                base64,
                                                'smey_auto_caption.mp4'
                                            );
                                        };

                                        reader.readAsDataURL(blob);
                                    });

                                return false;
                            }
                        }, true);
                    })();
                """.trimIndent()

                view?.evaluateJavascript(
                    script,
                    null
                )
            }
        }

        setContentView(webView)
    }

    inner class DownloadBridge {

        @JavascriptInterface
        fun saveFile(
            base64: String,
            fileName: String
        ) {
            try {

                val data =
                    Base64.decode(
                        base64,
                        Base64.DEFAULT
                    )

                val values =
                    ContentValues().apply {
                        put(
                            MediaStore.Downloads.DISPLAY_NAME,
                            fileName
                        )

                        put(
                            MediaStore.Downloads.MIME_TYPE,
                            "video/mp4"
                        )

                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                        )
                    }

                val resolver = contentResolver

                val uri =
                    resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )

                if (uri != null) {

                    resolver.openOutputStream(uri).use { output ->
                        output?.write(data)
                    }

                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "✅ វីដេអូរក្សាទុកក្នុង Downloads ហើយ",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "❌ ទាញយកមិនបាន",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
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
