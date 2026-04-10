import SwiftUI
import ComposeApp

// ═══════════════════════════════════════════════════════════════════
// MARK: - Swift Local Model
// ═══════════════════════════════════════════════════════════════════

/// BreedMonthlyGuide (Kotlin) 의 Swift 친화적 복사본.
/// Int32 타입 변환을 ViewModel 에서 한 번만 처리한다.
struct BreedMonthlyGuideItem: Identifiable {
    let id: Int64
    let breedId: Int
    let month: Int
    let weightMinG: Int
    let weightMaxG: Int
    let foodDryG: Int
    let foodWetG: Int
    let waterMl: Int
    let treatMaxG: Int

    /// 체중 최솟값을 kg 문자열로 반환 (소수점 1자리)
    var weightMinKg: String {
        String(format: "%.1f", Double(weightMinG) / 1000.0)
    }
    /// 체중 최댓값을 kg 문자열로 반환
    var weightMaxKg: String {
        String(format: "%.1f", Double(weightMaxG) / 1000.0)
    }

    /// month 를 "N개월" 또는 "N년 M개월" 형식으로 반환
    var monthLabel: String {
        if month >= 13 {
            let year = month / 12
            let remain = month % 12
            return remain == 0 ? "\(year)년" : "\(year)년 \(remain)개월"
        }
        return "\(month)개월"
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - CareGuideViewModel
// ═══════════════════════════════════════════════════════════════════

/// iOS 케어 가이드 화면 ObservableObject ViewModel.
///
/// - CareGuideKotlinViewModel 을 내부에서 생성하고 콜백으로 데이터를 수신한다.
/// - 대표 고양이를 자동으로 로드하므로 catId 파라미터가 필요하지 않다.
/// - breedId 가 없는 경우 hasBreed = false 로 안내 UI 를 표시한다.
class CareGuideViewModel: ObservableObject {

    // MARK: Published State
    @Published var catName: String = ""
    @Published var breedName: String = ""
    @Published var ageMonth: Int = 0
    @Published var guides: [BreedMonthlyGuideItem] = []
    @Published var hasBreed: Bool = true
    @Published var isLoading: Bool = true

    // MARK: Computed

    /// 현재 월령과 일치하는 가이드 항목.
    /// 정확히 일치하는 달이 없으면 가장 가까운 이전 달을 반환한다.
    var currentGuide: BreedMonthlyGuideItem? {
        guides.last { $0.month <= ageMonth } ?? guides.first
    }

    // MARK: Private
    private let kotlinViewModel = CareGuideKotlinViewModel()

    // MARK: Load

    /// 대표 고양이 기준으로 케어 가이드를 로드한다.
    /// CareGuideView.onAppear 에서 호출.
    func load() {
        kotlinViewModel.loadData(
            onLoaded: { [weak self] catName, breedName, ageMonth, guides in
                DispatchQueue.main.async {
                    self?.catName = catName
                    self?.breedName = breedName
                    // ageMonth 는 KotlinInt (boxed) → Int 변환
                    self?.ageMonth = Int(truncating: ageMonth)
                    self?.guides = (guides as? [BreedMonthlyGuide] ?? []).map { g in
                        BreedMonthlyGuideItem(
                            id: g.id,
                            breedId: Int(g.breedId),
                            month: Int(g.month),
                            weightMinG: Int(g.weightMinG),
                            weightMaxG: Int(g.weightMaxG),
                            foodDryG: Int(g.foodDryG),
                            foodWetG: Int(g.foodWetG),
                            waterMl: Int(g.waterMl),
                            treatMaxG: Int(g.treatMaxG)
                        )
                    }
                    self?.hasBreed = true
                    self?.isLoading = false
                }
            },
            onNoBreed: { [weak self] in
                DispatchQueue.main.async {
                    self?.hasBreed = false
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
