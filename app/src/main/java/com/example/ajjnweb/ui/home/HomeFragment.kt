package com.example.ajjnweb

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ajjnweb.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    var onNavigationStateChanged: ((canGoBack: Boolean, canGoForward: Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWebView()
        setupListeners()

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

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    binding.urlEditText.setText(url)

                    updateNavigationState() // ← ВЫЗЫВАЕМ ОБНОВЛЕНИЕ СОСТОЯНИЯ

                    activity?.title = view?.title ?: "AjjnWeb"
                }

                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean {
                    url?.let { loadUrl(it) }
                    return true
                }

                override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                }
            }

            // Слушатель изменения истории навигации
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && canGoBack()) {
                    goBack()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupListeners() {
        binding.goButton.setOnClickListener { loadUrl() }

        binding.urlEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadUrl()
                true
            } else {
                false
            }
        }

        binding.goButton.setOnLongClickListener {
            reload()
            Toast.makeText(requireContext(), "Обновление...", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun loadUrl(url: String? = null) {
        var inputText = url ?: binding.urlEditText.text.toString().trim()

        if (inputText.isEmpty()) {
            Toast.makeText(requireContext(), "Введите адрес", Toast.LENGTH_SHORT).show()
            return
        }

        if (!inputText.startsWith("http://") && !inputText.startsWith("https://")) {
            if (inputText.contains(".")) {
                inputText = "https://$inputText"
            } else {
                inputText = "https://www.google.com/search?q=${java.net.URLEncoder.encode(inputText, "UTF-8")}"
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.webView.loadUrl(inputText)

        hideKeyboard()
    }

    fun updateNavigationState() {
        onNavigationStateChanged?.invoke(canGoBack(), canGoForward())
    }

    private fun hideKeyboard() {
        binding.urlEditText.clearFocus()
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.urlEditText.windowToken, 0)
    }

    fun canGoBack(): Boolean = binding.webView.canGoBack()

    fun goBack() {
        if (canGoBack()) {
            binding.webView.goBack()
            updateNavigationState() // ← ОБНОВЛЯЕМ СОСТОЯНИЕ ПОСЛЕ НАВИГАЦИИ
        }
    }

    fun canGoForward(): Boolean = binding.webView.canGoForward()

    fun goForward() {
        if (canGoForward()) {
            binding.webView.goForward()
            updateNavigationState() // ← ОБНОВЛЯЕМ СОСТОЯНИЕ ПОСЛЕ НАВИГАЦИИ
        }
    }

    fun reload() {
        binding.webView.reload()
    }

    fun getCurrentUrl(): String = binding.webView.url ?: ""
    fun getCurrentTitle(): String = binding.webView.title ?: ""

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onNavigationStateChanged = null
    }
}