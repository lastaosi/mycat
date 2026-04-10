import SwiftUI
import MapKit
import CoreLocation

// ═══════════════════════════════════════════════════════════════════
// MARK: - NearbyVetView  (Android NearbyVetScreen 대응)
// ═══════════════════════════════════════════════════════════════════

struct NearbyVetView: View {

    @StateObject private var viewModel = NearbyVetViewModel()
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        VStack(spacing: 0) {

            // ─── TopBar ──────────────────────────────────────────
            NearbyVetTopBar()
                .environmentObject(drawerState)

            // ─── 본문 ─────────────────────────────────────────────
            if viewModel.locationDenied {
                NearbyVetPermissionDeniedView()
            } else {
                GeometryReader { geo in
                    VStack(spacing: 0) {

                        // ─── 지도 (상단 40%) ──────────────────────
                        NearbyVetMap(viewModel: viewModel)
                            .frame(height: geo.size.height * 0.40)

                        // ─── 구분선 ───────────────────────────────
                        Rectangle()
                            .fill(MyCatColors.border)
                            .frame(height: 1)

                        // ─── 목록 (하단 60%) ──────────────────────
                        NearbyVetList(viewModel: viewModel)
                            .frame(height: geo.size.height * 0.60 - 1)
                    }
                }
            }
        }
        .background(MyCatColors.background)
        .navigationBarHidden(true)
        .onAppear { viewModel.requestLocation() }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - NearbyVetTopBar
// ═══════════════════════════════════════════════════════════════════

private struct NearbyVetTopBar: View {
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        HStack(spacing: 0) {
            DrawerHamburgerButton(tint: MyCatColors.onPrimary)
                .environmentObject(drawerState)

            Spacer().frame(width: 4)

            Text("근처 동물병원")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(MyCatColors.onPrimary)

            Spacer()
        }
        .padding(.horizontal, 8)
        .frame(height: 56)
        .background(MyCatColors.primary)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - NearbyVetMap  (MapKit 지도)
// ═══════════════════════════════════════════════════════════════════

private struct NearbyVetMap: View {
    @ObservedObject var viewModel: NearbyVetViewModel

    var body: some View {
        ZStack {
            Map(
                coordinateRegion: $viewModel.region,
                showsUserLocation: true,
                annotationItems: viewModel.vets
            ) { vet in
                MapAnnotation(coordinate: vet.coordinate) {
                    VetMapPin(isSelected: viewModel.selectedVet?.id == vet.id)
                        .onTapGesture { viewModel.selectVet(vet) }
                }
            }

            // 로딩 오버레이
            if viewModel.isLoading {
                Color.black.opacity(0.30)
                    .ignoresSafeArea()
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.3)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - VetMapPin  (지도 핀 마커)
// ═══════════════════════════════════════════════════════════════════

private struct VetMapPin: View {
    let isSelected: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(isSelected ? MyCatColors.primary : Color.red)
                .frame(width: isSelected ? 18 : 14, height: isSelected ? 18 : 14)
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
            if isSelected {
                Circle()
                    .stroke(Color.white, lineWidth: 2)
                    .frame(width: 18, height: 18)
            }
        }
        .animation(.easeInOut(duration: 0.18), value: isSelected)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - NearbyVetList  (동물병원 목록)
// ═══════════════════════════════════════════════════════════════════

private struct NearbyVetList: View {
    @ObservedObject var viewModel: NearbyVetViewModel

    var body: some View {
        Group {
            if let error = viewModel.errorMessage {
                // 오류 상태
                VStack(spacing: 14) {
                    Text("😿")
                        .font(.system(size: 40))
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                        .multilineTextAlignment(.center)
                    Button(action: { viewModel.retry() }) {
                        Text("다시 시도")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(MyCatColors.onPrimary)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 10)
                            .background(MyCatColors.primary)
                            .clipShape(Capsule())
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()

            } else if !viewModel.isLoading && viewModel.vets.isEmpty {
                // 결과 없음
                VStack(spacing: 12) {
                    Text("🏥")
                        .font(.system(size: 40))
                    Text("근처 동물병원이 없어요")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(MyCatColors.onBackground)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            } else {
                // 목록
                ScrollView {
                    LazyVStack(spacing: 10) {
                        // 결과 수 헤더
                        if !viewModel.vets.isEmpty {
                            HStack {
                                Text("근처 동물병원 \(viewModel.vets.count)곳")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(MyCatColors.onBackground)
                                Spacer()
                            }
                        }

                        ForEach(viewModel.vets) { vet in
                            VetCard(
                                vet: vet,
                                isSelected: viewModel.selectedVet?.id == vet.id,
                                onTap: { viewModel.selectVet(vet) }
                            )
                        }

                        Spacer().frame(height: 16)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 14)
                }
            }
        }
        .background(MyCatColors.background)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - VetCard  (동물병원 카드)
// ═══════════════════════════════════════════════════════════════════

private struct VetCard: View {
    let vet: VetPlaceItem
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {

                // ─── 상단: 이름 + 거리 ────────────────────────────
                HStack(alignment: .top, spacing: 8) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(vet.name)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(MyCatColors.onBackground)
                            .multilineTextAlignment(.leading)
                        Text(vet.address)
                            .font(.system(size: 12))
                            .foregroundColor(MyCatColors.textMuted)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }

                    Spacer()

                    if let dist = vet.distanceText {
                        Text(dist)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(MyCatColors.primary)
                            .fixedSize()
                    }
                }

                // ─── 하단: 별점 + 영업상태 + 액션 버튼 ──────────
                HStack(spacing: 8) {

                    // 별점
                    if let rating = vet.ratingText {
                        HStack(spacing: 2) {
                            Text("⭐")
                                .font(.system(size: 11))
                            Text(rating)
                                .font(.system(size: 12))
                                .foregroundColor(MyCatColors.onBackground)
                        }
                    }

                    // 영업 상태 배지
                    if let isOpen = vet.isOpen {
                        Text(isOpen ? "영업중" : "영업종료")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(isOpen ? Color(red: 0.13, green: 0.60, blue: 0.35) : MyCatColors.textMuted)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(
                                Capsule()
                                    .fill(isOpen
                                          ? Color(red: 0.13, green: 0.60, blue: 0.35).opacity(0.12)
                                          : MyCatColors.textMuted.opacity(0.12))
                            )
                    }

                    Spacer()

                    // 전화 버튼
                    if let phone = vet.phoneNumber {
                        Link(destination: URL(string: "tel:\(phone.filter { $0.isNumber })")!) {
                            Image(systemName: "phone.fill")
                                .font(.system(size: 14))
                                .foregroundColor(MyCatColors.primary)
                                .frame(width: 34, height: 34)
                                .background(MyCatColors.primary.opacity(0.10))
                                .clipShape(Circle())
                        }
                        .onTapGesture {} // Link 위에 Button 버블링 방지
                    }

                    // 길찾기 버튼 (Apple Maps)
                    Button(action: {
                        let item = MKMapItem(placemark: MKPlacemark(coordinate: vet.coordinate))
                        item.name = vet.name
                        item.openInMaps(launchOptions: [
                            MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving
                        ])
                    }) {
                        Image(systemName: "map.fill")
                            .font(.system(size: 14))
                            .foregroundColor(MyCatColors.primary)
                            .frame(width: 34, height: 34)
                            .background(MyCatColors.primary.opacity(0.10))
                            .clipShape(Circle())
                    }
                }
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(isSelected ? MyCatColors.primary.opacity(0.07) : MyCatColors.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(
                        isSelected ? MyCatColors.primary.opacity(0.5) : MyCatColors.border,
                        lineWidth: isSelected ? 1.5 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .animation(.easeInOut(duration: 0.15), value: isSelected)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - NearbyVetPermissionDeniedView  (위치 권한 거부 안내)
// ═══════════════════════════════════════════════════════════════════

private struct NearbyVetPermissionDeniedView: View {

    var body: some View {
        Spacer()
        VStack(spacing: 16) {
            Text("📍")
                .font(.system(size: 56))
            Text("위치 권한이 필요해요")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(MyCatColors.onBackground)
            Text("근처 동물병원을 찾으려면\n설정에서 위치 접근을 허용해주세요")
                .font(.system(size: 13))
                .foregroundColor(MyCatColors.textMuted)
                .multilineTextAlignment(.center)
            Button(action: {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }) {
                Text("설정 열기")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(MyCatColors.onPrimary)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 12)
                    .background(MyCatColors.primary)
                    .clipShape(Capsule())
            }
        }
        .padding(.horizontal, 32)
        Spacer()
    }
}
