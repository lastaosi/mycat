package com.lastaosi.mycat.presentation.nearbyvet

import com.google.android.gms.maps.model.LatLng

data class NearbyVetUiState(
    val currentLocation: LatLng? = null,
    val vets: List<VetPlace> = emptyList(),
    val selectedVet: VetPlace? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class VetPlace(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng,
    val rating: Float? = null,
    val isOpen: Boolean? = null,
    val phoneNumber: String? = null,
    val distance: Float? = null  // 현재 위치로부터 거리 (m)
)