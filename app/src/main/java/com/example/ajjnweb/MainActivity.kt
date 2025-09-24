package com.example.ajjnweb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
// import android.print.PrintManager
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.ajjnweb.databinding.ActivityMainBinding
import java.net.URLEncoder
// import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.core.content.edit
import android.view.MotionEvent


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setupWebView()
        setupClickListeners()
        setupBackPressedHandler() // Добавляем обработчик назад
        loadHomePage()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    // Если нельзя идти назад в WebView, разрешаем стандартное поведение
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
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
        }
    }

    private fun setupClickListeners() {
        binding.menuButton.setOnClickListener { showBrowserMenu() }

        // ПРОСТАЯ обработка ввода - никаких hint!
        binding.urlEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadUrl()
                true
            } else {
                false
            }
        }

        // При клике просто фокусируем
        binding.urlEditText.setOnClickListener {
            binding.urlEditText.requestFocus()
        }
    }

    private fun loadUrl(url: String? = null) {
        var inputText = url ?: binding.urlEditText.text.toString().trim()

        if (inputText.isEmpty()) {
            inputText = "https://www.nytimes.com"
        }

        if (!inputText.startsWith("http://") && !inputText.startsWith("https://")) {
            inputText = if (inputText.contains(".")) {
                "https://$inputText"
            } else {
                "https://www.nytimes.com"
            }
        }

        binding.webView.loadUrl(inputText)
        hideKeyboard()
    }

    private fun hideKeyboard() {
        binding.urlEditText.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.urlEditText.windowToken, 0)
    }

    private fun showBrowserMenu() {
        val menuItems = arrayOf(
            "← Назад",
            "→ Вперед",
            "⟳ Обновить",
            "＋ Новая вкладка",
            "📚 История",
            "🔖 Закладки",
            "📤 Поделиться",
            "⚙️ Настройки",
            "ℹ️ О программе"
        )

        AlertDialog.Builder(this)
            .setTitle("Меню браузера")
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> goBack()
                    1 -> goForward()
                    2 -> refresh()
                    3 -> newTab()
                    4 -> showHistory()
                    5 -> showBookmarks()
                    6 -> sharePage()
                    7 -> showSettings()
                    8 -> showAbout()
                }
            }
            .show()
    }

    private fun goBack() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            Toast.makeText(this, "Нельзя вернуться назад", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goForward() {
        if (binding.webView.canGoForward()) {
            binding.webView.goForward()
        } else {
            Toast.makeText(this, "Нельзя перейти вперед", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refresh() {
        binding.webView.reload()
    }

    private fun newTab() {
        Toast.makeText(this, "Новая вкладка", Toast.LENGTH_SHORT).show()
        loadUrl("https://www.nytimes.com")
    }

    private fun showHistory() {
        val history = getHistory()
        val urls = history.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.history)
            .setItems(if (urls.isEmpty()) arrayOf(getString(R.string.empty_history)) else urls.takeLast(10).toTypedArray()) { _, which ->
                if (history.isNotEmpty()) {
                    loadUrl(history[which].first)
                }
            }
            .setPositiveButton(R.string.clear_history) { _, _ ->
                clearHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showBookmarks() {
        val bookmarks = getBookmarks()
        val titles = bookmarks.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.bookmarks)
            .setItems(if (titles.isEmpty()) arrayOf(getString(R.string.no_bookmarks)) else titles) { _, which ->
                if (bookmarks.isNotEmpty()) {
                    loadUrl(bookmarks[which].second)
                }
            }
            .setPositiveButton(R.string.add_bookmark) { _, _ ->
                addBookmark()
            }
            .setNegativeButton(R.string.cancel, null)
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
            "Режим инкогнито"
        )

        AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> clearCache()
                    1 -> clearHistory()
                    2 -> clearCookies()
                    3 -> incognitoMode()
                }
            }
            .show()
    }

    private fun clearCache() {
        binding.webView.clearCache(true)
        WebStorage.getInstance().deleteAllData()
        Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun clearHistory() {
        prefs.edit { remove("history") }
        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        Toast.makeText(this, R.string.cookies_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun incognitoMode() {
        Toast.makeText(this, "Режим инкогнито", Toast.LENGTH_SHORT).show()
    }

    private fun saveToHistory(url: String, title: String) {
        prefs.edit {
            val history = prefs.getStringSet("history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            history.add("$title|$url|${System.currentTimeMillis()}")
            putStringSet("history", history)
        }
    }

    private fun getHistory(): List<Pair<String, String>> {
        return prefs.getStringSet("history", setOf())?.map {
            val parts = it.split("|")
            parts[1] to parts[0]
        } ?: emptyList()
    }

    private fun addBookmark() {
        val url = binding.webView.url ?: return
        val title = binding.webView.title ?: "Без названия"
        prefs.edit {
            val bookmarks = prefs.getStringSet("bookmarks", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            bookmarks.add("$title|$url")
            putStringSet("bookmarks", bookmarks)
        }
        Toast.makeText(this, "Закладка добавлена", Toast.LENGTH_SHORT).show()
    }

    private fun getBookmarks(): List<Pair<String, String>> {
        return prefs.getStringSet("bookmarks", setOf())?.map {
            val parts = it.split("|")
            parts[0] to parts[1]
        } ?: emptyList()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setMessage(getString(R.string.about_message))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadHomePage() {
        loadUrl("https://www.nytimes.com")
    }
}