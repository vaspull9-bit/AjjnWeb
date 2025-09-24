package com.example.ajjnweb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.ajjnweb.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val tabs = mutableListOf<WebView>()
    private var currentTabIndex = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setupWebView()
        setupClickListeners()
        loadHomePage()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.progress = 0
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                binding.urlEditText.setText(url)
                updateNavigationButtons()
                saveToHistory(url ?: "", view?.title ?: "")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("http")) {
                    view?.loadUrl(url)
                }
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = newProgress
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                supportActionBar?.title = title ?: "AjjnWeb"
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                val newWebView = WebView(this@MainActivity)
                setupWebViewSettings(newWebView)
                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }

        tabs.add(webView)
    }

    private fun setupWebViewSettings(webView: WebView) {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { goBack() }
        binding.forwardButton.setOnClickListener { goForward() }
        binding.refreshButton.setOnClickListener { refresh() }
        binding.goButton.setOnClickListener { loadUrl() }
        binding.menuButton.setOnClickListener { showBrowserMenu() }

        binding.urlEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadUrl()
                true
            } else {
                false
            }
        }
    }

    private fun loadUrl(url: String? = null) {
        var inputText = url ?: binding.urlEditText.text.toString().trim()

        if (inputText.isEmpty()) return

        if (!inputText.startsWith("http://") && !inputText.startsWith("https://") && !inputText.startsWith("file://")) {
            inputText = if (inputText.contains(".")) {
                "https://$inputText"
            } else {
                "https://www.google.com/search?q=${java.net.URLEncoder.encode(inputText, "UTF-8")}"
            }
        }

        currentWebView().loadUrl(inputText)
        hideKeyboard()
    }

    private fun goBack() {
        if (currentWebView().canGoBack()) {
            currentWebView().goBack()
        }
        updateNavigationButtons()
    }

    private fun goForward() {
        if (currentWebView().canGoForward()) {
            currentWebView().goForward()
        }
        updateNavigationButtons()
    }

    private fun refresh() {
        currentWebView().reload()
    }

    private fun updateNavigationButtons() {
        binding.backButton.isEnabled = currentWebView().canGoBack()
        binding.forwardButton.isEnabled = currentWebView().canGoForward()
    }

    private fun hideKeyboard() {
        binding.urlEditText.clearFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.urlEditText.windowToken, 0)
    }

    private fun showBrowserMenu() {
        val menuItems = arrayOf(
            "Новая вкладка",
            "Новое окно",
            "История",
            "Закладки",
            "Скачать страницу",
            "Поделиться",
            "Найти на странице",
            "Настройки",
            "Печать",
            "О программе"
        )

        AlertDialog.Builder(this)
            .setTitle("Меню браузера")
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> newTab()
                    1 -> newWindow()
                    2 -> showHistory()
                    3 -> showBookmarks()
                    4 -> downloadPage()
                    5 -> sharePage()
                    6 -> findOnPage()
                    7 -> showSettings()
                    8 -> printPage()
                    9 -> showAbout()
                }
            }
            .show()
    }

    private fun newTab() {
        val newWebView = WebView(this)
        setupWebViewSettings(newWebView)

        // Создаем систему вкладок (упрощенная версия)
        Toast.makeText(this, "Новая вкладка", Toast.LENGTH_SHORT).show()
        loadUrl("https://www.google.com")
    }

    private fun newWindow() {
        Toast.makeText(this, "Новое окно", Toast.LENGTH_SHORT).show()
    }

    private fun showHistory() {
        val history = getHistory()
        val urls = history.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("История")
            .setItems(urls.takeLast(10).toTypedArray()) { _, which ->
                loadUrl(history[which].first)
            }
            .setPositiveButton("Очистить историю") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showBookmarks() {
        val bookmarks = getBookmarks()
        val titles = bookmarks.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Закладки")
            .setItems(if (titles.isEmpty()) arrayOf("Нет закладок") else titles) { _, which ->
                if (bookmarks.isNotEmpty()) {
                    loadUrl(bookmarks[which].second)
                }
            }
            .setPositiveButton("Добавить текущую") { _, _ ->
                addBookmark()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun downloadPage() {
        Toast.makeText(this, "Скачивание страницы", Toast.LENGTH_SHORT).show()
    }

    private fun sharePage() {
        val url = currentWebView().url ?: ""
        val title = currentWebView().title ?: ""

        if (url.isNotEmpty()) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$title\n$url")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Поделиться страницей"))
        }
    }

    private fun findOnPage() {
        Toast.makeText(this, "Поиск на странице", Toast.LENGTH_SHORT).show()
    }

    private fun showSettings() {
        val settings = arrayOf(
            "Очистить кэш",
            "Очистить историю",
            "Очистить cookies",
            "Режим инкогнито",
            "Блокировка рекламы"
        )

        AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> clearCache()
                    1 -> clearHistory()
                    2 -> clearCookies()
                    3 -> incognitoMode()
                    4 -> adBlock()
                }
            }
            .show()
    }

    private fun printPage() {
        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
        val jobName = "${currentWebView().title} - Document"
        printManager.print(jobName, currentWebView().createPrintDocumentAdapter(), null)
    }

    private fun showAbout() {
        val aboutMessage = "VShargin (C) 2025. vaspull9@gmail.com\nAjjnWeb v1.1.5\nПолнофункциональный браузер"
        AlertDialog.Builder(this)
            .setTitle("О программе")
            .setMessage(aboutMessage)
            .setPositiveButton("OK", null)
            .show()
    }

    // Вспомогательные функции
    private fun currentWebView(): WebView = binding.webView

    private fun loadHomePage() {
        loadUrl("https://www.google.com")
    }

    private fun saveToHistory(url: String, title: String) {
        val history = prefs.getStringSet("history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        history.add("$title|$url|${System.currentTimeMillis()}")
        prefs.edit().putStringSet("history", history).apply()
    }

    private fun getHistory(): List<Pair<String, String>> {
        return prefs.getStringSet("history", setOf())?.map {
            val parts = it.split("|")
            parts[1] to parts[0]
        } ?: emptyList()
    }

    private fun clearHistory() {
        prefs.edit().remove("history").apply()
        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
    }

    private fun addBookmark() {
        val url = currentWebView().url ?: return
        val title = currentWebView().title ?: "Без названия"
        val bookmarks = prefs.getStringSet("bookmarks", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        bookmarks.add("$title|$url")
        prefs.edit().putStringSet("bookmarks", bookmarks).apply()
        Toast.makeText(this, "Закладка добавлена", Toast.LENGTH_SHORT).show()
    }

    private fun getBookmarks(): List<Pair<String, String>> {
        return prefs.getStringSet("bookmarks", setOf())?.map {
            val parts = it.split("|")
            parts[0] to parts[1]
        } ?: emptyList()
    }

    private fun clearCache() {
        currentWebView().clearCache(true)
        WebStorage.getInstance().deleteAllData()
        Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show()
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        Toast.makeText(this, "Cookies очищены", Toast.LENGTH_SHORT).show()
    }

    private fun incognitoMode() {
        Toast.makeText(this, "Режим инкогнито", Toast.LENGTH_SHORT).show()
    }

    private fun adBlock() {
        Toast.makeText(this, "Блокировка рекламы", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (currentWebView().canGoBack()) {
            currentWebView().goBack()
        } else {
            super.onBackPressed()
        }
    }
}