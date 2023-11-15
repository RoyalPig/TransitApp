package com.example.transitapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY

class StartActivity : AppCompatActivity() {
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private val REQUEST_CODE = 100

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)


        //Setup Location Services
        //Ask permission and get the location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //Check to see if this is the location permission 100
        if (requestCode == REQUEST_CODE) {
            //Check to see if permission granted or denied
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            }

        }
    }

    private fun getLocation() {
        //Permission already granted, get location
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("TESTING", "Hurray, you did it")
            fusedLocationProviderClient?.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null)
                ?.addOnSuccessListener { location: Location? ->
                    location?.let {
                        // Create an Intent to start MainActivity
                        val intent = Intent(this@StartActivity, MainActivity::class.java).apply {
                            // Pass location data
                            putExtra("latitude", location.latitude)
                            putExtra("longitude", location.longitude)
                        }
                        startActivity(intent)
                        finish() // Finish StartActivity after starting MainActivity
                    }
                    Log.i("LocationInfo", "Latitude: ${location?.latitude}, Longitude: ${location?.longitude}")
                }


        } else {
            //Ask for permission
            askPermission()
        }
    }

    private fun askPermission() {
        //Request location permission from user
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE
        )
    }

}