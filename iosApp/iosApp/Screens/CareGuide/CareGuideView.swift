import SwiftUI
import ComposeApp

// ═══════════════════════════════════════════════════════════════════
// MARK: - CareGuideView  (Android CareGuideScreen 대응)
// ═══════════════════════════════════════════════════════════════════

struct CareGuideView: View {

    @StateObject private var viewModel = CareGuideViewModel()
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        VStack(spacing: 0) {

            // ─── TopBar ──────────────────────────────────────────
            CareGuideTopBar(
                catName: viewModel.catName,
                breedName: viewModel.breedName
            )
            .environmentObject(drawerState)

            // ─── 본문 ─────────────────────────────────────────────
            if viewModel.isLoading {
                Spacer()
                ProgressView()
                    .scaleEffect(1.2)
                Spacer()
            } else if !viewModel.hasBreed {
                CareGuideNoBreedView()
            } else if viewModel.guides.isEmpty {
                CareGuideEmptyView()
            } else {
                CareGuideContent(viewModel: viewModel)
            }
        }
        .background(MyCatColors.background)
        .navigationBarHidden(true)
        .onAppear { viewModel.load() }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - CareGuideTopBar
// ═══════════════════════════════════════════════════════════════════

private struct CareGuideTopBar: View {
    let catName: String
    let breedName: String
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        HStack(spacing: 0) {
            DrawerHamburgerButton(tint: MyCatColors.onPrimary)
                .environmentObject(drawerState)

            Spacer().frame(width: 4)

            VStack(alignment: .leading, spacing: 1) {
                Text("케어 가이드")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(MyCatColors.onPrimary)
                if !catName.isEmpty {
                    Text("\(catName) · \(breedName)")
                        .font(.system(size: 12))
                        .foregroundColor(MyCatColors.onPrimary.opacity(0.85))
                        .lineLimit(1)
                }
            }

            Spacer()
        }
        .padding(.horizontal, 8)
        .frame(height: 56)
        .background(MyCatColors.primary)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - CareGuideContent  (가이드 목록 본문)
// ═══════════════════════════════════════════════════════════════════

private struct CareGuideContent: View {
    @ObservedObject var viewModel: CareGuideViewModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 10) {

                // ─── 현재 월령 하이라이트 카드 ──────────────────
                if let current = viewModel.currentGuide {
                    CurrentMonthCard(guide: current, ageMonth: viewModel.ageMonth)
                }

                // ─── 월령별 가이드 섹션 헤더 ─────────────────────
                HStack {
                    Text("월령별 가이드")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                    Spacer()
                }
                .padding(.top, 4)

                // ─── 전체 월령 카드 리스트 ───────────────────────
                ForEach(viewModel.guides) { guide in
                    GuideMonthCard(
                        guide: guide,
                        isCurrent: guide.month == viewModel.ageMonth
                    )
                }

                Spacer().frame(height: 16)
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - CurrentMonthCard  (현재 월령 강조 카드)
// ═══════════════════════════════════════════════════════════════════

private struct CurrentMonthCard: View {
    let guide: BreedMonthlyGuideItem
    let ageMonth: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {

            // 헤더
            Text("🐾 현재 \(ageMonth)개월")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(MyCatColors.onPrimary)

            // 급여량 칩 3개 (건식 / 습식 / 물)
            HStack(spacing: 8) {
                GuideChip(label: "건식", value: "\(guide.foodDryG)g")
                GuideChip(label: "습식", value: "\(guide.foodWetG)g")
                GuideChip(label: "물",   value: "\(guide.waterMl)ml")
                GuideChip(label: "간식", value: "\(guide.treatMaxG)g")
            }

            // 적정 체중
            Text("적정 체중  \(guide.weightMinKg)kg — \(guide.weightMaxKg)kg")
                .font(.system(size: 12))
                .foregroundColor(MyCatColors.onPrimary.opacity(0.85))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(MyCatColors.primary)
                .shadow(color: MyCatColors.primary.opacity(0.3), radius: 6, x: 0, y: 3)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - GuideChip  (급여량 표시 칩)
// ═══════════════════════════════════════════════════════════════════

private struct GuideChip: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(MyCatColors.onPrimary.opacity(0.85))
            Text(value)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(MyCatColors.onPrimary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(MyCatColors.onPrimary.opacity(0.18))
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - GuideMonthCard  (월령별 카드)
// ═══════════════════════════════════════════════════════════════════

private struct GuideMonthCard: View {
    let guide: BreedMonthlyGuideItem
    let isCurrent: Bool

    var body: some View {
        HStack(alignment: .center, spacing: 12) {

            // ─── 월령 레이블 배지 ──────────────────────────────
            Text(guide.monthLabel)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(isCurrent ? MyCatColors.onPrimary : MyCatColors.textMuted)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(isCurrent ? MyCatColors.primary : MyCatColors.surface)
                )
                .frame(minWidth: 60)

            // ─── 수치 데이터 ───────────────────────────────────
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 16) {
                    GuideDataItem(label: "건식", value: "\(guide.foodDryG)g")
                    GuideDataItem(label: "습식", value: "\(guide.foodWetG)g")
                    GuideDataItem(label: "물",   value: "\(guide.waterMl)ml")
                    GuideDataItem(label: "간식", value: "\(guide.treatMaxG)g")
                }
                Text("체중 \(guide.weightMinKg)—\(guide.weightMaxKg)kg")
                    .font(.system(size: 11))
                    .foregroundColor(MyCatColors.textMuted)
            }

            Spacer()
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(isCurrent
                      ? MyCatColors.primary.opacity(0.07)
                      : MyCatColors.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(
                    isCurrent ? MyCatColors.primary.opacity(0.4) : MyCatColors.border,
                    lineWidth: isCurrent ? 1.5 : 1
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - GuideDataItem
// ═══════════════════════════════════════════════════════════════════

private struct GuideDataItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(MyCatColors.textMuted)
            Text(value)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(MyCatColors.onBackground)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - CareGuideNoBreedView  (품종 미등록 안내)
// ═══════════════════════════════════════════════════════════════════

private struct CareGuideNoBreedView: View {
    var body: some View {
        Spacer()
        VStack(spacing: 12) {
            Text("🐱")
                .font(.system(size: 56))
            Text("품종을 등록하면 케어 가이드를 볼 수 있어요")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(MyCatColors.onBackground)
                .multilineTextAlignment(.center)
            Text("프로필에서 품종을 등록해 주세요")
                .font(.system(size: 13))
                .foregroundColor(MyCatColors.textMuted)
        }
        .padding(.horizontal, 32)
        Spacer()
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - CareGuideEmptyView  (데이터 없음)
// ═══════════════════════════════════════════════════════════════════

private struct CareGuideEmptyView: View {
    var body: some View {
        Spacer()
        VStack(spacing: 12) {
            Text("📋")
                .font(.system(size: 56))
            Text("가이드 데이터가 없어요")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(MyCatColors.onBackground)
        }
        Spacer()
    }
}
