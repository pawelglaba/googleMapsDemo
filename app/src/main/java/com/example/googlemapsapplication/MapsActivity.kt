package com.example.googlemapsapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
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
import org.json.JSONException
import org.json.JSONObject

/**
 * Aktywność odpowiedzialna za wyświetlanie mapy Google i pokazywanie pobliskich uniwersytetów.
 * Mapa jest inicjalizowana, gdy aktywność jest tworzona, a po załadowaniu bieżącej lokalizacji użytkownika
 * wyświetla markery dla pobliskich uniwersytetów.
 *
 * Implementuje [OnMapReadyCallback] do inicjalizacji mapy i [GoogleMap.OnMarkerClickListener]
 * do obsługi kliknięć markerów.
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // Zmienna do przechowywania instancji mapy Google
    private lateinit var mMap: GoogleMap

    // Powiązanie widoku (binding) z layoutem
    private lateinit var binding: ActivityMapsBinding

    // Ostatnia znana lokalizacja użytkownika
    private lateinit var lastLocation: Location

    // Klient do uzyskiwania lokalizacji użytkownika
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        // Stała reprezentująca kod żądania uprawnień lokalizacji
        private const val LOCATION_REQUEST_CODE = 1
    }

    /**
     * Inicjalizuje aktywność. Ustawia fragment mapy i klienta lokalizacji.
     *
     * @param savedInstanceState Stan aplikacji zapisany z poprzednich instancji, jeśli istnieje.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Uzyskaj fragment mapy i zarejestruj callback, który zostanie wywołany, gdy mapa będzie gotowa do użycia
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicjalizacja klienta lokalizacji, który pobiera dane lokalizacyjne użytkownika
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Metoda wywoływana, gdy mapa jest gotowa do użycia. Ustawia opcje mapy, takie jak kontrolki zoomu
     * i nasłuchuje kliknięć markerów. Wywołuje także [setUpMap], aby skonfigurować mapę z lokalizacją użytkownika.
     *
     * @param googleMap Instancja GoogleMap, która jest gotowa do manipulacji.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        // Przypisz instancję mapy do zmiennej
        mMap = googleMap

        // Włącz kontrolki zoomu na mapie
        mMap.uiSettings.isZoomControlsEnabled = true

        // Zarejestruj nasłuchiwanie kliknięć markerów
        mMap.setOnMarkerClickListener(this)

        // Konfiguruj mapę, aby pobierać lokalizację użytkownika
        setUpMap()
    }

    /**
     * Konfiguracja mapy. Sprawdza uprawnienia lokalizacji i, jeśli są przyznane,
     * ustawia lokalizację użytkownika na mapie oraz dodaje marker w bieżącej lokalizacji.
     * Wywołuje także [findNearbyUniversities], aby wyszukać pobliskie uniwersytety.
     */
    private fun setUpMap() {
        // Sprawdź, czy aplikacja ma uprawnienia do uzyskiwania lokalizacji
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Jeśli nie ma uprawnień, żądaj ich od użytkownika
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }

        // Włącz funkcję lokalizacji użytkownika na mapie
        mMap.isMyLocationEnabled = true

        // Pobierz ostatnią znaną lokalizację użytkownika
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                // Zapisz ostatnią lokalizację użytkownika
                lastLocation = location

                // Stwórz obiekt LatLng z aktualnej lokalizacji
                val currentLatLong = LatLng(location.latitude, location.longitude)

                // Dodaj marker na mapie w bieżącej lokalizacji
                placeMarkerOnMap(currentLatLong)

                // Przesuń kamerę mapy do bieżącej lokalizacji
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 11f))

                // Wyszukaj pobliskie uniwersytety
                findNearbyUniversities(currentLatLong)
            }
        }
    }

    /**
     * Wyszukuje pobliskie uniwersytety za pomocą Google Places API i dodaje markery dla każdego
     * znalezionego miejsca.
     *
     * @param location Bieżąca lokalizacja użytkownika.
     */
    private fun findNearbyUniversities(location: LatLng) {
        // Zbuduj URL zapytania do Google Places API (Nearby Search)
        val apiKey = getString(R.string.google_maps_key)
        val locationString = "${location.latitude},${location.longitude}"
        val radius = 5000  // Promień wyszukiwania w metrach
        val type = "university"
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$locationString&radius=$radius&type=$type&key=$apiKey"

        // Utwórz żądanie HTTP do API
        val request = object : StringRequest(
            Method.GET, url,
            Response.Listener { response ->
                try {
                    // Parsuj odpowiedź JSON
                    val jsonObject = JSONObject(response)
                    val results = jsonObject.getJSONArray("results")

                    // Iteracja po wynikach i dodanie markerów na mapie
                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val latLng = place.getJSONObject("geometry")
                            .getJSONObject("location")
                        val lat = latLng.getDouble("lat")
                        val lng = latLng.getDouble("lng")
                        val placeName = place.getString("name")

                        // Dodaj marker dla każdego miejsca na mapie
                        mMap.addMarker(
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
                // Obsługa błędów
                Log.e("MapsActivity", "Błąd podczas wyszukiwania: ${error.message}")
            }) {}

        // Dodaj żądanie do kolejki
        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Dodaje marker w podanej lokalizacji [LatLng].
     *
     * @param currentLatLong Obiekt LatLng zawierający współrzędne do umieszczenia markera.
     */
    private fun placeMarkerOnMap(currentLatLong: LatLng) {
        // Stwórz opcje dla markera
        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("$currentLatLong")

        // Dodaj marker na mapie
        mMap.addMarker(markerOptions)
    }

    /**
     * Obsługuje kliknięcia markerów. W tym przypadku nie dodaje specjalnych zachowań
     * dla kliknięcia, więc metoda po prostu zwraca `false`.
     *
     * @param p0 Marker, który został kliknięty.
     * @return Boolean wskazujący, czy zdarzenie kliknięcia zostało obsłużone.
     */
    override fun onMarkerClick(p0: Marker) = false
}
