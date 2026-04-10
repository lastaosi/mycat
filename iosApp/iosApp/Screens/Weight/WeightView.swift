import SwiftUI
import Charts

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WeightView  (Android WeightScreen 대응)
// ─────────────────────────────────────────────────────────────────────────────

struct WeightView: View {
    /// HomeView 에서 넘어오는 고양이 ID
    let catId: Int64
    /// nil 이면 @Environment(\.dismiss) 로 자동 처리 (NavigationStack push 시)
    var onBack: (() -> Void)? = nil

    @StateObject private var viewModel: WeightViewModel
    @Environment(\.dismiss) private var dismiss

    init(catId: Int64, onBack: (() -> Void)? = nil) {
        self.catId = catId
        self.onBack = onBack
        _viewModel = StateObject(wrappedValue: WeightViewModel(catId: catId))
    }

    private func handleBack() {
        if let onBack { onBack() } else { dismiss() }
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                // ─── TopBar ───────────────────────────────────────────────
                WeightTopBar(catName: viewModel.catName)

                // ─── Tab Row ──────────────────────────────────────────────
                WeightTabRow(selectedTab: $viewModel.selectedTab)

                // ─── Tab Content ──────────────────────────────────────────
                if viewModel.selectedTab == .myCat {
                    MyCatWeightTab(viewModel: viewModel)
                } else {
                    BreedAverageTab(viewModel: viewModel)
                }
            }
            .background(MyCatColors.background)

            // ─── FAB ──────────────────────────────────────────────────────
            Button(action: {
                viewModel.showInputDialog = true
            }) {
                Image(systemName: "plus")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(MyCatColors.onPrimary)
                    .frame(width: 56, height: 56)
                    .background(MyCatColors.primary)
                    .clipShape(Circle())
                    .shadow(color: MyCatColors.primary.opacity(0.4), radius: 6, x: 0, y: 3)
            }
            .padding(.trailing, 20)
            .padding(.bottom, 28)
        }
        .navigationBarHidden(true)
        .onAppear {
            viewModel.load()
        }
        // ─── 체중 입력 Sheet ───────────────────────────────────────────────
        .sheet(isPresented: $viewModel.showInputDialog) {
            WeightInputSheet(
                onDismiss: { viewModel.showInputDialog = false },
                onSave: { weightKg, memo in
                    viewModel.insertWeight(weightKg: weightKg, memo: memo)
                }
            )
            .presentationDetents([.height(280)])
            .presentationDragIndicator(.visible)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WeightTopBar
// ─────────────────────────────────────────────────────────────────────────────

private struct WeightTopBar: View {
    let catName: String
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        HStack(spacing: 4) {
            DrawerHamburgerButton(tint: MyCatColors.onPrimary)
                .environmentObject(drawerState)

            VStack(alignment: .leading, spacing: 1) {
                Text("체중 기록")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(MyCatColors.onPrimary)
                if !catName.isEmpty {
                    Text(catName)
                        .font(.system(size: 12))
                        .foregroundColor(MyCatColors.onPrimary.opacity(0.85))
                }
            }

            Spacer()
        }
        .padding(.horizontal, 8)
        .frame(height: 56)
        .background(MyCatColors.primary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WeightTabRow
// ─────────────────────────────────────────────────────────────────────────────

private struct WeightTabRow: View {
    @Binding var selectedTab: WeightTabType

    var body: some View {
        HStack(spacing: 0) {
            ForEach(WeightTabType.allCases, id: \.self) { tab in
                Button(action: { selectedTab = tab }) {
                    VStack(spacing: 0) {
                        Text(tab.label)
                            .font(.system(
                                size: 13,
                                weight: selectedTab == tab ? .bold : .regular
                            ))
                            .foregroundColor(
                                selectedTab == tab ? MyCatColors.primary : MyCatColors.textMuted
                            )
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)

                        // 탭 인디케이터
                        Rectangle()
                            .fill(selectedTab == tab ? MyCatColors.primary : Color.clear)
                            .frame(height: 2)
                    }
                }
            }
        }
        .background(Color.white)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MyCatWeightTab  (내 고양이 추이 탭)
// ─────────────────────────────────────────────────────────────────────────────

private struct MyCatWeightTab: View {
    @ObservedObject var viewModel: WeightViewModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                // 요약 카드
                WeightSummaryCard(
                    latestWeightG: viewModel.latestWeightG,
                    recordCount: viewModel.weightHistory.count
                )

                // 그래프
                if viewModel.weightHistory.count >= 2 {
                    MyCatChartCard(history: viewModel.weightHistory)
                } else {
                    NoChartCard(message: "체중 기록이 2개 이상이면 그래프가 표시돼요")
                }

                // 기록 목록 헤더
                HStack {
                    Text("기록 목록")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                    Spacer()
                }
                .padding(.top, 4)

                // 기록 없을 때
                if viewModel.weightHistory.isEmpty {
                    Text("아직 체중 기록이 없어요\nFAB 버튼을 눌러 추가해보세요")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                        .multilineTextAlignment(.center)
                        .padding(.vertical, 32)
                        .frame(maxWidth: .infinity)
                }

                // 기록 리스트 (최신순)
                ForEach(viewModel.weightHistory.sorted(by: { $0.recordedAt > $1.recordedAt })) { record in
                    WeightRecordRow(
                        record: record,
                        ageMonth: viewModel.ageMonthAt(recordedAt: record.recordedAt)
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
        }
        .background(MyCatColors.background)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - BreedAverageTab  (품종 평균 성장 탭)
// ─────────────────────────────────────────────────────────────────────────────

private struct BreedAverageTab: View {
    @ObservedObject var viewModel: WeightViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                // 범례
                BreedChartLegend()
                    .frame(maxWidth: .infinity, alignment: .leading)

                // 그래프
                if !viewModel.breedAverageData.isEmpty {
                    BreedChartCard(data: viewModel.breedAverageData)
                } else {
                    NoChartCard(message: "품종 정보가 없으면 평균 데이터를 표시할 수 없어요")
                }

                // 데이터 범위 안내
                if let lastMonth = viewModel.breedAverageData.last?.month {
                    HStack {
                        Text("1개월 ~ \(lastMonth)개월 데이터")
                            .font(.system(size: 12))
                            .foregroundColor(MyCatColors.textMuted)
                        Spacer()
                    }
                }
            }
            .padding(16)
        }
        .background(MyCatColors.background)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WeightSummaryCard
// ─────────────────────────────────────────────────────────────────────────────

private struct WeightSummaryCard: View {
    let latestWeightG: Int?
    let recordCount: Int

    var body: some View {
        HStack {
            // 현재 체중
            VStack(spacing: 4) {
                Text(latestWeightG.map { String(format: "%.1fkg", Double($0) / 1000) } ?? "-")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(MyCatColors.primary)
                Text("현재 체중")
                    .font(.system(size: 12))
                    .foregroundColor(MyCatColors.textMuted)
            }
            .frame(maxWidth: .infinity)

            Divider()
                .frame(height: 48)
                .background(MyCatColors.border)

            // 총 기록
            VStack(spacing: 4) {
                Text("\(recordCount)회")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(MyCatColors.primary)
                Text("총 기록")
                    .font(.system(size: 12))
                    .foregroundColor(MyCatColors.textMuted)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MyCatChartCard  (내 고양이 체중 추이 차트)
// ─────────────────────────────────────────────────────────────────────────────

private struct MyCatChartCard: View {
    let history: [WeightRecordItem]

    /// 날짜순 정렬된 데이터
    private var sorted: [WeightRecordItem] {
        history.sorted(by: { $0.recordedAt < $1.recordedAt })
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Chart(sorted) { item in
                // 라인
                LineMark(
                    x: .value("날짜", WeightViewModel.toDate(item.recordedAt)),
                    y: .value("체중", Double(item.weightG) / 1000.0)
                )
                .foregroundStyle(MyCatColors.primary)
                .interpolationMethod(.catmullRom)

                // 포인트
                PointMark(
                    x: .value("날짜", WeightViewModel.toDate(item.recordedAt)),
                    y: .value("체중", Double(item.weightG) / 1000.0)
                )
                .foregroundStyle(MyCatColors.primary)
                .symbolSize(40)
            }
            .chartYAxis {
                AxisMarks(position: .leading) { value in
                    AxisValueLabel {
                        if let v = value.as(Double.self) {
                            Text(String(format: "%.1fkg", v))
                                .font(.system(size: 10))
                                .foregroundColor(MyCatColors.textMuted)
                        }
                    }
                    AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                        .foregroundStyle(MyCatColors.border)
                }
            }
            .chartXAxis {
                AxisMarks(values: .automatic(desiredCount: 4)) {
                    AxisValueLabel()
                    AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                        .foregroundStyle(MyCatColors.border)
                }
            }
            .frame(height: 220)
            .padding(16)
        }
        .background(Color.white)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - BreedChartCard  (품종 평균 성장 차트)
// ─────────────────────────────────────────────────────────────────────────────

/// 품종 평균 차트용 포인트 (avg / min / max 구분을 위한 래퍼)
private struct BreedChartPoint: Identifiable {
    let id = UUID()
    let month: Int
    let weightKg: Double
    let seriesLabel: String
}

private struct BreedChartCard: View {
    let data: [BreedAvgItem]

    /// avg / min / max 세 가지 시리즈를 하나의 배열로 펼친다
    private var chartPoints: [BreedChartPoint] {
        var pts: [BreedChartPoint] = []
        for item in data {
            pts.append(BreedChartPoint(month: item.month, weightKg: Double(item.avgWeightG) / 1000, seriesLabel: "평균"))
            pts.append(BreedChartPoint(month: item.month, weightKg: Double(item.weightMinG) / 1000, seriesLabel: "최소"))
            pts.append(BreedChartPoint(month: item.month, weightKg: Double(item.weightMaxG) / 1000, seriesLabel: "최대"))
        }
        return pts
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Chart(chartPoints) { pt in
                LineMark(
                    x: .value("개월", pt.month),
                    y: .value("체중", pt.weightKg)
                )
                .foregroundStyle(by: .value("구분", pt.seriesLabel))
                .interpolationMethod(.catmullRom)

                PointMark(
                    x: .value("개월", pt.month),
                    y: .value("체중", pt.weightKg)
                )
                .foregroundStyle(by: .value("구분", pt.seriesLabel))
                .symbolSize(30)
            }
            .chartForegroundStyleScale([
                "평균": MyCatColors.primary,
                "최소": MyCatColors.success,
                "최대": MyCatColors.secondary
            ])
            .chartLegend(.hidden)      // 범례는 BreedChartLegend 로 별도 표시
            .chartYAxis {
                AxisMarks(position: .leading) { value in
                    AxisValueLabel {
                        if let v = value.as(Double.self) {
                            Text(String(format: "%.1fkg", v))
                                .font(.system(size: 10))
                                .foregroundColor(MyCatColors.textMuted)
                        }
                    }
                    AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                        .foregroundStyle(MyCatColors.border)
                }
            }
            .chartXAxis {
                AxisMarks { value in
                    AxisValueLabel {
                        if let month = value.as(Int.self) {
                            Text("\(month)개월")
                                .font(.system(size: 10))
                                .foregroundColor(MyCatColors.textMuted)
                        }
                    }
                    AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                        .foregroundStyle(MyCatColors.border)
                }
            }
            .frame(height: 280)
            .padding(16)
        }
        .background(Color.white)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - NoChartCard
// ─────────────────────────────────────────────────────────────────────────────

private struct NoChartCard: View {
    let message: String

    var body: some View {
        ZStack {
            Text(message)
                .font(.system(size: 13))
                .foregroundColor(MyCatColors.textMuted)
                .multilineTextAlignment(.center)
                .padding(16)
        }
        .frame(maxWidth: .infinity, minHeight: 180)
        .background(Color.white)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WeightRecordRow
// ─────────────────────────────────────────────────────────────────────────────

private struct WeightRecordRow: View {
    let record: WeightRecordItem
    let ageMonth: Int

    var body: some View {
        HStack {
            // 왼쪽: 체중 + 메모
            VStack(alignment: .leading, spacing: 2) {
                Text(String(format: "%.1fkg", Double(record.weightG) / 1000))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                if let memo = record.memo, !memo.isEmpty {
                    Text(memo)
                        .font(.system(size: 12))
                        .foregroundColor(MyCatColors.textMuted)
                }
            }

            Spacer()

            // 오른쪽: 날짜 + 개월수
            VStack(alignment: .trailing, spacing: 2) {
                Text(WeightViewModel.formatDate(record.recordedAt))
                    .font(.system(size: 12))
                    .foregroundColor(MyCatColors.textMuted)
                Text("\(ageMonth)개월")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(MyCatColors.primary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: .black.opacity(0.04), radius: 2, x: 0, y: 1)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - BreedChartLegend
// ─────────────────────────────────────────────────────────────────────────────

private struct BreedChartLegend: View {
    var body: some View {
        HStack(spacing: 16) {
            LegendItem(color: MyCatColors.primary, label: "평균")
            LegendItem(color: MyCatColors.success,  label: "최소")
            LegendItem(color: MyCatColors.secondary, label: "최대")
        }
    }
}

private struct LegendItem: View {
    let color: Color
    let label: String

    var body: some View {
        HStack(spacing: 4) {
            RoundedRectangle(cornerRadius: 2)
                .fill(color)
                .frame(width: 12, height: 12)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(MyCatColors.textMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WeightInputSheet  (Android WeightInputDialog 대응)
// ─────────────────────────────────────────────────────────────────────────────

private struct WeightInputSheet: View {
    let onDismiss: () -> Void
    let onSave: (String, String) -> Void

    @State private var weightInput: String = ""
    @State private var memoInput: String = ""
    @State private var isError: Bool = false
    @FocusState private var weightFieldFocused: Bool

    var body: some View {
        VStack(spacing: 16) {
            // 타이틀
            HStack {
                Text("체중 기록")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 22))
                        .foregroundColor(MyCatColors.textMuted)
                }
            }

            // 체중 입력
            VStack(alignment: .leading, spacing: 4) {
                TextField("체중 (kg) — 예: 3.5", text: $weightInput)
                    .keyboardType(.decimalPad)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(isError ? Color.red : MyCatColors.border, lineWidth: 1)
                    )
                    .focused($weightFieldFocused)
                    .onChange(of: weightInput) { isError = false }

                if isError {
                    Text("올바른 체중을 입력해주세요")
                        .font(.system(size: 11))
                        .foregroundColor(.red)
                }
            }

            // 메모 입력
            TextField("메모 (선택)", text: $memoInput)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(MyCatColors.border, lineWidth: 1)
                )

            // 버튼 행
            HStack(spacing: 12) {
                // 취소
                Button(action: onDismiss) {
                    Text("취소")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(MyCatColors.textMuted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(MyCatColors.surface)
                        .cornerRadius(10)
                }

                // 저장
                Button(action: {
                    if weightInput.toDouble == nil {
                        isError = true
                    } else {
                        onSave(weightInput, memoInput)
                    }
                }) {
                    Text("저장")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(MyCatColors.onPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(MyCatColors.primary)
                        .cornerRadius(10)
                }
            }
        }
        .padding(20)
        .onAppear {
            weightFieldFocused = true
        }
    }
}

// MARK: - String Helper
private extension String {
    var toDouble: Double? { Double(self) }
}
