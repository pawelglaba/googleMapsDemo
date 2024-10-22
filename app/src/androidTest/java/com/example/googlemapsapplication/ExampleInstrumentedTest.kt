//package com.example.googlemapsapplication
//
//import android.R
//import android.location.Address
//import android.location.Geocoder
//import android.text.TextUtils
//import android.view.View
//import android.widget.EditText
//import android.widget.Toast
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import androidx.test.platform.app.InstrumentationRegistry
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.model.BitmapDescriptorFactory
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.MarkerOptions
//import org.junit.Assert.*
//import org.junit.Test
//import org.junit.runner.RunWith
//import java.io.IOException
//
//
///**
// * Instrumented test, which will execute on an Android device.
// *
// * See [testing documentation](http://d.android.com/tools/testing).
// */
//@RunWith(AndroidJUnit4::class)
//class ExampleInstrumentedTest {
//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("com.example.googlemapsapplication", appContext.packageName)
//    }
//    fun onClick(v: View) {
//        val hospital = "hospital"
//        val school = "school"
//        val restaurant = "restaurant"
//        val transferData = arrayOfNulls<Any>(2)
//        val getNearbyPlaces: GetNearbyPlaces = GetNearbyPlaces()
//
//
//        when (v.id) {
//            R.id.search_address -> {
//                val addressField = findViewById(R.id.location_search) as EditText
//                val address = addressField.text.toString()
//
//                var addressList: List<Address>? = null
//                val userMarkerOptions = MarkerOptions()
//
//                if (!TextUtils.isEmpty(address)) {
//                    val geocoder = Geocoder(this)
//
//                    try {
//                        addressList = geocoder.getFromLocationName(address, 6)
//
//                        if (addressList != null) {
//                            var i = 0
//                            while (i < addressList.size) {
//                                val userAddress = addressList[i]
//                                val latLng = LatLng(userAddress.latitude, userAddress.longitude)
//
//                                userMarkerOptions.position(latLng)
//                                userMarkerOptions.title(address)
//                                userMarkerOptions.icon(
//                                    BitmapDescriptorFactory.defaultMarker(
//                                        BitmapDescriptorFactory.HUE_ORANGE
//                                    )
//                                )
//                                mMap.addMarker(userMarkerOptions)
//                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
//                                mMap.animateCamera(CameraUpdateFactory.zoomTo(10f))
//                                i++
//                            }
//                        } else {
//                            Toast.makeText(this, "Location not found...", Toast.LENGTH_SHORT).show()
//                        }
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                } else {
//                    Toast.makeText(this, "please write any location name...", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            }
//
//            R.id.hospitals_nearby -> {
//                mMap.clear()
//                val url: String = getUrl(latitide, longitude, hospital)
//                transferData[0] = mMap
//                transferData[1] = url
//
//                getNearbyPlaces.execute(transferData)
//                Toast.makeText(this, "Searching for Nearby Hospitals...", Toast.LENGTH_SHORT).show()
//                Toast.makeText(this, "Showing Nearby Hospitals...", Toast.LENGTH_SHORT).show()
//            }
//
//            R.id.schools_nearby -> {
//                mMap.clear()
//                url = getUrl(latitide, longitude, school)
//                transferData[0] = mMap
//                transferData[1] = url
//
//                getNearbyPlaces.execute(transferData)
//                Toast.makeText(this, "Searching for Nearby Schools...", Toast.LENGTH_SHORT).show()
//                Toast.makeText(this, "Showing Nearby Schools...", Toast.LENGTH_SHORT).show()
//            }
//
//            R.id.restaurants_nearby -> {
//                mMap.clear()
//                url = getUrl(latitide, longitude, restaurant)
//                transferData[0] = mMap
//                transferData[1] = url
//
//                getNearbyPlaces.execute(transferData)
//                Toast.makeText(this, "Searching for Nearby Restaurants...", Toast.LENGTH_SHORT)
//                    .show()
//                Toast.makeText(this, "Showing Nearby Restaurants...", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//}