
package com.nael.aixd

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val apiUrl =
        "https://ai-xd-backend.anomaliyanzz17.workers.dev/chat"

    private val executor = Executors.newSingleThreadExecutor()
    private val messages = mutableListOf<Pair<String, String>>()

    private lateinit var chatLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: TextView
    private lateinit var statusText: TextView

    private var isLoading = false
    private var welcomeView: View? = null

    private val bg = Color.rgb(12, 12, 18)
    private val panel = Color.rgb(23, 23, 33)
    private val purple = Color.rgb(155, 92, 255)
    private val white = Color.rgb(240, 240, 248)
    private val muted = Color.rgb(155, 155, 175)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        buildUI()
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        // HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(17), dp(20), dp(17))
            setBackgroundColor(Color.rgb(18, 18, 27))
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "AI-XD"
            textSize = 24f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        statusText = TextView(this).apply {
            text = "● AI ASSISTANT"
            textSize = 10f
            setTextColor(Color.rgb(175, 135, 255))
        }

        brand.addView(title)
        brand.addView(statusText)

        val spacer = View(this)
        header.addView(
            brand,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val newChat = TextView(this).apply {
            text = "＋"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = rounded(panel, 15)
            setPadding(dp(12), dp(3), dp(12), dp(3))
            setOnClickListener { newConversation() }
        }

        header.addView(newChat)
        root.addView(header)

        // CHAT AREA
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        chatLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(chatLayout)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams( -1, 0, 1f)
        )

        // MESSAGE COMPOSER
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(15))
            setBackgroundColor(Color.rgb(18, 18, 27))
        }

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, 25)
            setPadding(dp(12), dp(5), dp(7), dp(5))
        }

        input = EditText(this).apply {
            hint = "Tanyakan sesuatu..."
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            background = null
            minHeight = dp(48)
            maxLines = 4
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(8), dp(8), dp(8), dp(8))

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else false
            }
        }

        composer.addView(
            input,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        sendButton = TextView(this).apply {
            text = "➤"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(purple, 50)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { sendMessage() }
        }

        composer.addView(sendButton)
        bottom.addView(composer)

        val footer = TextView(this).apply {
            text = "AI-XD dapat membuat kesalahan. Periksa informasi penting."
            textSize = 10f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(9), 0, 0)
        }

        bottom.addView(footer)
        root.addView(bottom)

        setContentView(root)
        showWelcome()
    }

    // WELCOME SCREEN
    private fun showWelcome() {
        chatLayout.removeAllViews()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(65), dp(12), dp(35))
        }

        val logo = TextView(this).apply {
            text = "✦"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(purple)
        }

        val heading = TextView(this).apply {
            text = "What can I help with?"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "Tanyakan apa saja kepada AI-XD"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(muted)
        }

        container.addView(logo)
        container.addView(heading)
        container.addView(subtitle)

        addSuggestion(container, "Belajar dan mengerjakan soal")
        addSuggestion(container, "Membantu coding")
        addSuggestion(container, "Menulis dan membuat ide")
        addSuggestion(container, "Tanya apa saja")

        chatLayout.addView(container)
        welcomeView = container
    }

    private fun addSuggestion(parent: LinearLayout, text: String) {
        val item = TextView(this).apply {
            this.text = "✦   $text"
            textSize = 13f
            setTextColor(white)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(panel, 16)

            setOnClickListener {
                input.setText(
                    when {
                        text.contains("Belajar") ->
                            "Bantu aku belajar sesuatu."
                        text.contains("coding") ->
                            "Bantu aku belajar coding."
                        text.contains("Menulis") ->
                            "Bantu aku membuat tulisan."
                        else -> "Halo AI-XD!"
                    }
                )
                sendMessage()
            }
        }

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, dp(8), 0, 0)
        parent.addView(item, params)
    }

    // SEND MESSAGE
    private fun sendMessage() {
        if (isLoading) return

        val question = input.text.toString().trim()
        if (question.isEmpty()) return

        welcomeView?.let {
            chatLayout.removeView(it)
            welcomeView = null
        }

        input.text.clear()
        hideKeyboard()

        messages.add(Pair("user", question))
        addBubble("user", question)
        setLoading(true)

        executor.execute {
            try {
                val connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 60000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type", "application/json"
                )
                connection.setRequestProperty(
                    "Accept", "application/json"
                )

                val jsonMessages = JSONArray()

                messages.takeLast(30).forEach { message ->
                    val item = JSONObject()
                    item.put("role", message.first)
                    item.put("content", message.second)
                    jsonMessages.put(item)
                }

                val body = JSONObject()
                body.put("messages", jsonMessages)

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val response = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                connection.disconnect()

                if (responseCode !in 200..299) {
                    throw Exception("Server error: HTTP $responseCode")
                }

                val result = JSONObject(response)

                if (!result.optBoolean("success", true)) {
                    throw Exception(
                        result.optString("error", "AI gagal menjawab.")
                    )
                }

                val reply = result.optString("reply", "")

                if (reply.isBlank()) {
                    throw Exception("AI memberikan jawaban kosong.")
                }

                runOnUiThread {
                    messages.add(Pair("assistant", reply))
                    setLoading(false)
                    addBubble("assistant", reply)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    setLoading(false)
                    addBubble(
                        "assistant",
                        "AI-XD gagal menjawab.\n\n${e.message}"
                    )
                }
            }
        }
    }

    // CHAT BUBBLES
    private fun addBubble(role: String, message: String) {
        val isUser = role == "user"

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }

        val bubble = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(white)
            setPadding(dp(15), dp(12), dp(15), dp(12))
            background = rounded(
                if (isUser) Color.rgb(91, 57, 155)
                else Color.rgb(30, 30, 42),
                19
            )
        }

        val params = LinearLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.82f).toInt(),
            -2
        )

        row.addView(bubble, params)
        chatLayout.addView(row)
        scrollToBottom()
    }

    // LOADING STATE
    private fun setLoading(loading: Boolean) {
        isLoading = loading
        sendButton.isEnabled = !loading
        sendButton.alpha = if (loading) 0.5f else 1f

        statusText.text =
            if (loading) "● AI SEDANG BERPIKIR..."
            else "● AI ASSISTANT"
    }

    // NEW CHAT
    private fun newConversation() {
        if (isLoading) return
        messages.clear()
        showWelcome()
    }

    // HELPERS
    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun hideKeyboard() {
        val manager =
            getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager

        manager.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
