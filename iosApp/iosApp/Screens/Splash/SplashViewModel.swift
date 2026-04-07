import Foundation
import ComposeApp

// MARK: - Splash 목적지
enum SplashDestination: Equatable {
    case loading
    case main
    case profileRegister
}

// MARK: - SplashViewModel
// Kotlin SplashKotlinViewModel을 래핑하는 Swift ObservableObject
@MainActor
final class SplashViewModel: ObservableObject {
    @Published var destination: SplashDestination = .loading

    private let kotlinVM = SplashKotlinViewModel()

    func start() {
        kotlinVM.checkFirstRun(
            onHasProfile: { [weak self] in
                Task { @MainActor [weak self] in
                    self?.destination = .main
                }
            },
            onNoProfile: { [weak self] in
                Task { @MainActor [weak self] in
                    self?.destination = .profileRegister
                }
            }
        )
    }

    deinit {
        kotlinVM.dispose()
    }
}
