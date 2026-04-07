import Foundation

// MARK: - 앱 네비게이션 라우트
// Android NavRoutes와 동일한 구조
enum AppRoute: Hashable {
    case splash
    case main
    case profileRegister
    case profileEdit(catId: Int64)
    case weight(catId: Int64)
    case vaccination(catId: Int64)
    case medication(catId: Int64)
    case diary(catId: Int64)
    case healthCheck
    case careGuide
    case nearbyVet
}
