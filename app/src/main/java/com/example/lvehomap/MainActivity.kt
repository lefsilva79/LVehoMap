package com.example.lvehomap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var captureButton: Button
    private lateinit var startNavigationButton: Button
    private val deliveryPoints = mutableListOf<DeliveryPoint>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa os botões
        captureButton = findViewById(R.id.captureButton)
        startNavigationButton = findViewById(R.id.startNavigationButton)

        // Inicializa o fragmento do mapa
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.map, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        // Inicializa o cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configura os listeners dos botões
        setupButtons()
    }

    private fun setupButtons() {
        captureButton.setOnClickListener {
            getCurrentLocation()
        }

        startNavigationButton.setOnClickListener {
            if (deliveryPoints.isNotEmpty()) {
                navigateToFirstPoint()
            } else {
                Toast.makeText(this, "Capture alguns pontos primeiro!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Adiciona o ponto à lista
                    val point = DeliveryPoint(
                        deliveryPoints.size + 1,
                        it.latitude,
                        it.longitude
                    )
                    deliveryPoints.add(point)

                    // Adiciona marcador no mapa
                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Ponto ${point.number}")
                    )

                    Toast.makeText(this, "Ponto ${point.number} capturado!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToFirstPoint() {
        val point = deliveryPoints.firstOrNull()
        if (point != null) {
            val gmmIntentUri = Uri.parse("google.navigation:q=${point.latitude},${point.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Maps não instalado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    Toast.makeText(this, "Permissão de localização necessária", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    data class DeliveryPoint(
        val number: Int,
        val latitude: Double,
        val longitude: Double
    )
}