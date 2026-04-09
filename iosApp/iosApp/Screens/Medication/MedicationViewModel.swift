import SwiftUI
import ComposeApp

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Swift Local Models
// ─────────────────────────────────────────────────────────────────────────────

/// Kotlin Medication 모델의 Swift 친화적 복사본.
/// Int 변환 및 옵셔널 처리를 ViewModel 에서 한 번만 수행하기 위해 사용.
struct MedicationItem: Identifiable {
    let id: Int64
    let catId: Int64
    let name: String
    let dosage: String?
    let medicationType: MedicationTypeSwift
    let intervalDays: Int?
    let startDate: Int64        // epoch ms
    let endDate: Int64?         // epoch ms
    let memo: String?
    let isActive: Bool
    let createdAt: Int64
}

/// Kotlin MedicationType enum 의 Swift 대응 enum
enum MedicationTypeSwift: String, CaseIterable {
    case once     = "ONCE"
    case daily    = "DAILY"
    case interval = "INTERVAL"
    case period   = "PERIOD"

    var label: String {
        switch self {
        case .once:     return "1회"
        case .daily:    return "매일"
        case .interval: return "간격"
        case .period:   return "기간"
        }
    }

    /// Android MedicationTypeBadge 와 동일한 색상 매핑
    var badgeColor: MedicationBadgeColor {
        switch self {
        case .once:     return .muted
        case .daily:    return .primary
        case .interval: return .secondary
        case .period:   return .success
        }
    }
}

enum MedicationBadgeColor {
    case muted, primary, secondary, success
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MedicationViewModel
// ─────────────────────────────────────────────────────────────────────────────

/// Android MedicationViewModel 과 동일한 역할을 수행하는 iOS ObservableObject.
///
/// - MedicationKotlinViewModel 을 내부에서 생성하고 콜백으로 데이터를 받는다.
/// - 알람 스케줄링은 MedicationNotificationManager 를 통해 처리한다.
class MedicationViewModel: ObservableObject {

    // MARK: - Published State

    @Published var catName: String = ""
    @Published var activeMedications: [MedicationItem] = []
    @Published var inactiveMedications: [MedicationItem] = []
    @Published var showInputSheet: Bool = false
    @Published var editingItem: MedicationItem? = nil   // nil → 신규, non-nil → 수정

    // MARK: - Private

    private let kotlinViewModel = MedicationKotlinViewModel()
    private let notificationManager = MedicationNotificationManager.shared
    let catId: Int64

    // MARK: - Init

    init(catId: Int64) {
        self.catId = catId
    }

    // MARK: - Load

    /// 데이터를 로드한다. MedicationView.onAppear 에서 호출.
    func load() {
        kotlinViewModel.loadData(
            catId: catId,
            onCatLoaded: { [weak self] name in
                DispatchQueue.main.async {
                    self?.catName = name
                }
            },
            onActiveMedications: { [weak self] list in
                DispatchQueue.main.async {
                    let items = (list as? [Medication] ?? []).map { MedicationViewModel.toItem($0) }
                    self?.activeMedications = items
                }
            },
            onAllMedications: { [weak self] list in
                DispatchQueue.main.async {
                    let items = (list as? [Medication] ?? [])
                        .filter { !$0.isActive }
                        .map { MedicationViewModel.toItem($0) }
                    self?.inactiveMedications = items
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
    func onEditTap(_ item: MedicationItem) {
        editingItem = item
        showInputSheet = true
    }

    /// 시트 닫기
    func onDismissSheet() {
        showInputSheet = false
        editingItem = nil
    }

    // MARK: - Save

    /// 약 복용 기록 저장 (신규 or 수정)
    func saveMedication(
        name: String,
        medicationType: MedicationTypeSwift,
        dosage: String,
        startDate: Int64,
        endDate: Int64?,
        intervalDays: Int?,
        memo: String,
        alarmTimes: [String]
    ) {
        let medId = editingItem?.id ?? 0

        kotlinViewModel.saveMedication(
            medicationId: medId,
            name: name,
            medicationType: medicationType.rawValue,
            dosage: dosage,
            startDate: startDate,
            endDate: endDate.map { KotlinLong(value: $0) },
            intervalDays: intervalDays.map { KotlinInt(value: Int32($0)) },
            memo: memo,
            alarmTimes: alarmTimes,
            onScheduleAlarms: { [weak self] mId, catName, medName, times in
                guard let self = self else { return }
                let timeList = (times as? [String]) ?? []
                self.notificationManager.scheduleAlarms(
                    medicationId: mId.int64Value,
                    catName: catName,
                    medName: medName,
                    alarmTimes: timeList
                )
            },
            onCancelAlarms: { [weak self] _ in
                // alarmIds 로 개별 취소도 가능하지만 medicationId prefix 로 일괄 취소가 간단함
                // editingItem?.id 는 이미 캡처되어 있음
                if let editId = self?.editingItem?.id {
                    self?.notificationManager.cancelAlarms(medicationId: editId)
                }
            },
            onComplete: { [weak self] in
                DispatchQueue.main.async {
                    self?.showInputSheet = false
                    self?.editingItem = nil
                }
            }
        )
    }

    // MARK: - Delete

    /// 약 삭제 (알람도 함께 취소)
    func deleteMedication(id: Int64) {
        notificationManager.cancelAlarms(medicationId: id)
        kotlinViewModel.deleteMedication(
            medicationId: id,
            onCancelAlarms: { _ in
                // Swift 쪽에서 이미 취소 처리 완료
            },
            onComplete: {
                // Flow 업데이트로 자동 반영
            }
        )
    }

    // MARK: - Toggle Active

    /// 복용 중 / 완료 토글
    func toggleActive(item: MedicationItem) {
        kotlinViewModel.toggleActive(
            medicationId: item.id,
            currentIsActive: item.isActive,
            onCancelAlarms: { [weak self] _ in
                self?.notificationManager.cancelAlarms(medicationId: item.id)
            },
            onScheduleAlarms: { [weak self] mId, catName, medName, times in
                guard let self = self else { return }
                let timeList = (times as? [String]) ?? []
                self.notificationManager.scheduleAlarms(
                    medicationId: mId.int64Value,
                    catName: catName,
                    medName: medName,
                    alarmTimes: timeList
                )
            },
            onComplete: {
                // Flow 자동 반영
            }
        )
    }

    // MARK: - Helpers

    /// epoch ms → "yyyy-MM-dd" 문자열
    static func formatDate(_ epochMs: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMs) / 1000)
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        return fmt.string(from: date)
    }

    /// epoch ms → Date
    static func toDate(_ epochMs: Int64) -> Date {
        Date(timeIntervalSince1970: Double(epochMs) / 1000)
    }

    /// Date → epoch ms (Int64)
    static func toEpochMs(_ date: Date) -> Int64 {
        Int64(date.timeIntervalSince1970 * 1000)
    }

    /// Kotlin Medication → Swift MedicationItem 변환
    static func toItem(_ m: Medication) -> MedicationItem {
        let type = MedicationTypeSwift(rawValue: m.medicationType.name) ?? .daily
        return MedicationItem(
            id: m.id,
            catId: m.catId,
            name: m.name,
            dosage: m.dosage,
            medicationType: type,
            intervalDays: m.intervalDays?.intValue,
            startDate: m.startDate,
            endDate: m.endDate?.int64Value,
            memo: m.memo,
            isActive: m.isActive,
            createdAt: m.createdAt
        )
    }

    // MARK: - Deinit

    deinit {
        kotlinViewModel.dispose()
    }
}
