import SwiftUI
import ComposeApp

// MARK: - HomeViewModel

/// Android MainViewModel 과 동일한 역할을 수행하는 iOS ObservableObject.
///
/// - HomeKotlinViewModel 을 내부에서 생성하고 콜백을 통해 데이터를 받는다.
/// - 대표 고양이를 기준으로 오늘의 케어 가이드, 건강 체크리스트,
///   최근 체중, 랜덤 팁 등을 @Published 로 노출한다.
class HomeViewModel: ObservableObject {

    // MARK: - Published State

    @Published var catId: Int64 = 0         // NavigationLink(value: .weight/vaccination/...) 에 사용
    @Published var catName: String = ""
    @Published var breedName: String = ""
    @Published var foodDryG: Int = 0
    @Published var foodWetG: Int = 0
    @Published var waterMl: Int = 0
    @Published var weightMinG: Int = 0
    @Published var weightMaxG: Int = 0
    @Published var latestWeightG: Int = 0
    @Published var randomTip: String = ""
    @Published var healthCheckTitles: [String] = []

    // MARK: - Private

    private let kotlinViewModel = HomeKotlinViewModel()

    // MARK: - Load

    /// 데이터를 로드한다. HomeView.onAppear 에서 호출.
    func load() {
        kotlinViewModel.loadData(
            onCatLoaded: { [weak self] catId, name, breedName in
                DispatchQueue.main.async {
                    self?.catId = catId.int64Value
                    self?.catName = name
                    self?.breedName = breedName
                }
            },
            onGuideLoaded: { [weak self] dryG, wetG, waterMl, minG, maxG in
                DispatchQueue.main.async {
                    self?.foodDryG = Int(truncating: dryG)
                    self?.foodWetG = Int(truncating: wetG)
                    self?.waterMl = Int(truncating: waterMl)
                    self?.weightMinG = Int(truncating: minG)
                    self?.weightMaxG = Int(truncating: maxG)
                }
            },
            onWeightLoaded: { [weak self] weightG in
                DispatchQueue.main.async {
                    self?.latestWeightG = Int(truncating: weightG)
                }
            },
            onTipLoaded: { [weak self] tip in
                DispatchQueue.main.async {
                    self?.randomTip = tip
                }
            },
            onHealthCheckLoaded: { [weak self] titles in
                DispatchQueue.main.async {
                    self?.healthCheckTitles = titles as? [String] ?? []
                }
            }
        )
    }
    
    // MARK: - Reload

    /// KotlinViewModel 을 재구독하여 데이터를 다시 로드한다.
    /// (현재는 dispose 후 load() 재호출로 구현 — 추후 개선 예정)
    func reload() {
        kotlinViewModel.dispose()
        load()
    }

    // MARK: - Deinit

    deinit {
        kotlinViewModel.dispose()
    }
}
