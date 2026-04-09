import SwiftUI
import ComposeApp

// MARK: - Swift Local Models

/// WeightRecord (Kotlin) 의 Swift 친화적 복사본.
/// Int32/Int64 타입 변환을 ViewModel 에서 한 번만 처리하기 위해 사용.
struct WeightRecordItem: Identifiable {
    let id: Int64
    let weightG: Int
    let recordedAt: Int64   // epoch milliseconds
    let memo: String?
}

/// BreedAvgPoint (Kotlin) 의 Swift 친화적 복사본.
struct BreedAvgItem: Identifiable {
    var id: Int { month }
    let month: Int
    let weightMinG: Int
    let weightMaxG: Int
    let avgWeightG: Int
}

/// 체중 화면 탭
enum WeightTabType: CaseIterable {
    case myCat
    case breedAverage

    var label: String {
        switch self {
        case .myCat:        return "내 고양이 추이"
        case .breedAverage: return "품종 평균 성장"
        }
    }
}

// MARK: - WeightViewModel

/// Android WeightViewModel 과 동일한 역할을 수행하는 iOS ObservableObject.
///
/// - WeightKotlinViewModel 을 내부에서 생성하고 콜백을 통해 데이터를 받는다.
/// - catId 는 HomeViewModel(HomeView) 에서 넘어오는 고양이 ID.
class WeightViewModel: ObservableObject {

    // MARK: Published State
    @Published var catName: String = ""
    @Published var birthDate: String = ""           // "yyyy-MM" 형식
    @Published var weightHistory: [WeightRecordItem] = []
    @Published var breedAverageData: [BreedAvgItem] = []
    @Published var latestWeightG: Int? = nil
    @Published var selectedTab: WeightTabType = .myCat
    @Published var showInputDialog: Bool = false

    // MARK: Private
    private let kotlinViewModel = WeightKotlinViewModel()
    let catId: Int64

    // MARK: Init
    init(catId: Int64) {
        self.catId = catId
    }

    // MARK: Load

    /// 데이터를 로드한다. WeightView.onAppear 에서 호출.
    func load() {
        kotlinViewModel.loadData(
            catId: catId,
            onCatLoaded: { [weak self] catName, birthDate in
                DispatchQueue.main.async {
                    self?.catName = catName
                    self?.birthDate = birthDate
                }
            },
            onWeightHistoryLoaded: { [weak self] records in
                DispatchQueue.main.async {
                    // List<WeightRecord> → [WeightRecordItem]
                    let items: [WeightRecordItem] = (records as? [WeightRecord] ?? []).map { r in
                        WeightRecordItem(
                            id: r.id,
                            weightG: Int(r.weightG),
                            recordedAt: r.recordedAt,
                            memo: r.memo
                        )
                    }
                    self?.weightHistory = items
                    self?.latestWeightG = items.last?.weightG
                }
            },
            onBreedAverageLoaded: { [weak self] points in
                DispatchQueue.main.async {
                    // List<BreedAvgPoint> → [BreedAvgItem]
                    let items: [BreedAvgItem] = (points as? [BreedAvgPoint] ?? []).map { p in
                        BreedAvgItem(
                            month: Int(p.month),
                            weightMinG: Int(p.weightMinG),
                            weightMaxG: Int(p.weightMaxG),
                            avgWeightG: Int(p.avgWeightG)
                        )
                    }
                    self?.breedAverageData = items
                }
            }
        )
    }

    // MARK: Actions

    /// 체중 저장. WeightInputSheet 저장 버튼에서 호출.
    func insertWeight(weightKg: String, memo: String) {
        kotlinViewModel.insertWeight(
            catId: catId,
            weightKg: weightKg,
            memo: memo,
            onComplete: { [weak self] in
                DispatchQueue.main.async {
                    self?.showInputDialog = false
                }
            }
        )
    }

    // MARK: Helpers

    /// 기록 시점의 개월 수 계산 (birthDate: "yyyy-MM", recordedAt: epoch ms)
    func ageMonthAt(recordedAt: Int64) -> Int {
        guard !birthDate.isEmpty else { return 0 }
        let parts = birthDate.split(separator: "-")
        guard parts.count >= 2,
              let birthYear = Int(parts[0]),
              let birthMonth = Int(parts[1]) else { return 0 }

        let date = Date(timeIntervalSince1970: Double(recordedAt) / 1000)
        let cal = Calendar.current
        let comps = cal.dateComponents([.year, .month], from: date)
        guard let recYear = comps.year, let recMonth = comps.month else { return 0 }

        return (recYear - birthYear) * 12 + (recMonth - birthMonth)
    }

    /// epoch ms → "yyyy.MM.dd" 형식 문자열
    static func formatDate(_ epochMs: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMs) / 1000)
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy.MM.dd"
        return fmt.string(from: date)
    }

    /// epoch ms → Date (Swift Charts x축용)
    static func toDate(_ epochMs: Int64) -> Date {
        return Date(timeIntervalSince1970: Double(epochMs) / 1000)
    }

    // MARK: Deinit
    deinit {
        kotlinViewModel.dispose()
    }
}
