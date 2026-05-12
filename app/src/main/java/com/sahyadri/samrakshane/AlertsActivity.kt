package com.sahyadri.samrakshane

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AlertsActivity : AppCompatActivity() {

    private lateinit var alertsContainer: LinearLayout
    private lateinit var tvNoAlerts: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        alertsContainer = findViewById(R.id.alertsContainer)
        tvNoAlerts = findViewById(R.id.tvNoAlerts)

        loadAlertsFromFirebase()
    }

    private fun loadAlertsFromFirebase() {
        val database = FirebaseDatabase.getInstance().getReference("alerts")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                alertsContainer.removeAllViews()

                if (!snapshot.exists()) {
                    tvNoAlerts.visibility = View.VISIBLE
                    return
                }

                tvNoAlerts.visibility = View.GONE

                for (alertSnapshot in snapshot.children) {
                    val type = alertSnapshot.child("type").getValue(String::class.java) ?: "Unknown"
                    val description = alertSnapshot.child("description").getValue(String::class.java) ?: ""
                    val latitude = alertSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = alertSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val status = alertSnapshot.child("status").getValue(String::class.java) ?: "Reported"

                    addAlertCard(type, description, latitude, longitude, status)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AlertsActivity,
                    "Failed to load alerts!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addAlertCard(type: String, description: String,
                             latitude: Double, longitude: Double, status: String) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(32, 24, 32, 24)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(16, 8, 16, 8)
        card.layoutParams = params
        card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)

        val emoji = when (type) {
            "Forest Fire" -> "🔥"
            "Landslide" -> "⛰️"
            "Illegal Logging" -> "🪓"
            "Wildlife Sighting" -> "🐆"
            else -> "📍"
        }

        val tvType = TextView(this)
        tvType.text = "$emoji $type"
        tvType.textSize = 16f
        tvType.setTypeface(null, android.graphics.Typeface.BOLD)
        tvType.setTextColor(resources.getColor(R.color.forest_dark, null))

        val tvDesc = TextView(this)
        tvDesc.text = description
        tvDesc.textSize = 13f
        tvDesc.setPadding(0, 4, 0, 4)

        val tvLocation = TextView(this)
        tvLocation.text = "📍 %.4f° N, %.4f° E".format(latitude, longitude)
        tvLocation.textSize = 12f
        tvLocation.setTextColor(resources.getColor(R.color.forest_dark, null))

        val tvStatus = TextView(this)
        tvStatus.text = status
        tvStatus.textSize = 12f
        tvStatus.setPadding(16, 8, 16, 8)

        val statusColor = when (status) {
            "Reported" -> 0xFFE65100.toInt()
            "Verified" -> 0xFF2C5F2D.toInt()
            "Team Dispatched" -> 0xFF1565C0.toInt()
            else -> 0xFF757575.toInt()
        }
        tvStatus.setTextColor(statusColor)

        card.addView(tvType)
        card.addView(tvDesc)
        card.addView(tvLocation)
        card.addView(tvStatus)

        alertsContainer.addView(card)
    }
}