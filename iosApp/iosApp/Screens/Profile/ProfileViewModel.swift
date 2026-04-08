import SwiftUI
import ComposeApp

class ProfileViewModel: ObservableObject {
    @Published var name: String = ""
    @Published var birthDate: String = ""
    @Published var gender: String = "UNKNOWN"
    @Published var breedId: Int? = nil
    @Published var breedNameCustom: String = ""
    @Published var isNeutered: Bool = false
    @Published var memo: String = ""
    @Published var photoPath: String? = nil
    @Published var breedSearchResults: [(Int, String)] = []
    @Published var geminiResult: String = ""
    @Published var isLoading: Bool = false
    @Published var isSaved: Bool = false
    @Published var errorMessage: String? = nil

    private let kotlinViewModel = ProfileKotlinViewModel()
    var catId: Int64? = nil

    // 품종 검색
    func searchBreeds(keyword: String) {
        guard keyword.count >= 1 else {
            breedSearchResults = []
            return
        }
        kotlinViewModel.searchBreeds(keyword: keyword) { [weak self] results in
            DispatchQueue.main.async {
                self?.breedSearchResults = results.compactMap { pair in
                    guard let id = pair.first as? Int32,
                          let name = pair.second as? String else { return nil }
                    return (Int(id), name)
                }
            }
        }
    }

    // 품종 선택
    func selectBreed(id: Int, name: String) {
        breedId = id
        breedNameCustom = name
        breedSearchResults = []
    }

    // Gemini 품종 인식
    func recognizeBreed(imageData: Data) {
        isLoading = true
        let bytes = KotlinByteArray(size: Int32(imageData.count))
        for (index, byte) in imageData.enumerated() {
            bytes.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        kotlinViewModel.recognizeBreed(
            imageBytes: bytes,
            onResult: { [weak self] breedName, breedId, confidence in
                DispatchQueue.main.async {
                    self?.breedNameCustom = breedName
                    self?.breedId = breedId.map { Int($0.intValue) }
                    let confidenceValue = confidence.doubleValue
                    self?.geminiResult = "\(breedName) (\(Int(confidenceValue * 100))%)"
                    self?.isLoading = false
                }
            },
            onError: { [weak self] in
                DispatchQueue.main.async {
                    self?.errorMessage = "품종 인식에 실패했어요"
                    self?.isLoading = false
                }
            }
        )
    }

    // 저장
    func save() {
        guard !name.isEmpty, !birthDate.isEmpty else {
            errorMessage = "이름과 생년월은 필수예요"
            return
        }
        let formattedDate = birthDate.count == 6 ?
               "\(birthDate.prefix(4))-\(birthDate.suffix(2))" : birthDate
        isLoading = true
        kotlinViewModel.saveCat(
            catId: catId.map { KotlinLong(value: $0) },
            name: name,
            birthDate: formattedDate,
            gender: gender,
            breedId: breedId != nil ? KotlinInt(value: Int32(breedId!)) : nil,
            breedNameCustom: breedNameCustom,
            isNeutered: isNeutered,
            memo: memo,
            photoPath: photoPath,
            onSuccess: { [weak self] in
                DispatchQueue.main.async {
                    self?.isLoading = false
                    self?.isSaved = true
                }
            },
            onError: { [weak self] in
                DispatchQueue.main.async {
                    self?.isLoading = false
                    self?.errorMessage = "저장에 실패했어요"
                }
            }
        )
    }

    deinit {
        kotlinViewModel.dispose()
    }
}
