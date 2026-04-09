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

/**
 * 근처 동물병원 화면 ViewModel.
 *
 * ## 데이터 흐름
 * 1. [loadNearbyVets] 호출
 * 2. [getCurrentLocation]: FusedLocationProviderClient 로 현재 GPS 좌표 획득
 *    (suspendCancellableCoroutine 으로 콜백 → suspend 변환)
 * 3. [searchNearbyVets]: Google Places SearchNearby API 호출 (반경 2km, 최대 20개)
 * 4. 결과를 [VetPlace] 로 매핑 후 [calculateDistance](Haversine 공식)로 거리 계산 → 거리순 정렬
 *
 * ## @SuppressLint("MissingPermission")
 * 위치 권한(ACCESS_FINE_LOCATION)은 NearbyVetScreen 에서 PermissionLauncher 로 사전 확인 후
 * 권한이 있을 때만 이 ViewModel 메서드를 호출한다.
 * Lint 는 ViewModel 레이어에서 권한 확인 여부를 알 수 없으므로 억제한다.
 *
 * ## Haversine 공식 ([calculateDistance])
 * 구면 삼각법 기반 두 위경도 간 거리 계산 공식.
 * 지구 반지름(R=6,371,000m) 을 사용해 미터(m) 단위로 반환한다.
 */
class NearbyVetViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(NearbyVetUiState())
    val uiState: StateFlow<NearbyVetUiState> = _uiState

    // lazy 초기화: Places, LocationServices 는 앱 컨텍스트가 필요하므로 생성자에서 바로 만들지 않음
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

    /**
     * 현재 GPS 위치를 suspend 함수로 반환한다.
     *
     * FusedLocationProviderClient 는 콜백 기반 API 이므로
     * [suspendCancellableCoroutine] 으로 코루틴 세계로 브릿지한다.
     * - onSuccess: cont.resume → 정상 반환
     * - onFailure: cont.resumeWithException → 상위 catch 로 전파
     * - 코루틴 취소 시: invokeOnCancellation 에서 CancellationTokenSource.cancel() 호출
     */
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
            // 코루틴이 취소되면 위치 요청도 취소
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

    /**
     * Haversine 공식으로 두 위경도 좌표 간 거리를 계산한다 (단위: 미터).
     *
     * Haversine 공식:
     *   a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlng/2)
     *   c = 2·atan2(√a, √(1−a))
     *   d = R · c   (R = 지구 반지름 6,371,000m)
     *
     * 평면 좌표 기반 유클리드 거리보다 정확하며,
     * 수 km 수준의 거리에서도 오차가 작다.
     */
    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val r = 6371000f  // 지구 반지름 (m)
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