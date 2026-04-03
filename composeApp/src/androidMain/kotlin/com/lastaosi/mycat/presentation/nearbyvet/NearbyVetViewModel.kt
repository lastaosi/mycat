package com.lastaosi.mycat.presentation.nearbyvet

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NearbyVetViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(NearbyVetUiState())
    val uiState: StateFlow<NearbyVetUiState> = _uiState

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private val placesClient by lazy {
        Places.createClient(context)
    }

    @SuppressLint("MissingPermission")
    fun loadNearbyVets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 현재 위치 가져오기
                val location = getCurrentLocation()
                _uiState.update { it.copy(currentLocation = location) }

                // 근처 동물병원 검색
                searchNearbyVets(location)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "위치를 가져올 수 없어요: ${e.message}"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): LatLng =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(LatLng(location.latitude, location.longitude))
                } else {
                    cont.resumeWithException(Exception("위치를 가져올 수 없어요"))
                }
            }.addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
            cont.invokeOnCancellation { cts.cancel() }
        }

    private suspend fun searchNearbyVets(location: LatLng) {
        try {
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.RATING,
                Place.Field.OPENING_HOURS,
                Place.Field.PHONE_NUMBER
            )

            val searchRequest = SearchNearbyRequest.builder(
                com.google.android.libraries.places.api.model.CircularBounds.newInstance(
                    location, 2000.0  // 2km 반경
                ),
                placeFields
            )
                .setIncludedTypes(listOf("veterinary_care"))
                .setMaxResultCount(20)
                .build()

            suspendCancellableCoroutine { cont ->
                placesClient.searchNearby(searchRequest)
                    .addOnSuccessListener { response ->
                        val vets = response.places.map { place ->
                            val placeLatLng = place.latLng ?: location
                            val distance = calculateDistance(location, placeLatLng)
                            VetPlace(
                                placeId = place.id ?: "",
                                name = place.name ?: "이름 없음",
                                address = place.address ?: "주소 없음",
                                latLng = placeLatLng,
                                rating = place.rating?.toFloat(),
                                isOpen = place.openingHours?.periods?.isNotEmpty(),
                                phoneNumber = place.phoneNumber,
                                distance = distance
                            )
                        }.sortedBy { it.distance }

                        _uiState.update {
                            it.copy(vets = vets, isLoading = false)
                        }
                        cont.resume(Unit)
                    }
                    .addOnFailureListener { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "동물병원 검색 실패: ${e.message}"
                            )
                        }
                        cont.resume(Unit)
                    }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "검색 중 오류: ${e.message}")
            }
        }
    }

    fun onVetSelected(vet: VetPlace?) {
        _uiState.update { it.copy(selectedVet = vet) }
    }

    fun onRetry() {
        loadNearbyVets()
    }

    // Haversine 공식으로 두 좌표 사이 거리 계산 (m)
    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val r = 6371000f
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toFloat()
    }
}