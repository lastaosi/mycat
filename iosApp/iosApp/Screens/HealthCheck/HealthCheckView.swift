import SwiftUI
import ComposeApp

// ═══════════════════════════════════════════════════════════════════
// MARK: - HealthCheckView  (Android HealthCheckScreen 대응)
// ═══════════════════════════════════════════════════════════════════

struct HealthCheckView: View {

    @StateObject private var viewModel = HealthCheckViewModel()
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        VStack(spacing: 0) {

            // ─── 상단 TopBar ──────────────────────────────────────
            HealthCheckTopBar(catName: viewModel.catName)
                .environmentObject(drawerState)

            // ─── 탭 바 ────────────────────────────────────────────
            HealthCheckTabBar(selectedTab: $viewModel.selectedTab)

            // ─── 본문 ─────────────────────────────────────────────
            if viewModel.isLoading {
                Spacer()
                ProgressView()
                    .scaleEffect(1.2)
                Spacer()
            } else if viewModel.groupedItems.isEmpty {
                HealthCheckEmptyState()
            } else {
                HealthCheckGroupedList(
                    groupedItems: viewModel.groupedItems,
                    currentAgeMonth: viewModel.ageMonth
                )
            }
        }
        .background(MyCatColors.background)
        .navigationBarHidden(true)
        .onAppear { viewModel.load() }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - HealthCheckTopBar
// ═══════════════════════════════════════════════════════════════════

private struct HealthCheckTopBar: View {
    let catName: String
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        HStack(spacing: 0) {
            // 햄버거 버튼 (DrawerState EnvironmentObject 공유)
            DrawerHamburgerButton()
                .frame(width: 44, height: 44)

            Spacer().frame(width: 8)

            // 화면 제목 + 고양이 이름
            VStack(alignment: .leading, spacing: 1) {
                Text("건강 체크리스트")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(MyCatColors.onPrimary)
                if !catName.isEmpty {
                    Text(catName)
                        .font(.system(size: 12))
                        .foregroundColor(MyCatColors.onPrimary.opacity(0.8))
                }
            }

            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(MyCatColors.primary)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - HealthCheckTabBar  (4탭 가로 스크롤)
// ═══════════════════════════════════════════════════════════════════

private struct HealthCheckTabBar: View {
    @Binding var selectedTab: HealthCheckTab

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(HealthCheckTab.allCases, id: \.self) { tab in
                    HealthCheckTabChip(
                        tab: tab,
                        isSelected: selectedTab == tab,
                        onTap: { selectedTab = tab }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .background(MyCatColors.surface)
        .overlay(
            Rectangle()
                .frame(height: 1)
                .foregroundColor(MyCatColors.border),
            alignment: .bottom
        )
    }
}

private struct HealthCheckTabChip: View {
    let tab: HealthCheckTab
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 4) {
                Text(tab.emoji)
                    .font(.system(size: 13))
                Text(tab.label)
                    .font(.system(size: 13, weight: isSelected ? .bold : .regular))
                    .foregroundColor(isSelected ? MyCatColors.primary : MyCatColors.textSecondary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(isSelected ? MyCatColors.primary.opacity(0.12) : Color.clear)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(isSelected ? MyCatColors.primary : MyCatColors.border, lineWidth: 1)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - HealthCheckGroupedList  (월령별 그룹 리스트)
// ═══════════════════════════════════════════════════════════════════

private struct HealthCheckGroupedList: View {
    let groupedItems: [(month: Int, items: [HealthCheckItem])]
    let currentAgeMonth: Int

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 0, pinnedViews: []) {
                ForEach(groupedItems, id: \.month) { group in
                    // 월령 헤더
                    HealthCheckMonthHeader(
                        month: group.month,
                        currentAgeMonth: currentAgeMonth
                    )

                    // 해당 월 항목 카드들
                    VStack(spacing: 8) {
                        ForEach(group.items) { item in
                            HealthCheckItemCard(item: item)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)
                }

                Spacer().frame(height: 24)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - HealthCheckMonthHeader  (월령 섹션 헤더)
// ═══════════════════════════════════════════════════════════════════

/// 월령 헤더.
///
/// | 상태          | 배경색              | 텍스트 색           | 배지        |
/// |--------------|--------------------|--------------------|------------|
/// | 지난 월령     | surface (연회색)    | textMuted (회색)   | 없음        |
/// | 현재 월령     | primary (주황)      | onPrimary (흰색)   | "← 현재"   |
/// | 미래 월령     | border (연베이지)   | textSecondary      | 없음        |
private struct HealthCheckMonthHeader: View {
    let month: Int
    let currentAgeMonth: Int

    private var isPast:    Bool { month < currentAgeMonth }
    private var isCurrent: Bool { month == currentAgeMonth }
    private var isFuture:  Bool { month > currentAgeMonth }

    private var bgColor: Color {
        if isCurrent { return MyCatColors.primary }
        if isPast    { return MyCatColors.surface }
        return MyCatColors.border.opacity(0.4)
    }

    private var textColor: Color {
        if isCurrent { return MyCatColors.onPrimary }
        if isPast    { return MyCatColors.textMuted }
        return MyCatColors.textSecondary
    }

    var body: some View {
        HStack(spacing: 8) {
            Text("\(month)개월")
                .font(.system(size: 14, weight: isCurrent ? .bold : .semibold))
                .foregroundColor(textColor)

            if isCurrent {
                Text("← 현재")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(MyCatColors.onPrimary.opacity(0.85))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(
                        Capsule()
                            .fill(MyCatColors.onPrimary.opacity(0.20))
                    )
            }

            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(bgColor)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - HealthCheckItemCard  (체크리스트 항목 카드)
// ═══════════════════════════════════════════════════════════════════

private struct HealthCheckItemCard: View {
    let item: HealthCheckItem

    var body: some View {
        HStack(alignment: .top, spacing: 12) {

            // ── 타입 아이콘 배경 원 ──────────────────────────────
            ZStack {
                Circle()
                    .fill(item.itemType.backgroundColor)
                    .frame(width: 42, height: 42)
                Text(item.itemType.emoji)
                    .font(.system(size: 20))
            }

            // ── 텍스트 영역 ─────────────────────────────────────
            VStack(alignment: .leading, spacing: 4) {

                // 제목 행: 타이틀 + 권장 배지
                HStack(alignment: .center, spacing: 6) {
                    Text(item.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(MyCatColors.onBackground)

                    if item.isRecommended {
                        Text("권장")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(MyCatColors.primary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(
                                Capsule()
                                    .fill(MyCatColors.primary.opacity(0.12))
                            )
                            .overlay(
                                Capsule()
                                    .stroke(MyCatColors.primary.opacity(0.4), lineWidth: 1)
                            )
                    }

                    Spacer()

                    // 항목 타입 라벨 (오른쪽 끝)
                    Text(item.itemType.label)
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(item.itemType.iconColor)
                }

                // 설명 텍스트
                if !item.description.isEmpty {
                    Text(item.description)
                        .font(.system(size: 12))
                        .foregroundColor(MyCatColors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(MyCatColors.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(MyCatColors.border, lineWidth: 1)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - HealthCheckEmptyState
// ═══════════════════════════════════════════════════════════════════

private struct HealthCheckEmptyState: View {
    var body: some View {
        Spacer()
        VStack(spacing: 16) {
            Text("❤️")
                .font(.system(size: 56))
            Text("체크리스트 항목이 없어요")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(MyCatColors.onBackground)
            Text("다른 탭을 선택해보세요")
                .font(.system(size: 13))
                .foregroundColor(MyCatColors.textMuted)
        }
        Spacer()
    }
}
