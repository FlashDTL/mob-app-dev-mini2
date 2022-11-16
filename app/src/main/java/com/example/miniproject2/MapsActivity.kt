package com.example.miniproject2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.miniproject2.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationHelper: LocationHelper

    companion object{
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationHelper = LocationHelper(applicationContext)
    }


    val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult) {
            val location = result.locations[0]
            val latlng = LatLng(location.latitude, location.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16F))

            Ion.with(applicationContext)
                .load("https://en.wikipedia.org/w/api.php?action=query&&generator=geosearch&prop=coordinates|pageimages|description|info&&pithumbsize=400&ggsradius=500&ggslimit=10&format=json")
                .addQuery("ggscoord", "${location.latitude}|${location.longitude}")
                .asJsonObject()
                .setCallback{e, result ->
                    if (e != null){
                        Log.e("ERROR", "something went wrong")
                    }
                    else{
                        handleResponse(result)
                    }
                }

        }
    }
    override fun onResume() {
        super.onResume()
        locationHelper.requestLocationUpdates(locationCallback)
    }

    override fun onPause(){
        super.onPause()
        locationHelper.stopLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true
        mMap.setOnMyLocationClickListener(this)

    }

    private fun handleResponse(result: JsonObject) { //get nearby places
        val pages = result.get("query").asJsonObject.get("pages").asJsonObject
        for (key in pages.keySet()){
            val title = pages.get(key.toString()).asJsonObject.get("title").asString
            val lat = pages.get(key.toString()).asJsonObject.get("coordinates").asJsonArray.get(0).asJsonObject.get("lat").asDouble
            val lon = pages.get(key.toString()).asJsonObject.get("coordinates").asJsonArray.get(0).asJsonObject.get("lon").asDouble
            val latlon = LatLng(lat, lon)
            mMap.addMarker(
                MarkerOptions().position(latlon).title(title)
            )
        }


    }

    override fun onMyLocationClick(location: Location) {

    }
}