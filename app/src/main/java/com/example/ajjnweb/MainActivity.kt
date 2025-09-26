// AjjnWeb v1.5.3 - Добавляем выбор поисковой системы и улучшенное управление вкладками
package com.example.ajjnweb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.ajjnweb.databinding.ActivityMainBinding
import java.net.URLEncoder
import android.view.inputmethod.InputMethodManager
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    // Поисковые системы
    private val searchEngines = mapOf(
        "Google" to "https://www.google.com/search?q=",
        "Яндекс" to "https://yandex.ru/search/?text=",
        "Bing" to "https://www.bing.com/search?q=",
        "Yahoo" to "https://search.yahoo.com/search?p=",
        "DuckDuckGo" to "https://duckduckgo.com/?q=",
        "Baidu" to "https://www.baidu.com/s?wd=",
        "StartPage" to "https://www.startpage.com/do/search?query=",
        "Ask.com" to "https://www.ask.com/web?q=",
        "You.com" to "https://you.com/search?q=",
        "Wolfram Alpha" to "https://www.wolframalpha.com/input?i=",
        "TinEye" to "https://tineye.com/search?url=",
        "Brave" to "https://search.brave.com/search?q=",
        "FreeSound" to "https://freesound.org/search/?q=",
        "Creative Commons Search" to "https://search.creativecommons.org/search?q=",
        "Giphy" to "https://giphy.com/search/"
    )

    // Данные о вкладках
    private data class Tab(
        val id: Int,
        var url: String,
        var title: String,
        var isIncognito: Boolean = false,
        var history: List<String> = emptyList(),
        var scrollPosition: Int = 0
    )

    private val tabs = mutableListOf<Tab>()
    private var currentTabId = 0
    private var nextTabId = 1

    // Виджеты
    data class Widget(val name: String, val url: String, val icon: String)

    private val defaultWidgets = listOf(
        Widget("Google", "https://www.google.com", "🔍"),
        Widget("YouTube", "https://www.youtube.com", "📺"),
        Widget("Gmail", "https://mail.google.com", "📧"),
        Widget("ВКонтакте", "https://vk.com", "👥"),
        Widget("Яндекс", "https://yandex.ru", "🌐"),
        Widget("Twitter", "https://twitter.com", "🐦"),
        Widget("Instagram", "https://instagram.com", "📷"),
        Widget("Facebook", "https://facebook.com", "👤"),
        Widget("GitHub", "https://github.com", "💻"),
        Widget("Stack Overflow", "https://stackoverflow.com", "❓"),
        Widget("Reddit", "https://reddit.com", "📱"),
        Widget("Wikipedia", "https://wikipedia.org", "📚")
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

    private fun getSelectedSearchEngine(): String {
        return prefs.getString("search_engine", "Google") ?: "Google"
    }

    private fun getSearchEngineUrl(): String {
        return searchEngines[getSelectedSearchEngine()] ?: searchEngines["Google"]!!
    }

    private fun getWidgets(): List<Widget> {
        val widgetsJson = prefs.getString("custom_widgets", null)
        return if (widgetsJson != null) {
            try {
                val jsonArray = JSONArray(widgetsJson)
                val widgets = mutableListOf<Widget>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    widgets.add(Widget(
                        jsonObject.getString("name"),
                        jsonObject.getString("url"),
                        jsonObject.getString("icon")
                    ))
                }
                widgets
            } catch (e: Exception) {
                defaultWidgets
            }
        } else {
            defaultWidgets
        }
    }

    private fun saveWidgets(widgets: List<Widget>) {
        val jsonArray = JSONArray()
        widgets.forEach { widget ->
            val jsonObject = JSONObject()
            jsonObject.put("name", widget.name)
            jsonObject.put("url", widget.url)
            jsonObject.put("icon", widget.icon)
            jsonArray.put(jsonObject)
        }
        prefs.edit { putString("custom_widgets", jsonArray.toString()) }
    }

    private fun initializeFirstTab() {
        val firstTab = Tab(nextTabId++, "", "Домашняя страница")
        tabs.add(firstTab)
        currentTabId = firstTab.id
        showHomePageWithWidgets()
        updateTabsCounter()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    val currentTab = getCurrentTab()
                    if (currentTab?.url?.isNotEmpty() == true) {
                        showHomePageWithWidgets()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
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

        webView.addJavascriptInterface(JavaScriptInterface(), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.progress = 0
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                binding.urlEditText.setText(url)

                val currentTab = getCurrentTab()
                currentTab?.let {
                    it.url = url ?: ""
                    it.title = view?.title ?: "Без названия"
                }

                if (!currentTab?.isIncognito!!) {
                    saveToHistory(url ?: "", view?.title ?: "")
                }
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
                val currentTab = getCurrentTab()
                val tabTitle = title ?: "AjjnWeb"
                supportActionBar?.title = if (currentTab?.isIncognito == true) "$tabTitle (Инкогнито)" else tabTitle

                currentTab?.title = tabTitle
            }
        }
    }

    private fun setupClickListeners() {
        binding.menuButton.setOnClickListener { showBrowserMenu() }
        binding.tabsCounterButton.setOnClickListener { showAdvancedTabsOverview() }

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
                "${getSearchEngineUrl()}${URLEncoder.encode(inputText, "UTF-8")}"
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

        val currentTab = getCurrentTab()
        currentTab?.let {
            it.url = ""
            it.title = "Домашняя страница"
        }
    }

    private fun createWidgetsHtml(): String {
        val widgets = getWidgets().joinToString("") { widget ->
            """
            <a class="widget" href="${widget.url}" onclick="handleWidgetClick('${widget.url}')">
                <div class="widget-icon">${widget.icon}</div>
                <div class="widget-title">${widget.name}</div>
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
                    .edit-section {
                        text-align: center;
                        margin-top: 30px;
                    }
                    .edit-button {
                        background: #4285f4;
                        color: white;
                        border: none;
                        padding: 12px 24px;
                        border-radius: 24px;
                        cursor: pointer;
                        font-size: 14px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>Мои виджеты</h2>
                </div>
                <div class="widgets-container">
                    $widgets
                </div>
                <div class="edit-section">
                    <button class="edit-button" onclick="editWidgets()">Редактировать виджеты</button>
                </div>

                <script>
                    function handleWidgetClick(url) {
                        if (window.Android) {
                            window.Android.trackWidgetClick(url);
                        }
                    }
                    
                    function editWidgets() {
                        if (window.Android) {
                            window.Android.editWidgets();
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    @SuppressLint("JavascriptInterface")
    inner class JavaScriptInterface {
        @Suppress("UNUSED") // ДОБАВИТЬ эту аннотацию
        @JavascriptInterface
        fun editWidgets() {
            runOnUiThread {
                showEditWidgetsDialog()
            }
        }
    }

    // Остальные методы для виджетов остаются без изменений
    // showEditWidgetsDialog(), showWidgetOptionsDialog(), etc.

    private fun showAdvancedTabsOverview() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tabs_overview, null)
        val tabsContainer = dialogView.findViewById<LinearLayout>(R.id.tabsContainer)
        val newTabButton = dialogView.findViewById<Button>(R.id.newTabButton)
        val newIncognitoTabButton = dialogView.findViewById<Button>(R.id.newIncognitoTabButton)
        val closeAllTabsButton = dialogView.findViewById<Button>(R.id.closeAllTabsButton)
        val selectTabsButton = dialogView.findViewById<Button>(R.id.selectTabsButton)
        val clearDataButton = dialogView.findViewById<Button>(R.id.clearDataButton)
        val settingsButton = dialogView.findViewById<Button>(R.id.settingsButton)

        // Очищаем и заполняем контейнер вкладок
        tabsContainer.removeAllViews()
        tabs.forEachIndexed { index, tab ->
            val tabView = layoutInflater.inflate(R.layout.item_tab_preview, null)
            val tabTitle = tabView.findViewById<TextView>(R.id.tabTitle)
            val tabUrl = tabView.findViewById<TextView>(R.id.tabUrl)
            val tabIcon = tabView.findViewById<TextView>(R.id.tabIcon)
            val closeButton = tabView.findViewById<ImageButton>(R.id.closeTabButton)

            tabTitle.text = tab.title.takeIf { it.isNotEmpty() } ?: "Новая вкладка"
            tabUrl.text = tab.url.takeIf { it.isNotEmpty() } ?: "Домашняя страница"
            tabIcon.text = if (tab.isIncognito) "👤" else "🌐"

            tabView.setOnClickListener {
                switchToTab(tab.id)
            }

            closeButton.setOnClickListener {
                closeTab(index)
                showAdvancedTabsOverview() // Обновляем диалог
            }

            tabsContainer.addView(tabView)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Вкладки - ${tabs.size}")
            .create()

        newTabButton.setOnClickListener {
            newTab()
            dialog.dismiss()
        }

        newIncognitoTabButton.setOnClickListener {
            newIncognitoTab()
            dialog.dismiss()
        }

        closeAllTabsButton.setOnClickListener {
            closeAllTabs()
            dialog.dismiss()
        }

        selectTabsButton.setOnClickListener {
            showMultiSelectTabsDialog()
            dialog.dismiss()
        }

        clearDataButton.setOnClickListener {
            showClearDataDialog()
            dialog.dismiss()
        }

        settingsButton.setOnClickListener {
            showSettings()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMultiSelectTabsDialog() {
        val tabTitles = tabs.map { it.title.takeIf { t -> t.isNotEmpty() } ?: "Новая вкладка" }.toTypedArray()
        val selectedItems = BooleanArray(tabs.size) { false }

        AlertDialog.Builder(this)
            .setTitle("Выбрать вкладки")
            .setMultiChoiceItems(tabTitles, selectedItems) { _, which, isChecked ->
                selectedItems[which] = isChecked
            }
            .setPositiveButton("Закрыть выбранные") { _, _ ->
                val tabsToRemove = selectedItems.indices.filter { selectedItems[it] }.reversed()
                tabsToRemove.forEach { index ->
                    closeTab(index)
                }
                Toast.makeText(this, "Закрыто вкладок: ${tabsToRemove.size}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showClearDataDialog() {
        val options = arrayOf("Очистить историю", "Очистить кэш", "Очистить cookies", "Очистить все данные")

        AlertDialog.Builder(this)
            .setTitle("Удалить данные браузера")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> clearHistory()
                    1 -> clearCache()
                    2 -> clearCookies()
                    3 -> {
                        clearHistory()
                        clearCache()
                        clearCookies()
                        Toast.makeText(this, "Все данные очищены", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun closeTab(index: Int) {
        if (tabs.size > 1 && index in tabs.indices) {
            val removedTab = tabs.removeAt(index)
            if (currentTabId == removedTab.id) {
                currentTabId = tabs.last().id
                switchToTab(currentTabId)
            }
            updateTabsCounter()
        }
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
        val newTab = Tab(nextTabId++, "", "Новая вкладка инкогнито", true)
        tabs.add(newTab)
        currentTabId = newTab.id
        showHomePageWithWidgets()
        updateTabsCounter()
        Toast.makeText(this, "Новая вкладка инкогнито создана", Toast.LENGTH_SHORT).show()
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

    private fun showSearchEngineSelection() {
        val engines = searchEngines.keys.toTypedArray()
        val currentEngine = getSelectedSearchEngine()

        AlertDialog.Builder(this)
            .setTitle("Выбор поисковой системы")
            .setSingleChoiceItems(engines, engines.indexOf(currentEngine)) { dialog, which ->
                prefs.edit { putString("search_engine", engines[which]) }
                dialog.dismiss()
                Toast.makeText(this, "Поисковая система изменена на: ${engines[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showBrowserMenu() {
        val menuItems = arrayOf(
            "← Назад",
            "→ Вперед",
            "⟳ Обновить",
            "＋ Новая вкладка",
            "🔍 Найти на странице",
            "🌐 Перевести страницу",
            "🔊 Озвучить страницу",
            "📚 История",
            "🔖 Закладки",
            "📤 Поделиться",
            "⚙️ Настройки",
            "ℹ️ О программе v1.5.3"
        )

        AlertDialog.Builder(this)
            .setTitle("Меню браузера")
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> goBack()
                    1 -> goForward()
                    2 -> refresh()
                    3 -> newTab()
                    4 -> findOnPage()
                    5 -> translatePage()
                    6 -> speakPage()
                    7 -> showHistory()
                    8 -> showBookmarks()
                    9 -> sharePage()
                    10 -> showSettings()
                    11 -> showAbout()
                }
            }
            .show()
    }

    private fun findOnPage() {
        Toast.makeText(this, "Поиск на странице (скоро в v1.5.4)", Toast.LENGTH_SHORT).show()
    }

    private fun translatePage() {
        val currentUrl = binding.webView.url ?: ""
        if (currentUrl.isNotEmpty()) {
            val translateUrl = "https://translate.google.com/translate?hl=ru&sl=auto&tl=ru&u=${URLEncoder.encode(currentUrl, "UTF-8")}"
            binding.webView.loadUrl(translateUrl)
            Toast.makeText(this, "Перевод страницы...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakPage() {
        Toast.makeText(this, "Озвучка страницы (скоро в v1.5.4)", Toast.LENGTH_SHORT).show()
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

    private fun showSettings() {
        val settings = arrayOf(
            "Поисковая система",
            "Очистить кэш",
            "Очистить историю",
            "Очистить cookies",
            "Режим инкогнито"
        )

        AlertDialog.Builder(this)
            .setTitle("Настройки v1.5.3")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> showSearchEngineSelection()
                    1 -> clearCache()
                    2 -> clearHistory()
                    3 -> clearCookies()
                    4 -> toggleIncognitoMode()
                }
            }
            .show()
    }

    private fun toggleIncognitoMode() {
        val currentTab = getCurrentTab()
        currentTab?.isIncognito = !(currentTab?.isIncognito ?: false)
        Toast.makeText(this,
            if (currentTab?.isIncognito == true) "Режим инкогнито активирован" else "Режим инкогнито выключен",
            Toast.LENGTH_SHORT).show()
    }

    // Остальные методы без изменений
    // showHistory(), showBookmarks(), sharePage(), clearCache(), clearHistory(), clearCookies(),
    // saveToHistory(), getHistory(), addBookmark(), getBookmarks(), showAbout()

    private fun showHistory() {
        val currentTab = getCurrentTab()
        if (currentTab?.isIncognito == true) {
            Toast.makeText(this, "История недоступна в режиме инкогнито", Toast.LENGTH_SHORT).show()
            return
        }

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
        val currentTab = getCurrentTab()
        if (currentTab?.isIncognito == true) {
            Toast.makeText(this, "Закладки недоступны в режиме инкогнито", Toast.LENGTH_SHORT).show()
            return
        }

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

    private fun clearCache() {
        binding.webView.clearCache(true)
        WebStorage.getInstance().deleteAllData()
        Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun clearHistory() {
        val currentTab = getCurrentTab()
        if (currentTab?.isIncognito == true) {
            Toast.makeText(this, "История недоступна в режиме инкогнито", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit { remove("history") }
        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        Toast.makeText(this, R.string.cookies_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun saveToHistory(url: String, title: String) {
        val currentTab = getCurrentTab()
        if (currentTab?.isIncognito == true) return

        prefs.edit {
            val history = prefs.getStringSet("history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            history.add("$title|$url|${System.currentTimeMillis()}")
            putStringSet("history", history)
        }
    }

    private fun getHistory(): List<Pair<String, String>> {
        val currentTab = getCurrentTab()
        if (currentTab?.isIncognito == true) return emptyList()

        return prefs.getStringSet("history", setOf())?.map {
            val parts = it.split("|")
            parts[1] to parts[0]
        } ?: emptyList()
    }

    private fun addBookmark() {
        val currentTab = getCurrentTab()
        if (currentTab?.isIncognito == true) {
            Toast.makeText(this, "Закладки недоступны в режиме инкогнито", Toast.LENGTH_SHORT).show()
            return
        }

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
        val currentTab = getCurrentTab()
        if (currentTab?.isIncognito == true) return emptyList()

        return prefs.getStringSet("bookmarks", setOf())?.map {
            val parts = it.split("|")
            parts[0] to parts[1]
        } ?: emptyList()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setMessage("${getString(R.string.about_message)}\n\nВерсия 1.5.3 - Добавлен выбор поисковой системы и улучшенное управление вкладками")
            .setPositiveButton("OK", null)
            .show()
    }

    // Методы для редактирования виджетов (без изменений из v1.5.2)
    private fun showEditWidgetsDialog() {
        val widgets = getWidgets().toMutableList()
        val widgetNames = widgets.map { it.name }.toMutableList()

        AlertDialog.Builder(this)
            .setTitle("Редактирование виджетов (${widgets.size}/12)")
            .setItems(widgetNames.toTypedArray()) { _, which ->
                showWidgetOptionsDialog(widgets, which)
            }
            .setPositiveButton("Добавить виджет") { _, _ ->
                showAddWidgetDialog(widgets)
            }
            .setNegativeButton("Сохранить") { _, _ ->
                saveWidgets(widgets)
                showHomePageWithWidgets()
                Toast.makeText(this, "Виджеты сохранены", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Сбросить") { _, _ ->
                showResetWidgetsConfirmation()
            }
            .show()
    }

    private fun showWidgetOptionsDialog(widgets: MutableList<Widget>, index: Int) {
        val options = arrayOf("Редактировать", "Удалить", "Переместить вверх", "Переместить вниз")

        AlertDialog.Builder(this)
            .setTitle("Действие с \"${widgets[index].name}\"")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditWidgetDialog(widgets, index)
                    1 -> showDeleteWidgetConfirmation(widgets, index)
                    2 -> moveWidgetUp(widgets, index)
                    3 -> moveWidgetDown(widgets, index)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditWidgetDialog(widgets: MutableList<Widget>, index: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_widget, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.widgetNameEditText)
        val urlEditText = dialogView.findViewById<EditText>(R.id.widgetUrlEditText)

        val currentWidget = widgets[index]
        nameEditText.setText(currentWidget.name)
        urlEditText.setText(currentWidget.url)

        AlertDialog.Builder(this)
            .setTitle("Редактировать виджет")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = nameEditText.text.toString().trim()
                val newUrl = urlEditText.text.toString().trim()

                if (newName.isNotEmpty() && newUrl.isNotEmpty()) {
                    if (!newUrl.startsWith("http")) {
                        Toast.makeText(this, "URL должен начинаться с http:// или https://", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    widgets[index] = Widget(newName, newUrl, currentWidget.icon)
                    saveWidgets(widgets)
                    showEditWidgetsDialog()
                    Toast.makeText(this, "Виджет обновлен", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAddWidgetDialog(widgets: MutableList<Widget>) {
        if (widgets.size >= 12) {
            Toast.makeText(this, "Максимум 12 виджетов", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_widget, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.widgetNameEditText)
        val urlEditText = dialogView.findViewById<EditText>(R.id.widgetUrlEditText)

        AlertDialog.Builder(this)
            .setTitle("Добавить виджет")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val url = urlEditText.text.toString().trim()

                if (name.isNotEmpty() && url.isNotEmpty()) {
                    val fullUrl = if (!url.startsWith("http")) "https://$url" else url
                    val icon = getSiteIconForUrl(fullUrl)
                    widgets.add(Widget(name, fullUrl, icon))
                    saveWidgets(widgets)
                    showEditWidgetsDialog()
                    Toast.makeText(this, "Виджет добавлен", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteWidgetConfirmation(widgets: MutableList<Widget>, index: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить виджет?")
            .setMessage("Вы уверены, что хотите удалить \"${widgets[index].name}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                widgets.removeAt(index)
                saveWidgets(widgets)
                showEditWidgetsDialog()
                Toast.makeText(this, "Виджет удален", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showResetWidgetsConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Сбросить виджеты?")
            .setMessage("Вернуть виджеты к настройкам по умолчанию?")
            .setPositiveButton("Сбросить") { _, _ ->
                saveWidgets(defaultWidgets)
                showHomePageWithWidgets()
                Toast.makeText(this, "Виджеты сброшены", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun moveWidgetUp(widgets: MutableList<Widget>, index: Int) {
        if (index > 0) {
            Collections.swap(widgets, index, index - 1)
            saveWidgets(widgets)
            showEditWidgetsDialog()
        }
    }

    private fun moveWidgetDown(widgets: MutableList<Widget>, index: Int) {
        if (index < widgets.size - 1) {
            Collections.swap(widgets, index, index + 1)
            saveWidgets(widgets)
            showEditWidgetsDialog()
        }
    }

    private fun getSiteIconForUrl(url: String): String {
        return when {
            url.contains("google") -> "🔍"
            url.contains("youtube") -> "📺"
            url.contains("mail") || url.contains("gmail") -> "📧"
            url.contains("vk") -> "👥"
            url.contains("yandex") -> "🌐"
            url.contains("twitter") -> "🐦"
            url.contains("instagram") -> "📷"
            url.contains("facebook") -> "👤"
            url.contains("github") -> "💻"
            url.contains("stackoverflow") -> "❓"
            url.contains("reddit") -> "📱"
            url.contains("wikipedia") -> "📚"
            else -> "🌐"
        }
    }
}