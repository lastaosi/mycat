import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Koin DI 초기화 (Android MyCatApplication.startKoin() 대응)
        KoinIosKt.doInitKoin()
        // 약 복용 알람 권한 요청 (앱 최초 실행 시 한 번)
        MedicationNotificationManager.shared.requestAuthorization()
    }

    var body: some Scene {
        WindowGroup {
            AppRoot()
        }
    }
}
