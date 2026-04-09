import SwiftUI
import ComposeApp

// MARK: - MainDrawerViewModel
/// 사이드 드로어 헤더 데이터를 관리하는 Swift ViewModel.
/// Kotlin MainKotlinViewModel 에서 고양이 목록을 가져와
/// representativeCat / allCats 를 @Published 로 노출.
class MainDrawerViewModel: ObservableObject {

    // ── Published 상태 ──────────────────────────────────────────────────
    @Published var representativeCat: CatSummaryIos?
    @Published var allCats: [CatSummaryIos] = []

    // ── Private ─────────────────────────────────────────────────────────
    private var kotlinViewModel: MainKotlinViewModel? = MainKotlinViewModel()

    // ── 현재 대표 고양이 ID (catId 기반 화면 이동용) ─────────────────────
    var currentCatId: Int64 {
        representativeCat?.id ?? 0
    }

    // MARK: - Load
    func load() {
        kotlinViewModel?.loadCats { [weak self] cats in
            DispatchQueue.main.async {
                // Kotlin List<CatSummaryIos> → Swift [CatSummaryIos]
                let catList = cats as? [CatSummaryIos] ?? cats.compactMap { $0 as? CatSummaryIos }
                self?.allCats = catList
                self?.representativeCat = catList.first { $0.isRepresentative } ?? catList.first
            }
        }
    }

    // MARK: - Cat 전환
    func selectCat(catId: Int64) {
        kotlinViewModel?.setRepresentative(catId: catId)
    }

    // MARK: - Cleanup
    deinit {
        kotlinViewModel?.dispose()
        kotlinViewModel = nil
    }
}
