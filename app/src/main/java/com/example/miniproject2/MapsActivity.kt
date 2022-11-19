package com.example.miniproject2
// Some sources:  https://stackoverflow.com/questions/43974986/how-to-set-click-listener-on-marker-in-google-map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.miniproject2.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
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

        // Checking and asking for permissions:
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }

        locationHelper = LocationHelper(applicationContext)     // This should go AFTER given permissions. Otherwise location tracking does not work
    }

    override fun onResume() {
        super.onResume()
        locationHelper.requestLocationUpdates(locationCallback)
    }

    override fun onPause(){
        super.onPause()
//        mMap.clear()
        locationHelper.stopLocationUpdates()
    }



    //Helpers-variables:
    var prevLat  = 0.0
    var prevLon  = 0.0

    /**
     * This function is triggered every 5s (as a parameter in LocationRequest.Builder in LocationHelper).
     * When location (significantly) changes:
     * - Zoom on the new location,
     * - Send request to wiki about surrounding Points Of Interest,
     * - Triggers handleResponse-function to handle the wiki-response.
     */
    val locationCallback = object : LocationCallback(){

        override fun onLocationResult(result: LocationResult) {
            Log.i("onLocationResult", "!!!!!")
            val location = result.locations[0]  //The size of the list is always 1. Always only 0th elem, no 1st.

            var lat = location.latitude
            var lon = location.longitude

            // Checking if location did change. Error is +/- 0.0005:
            if (prevLat-0.0005 > lat || lat > prevLat+0.0005   ||   prevLon-0.0005 > lon || lon > prevLon+0.0005) {
                val latlon = LatLng(lat, lon)
                Log.i("Check1", "Location Changed to: $latlon")

                prevLat = location.latitude
                prevLon = location.longitude

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlon, 14F))     //Zoom to new location

                // Making request from Wikipedia about surrounding Points of interest:
                Ion.with(applicationContext)
                    .load("https://en.wikipedia.org/w/api.php?action=query&&generator=geosearch&prop=coordinates|pageimages|description|info&&pithumbsize=400&ggsradius=500&ggslimit=10&format=json")
                    .addQuery("ggscoord", "${location.latitude}|${location.longitude}")
                    .asJsonObject()
                    .setCallback{e, result ->
                        if (e != null){
                            Log.e("ERROR", "Wiki did not respond well...")
                        }
                        else{
                            if(result.keySet().contains("query")){
                                handleResponse(result)
                            }
                        }
                    }
            }
        }
    }


    // List to access current markers:
    private var nearbyMarkers: ArrayList<Marker> = arrayListOf()

    /**
     * - Delete previous markers from the map.
     * - And while getting nearby places' info
     * - add new markers.
     * */
    private fun handleResponse(result: JsonObject) {
        //Deleting markers:
        nearbyMarkers.clear()
        mMap.clear()

        //Getting places' info:
        val pages = result.get("query").asJsonObject.get("pages").asJsonObject
        for (key in pages.keySet()){
            val lat = pages.get(key).asJsonObject.get("coordinates").asJsonArray.get(0).asJsonObject.get("lat").asDouble
            val lon = pages.get(key).asJsonObject.get("coordinates").asJsonArray.get(0).asJsonObject.get("lon").asDouble
            val latlon = LatLng(lat, lon)

            val title = pages.get(key).asJsonObject.get("title").asString
            val description = pages.get(key).asJsonObject.get("description").asString
            var picture = pages.get(key).asJsonObject.get("thumbnail")?.asJsonObject?.get("source")?.asString
//            Log.i("JSON1", pages.get(key).asJsonObject.toString())
            if (picture == null) {
                picture = "NoPicture"
            }

            val linkToWiki = "http://en.wikipedia.org/?curid=${pages.get(key).asJsonObject.get("pageid").asInt}"
//            Log.i("WikiPage", "Wiki Page: $linkToWiki")

            val poiInfo: ArrayList<String> = arrayListOf(linkToWiki, description, picture)

            // https://developers.google.com/maps/documentation/android-sdk/marker#maps_android_markers
            // Adding new markers:
            var newMarker = mMap.addMarker(
                MarkerOptions().position(latlon).title(title).
                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))   // Color(optional)
                    .snippet("This is default snippet")     //Snippet(optional)
            )
            if (newMarker != null) {
                newMarker.tag = poiInfo
                nearbyMarkers.add(newMarker)
            }
        }
    }





    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.isMyLocationEnabled = true         // My location button
        mMap.setOnMyLocationClickListener(this) // Click on a blue dot

        //Set Custom InfoWindow Adapter up:
        val adapter = myCustomInfoWindowAdapter(this)
        mMap.setInfoWindowAdapter(adapter)


        // Making every marker interactive:
        mMap.setOnMarkerClickListener { marker ->
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(100F))    // Painting the chosen marker
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 14F))              // And zoom to the marker
            marker.showInfoWindow()
            true
        }
        // Removing green color from the marker when infoWindow closed:
        mMap.setOnInfoWindowCloseListener{
            it.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        }

        // As I understood from android documentation - it is impossible to make infoWindow interactive (adding buttons). But it is possible to setOnInfoWindowClickListner:
        mMap.setOnInfoWindowClickListener { marker ->
            val thatPoiInfo: ArrayList<String> = marker.tag as ArrayList<String>    // Copying data from marker's tag to a list to access data
            var url = thatPoiInfo[0]
//            Log.i("thatPoiInfo", thatPoiInfo.toString())

            //Sending intent to open link in a browser:
            val intentToOpenBrowser = Intent(Intent.ACTION_VIEW)
            intentToOpenBrowser.data = Uri.parse(url)
            startActivity(intentToOpenBrowser)
        }
    }



    override fun onMyLocationClick(location: Location) {
        Log.i("onMyLocationClick", "You clicked on yourself!")
    }
}