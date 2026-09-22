package com.smey.autocaption

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
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

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                injectDownloadScript(webView)
            }
        }

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

                this@MainActivity.filePathCallback =
                    filePath

                val intent =
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "video/*"
                    }

                startActivityForResult(
                    intent,
                    100
                )

                return true
            }
        }

        webView.loadUrl(
            "https://khmer-auto-caption-vip1.streamlit.app/"
        )

        setContentView(webView)
    }

    private fun injectDownloadScript(
        webView: WebView
    ) {

        val script = """
            (function() {

                if (window.__smeyDownloadInstalled) {
                    return;
                }

                window.__smeyDownloadInstalled = true;

                document.addEventListener(
                    'click',
                    function(event) {

                        var link =
                            event.target.closest('a');

                        if (!link) {
                            return;
                        }

                        var href =
                            link.href || '';

                        var download =
                            link.getAttribute('download');

                        if (
                            !download &&
                            !href.startsWith('blob:') &&
                            !href.startsWith('data:')
                        ) {
                            return;
                        }

                        event.preventDefault();
                        event.stopPropagation();

                        fetch(href)
                            .then(function(response) {
                                return response.blob();
                            })
                            .then(function(blob) {

                                var reader =
                                    new FileReader();

                                reader.onloadend =
                                    function() {

                                        var result =
                                            reader.result;

                                        var base64 =
                                            result.split(',')[1];

                                        var name =
                                            download ||
                                            'smey_auto_caption.mp4';

                                        AndroidDownload
                                            .saveBase64File(
                                                name,
                                                base64,
                                                blob.type ||
                                                'video/mp4'
                                            );
                                    };

                                reader.readAsDataURL(blob);
                            })
                            .catch(function(error) {

                                console.log(
                                    'Download error:',
                                    error
                                );
                            });

                    },
                    true
                );

            })();
        """.trimIndent()

        webView.evaluateJavascript(
            script,
            null
        )
    }

    inner class DownloadBridge {

        @JavascriptInterface
        fun saveBase64File(
            fileName: String,
            base64: String,
            mimeType: String
        ) {

            try {

                val bytes =
                    Base64.decode(
                        base64,
                        Base64.DEFAULT
                    )

                val resolver =
                    contentResolver

                val values =
                    ContentValues().apply {

                        put(
                            MediaStore.Downloads.DISPLAY_NAME,
                            fileName
                        )

                        put(
                            MediaStore.Downloads.MIME_TYPE,
                            mimeType
                        )

                        if (Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.Q
                        ) {

                            put(
                                MediaStore.Downloads.RELATIVE_PATH,
                                Environment.DIRECTORY_DOWNLOADS
                            )
                        }
                    }

                val uri =
                    resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )

                if (uri != null) {

                    resolver.openOutputStream(
                        uri
                    )?.use { output ->

                        output.write(bytes)
                        output.flush()
                    }

                    runOnUiThread {

                        Toast.makeText(
                            this@MainActivity,
                            "✅ វីដេអូបានរក្សាទុកក្នុង Downloads",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    runOnUiThread {

                        Toast.makeText(
                            this@MainActivity,
                            "❌ មិនអាចរក្សាទុកវីដេអូបាន",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this@MainActivity,
                        "❌ Download មានបញ្ហា",
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

                    arrayOf(
                        data.data!!
                    )

                } else {

                    null
                }

            filePathCallback
                ?.onReceiveValue(result)

            filePathCallback = null
        }
    }

    override fun onDestroy() {

        filePathCallback
            ?.onReceiveValue(null)

        filePathCallback = null

        super.onDestroy()
    }
}
