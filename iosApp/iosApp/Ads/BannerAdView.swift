import SwiftUI
import GoogleMobileAds

// ═══════════════════════════════════════════════════════════════════
// MARK: - BannerAdView  (GADBannerView → SwiftUI UIViewRepresentable)
// ═══════════════════════════════════════════════════════════════════

/// GADBannerView 를 SwiftUI 에서 사용하기 위한 UIViewRepresentable 래퍼.
///
/// - Config.xcconfig 의 ADMOB_BANNER_ID (→ Info.plist) 값을 자동으로 읽는다.
/// - 읽기 실패 시 테스트 배너 ID(ca-app-pub-3940256099942544/2934735716) 로 폴백한다.
/// - 배너 크기는 `GADAdSizeBanner` (320×50pt) 고정.
struct BannerAdView: UIViewRepresentable {

    /// Info.plist 에서 ADMOB_BANNER_ID 값을 읽는다.
    private var adUnitID: String {
        (Bundle.main.object(forInfoDictionaryKey: "ADMOB_BANNER_ID") as? String)
            ?? "ca-app-pub-3940256099942544/2934735716"
    }

    // MARK: UIViewRepresentable

    func makeUIView(context: Context) -> GADBannerView {
        let banner = GADBannerView(adSize: GADAdSizeBanner)
        banner.adUnitID = adUnitID
        banner.rootViewController = context.coordinator.rootViewController()
        banner.load(GADRequest())
        return banner
    }

    func updateUIView(_ uiView: GADBannerView, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator() }

    // MARK: - Coordinator

    class Coordinator: NSObject {
        /// 배너 광고 표시에 필요한 rootViewController 를 반환한다.
        func rootViewController() -> UIViewController? {
            UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .first?.windows.first?.rootViewController
        }
    }
}
