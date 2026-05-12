package com.sahyadri.samrakshane

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardFire = findViewById<LinearLayout>(R.id.cardFire)
        val cardLandslide = findViewById<LinearLayout>(R.id.cardLandslide)
        val cardLogging = findViewById<LinearLayout>(R.id.cardLogging)
        val cardWildlife = findViewById<LinearLayout>(R.id.cardWildlife)
        val navAlerts = findViewById<LinearLayout>(R.id.navAlerts)
        val navEducation = findViewById<LinearLayout>(R.id.navEducation)

        cardFire.setOnClickListener { openReport("Forest Fire") }
        cardLandslide.setOnClickListener { openReport("Landslide") }
        cardLogging.setOnClickListener { openReport("Illegal Logging") }
        cardWildlife.setOnClickListener { openReport("Wildlife Sighting") }

        navAlerts.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }
        navEducation.setOnClickListener {
            startActivity(Intent(this, EducationActivity::class.java))
        }
    }

    private fun openReport(alertType: String) {
        val intent = Intent(this, ReportActivity::class.java)
        intent.putExtra("alertType", alertType)
        startActivity(intent)
    }
}