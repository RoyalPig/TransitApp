package com.example.transitapp.ui.Alerts

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.transitapp.databinding.FragmentAlertsBinding
import com.google.transit.realtime.GtfsRealtime
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Fetch and display alerts in the background thread
        Thread(Runnable {
            fetchAndDisplayAlerts()
        }).start()
    }

    private fun fetchAndDisplayAlerts() {
        try {
            val alerts = fetchAlerts()
            handler.post {
                displayAlerts(alerts)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle exceptions like showing an error message to the user
        }
    }

    private fun fetchAlerts(): List<String> {
        val alerts = mutableListOf<String>()
        val url = "https://gtfs.halifax.ca/realtime/Alert/Alerts.pb"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val feed = GtfsRealtime.FeedMessage.parseFrom(response.body?.byteStream())
            for (entity in feed.entityList) {
                if (entity.hasAlert()) {
                    val alertText = entity.alert.descriptionText.translationList.joinToString(" ") { it.text }
                    alerts.add(alertText)
                }
            }
        }

        return alerts
    }

    private fun displayAlerts(alerts: List<String>) {
        val alertsContainer: LinearLayout = binding.linearLayoutAlerts
        alertsContainer.removeAllViews()

        for (alert in alerts) {
            val textView = TextView(context).apply {
                text = alert
                textSize = 16f
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            alertsContainer.addView(textView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
