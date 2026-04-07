import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Koin DI 초기화 (Android MyCatApplication.startKoin() 대응)
        KoinIosKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            AppRoot()
        }
    }
}
