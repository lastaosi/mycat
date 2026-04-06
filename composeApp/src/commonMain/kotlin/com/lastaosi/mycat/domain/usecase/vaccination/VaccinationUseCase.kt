package com.lastaosi.mycat.domain.usecase.vaccination

import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase

data class VaccinationUseCase(
    val getCatById: GetCatByIdUseCase,
    val getVaccinations: GetVaccinationsUseCase,
    val saveVaccination: SaveVaccinationUseCase,
    val deleteVaccination: DeleteVaccinationUseCase
)