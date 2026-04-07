package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.repository.CatRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS 전용 Splash 로직.
 * - DB 고양이 수 확인 후 2초 최소 대기
 * - Swift 콜백으로 결과 전달 (KotlinBoolean 이슈 회피를 위해 두 콜백 사용)
 */
class SplashKotlinViewModel : KoinComponent {
    private val catRepository: CatRepository by inject()
    private val scope = MainScope()

    fun checkFirstRun(onHasProfile: () -> Unit, onNoProfile: () -> Unit) {
        scope.launch {
            val minDisplayJob = launch { delay(2000L) }
            val count = catRepository.getCount()
            minDisplayJob.join()
            if (count > 0L) onHasProfile() else onNoProfile()
        }
    }

    fun dispose() {
        scope.cancel()
    }
}
