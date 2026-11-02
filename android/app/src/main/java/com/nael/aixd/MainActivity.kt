
package com.nael.aixd

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.*
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    private data class ChatSession(
        val title: String,
        val messages: List<Pair<String, String>>
    )

    private val sessions = mutableListOf<ChatSession>()

    private lateinit var root: LinearLayout
    private lateinit var chatLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: TextView
    private lateinit var statusText: TextView
    private lateinit var historyButton: TextView

    private var isLoading = false
    private var thinkingView: View? = null
    private var thinkingAnimator: ValueAnimator? = null

    private val bg = Color.rgb(12, 12, 18)
    private val panel = Color.rgb(25, 25, 36)
    private val purple = Color.rgb(155, 92, 255)
    private val white = Color.rgb(240, 240, 248)
    private val muted = Color.rgb(155, 155, 175)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        buildUI()
    }

    private fun buildUI() {

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            clipToPadding = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->

            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            val keyboard = insets.getInsets(
                WindowInsetsCompat.Type.ime()
            )

            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                maxOf(bars.bottom, keyboard.bottom)
            )

            insets
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(10), dp(13), dp(10))
            setBackgroundColor(Color.rgb(18, 18, 27))
        }

        historyButton = createHeaderButton("☰")
        historyButton.setOnClickListener {
            showHistory()
        }

        header.addView(historyButton)

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, 0, 0)
        }

        val title = TextView(this).apply {
            text = "AI-XD"
            textSize = 21f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        statusText = TextView(this).apply {
            text = "● AI-XD 4.0"
            textSize = 10f
            setTextColor(Color.rgb(180, 140, 255))
        }

        brand.addView(title)
        brand.addView(statusText)

        header.addView(
            brand,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val newChat = createHeaderButton("＋")
        newChat.setOnClickListener {
            newConversation()
        }

        header.addView(newChat)
        root.addView(header)

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(dp(17), dp(15), dp(17), dp(15))
        }

        chatLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(
            chatLayout,
            ViewGroup.LayoutParams(-1, -2)
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(9), dp(13), dp(7))
            setBackgroundColor(Color.rgb(18, 18, 27))
        }

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, 25)
            setPadding(dp(11), dp(5), dp(7), dp(5))
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
                } else {
                    false
                }
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

            setOnClickListener {
                sendMessage()
            }
        }

        composer.addView(sendButton)
        bottom.addView(composer)

        val footer = TextView(this).apply {
            text = "AI-XD 4.0 • Your AI Assistant"
            textSize = 10f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, 0)
        }

        bottom.addView(footer)
        root.addView(bottom)

        setContentView(root)

        root.post {
            ViewCompat.requestApplyInsets(root)
        }

        showWelcome()
    }

    private fun createHeaderButton(label: String): TextView {

        return TextView(this).apply {
            text = label
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = rounded(panel, 14)
            setPadding(dp(11), dp(4), dp(11), dp(4))
        }
    }

    private fun showWelcome() {

        chatLayout.removeAllViews()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(50), dp(12), dp(30))
        }

        val logo = TextView(this).apply {
            text = "✦"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(purple)
        }

        val heading = TextView(this).apply {
            text = "What can I help with?"
            textSize = 24f
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
        animateView(container)
    }

    private fun addSuggestion(
        parent: LinearLayout,
        text: String
    ) {

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
        private fun sendMessage() {

        if (isLoading) return

        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        if (messages.isEmpty()) {
            saveCurrentSession()
        }

        messages.add(Pair("user", text))

        if (chatLayout.childCount == 1 &&
            messages.size == 1
        ) {
            chatLayout.removeAllViews()
        }

        addBubble("user", text)

        input.text.clear()
        hideKeyboard()
        setLoading(true)
        showThinking()

        requestAI()
    }

    private fun requestAI() {

        val requestMessages = messages.toList()

        executor.execute {

            var answer = ""

            try {

                val connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val jsonMessages = JSONArray()

                requestMessages.forEach { message ->

                    val item = JSONObject()

                    item.put("role", message.first)
                    item.put("content", message.second)

                    jsonMessages.put(item)
                }

                val body = JSONObject()
                body.put("messages", jsonMessages)

                connection.outputStream.use {
                    it.write(body.toString().toByteArray())
                }

                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText =
                    responseStream.bufferedReader().use {
                        it.readText()
                    }

                if (responseCode !in 200..299) {
                    throw Exception(
                        "Server error: $responseCode"
                    )
                }

                val result = JSONObject(responseText)

                answer = result.optString("reply")

                if (answer.isBlank()) {
                    answer = result.optString("response")
                }

                if (answer.isBlank()) {
                    answer = result.optString("answer")
                }

                if (answer.isBlank()) {
                    answer = result.optString("content")
                }

                if (answer.isBlank()) {

                    val choices =
                        result.optJSONArray("choices")

                    if (choices != null &&
                        choices.length() > 0
                    ) {

                        answer = choices
                            .getJSONObject(0)
                            .optJSONObject("message")
                            ?.optString("content")
                            ?: ""
                    }
                }

                if (answer.isBlank()) {
                    answer = "AI-XD tidak menerima jawaban dari server."
                }

                connection.disconnect()

            } catch (e: Exception) {

                answer =
                    "AI-XD gagal menjawab.\n\n${e.message}"
            }

            runOnUiThread {

                removeThinking()
                setLoading(false)

                messages.add(Pair("assistant", answer))

                addBubble("assistant", answer)
                scrollToBottom()
            }
        }
    }

    private fun addBubble(
        role: String,
        text: String
    ) {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (role == "user") {
                Gravity.END
            } else {
                Gravity.START
            }

            setPadding(0, dp(7), 0, dp(7))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                if (role == "user") dp(14) else 0,
                dp(10),
                if (role == "user") dp(14) else 0,
                dp(10)
            )

            if (role == "user") {
                background = rounded(panel, 19)
            }
        }

        val messageView = renderMarkdown(text)

        content.addView(messageView)

        row.addView(
            content,
            LinearLayout.LayoutParams(
                if (role == "user") {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    ViewGroup.LayoutParams.MATCH_PARENT
                },
                -2
            )
        )

        if (role == "assistant") {

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
                setPadding(0, dp(5), 0, 0)
            }

            actions.addView(
                createAction("Copy") {
                    copyText(text)
                }
            )

            actions.addView(
                createAction("Like") {
                    Toast.makeText(
                        this@MainActivity,
                        "Terima kasih atas penilaiannya",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            actions.addView(
                createAction("Dislike") {
                    Toast.makeText(
                        this@MainActivity,
                        "Masukan diterima",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            actions.addView(
                createAction("Regenerate") {
                    regenerateAnswer()
                }
            )

            row.addView(actions)
        }

        chatLayout.addView(row)
        animateView(row)
        scrollToBottom()
    }

    private fun renderMarkdown(text: String): LinearLayout {

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val lines = text.split("\n")

        var index = 0
        var inCode = false
        val codeLines = mutableListOf<String>()

        while (index < lines.size) {

            val line = lines[index]

            if (line.trim().startsWith("```")) {

                if (!inCode) {
                    inCode = true
                    codeLines.clear()
                } else {
                    addCodeBlock(
                        container,
                        codeLines.joinToString("\n")
                    )
                    inCode = false
                }

                index++
                continue
            }

            if (inCode) {
                codeLines.add(line)
                index++
                continue
            }

            when {

                line.startsWith("### ") -> {
                    addMarkdownText(
                        container,
                        line.removePrefix("### "),
                        17f,
                        true
                    )
                }

                line.startsWith("## ") -> {
                    addMarkdownText(
                        container,
                        line.removePrefix("## "),
                        19f,
                        true
                    )
                }

                line.startsWith("# ") -> {
                    addMarkdownText(
                        container,
                        line.removePrefix("# "),
                        23f,
                        true
                    )
                }

                line.startsWith("- ") ||
                line.startsWith("* ") -> {

                    addListItem(
                        container,
                        line.substring(2),
                        false
                    )
                }

                Regex("^\\d+\\.\\s").containsMatchIn(line) -> {

                    val number =
                        line.substringBefore(".")

                    val content =
                        line.substringAfter(".").trim()

                    addListItem(
                        container,
                        content,
                        true,
                        number
                    )
                }

                line.isBlank() -> {
                    val space = View(this)
                    container.addView(
                        space,
                        LinearLayout.LayoutParams(
                            1,
                            dp(5)
                        )
                    )
                }

                else -> {
                    addMarkdownText(
                        container,
                        line,
                        15f,
                        false
                    )
                }
            }

            index++
        }

        if (inCode && codeLines.isNotEmpty()) {
            addCodeBlock(
                container,
                codeLines.joinToString("\n")
            )
        }

        return container
    }

    private fun addMarkdownText(
        parent: LinearLayout,
        text: String,
        size: Float,
        bold: Boolean
    ) {

        val view = TextView(this).apply {
            this.text = parseInlineMarkdown(text)
            textSize = size
            setTextColor(white)
            setLineSpacing(dp(3).toFloat(), 1f)

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }

            setPadding(0, dp(2), 0, dp(2))
        }

        parent.addView(view)
    }

    private fun addListItem(
        parent: LinearLayout,
        text: String,
        numbered: Boolean,
        number: String = ""
    ) {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, dp(3), 0, dp(3))
        }

        val marker = TextView(this).apply {

            this.text = if (numbered) {
                "$number."
            } else {
                "•"
            }

            textSize = 16f
            setTextColor(purple)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dp(9), 0)
        }

        val content = TextView(this).apply {
            this.text = parseInlineMarkdown(text)
            textSize = 15f
            setTextColor(white)
            setLineSpacing(dp(3).toFloat(), 1f)
        }

        row.addView(marker)

        row.addView(
            content,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        parent.addView(row)
    }

    private fun parseInlineMarkdown(
        text: String
    ): SpannableStringBuilder {

        val result = SpannableStringBuilder()
        val regex = Regex(
            "\\*\\*(.+?)\\*\\*|\\*(.+?)\\*|`(.+?)`"
        )

        var lastIndex = 0

        regex.findAll(text).forEach { match ->

            result.append(
                text.substring(lastIndex, match.range.first)
            )

            val value =
                match.groups[1]?.value
                    ?: match.groups[2]?.value
                    ?: match.groups[3]?.value
                    ?: ""

            val start = result.length
            result.append(value)
            val end = result.length

            when {
                match.groups[1] != null -> {
                    result.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                match.groups[2] != null -> {
                    result.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                match.groups[3] != null -> {
                    result.setSpan(
                        TypefaceSpan("monospace"),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    result.setSpan(
                        ForegroundColorSpan(purple),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            lastIndex = match.range.last + 1
        }

        result.append(text.substring(lastIndex))

        return result
    }

    private fun addCodeBlock(
        parent: LinearLayout,
        code: String
    ) {

        val codeView = TextView(this).apply {
            text = code
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.rgb(220, 210, 255))
            setPadding(
                dp(13),
                dp(13),
                dp(13),
                dp(13)
            )
            background = rounded(
                Color.rgb(30, 27, 42),
                12
            )
            setTextIsSelectable(true)
        }

        val params = LinearLayout.LayoutParams(
            -1,
            -2
        )

        params.setMargins(0, dp(7), 0, dp(7))

        parent.addView(codeView, params)
    }
        private fun createAction(
        label: String,
        action: () -> Unit
    ): TextView {

        return TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(muted)
            setPadding(
                dp(9),
                dp(7),
                dp(9),
                dp(7)
            )

            background = rounded(
                Color.TRANSPARENT,
                10
            )

            setOnClickListener {
                action()
            }
        }
    }

    private fun copyText(text: String) {

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText("AI-XD", text)
        )

        Toast.makeText(
            this,
            "Teks berhasil disalin",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun regenerateAnswer() {

        if (isLoading) return

        if (messages.isEmpty()) return

        if (messages.last().first == "assistant") {
            messages.removeAt(messages.lastIndex)
        }

        if (messages.isEmpty()) return

        chatLayout.removeAllViews()

        messages.forEach { message ->
            addBubble(
                message.first,
                message.second
            )
        }

        setLoading(true)
        showThinking()
        requestAI()
    }

    private fun showThinking() {

        removeThinking()

        val thinking = TextView(this).apply {
            text = "AI-XD sedang berpikir..."
            textSize = 13f
            setTextColor(muted)
            setPadding(
                dp(3),
                dp(12),
                dp(3),
                dp(12)
            )
        }

        thinkingView = thinking
        chatLayout.addView(thinking)

        thinkingAnimator = ValueAnimator.ofFloat(
            0.35f,
            1f
        ).apply {

            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener {
                thinking.alpha = it.animatedValue as Float
            }

            start()
        }

        scrollToBottom()
    }

    private fun removeThinking() {

        thinkingAnimator?.cancel()
        thinkingAnimator = null

        thinkingView?.let {
            chatLayout.removeView(it)
        }

        thinkingView = null
    }

    private fun setLoading(loading: Boolean) {

        isLoading = loading

        sendButton.isEnabled = !loading
        input.isEnabled = !loading

        sendButton.alpha =
            if (loading) 0.5f else 1f

        statusText.text =
            if (loading) {
                "● AI-XD sedang berpikir"
            } else {
                "● AI-XD 4.0"
            }
    }

    private fun newConversation() {

        if (isLoading) return

        saveCurrentSession()

        messages.clear()
        chatLayout.removeAllViews()

        showWelcome()

        Toast.makeText(
            this,
            "Percakapan baru dibuat",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveCurrentSession() {

        if (messages.isEmpty()) return

        val title = messages.firstOrNull {
            it.first == "user"
        }?.second ?: "Percakapan baru"

        sessions.add(
            0,
            ChatSession(
                title.take(35),
                messages.toList()
            )
        )
    }

    private fun showHistory() {

        if (sessions.isEmpty()) {

            Toast.makeText(
                this,
                "Belum ada riwayat percakapan",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val popup = PopupMenu(
            this,
            historyButton
        )

        popup.menu.add(
            0,
            -1,
            0,
            "Riwayat Percakapan"
        ).isEnabled = false

        sessions.forEachIndexed { index, session ->

            popup.menu.add(
                0,
                index,
                index + 1,
                session.title
            )
        }

        popup.setOnMenuItemClickListener { item ->

            val index = item.itemId

            if (index in sessions.indices) {
                restoreSession(index)
                true
            } else {
                false
            }
        }

        popup.show()
    }

    private fun restoreSession(index: Int) {

        if (index !in sessions.indices) return

        val session = sessions[index]

        messages.clear()
        messages.addAll(session.messages)

        chatLayout.removeAllViews()

        messages.forEach { message ->
            addBubble(
                message.first,
                message.second
            )
        }

        scrollToBottom()
    }

    private fun hideKeyboard() {

        val manager =
            getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager

        manager.hideSoftInputFromWindow(
            input.windowToken,
            0
        )
    }

    private fun scrollToBottom() {

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun animateView(view: View) {

        view.alpha = 0f
        view.translationY = dp(8).toFloat()

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {

        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    override fun onDestroy() {

        thinkingAnimator?.cancel()
        executor.shutdownNow()

        super.onDestroy()
    }
}
