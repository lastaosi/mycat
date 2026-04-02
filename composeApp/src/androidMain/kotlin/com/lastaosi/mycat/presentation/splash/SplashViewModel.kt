package com.lastaosi.mycat.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.repository.CatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 스플래시 화면에서 이동할 목적지 */
sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object ProfileRegister : SplashDestination()
    data object Main : SplashDestination()
}

/**
 * DB에 등록된 고양이 수로 첫 실행 여부를 판단하여 목적지를 결정한다.
 * - 0마리 → ProfileRegister (고양이 등록 화면)
 * - 1마리 이상 → Main
 */
class SplashViewModel(
    private val catRepository: CatRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    init {
        checkFirstRun()
    }

    private fun checkFirstRun() {
        viewModelScope.launch {
            val count = catRepository.getCount()
            _destination.value = if (count > 0) {
                SplashDestination.Main
            } else {
                SplashDestination.ProfileRegister
            }
        }
    }
}