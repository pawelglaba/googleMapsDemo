package com.example.googlemapsapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.googlemapsapplication.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import org.json.JSONException
import org.json.JSONObject

/**
 * Activity responsible for displaying a Google Map and showing nearby places such as universities.
 * Implements [OnMapReadyCallback] to initialize the map and [GoogleMap.OnMarkerClickListener] to handle marker clicks.
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {


    /**lateinit: This means the variable will be initialized later, and you don't need to assign a value immediately.
     *  If you try to access it before initialization, the app will throw an exception.
     */
    private lateinit var googleMap: GoogleMap

    /** In Android development, a binding object provides access to the views in your XML layout.
     *This is automatically generated for the activity_maps.xml layout. It allows you to interact with UI
     *  elements directly in the code without using findViewById()
     */
    private lateinit var binding: ActivityMapsBinding

    /**
     * Represents a geographic location (latitude, longitude, and additional details like accuracy or altitude).
     * This variable is used to track the user's current or last known position.
     */
    private lateinit var lastKnownLocation: Location
    // Declares a variable to manage location requests.
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //Represents a polyline, which is a series of connected lines (used for drawing routes or paths on the map
    private var currentRoute: Polyline? = null


    companion object {
        private const val LOCATION_REQUEST_CODE = 1 // 1 values is arbitrary.
        // Developers often use small integers to differentiate between multiple types of requests
    }

    /**
     * Initializes the activity, sets up map fragment, and initializes location client.
     * @param savedInstanceState The saved state of the activity, if available.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //The inflate() method creates an instance of the binding class by connecting it to the layout XML file
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        //This method initializes the map in the SupportMapFragment asynchronously (in the background)
        mapFragment.getMapAsync(this)

        //A part of Google Play Services that provides an efficient and battery-friendly way to get the user's location.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val searchButton = findViewById<Button>(R.id.searchButton)
        val addressEditText = findViewById<EditText>(R.id.addressEditText)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val radiusSeekBar = findViewById<SeekBar>(R.id.radiusSeekBar)
        val radiusTextView = findViewById<TextView>(R.id.radiusTextView)

        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                radiusTextView.text = "Radius: $progress m"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val selectedCategory = categorySpinner.selectedItem.toString()
                val radius = radiusSeekBar.progress
                findNearbyPlaces(selectedCategory, radius)
            }
        })

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categorySpinner.selectedItem.toString()
                val radius = radiusSeekBar.progress
                findNearbyPlaces(selectedCategory, radius)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        searchButton.setOnClickListener {
            val address = addressEditText.text.toString()
            if (address.isNotBlank()) {
                searchLocation(address)
            } else {
                val selectedCategory = categorySpinner.selectedItem.toString()
                val radius = radiusSeekBar.progress
                findNearbyPlaces(selectedCategory, radius)
            }
        }
    }

    /**
     * Searches for a location by address and places a marker on the map at the found location.
     * @param address The address to search for.
     */
    private fun searchLocation(address: String) {
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${address.replace(" ", "%20")}&key=$apiKey"

        val request = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val results = jsonObject.getJSONArray("results")

                    if (results.length() > 0) {
                        val location = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        val latLng = LatLng(lat, lng)
                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(latLng).title("Search Result"))
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    } else {
                        Log.e("MapsActivity", "No address found")
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Log.e("MapsActivity", "Error searching address: ${error.message}")
            }) {}

        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Finds nearby places of a given type within a specified radius.
     * @param type The type of places to search for.
     * @param radius The search radius in meters.
     */
    private fun findNearbyPlaces(type: String, radius: Int) {
        //The double colon :: is used to refer to the lastKnownLocation property itself, rather than its value.
        if (!::lastKnownLocation.isInitialized) {
            Log.e("MapsActivity", "User location not initialized.")
            return
        }

        val apiKey = getString(R.string.google_maps_key)
        val locationString = "${lastKnownLocation.latitude},${lastKnownLocation.longitude}"
        // Constructs a URL to query the Google Places API for nearby places:
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$locationString&radius=$radius&type=$type&key=$apiKey"

        //Making the HTTP Request
        val request = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                try {
                    /*
                    JSON (JavaScript Object Notation)

                    JSON is a lightweight data exchange format that is easy for humans to read and
                    write and easy for machines to parse and generate.

                     It is widely used for data communication between servers and applications,
                     especially in web and mobile applications.
                     */
                    val jsonObject = JSONObject(response)
                    val results = jsonObject.getJSONArray("results")
                    googleMap.clear()

                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val latLng = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = latLng.getDouble("lat")
                        val lng = latLng.getDouble("lng")
                        val placeName = place.getString("name")

                        googleMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(lat, lng))
                                .title(placeName)
                        )
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Log.e("MapsActivity", "Error finding places: ${error.message}")
            }) {}

        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Called when the map is ready to use. Configures the map and sets up user location.
     * @param map The GoogleMap instance that is ready for interaction.
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.setOnMarkerClickListener(this)

        setUpMap()
    }

    /**
     * Sets up the map with user location and displays markers for nearby places.
     */
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }
        //Purpose: Moves and zooms the camera to focus on the user's current location.
        googleMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastKnownLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 11f))

                val defaultCategory = "university"
                val defaultRadius = 5000
                findNearbyPlaces(defaultCategory, defaultRadius)
            }
        }
    }

    /**
     * Adds a marker at a given location on the map.
     * @param location The location where the marker will be placed.
     */
    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location).title(location.toString())
        googleMap.addMarker(markerOptions)
    }

    /**
     * Handles marker click events. Displays marker info and draws a route to the marker.
     * @param marker The clicked marker.
     * @return Boolean indicating whether the click event was handled.
     */
    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        currentRoute?.remove()
        drawRouteToMarker(marker.position)
        return true
    }

    /**
     * Draws a route from the user's location to the specified destination.
     * @param destination The destination location.
     */
    private fun drawRouteToMarker(destination: LatLng) {
        val origin = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)

        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&key=$apiKey"

        val request = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val routes = jsonObject.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val points = routes.getJSONObject(0)
                            .getJSONObject("overview_polyline")
                            .getString("points")
                        val decodedPath = decodePolyline(points)

                        currentRoute = googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(decodedPath)
                                .color(resources.getColor(R.color.teal_200))
                                .width(10f)
                        )
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Log.e("MapsActivity", "Error retrieving route: ${error.message}")
            }) {}

        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Decodes a polyline string into a list of [LatLng] points.
     * @param encoded The encoded polyline string.
     * @return A list of [LatLng] points.
     */

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            /*
            Why - 63?
            The encoded polyline uses 6-bit values, represented as readable ASCII characters.
            To extract the actual data, we need to subtract the offset of 63,
            which was added during the encoding process.

            b and 0x1f: Extracts the 5 least significant bits from b.
            The value 0x1f (31 in decimal) is a mask to isolate those bits.
            b and 0x1f shl shift: These 5 bits are shifted left by shift positions to assemble the final value.
            result = result or (...): Combines the extracted bits into result using a bitwise OR operation.
             */
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }
}
