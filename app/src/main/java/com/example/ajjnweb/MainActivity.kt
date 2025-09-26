// AjjnWeb v1.5.1 - Добавляем систему вкладок
package com.example.ajjnweb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.ajjnweb.databinding.ActivityMainBinding
import java.net.URLEncoder
import android.view.inputmethod.InputMethodManager
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    // Данные о вкладках
    private data class Tab(
        val id: Int,
        var url: String,
        var title: String,
        var history: List<String> = emptyList(),
        var scrollPosition: Int = 0
    )

    private val tabs = mutableListOf<Tab>()
    private var currentTabId = 0
    private var nextTabId = 1

    // Популярные сайты для виджетов
    private val popularSites = listOf(
        "Google" to "https://www.google.com",
        "YouTube" to "https://www.youtube.com",
        "Gmail" to "https://mail.google.com",
        "ВКонтакте" to "https://vk.com",
        "Яндекс" to "https://yandex.ru",
        "Twitter" to "https://twitter.com",
        "Instagram" to "https://instagram.com",
        "Facebook" to "https://facebook.com",
        "GitHub" to "https://github.com",
        "Stack Overflow" to "https://stackoverflow.com",
        "Reddit" to "https://reddit.com",
        "Wikipedia" to "https://wikipedia.org"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setupWebView()
        setupClickListeners()
        setupBackPressedHandler()
        initializeFirstTab()
    }

    private fun initializeFirstTab() {
        // Создаем первую вкладку с виджетами
        val firstTab = Tab(nextTabId++, "", "Домашняя страница")
        tabs.add(firstTab)
        currentTabId = firstTab.id
        showHomePageWithWidgets()
        updateTabsCounter()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTab = getCurrentTab()
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else if (currentTab?.url?.isNotEmpty() == true) {
                    // Если на странице сайта - возвращаемся к виджетам
                    showHomePageWithWidgets()
                } else {
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

                // Обновляем текущую вкладку
                val currentTab = getCurrentTab()
                currentTab?.let {
                    it.url = url ?: ""
                    it.title = view?.title ?: "Без названия"
                }

                saveToHistory(url ?: "", view?.title ?: "")
                updateTabsCounter()
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

                // Обновляем заголовок вкладки
                val currentTab = getCurrentTab()
                currentTab?.title = title ?: "Без названия"
            }
        }
    }

    private fun setupClickListeners() {
        binding.menuButton.setOnClickListener { showBrowserMenu() }
        binding.tabsCounterButton.setOnClickListener { showTabsOverview() }

        binding.urlEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadUrl()
                true
            } else {
                false
            }
        }

        binding.urlEditText.setOnClickListener {
            binding.urlEditText.requestFocus()
        }
    }

    private fun updateTabsCounter() {
        binding.tabsCounterButton.text = tabs.size.toString()
    }

    private fun getCurrentTab(): Tab? {
        return tabs.find { it.id == currentTabId }
    }

    private fun loadUrl(url: String? = null) {
        var inputText = url ?: binding.urlEditText.text.toString().trim()

        if (inputText.isEmpty()) {
            showHomePageWithWidgets()
            return
        }

        if (!inputText.startsWith("http://") && !inputText.startsWith("https://")) {
            inputText = if (inputText.contains(".")) {
                "https://$inputText"
            } else {
                "https://www.google.com/search?q=${URLEncoder.encode(inputText, "UTF-8")}"
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

    private fun showHomePageWithWidgets() {
        val widgetsHtml = createWidgetsHtml()
        binding.webView.loadDataWithBaseURL(null, widgetsHtml, "text/html", "UTF-8", null)

        // Обновляем текущую вкладку
        val currentTab = getCurrentTab()
        currentTab?.let {
            it.url = ""
            it.title = "Домашняя страница"
        }
    }

    private fun createWidgetsHtml(): String {
        val widgets = popularSites.joinToString("") { (name, url) ->
            """
            <a class="widget" href="$url">
                <div class="widget-icon">${getSiteIcon(name)}</div>
                <div class="widget-title">$name</div>
            </a>
            """
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 0; 
                        padding: 20px; 
                        background: #f0f0f0; 
                    }
                    .header { 
                        text-align: center; 
                        margin-bottom: 20px; 
                        color: #333; 
                    }
                    .widgets-container {
                        display: flex;
                        flex-wrap: wrap;
                        justify-content: center;
                        gap: 15px;
                        max-width: 1200px;
                        margin: 0 auto;
                    }
                    .widget { 
                        background: white; 
                        width: 100px;
                        height: 100px;
                        border-radius: 12px; 
                        text-align: center; 
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); 
                        cursor: pointer; 
                        transition: transform 0.2s;
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        align-items: center;
                        text-decoration: none;
                        color: #333;
                    }
                    .widget:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                    }
                    .widget-icon { 
                        font-size: 32px; 
                        margin-bottom: 8px; 
                    }
                    .widget-title { 
                        font-size: 12px; 
                        font-weight: bold;
                        padding: 0 5px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>Часто посещаемые сайты</h2>
                </div>
                <div class="widgets-container">
                    $widgets
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getSiteIcon(siteName: String): String {
        return when (siteName) {
            "Google" -> "🔍"
            "YouTube" -> "📺"
            "Gmail" -> "📧"
            "ВКонтакте" -> "👥"
            "Яндекс" -> "🌐"
            "Twitter" -> "🐦"
            "Instagram" -> "📷"
            "Facebook" -> "👤"
            "GitHub" -> "💻"
            "Stack Overflow" -> "❓"
            "Reddit" -> "📱"
            "Wikipedia" -> "📚"
            else -> "🌐"
        }
    }

    private fun showTabsOverview() {
        val tabItems = arrayOf(
            "Новая вкладка",
            "Новая вкладка инкогнито",
            "Закрыть все вкладки",
            *tabs.map { it.title.takeIf { t -> t.isNotEmpty() } ?: "Новая вкладка" }.toTypedArray()
        )

        AlertDialog.Builder(this)
            .setTitle("Вкладки (${tabs.size})")
            .setItems(tabItems) { _, which ->
                when (which) {
                    0 -> newTab()
                    1 -> newIncognitoTab()
                    2 -> closeAllTabs()
                    else -> {
                        // Переключаемся на существующую вкладку
                        val tabIndex = which - 3
                        if (tabIndex in tabs.indices) {
                            switchToTab(tabs[tabIndex].id)
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun newTab() {
        val newTab = Tab(nextTabId++, "", "Новая вкладка")
        tabs.add(newTab)
        currentTabId = newTab.id
        showHomePageWithWidgets()
        updateTabsCounter()
        Toast.makeText(this, "Новая вкладка создана", Toast.LENGTH_SHORT).show()
    }

    private fun newIncognitoTab() {
        Toast.makeText(this, "Режим инкогнито (скоро в v1.5.2)", Toast.LENGTH_SHORT).show()
    }

    private fun closeAllTabs() {
        if (tabs.size > 1) {
            tabs.clear()
            initializeFirstTab()
            Toast.makeText(this, "Все вкладки закрыты", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Нельзя закрыть последнюю вкладку", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchToTab(tabId: Int) {
        currentTabId = tabId
        val tab = getCurrentTab()
        tab?.let {
            if (it.url.isEmpty()) {
                showHomePageWithWidgets()
            } else {
                binding.webView.loadUrl(it.url)
            }
        }
        updateTabsCounter()
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
            "ℹ️ О программе v1.5.1"
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
            val currentTab = getCurrentTab()
            if (currentTab?.url?.isNotEmpty() == true) {
                showHomePageWithWidgets()
            }
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

    private fun showSettings() {
        val settings = arrayOf(
            "Очистить кэш",
            "Очистить историю",
            "Очистить cookies",
            "Режим инкогнито"
        )

        AlertDialog.Builder(this)
            .setTitle("Настройки v1.5.1")
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
        Toast.makeText(this, "Режим инкогнито (скоро в v1.5.2)", Toast.LENGTH_SHORT).show()
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
            .setMessage("${getString(R.string.about_message)}\n\nВерсия 1.5.1 - Добавлена система вкладок")
            .setPositiveButton("OK", null)
            .show()
    }
}