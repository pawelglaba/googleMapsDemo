package com.example.googlemapsapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.google.android.libraries.places.api.net.PlacesClient

/**
 * Główna aktywność aplikacji wyświetlająca mapę Google i umożliwiająca użytkownikowi
 * wybieranie punktów na mapie oraz uruchamianie nawigacji do wybranego punktu.
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // Zmienna przechowująca instancję mapy Google
    private lateinit var mMap: GoogleMap
    // Klient Google Places do pracy z miejscami
    private lateinit var placesClient: PlacesClient
    // Powiązanie widoku z layoutem
    private lateinit var binding: ActivityMapsBinding
    // Klient lokalizacji, aby uzyskać ostatnią lokalizację użytkownika
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // Obiekt śledzący lokalizację użytkownika
    private lateinit var locationTracker: LocationTracker
    // Zmienna przechowująca współrzędne wybranego miejsca na mapie
    private var selectedLocation: LatLng? = null

    companion object {
        // Kod żądania uprawnień lokalizacji
        private const val LOCATION_REQUEST_CODE = 1000
    }

    /**
     * Metoda `onCreate` inicjalizuje widok, klienta lokalizacji i mapę,
     * a także obsługuje przycisk do uruchamiania nawigacji.
     *
     * @param savedInstanceState stan zapisany z poprzednich uruchomień.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicjalizacja API Places z kluczem API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "YOUR_API_KEY")
        }
        placesClient = Places.createClient(this)

        // Inicjalizacja fragmentu mapy
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicjalizacja klienta lokalizacji
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        // Inicjalizacja śledzenia lokalizacji użytkownika
        locationTracker = LocationTracker(this)

        // Sprawdzenie uprawnień lokalizacji i rozpoczęcie śledzenia
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            locationTracker.startTracking()
        }

        // Inicjalizacja przycisku do uruchamiania nawigacji
        val startNavigationButton: Button = findViewById(R.id.startNavigationButton)
        startNavigationButton.setOnClickListener {
            // Jeśli wybrano miejsce, rozpocznij nawigację do tego punktu
            selectedLocation?.let {
                startNavigation(it.latitude, it.longitude)
            } ?: run {
                // Jeśli nie wybrano miejsca, wyświetl komunikat
                Toast.makeText(this, "Proszę wybrać punkt na mapie, aby rozpocząć nawigację.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Zatrzymuje śledzenie lokalizacji przy zamykaniu aktywności.
     */
    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stopTracking()
    }

    /**
     * Funkcja `onMapReady` jest wywoływana, gdy mapa jest gotowa do użycia.
     * Inicjalizuje mapę, włącza kontrolki zoomu i nasłuchuje kliknięć na mapie.
     *
     * @param googleMap instancja mapy Google gotowa do manipulacji.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Włącz kontrolki zoomu na mapie
        mMap.uiSettings.isZoomControlsEnabled = true

        // Zarejestruj nasłuchiwanie kliknięć markerów
        mMap.setOnMarkerClickListener(this)

        // Nasłuchiwanie kliknięcia na mapie - wybieranie punktu
        mMap.setOnMapClickListener { latLng ->
            // Aktualizuj kliknięte miejsce i dodaj marker
            selectedLocation = latLng
            mMap.clear() // Usuń poprzednie markery
            mMap.addMarker(MarkerOptions().position(latLng).title("Wybrane miejsce"))
            Log.d("MapClick", "Kliknięto: Lat: ${latLng.latitude}, Lng: ${latLng.longitude}")
        }

        // Konfiguruj mapę, aby pobierać lokalizację użytkownika
        setUpMap()
    }

    /**
     * Konfiguracja mapy: sprawdza uprawnienia lokalizacji,
     * włącza lokalizację użytkownika na mapie i dodaje marker w bieżącej lokalizacji.
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

        // Włącz funkcję lokalizacji użytkownika na mapie
        mMap.isMyLocationEnabled = true

        // Pobierz ostatnią znaną lokalizację użytkownika
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                val currentLatLong = LatLng(location.latitude, location.longitude)

                // Dodaj marker na bieżącej lokalizacji
                placeMarkerOnMap(currentLatLong)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 11f))
            }
        }
    }

    /**
     * Dodaje marker na mapie w podanej lokalizacji.
     *
     * @param currentLatLong współrzędne punktu, gdzie należy dodać marker.
     */
    private fun placeMarkerOnMap(currentLatLong: LatLng) {
        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("$currentLatLong")
        mMap.addMarker(markerOptions)
    }

    /**
     * Uruchamia aplikację Google Maps, aby rozpocząć nawigację do wybranego punktu docelowego.
     *
     * @param destinationLatitude Szerokość geograficzna celu nawigacji.
     * @param destinationLongitude Długość geograficzna celu nawigacji.
     */
    private fun startNavigation(destinationLatitude: Double, destinationLongitude: Double) {
        val uri = "google.navigation:q=$destinationLatitude,$destinationLongitude"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)).apply {
            setPackage("com.google.android.apps.maps")
        }

        // Sprawdzenie, czy aplikacja Google Maps jest dostępna
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Log.d("Navigation", "Nie można uruchomić aplikacji Google Maps.")
        }
    }

    /**
     * Metoda `onMarkerClick` jest wywoływana po kliknięciu markera.
     * W tej aplikacji kliknięcia markerów nie są specjalnie obsługiwane, więc zwraca `false`.
     *
     * @param p0 marker, który został kliknięty.
     * @return `false`, ponieważ nie jest konieczna specjalna obsługa kliknięcia.
     */
    override fun onMarkerClick(p0: Marker) = false

    /**
     * Sprawdza uprawnienia lokalizacji. Jeśli nie są one przyznane, prosi użytkownika o ich udzielenie.
     * Gdy uprawnienia są już przyznane, pobiera ostatnią znaną lokalizację użytkownika.
     */
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            getLastKnownLocation()
        }
    }

    /**
     * Metoda wywoływana po wyniku żądania uprawnień.
     * Sprawdza, czy użytkownik przyznał uprawnienia lokalizacyjne.
     * Jeśli uprawnienia są przyznane, rozpoczyna śledzenie lokalizacji.
     *
     * @param requestCode kod żądania uprawnień.
     * @param permissions tablica żądanych uprawnień.
     * @param grantResults tablica wyników odpowiadających uprawnieniom.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationTracker.startTracking()
            } else {
                Log.d("MainActivity", "Uprawnienia lokalizacyjne nie zostały przyznane.")
            }
        }
    }

    /**
     * Pobiera ostatnią znaną lokalizację użytkownika, jeśli uprawnienia zostały przyznane.
     * Jeśli uprawnienia nie zostały przyznane, wyświetla odpowiedni komunikat.
     */
    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Location", "Permission not granted")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("Location", "Lat: ${it.latitude}, Lng: ${it.longitude}")
            }
        }
    }
}
