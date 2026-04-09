import SwiftUI
import ComposeApp

// ═══════════════════════════════════════════════════════════════════
// MARK: - Swift Local Models
// ═══════════════════════════════════════════════════════════════════

/// HealthChecklist (Kotlin) 의 Swift 친화적 복사본.
/// KotlinInt/KotlinBoolean 타입 변환을 ViewModel 에서 한 번만 처리한다.
struct HealthCheckItem: Identifiable {
    let id: Int
    let month: Int
    let itemType: HealthItemTypeSwift
    let title: String
    let description: String
    let isBreedSpecific: Bool
    let isRecommended: Bool
}

/// Kotlin HealthItemType enum 의 Swift 대응 타입.
/// - rawValue: Kotlin enum.name 값과 동일하게 맞춰 매핑한다.
enum HealthItemTypeSwift: String {
    case vaccine = "VACCINE"
    case check   = "CHECK"
    case surgery = "SURGERY"

    var emoji: String {
        switch self {
        case .vaccine: return "💉"
        case .check:   return "🩺"
        case .surgery: return "🔬"
        }
    }

    var label: String {
        switch self {
        case .vaccine: return "예방접종"
        case .check:   return "건강검진"
        case .surgery: return "수술/처치"
        }
    }

    /// 항목 카드 배경색 (타입별 파스텔 톤)
    var backgroundColor: Color {
        switch self {
        case .vaccine: return Color(red: 0.90, green: 0.96, blue: 1.00)  // 연파랑
        case .check:   return Color(red: 0.90, green: 0.98, blue: 0.90)  // 연초록
        case .surgery: return Color(red: 1.00, green: 0.94, blue: 0.86)  // 연주황
        }
    }

    /// 항목 카드 아이콘 색상
    var iconColor: Color {
        switch self {
        case .vaccine: return Color(red: 0.20, green: 0.50, blue: 0.90)  // 파랑
        case .check:   return Color(red: 0.20, green: 0.70, blue: 0.35)  // 초록
        case .surgery: return Color(red: 0.85, green: 0.45, blue: 0.15)  // 주황
        }
    }
}

/// 건강 체크리스트 화면 상단 탭 목록.
enum HealthCheckTab: CaseIterable, Hashable {
    case all
    case vaccine
    case check
    case surgery

    var label: String {
        switch self {
        case .all:     return "전체"
        case .vaccine: return "예방접종"
        case .check:   return "건강검진"
        case .surgery: return "수술/처치"
        }
    }

    var emoji: String {
        switch self {
        case .all:     return "📋"
        case .vaccine: return "💉"
        case .check:   return "🩺"
        case .surgery: return "🔬"
        }
    }

    /// 해당 탭이 주어진 HealthItemTypeSwift 와 일치하는지 확인한다.
    func matches(_ type: HealthItemTypeSwift) -> Bool {
        switch self {
        case .all:     return true
        case .vaccine: return type == .vaccine
        case .check:   return type == .check
        case .surgery: return type == .surgery
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - HealthCheckViewModel
// ═══════════════════════════════════════════════════════════════════

/// iOS 건강 체크리스트 화면 ObservableObject ViewModel.
///
/// - HealthCheckKotlinViewModel 을 내부에서 생성하고 콜백으로 데이터를 수신한다.
/// - 대표 고양이를 자동으로 로드하므로 catId 파라미터가 필요하지 않다.
/// - filteredItems / groupedItems 는 selectedTab 에 따라 파생되는 computed property 다.
class HealthCheckViewModel: ObservableObject {

    // MARK: Published State
    @Published var catName: String = ""
    @Published var ageMonth: Int = 0        // 현재 고양이 월령 (월 헤더 하이라이트 기준)
    @Published var allItems: [HealthCheckItem] = []
    @Published var selectedTab: HealthCheckTab = .all
    @Published var isLoading: Bool = true

    // MARK: Private
    private let kotlinViewModel = HealthCheckKotlinViewModel()

    // MARK: Computed

    /// selectedTab 에 따라 필터링된 항목 목록.
    var filteredItems: [HealthCheckItem] {
        switch selectedTab {
        case .all: return allItems
        default:   return allItems.filter { selectedTab.matches($0.itemType) }
        }
    }

    /// filteredItems 를 월령(month)으로 그룹화하고 오름차순 정렬한 목록.
    /// SwiftUI ForEach 에서 직접 사용할 수 있도록 튜플 배열로 반환한다.
    var groupedItems: [(month: Int, items: [HealthCheckItem])] {
        let dict = Dictionary(grouping: filteredItems) { $0.month }
        return dict.keys.sorted().map { month in
            (month: month, items: dict[month] ?? [])
        }
    }

    // MARK: Load

    /// 대표 고양이 기준으로 건강 체크리스트를 로드한다.
    /// HealthCheckView.onAppear 에서 호출.
    func load() {
        kotlinViewModel.loadData(
            onCatLoaded: { [weak self] catName, ageMonth in
                DispatchQueue.main.async {
                    self?.catName = catName
                    // ageMonth 는 KotlinInt (boxed) → Int 변환
                    self?.ageMonth = Int(truncating: ageMonth)
                    self?.isLoading = false
                }
            },
            onItemsLoaded: { [weak self] items in
                DispatchQueue.main.async {
                    let list = (items as? [HealthChecklist] ?? []).map { h in
                        HealthCheckItem(
                            id: Int(h.id),
                            month: Int(h.month),
                            // h.itemType.name → Kotlin enum 이름 문자열 (VACCINE/CHECK/SURGERY)
                            itemType: HealthItemTypeSwift(rawValue: h.itemType.name) ?? .check,
                            title: h.title,
                            // Kotlin description 프로퍼티는 ObjC NSObject 충돌로 description_ 로 노출됨
                            description: h.description_,
                            isBreedSpecific: h.isBreedSpecific,
                            isRecommended: h.isRecommended
                        )
                    }
                    self?.allItems = list
                    self?.isLoading = false
                }
            }
        )
    }

    // MARK: Deinit
    deinit {
        kotlinViewModel.dispose()
    }
}
