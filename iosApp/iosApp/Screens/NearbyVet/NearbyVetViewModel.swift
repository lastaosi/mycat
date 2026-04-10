import SwiftUI
import MapKit
import CoreLocation

// ═══════════════════════════════════════════════════════════════════
// MARK: - Swift Local Models
// ═══════════════════════════════════════════════════════════════════

/// Google Places API 결과를 담는 Swift 친화적 동물병원 모델.
struct VetPlaceItem: Identifiable {
    let id: String                          // placeId
    let name: String
    let address: String
    let coordinate: CLLocationCoordinate2D
    let rating: Double?
    let isOpen: Bool?
    let phoneNumber: String?
    let distance: Double?                   // 현재 위치로부터 거리 (m)

    /// 거리 표시 문자열: 1km 미만이면 "NNNm", 이상이면 "N.Nkm"
    var distanceText: String? {
        guard let d = distance else { return nil }
        return d >= 1000 ? String(format: "%.1fkm", d / 1000) : "\(Int(d))m"
    }
    /// 별점 표시 문자열 (소수점 1자리)
    var ratingText: String? {
        guard let r = rating else { return nil }
        return String(format: "%.1f", r)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - Google Places API Codable 모델
// ═══════════════════════════════════════════════════════════════════

private struct PlacesSearchResponse: Codable {
    let places: [PlaceResult]?
}

private struct PlaceResult: Codable {
    let id: String?
    let displayName: PlaceDisplayName?
    let formattedAddress: String?
    let location: PlaceLocation?
    let rating: Double?
    let currentOpeningHours: PlaceOpeningHours?
    let nationalPhoneNumber: String?
}

private struct PlaceDisplayName: Codable {
    let text: String?
}

private struct PlaceLocation: Codable {
    let latitude: Double?
    let longitude: Double?
}

private struct PlaceOpeningHours: Codable {
    let openNow: Bool?
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - NearbyVetViewModel
// ═══════════════════════════════════════════════════════════════════

/// iOS 근처 동물병원 화면 ObservableObject ViewModel.
///
/// ## 데이터 흐름
/// 1. `requestLocation()` → CLLocationManager 권한 요청 or 위치 조회
/// 2. `locationManager(_:didUpdateLocations:)` → 현재 위치 수신
/// 3. `searchNearbyVets(lat:lng:)` → Google Places API (New) POST 호출
/// 4. 결과를 VetPlaceItem 으로 변환, CLLocation.distance 로 거리 계산 후 정렬
///
/// ## API
/// `POST https://places.googleapis.com/v1/places:searchNearby`
/// - 헤더: `X-Goog-Api-Key`, `X-Goog-FieldMask`
/// - 반경 2km, includedTypes: veterinary_care, 최대 20개
class NearbyVetViewModel: NSObject, ObservableObject, CLLocationManagerDelegate {

    // MARK: Published State
    @Published var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 37.5665, longitude: 126.9780),  // 서울 기본값
        latitudinalMeters: 3000,
        longitudinalMeters: 3000
    )
    @Published var vets: [VetPlaceItem] = []
    @Published var selectedVet: VetPlaceItem? = nil
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var locationDenied: Bool = false

    // MARK: Private
    private let locationManager = CLLocationManager()
    private var userLocation: CLLocation? = nil

    /// Info.plist 의 MAPS_API_KEY 값 (Config.xcconfig 에서 주입됨)
    private var apiKey: String {
        Bundle.main.object(forInfoDictionaryKey: "MAPS_API_KEY") as? String ?? ""
    }

    // MARK: Init
    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    // MARK: Location

    /// 위치 권한 상태에 따라 권한 요청 또는 위치 조회를 시작한다.
    /// NearbyVetView.onAppear 에서 호출.
    func requestLocation() {
        switch locationManager.authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            isLoading = true
            locationManager.requestLocation()
        case .denied, .restricted:
            locationDenied = true
        @unknown default:
            break
        }
    }

    /// 재시도: 오류 초기화 후 위치 조회 재시작.
    func retry() {
        errorMessage = nil
        vets = []
        selectedVet = nil
        requestLocation()
    }

    /// 병원 선택: 지도 카메라를 선택 병원으로 이동한다.
    func selectVet(_ vet: VetPlaceItem) {
        selectedVet = selectedVet?.id == vet.id ? nil : vet
        if let selected = selectedVet {
            withAnimation {
                region = MKCoordinateRegion(
                    center: selected.coordinate,
                    latitudinalMeters: 1000,
                    longitudinalMeters: 1000
                )
            }
        }
    }

    // MARK: CLLocationManagerDelegate

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            DispatchQueue.main.async { self.isLoading = true }
            manager.requestLocation()
        case .denied, .restricted:
            DispatchQueue.main.async { self.locationDenied = true }
        default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.first else { return }
        userLocation = loc
        DispatchQueue.main.async {
            self.region = MKCoordinateRegion(
                center: loc.coordinate,
                latitudinalMeters: 2500,
                longitudinalMeters: 2500
            )
        }
        Task { await searchNearbyVets(lat: loc.coordinate.latitude, lng: loc.coordinate.longitude) }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        DispatchQueue.main.async {
            self.isLoading = false
            self.errorMessage = "위치를 가져올 수 없어요.\n설정에서 위치 권한을 확인해주세요."
        }
    }

    // MARK: Google Places API

    /// Google Places API (New) searchNearby 를 호출해 근처 동물병원을 조회한다.
    ///
    /// - Parameters:
    ///   - lat: 현재 위도
    ///   - lng: 현재 경도
    @MainActor
    private func searchNearbyVets(lat: Double, lng: Double) async {
        guard !apiKey.isEmpty else {
            errorMessage = "API Key가 설정되지 않았어요."
            isLoading = false
            return
        }

        guard let url = URL(string: "https://places.googleapis.com/v1/places:searchNearby") else {
            isLoading = false
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-Goog-Api-Key")
        request.setValue(
            "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.currentOpeningHours,places.nationalPhoneNumber",
            forHTTPHeaderField: "X-Goog-FieldMask"
        )

        let body: [String: Any] = [
            "locationRestriction": [
                "circle": [
                    "center": ["latitude": lat, "longitude": lng],
                    "radius": 2000.0
                ]
            ],
            "includedTypes": ["veterinary_care"],
            "maxResultCount": 20,
            "languageCode": "ko"
        ]

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
            let (data, _) = try await URLSession.shared.data(for: request)
            let response = try JSONDecoder().decode(PlacesSearchResponse.self, from: data)
            let userLoc = CLLocation(latitude: lat, longitude: lng)

            let items: [VetPlaceItem] = (response.places ?? []).compactMap { place in
                guard let placeId = place.id,
                      let plcLat = place.location?.latitude,
                      let plcLng = place.location?.longitude else { return nil }

                let coord = CLLocationCoordinate2D(latitude: plcLat, longitude: plcLng)
                // CLLocation.distance(from:) 로 거리 계산 (m)
                let dist = userLoc.distance(from: CLLocation(latitude: plcLat, longitude: plcLng))

                return VetPlaceItem(
                    id: placeId,
                    name: place.displayName?.text ?? "이름 없음",
                    address: place.formattedAddress ?? "주소 없음",
                    coordinate: coord,
                    rating: place.rating,
                    isOpen: place.currentOpeningHours?.openNow,
                    phoneNumber: place.nationalPhoneNumber,
                    distance: dist
                )
            }.sorted { ($0.distance ?? 0) < ($1.distance ?? 0) }

            vets = items
            isLoading = false
        } catch {
            errorMessage = "동물병원 검색에 실패했어요.\n잠시 후 다시 시도해주세요."
            isLoading = false
        }
    }
}
