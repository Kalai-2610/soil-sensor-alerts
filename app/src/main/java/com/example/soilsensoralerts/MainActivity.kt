package com.example.soilsensoralerts

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class SensorAlert(
    val timestamp: String,
    val fieldId: String,
    val crop: String,
    val message: String
)

class MainActivity : AppCompatActivity() {
    private val apiBase =
        "https://wh-integration-assets.onrender.com/open/v1/sensor-result"

    private val client = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var listContainer: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var pageText: TextView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var refresh: SwipeRefreshLayout

    private var page = 1
    private val size = 10
    private var total = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadPage()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F7F5.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 28, 24, 20)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        val title = TextView(this).apply {
            text = "Soil Sensor Alerts"
            textSize = 23f
            setTextColor(0xFF1B1B1B.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

        val reload = Button(this).apply {
            text = "Refresh"
            setOnClickListener { loadPage() }
        }
        header.addView(reload)

        root.addView(header)

        refresh = SwipeRefreshLayout(this).apply {
            setColorSchemeColors(0xFF2E7D32.toInt())
        }

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        progress = ProgressBar(this).apply {
            visibility = View.GONE
        }
        listContainer.addView(progress, LinearLayout.LayoutParams(-1, 60))

        emptyText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(16, 50, 16, 50)
            visibility = View.GONE
        }
        listContainer.addView(emptyText)

        scroll.addView(listContainer)
        refresh.addView(scroll)
        refresh.setOnRefreshListener { loadPage() }

        root.addView(refresh, LinearLayout.LayoutParams(-1, 0, 1f))

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 12)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        prevButton = Button(this).apply {
            text = "← Previous"
            setOnClickListener {
                if (page > 1) {
                    page--
                    loadPage()
                }
            }
        }

        pageText = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 15f
        }

        nextButton = Button(this).apply {
            text = "Next →"
            setOnClickListener {
                if (page * size < total) {
                    page++
                    loadPage()
                }
            }
        }

        navigation.addView(prevButton, LinearLayout.LayoutParams(0, -2, 1f))
        navigation.addView(pageText, LinearLayout.LayoutParams(0, -2, 1f))
        navigation.addView(nextButton, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(navigation)

        setContentView(root)
    }

    private fun loadPage() {
        progress.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        refresh.isRefreshing = true

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    fetch(page)
                }
                total = result.first
                showAlerts(result.second)
                updatePagination()
            } catch (e: Exception) {
                emptyText.text = "Unable to load alerts.\n${e.message ?: "Unknown error"}"
                emptyText.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "API request failed", Toast.LENGTH_SHORT).show()
            } finally {
                progress.visibility = View.GONE
                refresh.isRefreshing = false
            }
        }
    }

    private fun fetch(pageNumber: Int): Pair<Int, List<SensorAlert>> {
        val url = "$apiBase?page=$pageNumber&size=$size&sortOrder=desc&sortBy=timestamp"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }

            val body = response.body?.string()
                ?: throw Exception("Empty API response")

            val json = JSONObject(body)
            if (!json.optBoolean("success", false)) {
                throw Exception(json.optString("error", "API returned success=false"))
            }

            val pagination = json.optJSONObject("pagination")
            val totalCount = pagination?.optInt("total", 0) ?: 0
            val data = json.optJSONArray("data")

            val alerts = mutableListOf<SensorAlert>()
            if (data != null) {
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    alerts.add(
                        SensorAlert(
                            timestamp = item.optString("timestamp", "-"),
                            fieldId = item.optString("field_id", "-"),
                            crop = item.optString("crop", "-"),
                            message = item.optString("sms_alert_message", "No alert message")
                        )
                    )
                }
            }
            return totalCount to alerts
        }
    }

    private fun showAlerts(alerts: List<SensorAlert>) {
        // Remove old cards while preserving progress and empty views.
        listContainer.removeViews(2, (listContainer.childCount - 2).coerceAtLeast(0))

        if (alerts.isEmpty()) {
            emptyText.text = "No sensor alerts found."
            emptyText.visibility = View.VISIBLE
            return
        }

        emptyText.visibility = View.GONE
        alerts.forEach { addCard(it) }
    }

    private fun addCard(alert: SensorAlert) {
        val card = MaterialCardView(this).apply {
            radius = 18f
            cardElevation = 2f
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            strokeWidth = 1
            strokeColor = 0xFFE1E5E1.toInt()
            setContentPadding(18, 18, 18, 18)
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val timestamp = TextView(this).apply {
            text = formatTimestamp(alert.timestamp)
            textSize = 14f
            setTextColor(0xFF666666.toInt())
        }

        val fieldCrop = TextView(this).apply {
            text = "${alert.fieldId}  •  ${alert.crop}"
            textSize = 19f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1B1B1B.toInt())
            setPadding(0, 7, 0, 10)
        }

        val label = TextView(this).apply {
            text = "SMS ALERT"
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF2E7D32.toInt())
        }

        val message = TextView(this).apply {
            text = alert.message
            textSize = 16f
            setTextColor(0xFF333333.toInt())
            setLineSpacing(2f, 1.05f)
            setPadding(0, 7, 0, 0)
        }

        box.addView(timestamp)
        box.addView(fieldCrop)
        box.addView(label)
        box.addView(message)
        card.addView(box)

        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 0, 0, 14)
        listContainer.addView(card, lp)
    }

    private fun updatePagination() {
        val pages = if (total <= 0) 1 else ((total + size - 1) / size)
        pageText.text = "$page / $pages"
        prevButton.isEnabled = page > 1
        nextButton.isEnabled = page < pages
    }

    private fun formatTimestamp(value: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val output = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            output.format(input.parse(value)!!)
        } catch (_: Exception) {
            value
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}