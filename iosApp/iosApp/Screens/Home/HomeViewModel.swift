import SwiftUI
import ComposeApp

class HomeViewModel: ObservableObject {
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

    private let kotlinViewModel = HomeKotlinViewModel()

    func load() {
        kotlinViewModel.loadData(
            onCatLoaded: { [weak self] name, breedName in
                DispatchQueue.main.async {
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
                    self?.healthCheckTitles = titles as! [String]
                }
            }
        )
    }
    
    func reload() {
        kotlinViewModel.dispose()
        // kotlinViewModel 재생성
        load()
    }

    deinit {
        kotlinViewModel.dispose()
    }
    
    
}
