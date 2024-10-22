package com.example.googlemapsapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    private lateinit var placesClient: PlacesClient
  //  private val locationPermissionCode = 101

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
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyD3LO9tHoyTtaMJQoGGRFMb2pQnqyxtZuI")
        }
        placesClient = Places.createClient(this)
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
                val searchBar = findViewById<EditText>(R.id.searchBar)
                searchBar.setOnEditorActionListener { _, _, _ ->
                    val query = searchBar.text.toString()
                    searchNearbyPlaces(query,currentLatLong)
                    true
                }
                // Wyszukaj pobliskie uniwersytety
                //findNearbyUniversities(currentLatLong)
            }
        }
    }
    /**
     * Wykonuje wyszukiwanie miejsc w pobliżu na podstawie zapytania użytkownika
     * i lokalizacji użytkownika za pomocą Google Places API.
     *
     * Funkcja konstruuje URL do API Google Places i inicjuje asynchroniczne
     * zadanie w celu pobrania wyników dla podanego słowa kluczowego (np. "hospital", "university").
     *
     * @param query Słowo kluczowe wyszukiwania, określające typ miejsca (np. "hospital", "university").
     * @param location Obiekt [LatLng] reprezentujący lokalizację, wokół której ma zostać wykonane wyszukiwanie.
     */
    private fun searchNearbyPlaces(query: String, location: LatLng) {
        // Pobierz klucz API z zasobów strings.xml
        val apiKey = getString(R.string.google_maps_key)

        // Konstruowanie URL do zapytania do Google Places API
        val urlString =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${location.latitude},${location.longitude}&radius=5000&type=$query&key=$apiKey"

        // Uruchomienie asynchronicznego zadania do wykonania zapytania do Google Places API
        NearbyPlacesTask().execute(urlString)
    }


    // Asynchroniczne zadanie, aby pobrać dane z API
    /**
     * Asynchroniczne zadanie do wysyłania zapytań HTTP do Google Places API w tle i zwracania wyników.
     *
     * Klasa `NearbyPlacesTask` dziedziczy po `AsyncTask`, co umożliwia jej wykonywanie operacji
     * sieciowych w tle bez blokowania głównego wątku UI. Zadanie wysyła zapytanie HTTP GET do API,
     * pobiera wyniki i przetwarza je, wywołując funkcję `parseNearbyPlaces()` po zakończeniu.
     */
    inner class NearbyPlacesTask : AsyncTask<String, Void, String>() {

        /**
         * Funkcja wykonuje zadanie w tle. Wysyła zapytanie HTTP do Google Places API,
         * pobiera odpowiedź jako ciąg znaków.
         *
         * @param params Parametry zadania, w tym URL zapytania jako pierwszy element tablicy.
         * @return Ciąg znaków będący odpowiedzią z Google Places API lub `null` w przypadku błędu.
         */
        override fun doInBackground(vararg params: String?): String? {
            val urlString = params[0] // URL zapytania API
            var response: String? = null

            try {
                // Tworzenie połączenia HTTP do podanego URL
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET" // Metoda GET

                // Pobieranie odpowiedzi z API
                val inputStream = connection.inputStream
                response = inputStream.bufferedReader().use { it.readText() } // Odczytanie odpowiedzi jako String
            } catch (e: Exception) {
                e.printStackTrace() // Wypisanie błędu do konsoli
            }

            return response // Zwrócenie odpowiedzi jako String (lub null w przypadku błędu)
        }

        /**
         * Funkcja wykonująca się po zakończeniu zadania w tle.
         * Odpowiedź z API jest przetwarzana lub wyświetlany jest komunikat o błędzie.
         *
         * @param result Wynik zapytania w postaci JSON lub null, jeśli nie udało się pobrać danych.
         */
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (result != null) {
                // Jeśli wynik jest niepusty, przetwórz dane
                parseNearbyPlaces(result)
            } else {
                // Wyświetl komunikat o nieudanym pobraniu danych
                Toast.makeText(this@MapsActivity, "Nie udało się pobrać miejsc", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Funkcja do przetwarzania wyniku JSON z Google Places API.
     * Odczytuje odpowiedź, analizuje listę miejsc i dodaje markery na mapie dla każdego znalezionego miejsca.
     *
     * @param result Wynik zapytania w formacie JSON (ciąg znaków).
     */
    private fun parseNearbyPlaces(result: String) {
        try {
            // Wyczyszczenie mapy z poprzednich markerów
            mMap.clear()

            // Konwersja wyniku do obiektu JSON
            val jsonObject = JSONObject(result)
            val resultsArray = jsonObject.getJSONArray("results") // Tablica wyników miejsc

            // Iteracja po wszystkich miejscach w odpowiedzi
            for (i in 0 until resultsArray.length()) {
                val placeObject = resultsArray.getJSONObject(i) // Pojedyncze miejsce
                val placeName = placeObject.getString("name") // Nazwa miejsca

                // Pobranie współrzędnych geograficznych miejsca
                val geometry = placeObject.getJSONObject("geometry")
                val location = geometry.getJSONObject("location")
                val lat = location.getDouble("lat")
                val lng = location.getDouble("lng")

                // Tworzenie obiektu LatLng na podstawie współrzędnych
                val placeLatLng = LatLng(lat, lng)

                // Dodanie markera na mapie dla tego miejsca
                mMap.addMarker(MarkerOptions().position(placeLatLng).title(placeName))
            }
        } catch (e: Exception) {
            e.printStackTrace() // Wypisanie błędu, jeśli JSON nie mógł zostać poprawnie przetworzony
        }
    }

    /**
     * Wyszukuje pobliskie uniwersytety za pomocą Google Places API i dodaje markery dla każdego
     * znalezionego miejsca.
     *
     * @param location Bieżąca lokalizacja użytkownika.
     */
//    private fun findNearbyUniversities(location: LatLng) {
//        // Zbuduj URL zapytania do Google Places API (Nearby Search)
//        val apiKey = getString(R.string.google_maps_key)
//        val locationString = "${location.latitude},${location.longitude}"
//        val radius = 5000  // Promień wyszukiwania w metrach
//        val type = "university"
//        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$locationString&radius=$radius&type=$type&key=$apiKey"
//
//        // Utwórz żądanie HTTP do API
//        val request = object : StringRequest(
//            Method.GET, url,
//            Response.Listener { response ->
//                try {
//                    // Parsuj odpowiedź JSON
//                    val jsonObject = JSONObject(response)
//                    val results = jsonObject.getJSONArray("results")
//
//                    // Iteracja po wynikach i dodanie markerów na mapie
//                    for (i in 0 until results.length()) {
//                        val place = results.getJSONObject(i)
//                        val latLng = place.getJSONObject("geometry")
//                            .getJSONObject("location")
//                        val lat = latLng.getDouble("lat")
//                        val lng = latLng.getDouble("lng")
//                        val placeName = place.getString("name")
//
//                        // Dodaj marker dla każdego miejsca na mapie
//                        mMap.addMarker(
//                            MarkerOptions()
//                                .position(LatLng(lat, lng))
//                                .title(placeName)
//                        )
//                    }
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//            },
//            Response.ErrorListener { error ->
//                // Obsługa błędów
//                Log.e("MapsActivity", "Błąd podczas wyszukiwania: ${error.message}")
//            }) {}
//
//        // Dodaj żądanie do kolejki
//        Volley.newRequestQueue(this).add(request)
//    }

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
