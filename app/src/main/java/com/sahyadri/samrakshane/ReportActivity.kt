package com.sahyadri.samrakshane

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class ReportActivity : AppCompatActivity() {

    private lateinit var tvGpsCoordinates: TextView
    private lateinit var tvAlertTypeTitle: TextView
    private lateinit var tvPhotoStatus: TextView
    private lateinit var etDescription: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnTakePhoto: LinearLayout
    private lateinit var ivPhotoPreview: ImageView
    private var alertType = ""
    private var latitude = 0.0
    private var longitude = 0.0
    private var photoUri: Uri? = null
    private lateinit var photoFile: File

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 101
        const val REQUEST_CAMERA_PERMISSION = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        alertType = intent.getStringExtra("alertType") ?: ""

        tvAlertTypeTitle = findViewById(R.id.tvAlertTypeTitle)
        tvGpsCoordinates = findViewById(R.id.tvGpsCoordinates)
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus)
        etDescription = findViewById(R.id.etDescription)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview)

        if (alertType.isNotEmpty()) {
            tvAlertTypeTitle.text = "Report: $alertType"
        }

        getLocation()

        btnTakePhoto.setOnClickListener {
            openCamera()
        }

        btnSubmit.setOnClickListener {
            val description = etDescription.text.toString().trim()
            if (description.isEmpty()) {
                Toast.makeText(this, "Please enter a description!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitAlert(description)
        }
    }

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        photoFile = File.createTempFile("ALERT_${timeStamp}_", ".jpg", storageDir)
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ivPhotoPreview.setImageURI(photoUri)
            ivPhotoPreview.visibility = View.VISIBLE
            tvPhotoStatus.visibility = View.VISIBLE
            btnTakePhoto.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else if (requestCode == 100 &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        }
    }

    private fun submitAlert(description: String) {
        if (isInternetAvailable()) {
            saveToFirebase(description)
        } else {
            saveLocally(description)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            val info = cm.activeNetworkInfo
            info != null && info.isConnected
        }
    }

    private fun saveToFirebase(description: String) {
        Toast.makeText(this, "⏳ Submitting alert...", Toast.LENGTH_SHORT).show()

        val database = FirebaseDatabase.getInstance().reference
        val alertId = database.child("alerts").push().key ?: run {
            saveLocally(description)
            return
        }

        val alertData = hashMapOf(
            "type" to alertType,
            "description" to description,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis(),
            "status" to "Reported",
            "hasPhoto" to (photoUri != null)
        )

        database.child("alerts").child(alertId).setValue(alertData)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Alert submitted successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "📦 No internet! Saving locally...", Toast.LENGTH_SHORT).show()
                saveLocally(description)
            }
    }

    private fun saveLocally(description: String) {
        val alert = AlertEntity(
            alertType, description, latitude, longitude,
            System.currentTimeMillis()
        )
        Executors.newSingleThreadExecutor().execute {
            try {
                AlertDatabase.getInstance(this).alertDao().insertAlert(alert)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncWork = OneTimeWorkRequest.Builder(SyncWorker::class.java)
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(this).enqueue(syncWork)
                runOnUiThread {
                    Toast.makeText(this,
                        "✅ Alert saved offline! Will auto-upload when internet returns.",
                        Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                tvGpsCoordinates.text = "%.4f° N, %.4f° E · Auto-detected".format(latitude, longitude)
            } else {
                tvGpsCoordinates.text = "GPS location not available"
            }
        }
    }
}