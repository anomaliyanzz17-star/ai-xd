package com.nael.aixd

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
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
    private data class ChatSession(
        var title: String,
        val messages: MutableList<Pair<String, String>> = mutableListOf()
    )
    private val sessions = mutableListOf<ChatSession>()
    private var currentSession: ChatSession? = null
    private var drawerPopup: PopupWindow? = null
    private var drawerHistory: LinearLayout? = null
    private var drawerSearch: EditText? = null
    private var touchStartX = 0f
    private var touchStartY = 0f

    private lateinit var root: LinearLayout
    private lateinit var chatLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: TextView
    private lateinit var statusText: TextView

    private var isLoading = false
    private var thinkingView: View? = null

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

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }

        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }

        buildUI()
    }

    private fun buildUI() {

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            clipToPadding = false
        }

        root.setOnApplyWindowInsetsListener { view, insets ->

            if (Build.VERSION.SDK_INT >= 30) {

                val systemBars = insets.getInsets(
                    WindowInsets.Type.systemBars()
                )

                val ime = insets.getInsets(
                    WindowInsets.Type.ime()
                )

                val bottomInset = maxOf(
                    systemBars.bottom,
                    ime.bottom
                )

                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    bottomInset
                )

            } else {

                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }

            insets
        }

        // HEADER

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(19), dp(12), dp(19), dp(12))
            setBackgroundColor(Color.rgb(18, 18, 27))
        }

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = rounded(panel, 13)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { openSidebar() }
        }
        header.addView(menuButton)

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, 0, 0)
        }

        val title = TextView(this).apply {
            text = "AI-XD"
            textSize = 23f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        statusText = TextView(this).apply {
            text = "● AI-XD 4.1"
            textSize = 10f
            setTextColor(Color.rgb(180, 140, 255))
        }

        brand.addView(title)
        brand.addView(statusText)

        header.addView(
            brand,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val newChat = TextView(this).apply {
            text = "＋"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = rounded(panel, 15)
            setPadding(dp(12), dp(3), dp(12), dp(3))

            setOnClickListener {
                newConversation()
            }
        }

        header.addView(newChat)
        root.addView(header)

        // CHAT AREA

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

        // INPUT

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(10), dp(13), dp(8))
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
            text = "AI-XD 4.1 • Your AI Assistant"
            textSize = 10f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        bottom.addView(footer)
        root.addView(bottom)

        setContentView(root)

        root.post {
            root.requestApplyInsets()
        }

        showWelcome()
    }

    // SIDEBAR

    private fun openSidebar() {
        if (drawerPopup?.isShowing == true) return

        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(18))
            setBackgroundColor(Color.rgb(20, 20, 30))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "✦  AI-XD"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        }
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val close = TextView(this).apply {
            text = "✕"
            textSize = 20f
            setTextColor(muted)
            setPadding(dp(12), dp(8), dp(8), dp(8))
            setOnClickListener { drawerPopup?.dismiss() }
        }
        top.addView(close)
        drawer.addView(top)

        val newChatButton = TextView(this).apply {
            text = "＋   Chat baru"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            setPadding(dp(15), dp(14), dp(15), dp(14))
            background = rounded(Color.rgb(76, 48, 122), 15)
            setOnClickListener {
                drawerPopup?.dismiss()
                newConversation()
            }
        }
        val newParams = LinearLayout.LayoutParams(-1, -2)
        newParams.setMargins(0, dp(24), 0, dp(14))
        drawer.addView(newChatButton, newParams)

        drawerSearch = EditText(this).apply {
            hint = "Cari percakapan..."
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 14f
            singleLine = true
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(panel, 13)
        }
        drawer.addView(drawerSearch, LinearLayout.LayoutParams(-1, -2))

        val historyTitle = TextView(this).apply {
            text = "RIWAYAT CHAT"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(muted)
            setPadding(dp(4), dp(22), 0, dp(9))
        }
        drawer.addView(historyTitle)

        val historyScroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        drawerHistory = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        historyScroll.addView(drawerHistory)
        drawer.addView(historyScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        addSidebarLink(drawer, "♙   Profil", "Profil akun akan tersedia pada pembaruan berikutnya.")
        addSidebarLink(drawer, "⚙   Pengaturan", "Pengaturan sedang dikembangkan.")
        addSidebarLink(drawer, "文   Bahasa", "Pilihan bahasa akan tersedia pada pembaruan berikutnya.")
        addSidebarLink(drawer, "ⓘ   Tentang AI-XD", "AI-XD adalah asisten AI independen.")

        drawerPopup = PopupWindow(
            drawer,
            minOf(dp(320), (resources.displayMetrics.widthPixels * 0.86f).toInt()),
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            setBackgroundDrawable(rounded(Color.rgb(20, 20, 30), 0))
            isOutsideTouchable = true
            elevation = dp(18).toFloat()
            showAtLocation(root, Gravity.TOP or Gravity.START, 0, 0)
        }

        refreshSidebarHistory("")
        drawerSearch?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshSidebarHistory(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun addSidebarLink(parent: LinearLayout, label: String, message: String) {
        val item = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(white)
            setPadding(dp(8), dp(12), dp(8), dp(12))
            setOnClickListener {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
        parent.addView(item)
    }

    private fun refreshSidebarHistory(query: String) {
        val list = drawerHistory ?: return
        list.removeAllViews()
        val filtered = sessions.asReversed().filter {
            it.title.contains(query, ignoreCase = true)
        }
        if (filtered.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (query.isBlank()) "Belum ada riwayat chat" else "Chat tidak ditemukan"
                textSize = 13f
                setTextColor(muted)
                setPadding(dp(7), dp(12), dp(7), dp(12))
            }
            list.addView(empty)
            return
        }
        filtered.forEach { session ->
            val item = TextView(this).apply {
                text = "▤   ${session.title}"
                textSize = 13f
                setTextColor(white)
                maxLines = 2
                setPadding(dp(10), dp(13), dp(10), dp(13))
                background = rounded(Color.rgb(28, 28, 40), 12)
                setOnClickListener {
                    restoreSession(session)
                    drawerPopup?.dismiss()
                }
            }
            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 0, 0, dp(7))
            list.addView(item, params)
        }
    }

    private fun saveCurrentSession() {
        val current = currentSession ?: return
        current.messages.clear()
        current.messages.addAll(messages)
        if (current.messages.isNotEmpty() && sessions.none { it === current }) {
            sessions.add(current)
        }
        if (drawerPopup?.isShowing == true) {
            refreshSidebarHistory(drawerSearch?.text?.toString().orEmpty())
        }
    }

    private fun restoreSession(session: ChatSession) {
        if (isLoading) return
        saveCurrentSession()
        currentSession = session
        messages.clear()
        messages.addAll(session.messages)
        chatLayout.removeAllViews()
        if (messages.isEmpty()) {
            showWelcome()
        } else {
            messages.forEach { addBubble(it.first, it.second) }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.rawX
                touchStartY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.rawX - touchStartX
                val dy = kotlin.math.abs(event.rawY - touchStartY)
                if (dy < dp(90)) {
                    if (drawerPopup?.isShowing == true && dx < -dp(90)) {
                        drawerPopup?.dismiss()
                    } else if (drawerPopup?.isShowing != true && touchStartX < dp(24) && dx > dp(100)) {
                        openSidebar()
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    // WELCOME

    private fun showWelcome() {

        chatLayout.removeAllViews()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(55), dp(12), dp(35))
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

    // SEND MESSAGE

    private fun sendMessage() {

        if (isLoading) return

        val question = input.text.toString().trim()

        if (question.isEmpty()) return

        if (question.length > 6000) {
            Toast.makeText(
                this,
                "Pesan terlalu panjang.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (messages.isEmpty()) {
            chatLayout.removeAllViews()
        }

        input.text.clear()
        hideKeyboard()

        if (currentSession == null) {
            currentSession = ChatSession(question.take(38))
        } else if (currentSession?.messages?.isEmpty() == true) {
            currentSession?.title = question.take(38)
        }
        messages.add(Pair("user", question))
        saveCurrentSession()
        addBubble("user", question)

        requestAI()
    }

    // AI REQUEST

    private fun requestAI() {

        if (isLoading) return

        setLoading(true)
        showThinking()

        executor.execute {

            var connection: HttpURLConnection? = null

            try {

                connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
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

                messages.takeLast(30).forEach { message ->

                    val item = JSONObject()
                    item.put("role", message.first)
                    item.put("content", message.second)

                    jsonMessages.put(item)
                }

                val body = JSONObject()
                body.put("messages", jsonMessages)

                connection.outputStream.use {
                    it.write(
                        body.toString().toByteArray(Charsets.UTF_8)
                    )
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

                if (responseCode !in 200..299) {
                    throw Exception("HTTP $responseCode: $response")
                }

                val result = JSONObject(response)

                if (!result.optBoolean("success", true)) {
                    throw Exception(
                        result.optString(
                            "error",
                            "AI gagal menjawab."
                        )
                    )
                }

                val reply = result.optString("reply", "")

                if (reply.isBlank()) {
                    throw Exception("Jawaban AI kosong.")
                }

                runOnUiThread {

                    removeThinking()
                    messages.add(Pair("assistant", reply))
                    saveCurrentSession()
                    setLoading(false)
                    addBubble("assistant", reply)
                }

            } catch (e: Exception) {

                runOnUiThread {

                    removeThinking()
                    setLoading(false)

                    addBubble(
                        "assistant",
                        "AI-XD gagal menjawab.\n\n${e.message}"
                    )
                }

            } finally {
                connection?.disconnect()
            }
        }
    }

    // CHAT DISPLAY

    private fun addBubble(
        role: String,
        message: String
    ) {

        if (role == "user") {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(8), 0, dp(8))
            }

            val bubble = TextView(this).apply {
                text = message
                textSize = 15f
                setTextColor(white)
                setPadding(dp(15), dp(12), dp(15), dp(12))
                background = rounded(
                    Color.rgb(91, 57, 155),
                    19
                )
            }

            val params = LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.78f).toInt(),
                -2
            )

            row.addView(bubble, params)
            chatLayout.addView(row)

        } else {

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(2), dp(15), dp(2), dp(15))
            }

            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val name = TextView(this).apply {
                text = "✦  AI-XD"
                textSize = 14f
                setTextColor(Color.rgb(190, 155, 255))
                typeface = Typeface.DEFAULT_BOLD
            }

            
            header.addView(
                name,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            val answer = TextView(this).apply {
                text = message
                textSize = 15f
                setTextColor(white)
                setLineSpacing(dp(3).toFloat(), 1f)
                setPadding(0, dp(9), 0, dp(5))
                setTextIsSelectable(true)
            }

            container.addView(header)
            container.addView(answer)

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, 0)
            }

            actions.addView(
                createAction("👍") {
                    Toast.makeText(
                        this@MainActivity,
                        "Terima kasih atas penilaiannya",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            actions.addView(
                createAction("👎") {
                    Toast.makeText(
                        this@MainActivity,
                        "Masukan diterima",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            actions.addView(
                createAction("Salin") {
                    copyText(message)
                }
            )

            actions.addView(
                createAction("↻") {
                    regenerateAnswer()
                }
            )

            container.addView(actions)
            chatLayout.addView(container)
        }

        scrollToBottom()
    }

    private fun createAction(
        label: String,
        action: () -> Unit
    ): TextView {

        return TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(8), dp(9), dp(8))

            setOnClickListener {
                action()
            }
        }
    }

    // COPY

    private fun copyText(message: String) {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText("AI-XD", message)
        )

        Toast.makeText(
            this,
            "Jawaban disalin",
            Toast.LENGTH_SHORT
        ).show()
    }

    // REGENERATE

    private fun regenerateAnswer() {

        if (isLoading) return
        if (messages.isEmpty()) return

        if (messages.last().first == "assistant") {
            messages.removeAt(messages.lastIndex)

            if (chatLayout.childCount > 0) {
                chatLayout.removeViewAt(
                    chatLayout.childCount - 1
                )
            }
        }

        if (messages.isEmpty()) return
        if (messages.last().first != "user") return

        requestAI()
    }

    // THINKING

    private fun showThinking() {

        removeThinking()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            setPadding(0, dp(12), 0, dp(12))
        }

        val bubble = TextView(this).apply {
            text = "✦  AI-XD sedang berpikir..."
            textSize = 13f
            setTextColor(Color.rgb(205, 185, 240))
            setPadding(dp(15), dp(12), dp(15), dp(12))
            background = rounded(
                Color.rgb(35, 29, 49),
                18
            )
        }

        row.addView(bubble)
        chatLayout.addView(row)
        thinkingView = row

        scrollToBottom()
    }

    private fun removeThinking() {

        thinkingView?.let {
            chatLayout.removeView(it)
        }

        thinkingView = null
    }

    // LOADING

    private fun setLoading(loading: Boolean) {

        isLoading = loading
        sendButton.isEnabled = !loading
        sendButton.alpha = if (loading) 0.5f else 1f

        statusText.text =
            if (loading) "● AI SEDANG BERPIKIR..."
            else "● AI-XD 4.1"
    }

    // NEW CHAT

    private fun newConversation() {

        if (isLoading) return

        saveCurrentSession()
        messages.clear()
        currentSession = null
        removeThinking()
        showWelcome()
    }

    // KEYBOARD

    private fun hideKeyboard() {

        val manager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        manager.hideSoftInputFromWindow(
            input.windowToken,
            0
        )
    }

    // SCROLL

    private fun scrollToBottom() {

        scrollView.post {
            scrollView.smoothScrollTo(
                0,
                chatLayout.height
            )
        }
    }

    // DESIGN

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
    executor.shutdownNow()
    super.onDestroy()
}
}
