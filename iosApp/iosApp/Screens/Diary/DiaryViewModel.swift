import SwiftUI
import ComposeApp

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Swift Local Models
// ─────────────────────────────────────────────────────────────────────────────

/// Kotlin CatDiary 모델의 Swift 친화적 복사본.
struct DiaryItem: Identifiable {
    let id: Int64
    let catId: Int64
    let title: String?
    let content: String
    let mood: DiaryMoodSwift?
    let photoPath: String?
    let createdAt: Int64   // epoch ms
    let updatedAt: Int64   // epoch ms
}

/// Kotlin DiaryMood enum 의 Swift 대응 enum
enum DiaryMoodSwift: String, CaseIterable {
    case happy   = "HAPPY"
    case normal  = "NORMAL"
    case sad     = "SAD"
    case sick    = "SICK"
    case playful = "PLAYFUL"

    var emoji: String {
        switch self {
        case .happy:   return "😸"
        case .normal:  return "😐"
        case .sad:     return "😿"
        case .sick:    return "🤒"
        case .playful: return "😺"
        }
    }

    var label: String {
        switch self {
        case .happy:   return "행복"
        case .normal:  return "보통"
        case .sad:     return "슬픔"
        case .sick:    return "아픔"
        case .playful: return "장난"
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - DiaryViewModel
// ─────────────────────────────────────────────────────────────────────────────

/// Android DiaryViewModel 과 동일한 역할을 수행하는 iOS ObservableObject.
///
/// - DiaryKotlinViewModel 을 내부에서 생성하고 콜백으로 데이터를 받는다.
/// - 다이어리 목록은 createdAt 내림차순 정렬 상태로 전달된다.
class DiaryViewModel: ObservableObject {

    // MARK: - Published State

    @Published var catName: String = ""
    @Published var diaries: [DiaryItem] = []
    @Published var showInputSheet: Bool = false
    @Published var editingItem: DiaryItem? = nil   // nil → 신규, non-nil → 수정

    // MARK: - Private

    private let kotlinViewModel = DiaryKotlinViewModel()
    let catId: Int64

    // MARK: - Init

    init(catId: Int64) {
        self.catId = catId
    }

    // MARK: - Load

    /// 데이터를 로드한다. DiaryView.onAppear 에서 호출.
    func load() {
        kotlinViewModel.loadData(
            catId: catId,
            onCatLoaded: { [weak self] name in
                DispatchQueue.main.async {
                    self?.catName = name
                }
            },
            onDiaries: { [weak self] list in
                DispatchQueue.main.async {
                    let items = (list as? [CatDiary] ?? []).map { DiaryViewModel.toItem($0) }
                    self?.diaries = items
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
    func onEditTap(_ item: DiaryItem) {
        editingItem = item
        showInputSheet = true
    }

    /// 시트 닫기
    func onDismissSheet() {
        showInputSheet = false
        editingItem = nil
    }

    // MARK: - Save

    /// 다이어리 저장 (신규 or 수정)
    func saveDiary(
        title: String,
        content: String,
        mood: DiaryMoodSwift?,
        photoPath: String?,
        dateMillis: Int64
    ) {
        let diaryId = editingItem?.id ?? 0

        kotlinViewModel.saveDiary(
            diaryId: diaryId,
            title: title,
            content: content,
            moodRaw: mood?.rawValue ?? "",
            photoPath: photoPath,
            dateMillis: dateMillis,
            onComplete: { [weak self] in
                DispatchQueue.main.async {
                    self?.showInputSheet = false
                    self?.editingItem = nil
                }
            }
        )
    }

    // MARK: - Delete

    /// 다이어리 삭제
    func deleteDiary(id: Int64) {
        kotlinViewModel.deleteDiary(
            diaryId: id,
            onComplete: {
                // Flow 자동 반영
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

    /// epoch ms → Date
    static func toDate(_ epochMs: Int64) -> Date {
        Date(timeIntervalSince1970: Double(epochMs) / 1000)
    }

    /// Date → epoch ms (Int64)
    static func toEpochMs(_ date: Date) -> Int64 {
        Int64(date.timeIntervalSince1970 * 1000)
    }

    /// Kotlin CatDiary → Swift DiaryItem 변환
    static func toItem(_ d: CatDiary) -> DiaryItem {
        let mood = d.mood.flatMap { DiaryMoodSwift(rawValue: $0.name) }
        return DiaryItem(
            id: d.id,
            catId: d.catId,
            title: d.title,
            content: d.content,
            mood: mood,
            photoPath: d.photoPath,
            createdAt: d.createdAt,
            updatedAt: d.updatedAt
        )
    }

    // MARK: - Deinit

    deinit {
        kotlinViewModel.dispose()
    }
}
