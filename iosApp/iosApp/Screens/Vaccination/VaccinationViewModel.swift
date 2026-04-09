import SwiftUI
import ComposeApp

// MARK: - Swift Local Models

/// VaccinationRecord (Kotlin) 의 Swift 친화적 복사본.
/// Int32/Int64 변환을 ViewModel 에서 한 번만 처리하기 위해 사용.
struct VaccinationItem: Identifiable {
    let id: Int64
    let catId: Int64
    let title: String
    let vaccinatedAt: Int64          // epoch ms
    let nextDueAt: Int64?            // epoch ms (없으면 nil)
    let memo: String?
    let isNotificationEnabled: Bool
}

// MARK: - VaccinationViewModel

/// Android VaccinationViewModel 과 동일한 역할을 수행하는 iOS ObservableObject.
///
/// - VaccinationKotlinViewModel 을 내부에서 생성하고 콜백을 통해 데이터를 받는다.
/// - catId 는 HomeView 에서 넘어오는 고양이 ID.
class VaccinationViewModel: ObservableObject {

    // MARK: - Published State

    @Published var catName: String = ""
    @Published var records: [VaccinationItem] = []
    @Published var showInputSheet: Bool = false
    @Published var editingItem: VaccinationItem? = nil   // nil → 신규, non-nil → 수정

    // MARK: - Private

    private let kotlinViewModel = VaccinationKotlinViewModel()
    let catId: Int64

    // MARK: - Init

    init(catId: Int64) {
        self.catId = catId
    }

    // MARK: - Load

    /// 데이터를 로드한다. VaccinationView.onAppear 에서 호출.
    func load() {
        kotlinViewModel.loadData(
            catId: catId,
            onCatLoaded: { [weak self] catName in
                DispatchQueue.main.async {
                    self?.catName = catName
                }
            },
            onRecordsLoaded: { [weak self] records in
                DispatchQueue.main.async {
                    let items: [VaccinationItem] = (records as? [VaccinationRecord] ?? []).map { r in
                        VaccinationItem(
                            id: r.id,
                            catId: r.catId,
                            title: r.title,
                            vaccinatedAt: r.vaccinatedAt,
                            nextDueAt: r.nextDueAt?.int64Value,
                            memo: r.memo,
                            isNotificationEnabled: r.isNotificationEnabled
                        )
                    }
                    self?.records = items
                }
            }
        )
    }

    // MARK: - Actions

    /// FAB 탭 → 신규 입력 시트 표시
    func onFabTap() {
        editingItem = nil
        showInputSheet = true
    }

    /// 수정 버튼 탭 → 수정 시트 표시
    func onEditTap(_ item: VaccinationItem) {
        editingItem = item
        showInputSheet = true
    }

    /// 시트 닫기
    func onDismissSheet() {
        showInputSheet = false
        editingItem = nil
    }

    /// 접종 기록 저장 (신규 or 수정)
    func saveVaccination(
        title: String,
        vaccinatedAt: Int64,
        nextDueAt: Int64?,
        memo: String,
        isNotificationEnabled: Bool
    ) {
        let recordId = editingItem?.id ?? 0
        kotlinViewModel.saveVaccination(
            recordId: recordId,
            title: title,
            vaccinatedAt: vaccinatedAt,
            nextDueAt: nextDueAt.map { KotlinLong(value: $0) },
            memo: memo,
            isNotificationEnabled: isNotificationEnabled,
            onComplete: { [weak self] in
                DispatchQueue.main.async {
                    self?.showInputSheet = false
                    self?.editingItem = nil
                }
            }
        )
    }

    /// 접종 기록 삭제
    func deleteVaccination(id: Int64) {
        kotlinViewModel.deleteVaccination(
            recordId: id,
            onComplete: {
                // Flow 업데이트로 자동 반영 — 별도 처리 불필요
            }
        )
    }

    // MARK: - Helpers

    /// epoch ms → "yyyy.MM.dd" 문자열
    static func formatDate(_ epochMs: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMs) / 1000)
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy.MM.dd"
        return fmt.string(from: date)
    }

    /// D-Day 계산 (nextDueAt 기준)
    static func dDayLabel(nextDueAt: Int64) -> String {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let diffDays = Int((nextDueAt - nowMs) / (1000 * 60 * 60 * 24))
        switch diffDays {
        case ..<0:  return "기한 지남"
        case 0:     return "오늘"
        default:    return "D-\(diffDays)"
        }
    }

    /// D-Day가 0~7이면 강조 색상
    static func dDayColor(nextDueAt: Int64) -> Color {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let diffDays = Int((nextDueAt - nowMs) / (1000 * 60 * 60 * 24))
        return (0...7).contains(diffDays) ? MyCatColors.primary : MyCatColors.onBackground
    }

    // MARK: - Deinit

    deinit {
        kotlinViewModel.dispose()
    }
}
