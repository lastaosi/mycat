import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    // 드로어 상태: MainDrawerView 에서 EnvironmentObject 로 주입
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 12) {
                    // 오늘의 케어 가이드 카드
                    if !viewModel.catName.isEmpty {
                        CareGuideCard(
                            catName: viewModel.catName,
                            breedName: viewModel.breedName,
                            foodDryG: viewModel.foodDryG,
                            foodWetG: viewModel.foodWetG,
                            waterMl: viewModel.waterMl,
                            weightMinG: viewModel.weightMinG,
                            weightMaxG: viewModel.weightMaxG
                        )
                    }

                    // 건강 체크리스트 카드
                    HealthCheckSummaryCard(titles: viewModel.healthCheckTitles)

                    // 최근 체중 카드 → 탭하면 WeightView 이동
                    NavigationLink(value: AppRoute.weight(catId: viewModel.catId)) {
                        LatestWeightCard(
                            weightG: viewModel.latestWeightG,
                            weightMinG: viewModel.weightMinG,
                            weightMaxG: viewModel.weightMaxG
                        )
                    }
                    .buttonStyle(.plain)    // 기본 NavigationLink 파란색 제거

                    // 예방접종 카드 → 탭하면 VaccinationView 이동
                    NavigationLink(value: AppRoute.vaccination(catId: viewModel.catId)) {
                        VaccinationSummaryCard()
                    }
                    .buttonStyle(.plain)

                    // 약 복용 관리 카드 → 탭하면 MedicationView 이동
                    NavigationLink(value: AppRoute.medication(catId: viewModel.catId)) {
                        MedicationSummaryCard()
                    }
                    .buttonStyle(.plain)

                    // 다이어리 카드 → 탭하면 DiaryView 이동
                    NavigationLink(value: AppRoute.diary(catId: viewModel.catId)) {
                        DiarySummaryCard()
                    }
                    .buttonStyle(.plain)

                    // 팁 배너
                    if !viewModel.randomTip.isEmpty {
                        TipBannerCard(tip: viewModel.randomTip)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
            .background(MyCatColors.background)

            // ─── 하단 배너 광고 ──────────────────────────────────
            BannerAdView()
                .frame(width: 320, height: 50)
                .frame(maxWidth: .infinity)
                .background(MyCatColors.surface)
            } // VStack end
            .navigationTitle("MY Cat")
            .navigationBarTitleDisplayMode(.large)
            // ─── 햄버거 버튼 (MainDrawerView 가 DrawerState 를 주입) ──
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    DrawerHamburgerButton()
                }
            }
            // ─── Navigation Destination ──────────────────────────────
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .weight(let catId):
                    WeightView(catId: catId)
                case .vaccination(let catId):
                    VaccinationView(catId: catId)
                case .medication(let catId):
                    MedicationView(catId: catId)
                case .diary(let catId):
                    DiaryView(catId: catId)
                default:
                    PlaceholderView(title: "준비 중") {}
                }
            }
        }
        .onAppear {
            viewModel.load()
        }
    }
}

// ─── CareGuideCard ───────────────────────────────────────────────────
struct CareGuideCard: View {
    let catName: String
    let breedName: String
    let foodDryG: Int
    let foodWetG: Int
    let waterMl: Int
    let weightMinG: Int
    let weightMaxG: Int

    var body: some View {
        MyCatCard {
            HStack {
                Text("🐾")
                    .font(.system(size: 18))
                VStack(alignment: .leading, spacing: 2) {
                    Text("오늘의 케어 가이드")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                    Text("\(catName) · \(breedName)")
                        .font(.system(size: 12))
                        .foregroundColor(MyCatColors.textMuted)
                }
                Spacer()
            }

            Spacer().frame(height: 12)

            HStack(spacing: 8) {
                CareChip(label: "건식", value: "\(foodDryG)g")
                CareChip(label: "습식", value: "\(foodWetG)g")
                CareChip(label: "물", value: "\(waterMl)ml")
            }

            if weightMinG > 0 && weightMaxG > 0 {
                Spacer().frame(height: 8)
                Text("적정 체중  \(String(format: "%.1f", Double(weightMinG)/1000))kg — \(String(format: "%.1f", Double(weightMaxG)/1000))kg")
                    .font(.system(size: 12))
                    .foregroundColor(MyCatColors.textMuted)
            }
        }
    }
}

// ─── CareChip ────────────────────────────────────────────────────────
struct CareChip: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(MyCatColors.textMuted)
            Text(value)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(MyCatColors.secondary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(MyCatColors.surface)
        .cornerRadius(10)
    }
}

// ─── HealthCheckSummaryCard ──────────────────────────────────────────
struct HealthCheckSummaryCard: View {
    let titles: [String]

    var body: some View {
        MyCatCard {
            HStack {
                Text("❤️").font(.system(size: 18))
                Text("건강 체크리스트")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                Spacer()
            }

            Spacer().frame(height: 10)

            if titles.isEmpty {
                Text("체크리스트 항목이 없어요")
                    .font(.system(size: 13))
                    .foregroundColor(MyCatColors.textMuted)
            } else {
                ForEach(titles, id: \.self) { title in
                    HStack {
                        Text("•")
                            .foregroundColor(MyCatColors.primary)
                        Text(title)
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.onBackground)
                        Spacer()
                    }
                    .padding(.vertical, 2)
                }
            }
        }
    }
}

// ─── LatestWeightCard ────────────────────────────────────────────────
struct LatestWeightCard: View {
    let weightG: Int
    let weightMinG: Int
    let weightMaxG: Int

    var body: some View {
        MyCatCard {
            HStack {
                Text("⚖️").font(.system(size: 18))
                Text("최근 체중")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                Spacer()
            }

            Spacer().frame(height: 8)

            if weightG > 0 {
                let isNormal = weightG >= weightMinG && weightG <= weightMaxG
                HStack {
                    Text(String(format: "%.1fkg", Double(weightG) / 1000))
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                    Spacer()
                    Text(isNormal ? "정상 범위" : "범위 벗어남")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(isNormal ? MyCatColors.success : MyCatColors.primary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(
                            (isNormal ? MyCatColors.success : MyCatColors.primary).opacity(0.15)
                        )
                        .cornerRadius(20)
                }
            } else {
                Text("아직 체중 기록이 없어요")
                    .font(.system(size: 13))
                    .foregroundColor(MyCatColors.textMuted)
            }
        }
    }
}

// ─── VaccinationSummaryCard ──────────────────────────────────────────
struct VaccinationSummaryCard: View {
    var body: some View {
        MyCatCard {
            HStack {
                Text("💉").font(.system(size: 18))
                Text("예방접종 관리")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13))
                    .foregroundColor(MyCatColors.textMuted)
            }
            Spacer().frame(height: 6)
            Text("접종 기록 및 다음 예정일을 관리하세요")
                .font(.system(size: 12))
                .foregroundColor(MyCatColors.textMuted)
        }
    }
}

// ─── MedicationSummaryCard ───────────────────────────────────────────
struct MedicationSummaryCard: View {
    var body: some View {
        MyCatCard {
            HStack {
                Text("💊").font(.system(size: 18))
                Text("약 복용 관리")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13))
                    .foregroundColor(MyCatColors.textMuted)
            }
            Spacer().frame(height: 6)
            Text("복용 중인 약과 알림 시간을 관리하세요")
                .font(.system(size: 12))
                .foregroundColor(MyCatColors.textMuted)
        }
    }
}

// ─── DiarySummaryCard ────────────────────────────────────────────────
struct DiarySummaryCard: View {
    var body: some View {
        MyCatCard {
            HStack {
                Text("📝").font(.system(size: 18))
                Text("다이어리")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13))
                    .foregroundColor(MyCatColors.textMuted)
            }
            Spacer().frame(height: 6)
            Text("소중한 순간을 기록하고 추억하세요")
                .font(.system(size: 12))
                .foregroundColor(MyCatColors.textMuted)
        }
    }
}

// ─── TipBannerCard ───────────────────────────────────────────────────
struct TipBannerCard: View {
    let tip: String

    var body: some View {
        HStack {
            Text("💡").font(.system(size: 16))
            Spacer().frame(width: 10)
            Text(tip)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(MyCatColors.onPrimary)
                .multilineTextAlignment(.leading)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(MyCatColors.primary)
        .cornerRadius(12)
    }
}

// ─── 공통 카드 래퍼 ──────────────────────────────────────────────────
struct MyCatCard<Content: View>: View {
    let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}
