package com.example.ajjnweb

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ajjnweb.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupClickListeners()
        loadUrl("https://www.google.com")
    }

    private fun setupWebView() {
        with(binding.webView) {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    binding.urlEditText.setText(url)
                    updateNavigationButtons()
                }

                override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = 0
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress < 100) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.progressBar.progress = newProgress
                    } else {
                        binding.progressBar.visibility = View.GONE
                    }
                }

                override fun onReceivedTitle(view: android.webkit.WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    supportActionBar?.title = title ?: "AjjnWeb"
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { goBack() }
        binding.forwardButton.setOnClickListener { goForward() }
        binding.refreshButton.setOnClickListener { refresh() }
        binding.goButton.setOnClickListener { loadUrl() }
        binding.menuButton.setOnClickListener { showMenu() }

        binding.urlEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadUrl()
                true
            } else {
                false
            }
        }

        binding.backButton.setOnLongClickListener {
            Toast.makeText(this, "Назад", Toast.LENGTH_SHORT).show()
            true
        }

        binding.forwardButton.setOnLongClickListener {
            Toast.makeText(this, "Вперед", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun loadUrl(url: String? = null) {
        var inputText = url ?: binding.urlEditText.text.toString().trim()

        if (inputText.isEmpty()) {
            Toast.makeText(this, "Введите адрес", Toast.LENGTH_SHORT).show()
            return
        }

        if (!inputText.startsWith("http://") && !inputText.startsWith("https://") && !inputText.startsWith("file://")) {
            if (inputText.contains(".")) {
                inputText = "https://$inputText"
            } else {
                inputText = "https://www.google.com/search?q=${java.net.URLEncoder.encode(inputText, "UTF-8")}"
            }
        }

        binding.webView.loadUrl(inputText)
        hideKeyboard()
    }

    private fun goBack() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            Toast.makeText(this, "Нельзя вернуться назад", Toast.LENGTH_SHORT).show()
        }
        updateNavigationButtons()
    }

    private fun goForward() {
        if (binding.webView.canGoForward()) {
            binding.webView.goForward()
        } else {
            Toast.makeText(this, "Нельзя перейти вперед", Toast.LENGTH_SHORT).show()
        }
        updateNavigationButtons()
    }

    private fun refresh() {
        binding.webView.reload()
        Toast.makeText(this, "Обновление...", Toast.LENGTH_SHORT).show()
    }

    private fun updateNavigationButtons() {
        binding.backButton.isEnabled = binding.webView.canGoBack()
        binding.forwardButton.isEnabled = binding.webView.canGoForward()
        binding.backButton.alpha = if (binding.webView.canGoBack()) 1.0f else 0.5f
        binding.forwardButton.alpha = if (binding.webView.canGoForward()) 1.0f else 0.5f
    }

    private fun hideKeyboard() {
        binding.urlEditText.clearFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.urlEditText.windowToken, 0)
    }

    private fun showMenu() {
        val options = arrayOf("Поделиться страницей", "О программе", "Выход")

        AlertDialog.Builder(this)
            .setTitle("Меню")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sharePage()
                    1 -> showAbout()
                    2 -> finish()
                }
            }
            .show()
    }

    private fun sharePage() {
        val url = binding.webView.url ?: ""
        val title = binding.webView.title ?: ""

        if (url.isNotEmpty()) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$title\n$url")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Поделиться страницей"))
        } else {
            Toast.makeText(this, "Нет страницы для分享", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAbout() {
        val aboutMessage = "VShargin (C) 2025. vaspull9@gmail.com\nAjjnWeb v1.1.4\nПолнофункциональный браузер"
        AlertDialog.Builder(this)
            .setTitle("О программе")
            .setMessage(aboutMessage)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}