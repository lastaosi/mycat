import SwiftUI

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MedicationView  (Android MedicationScreen 대응)
// ─────────────────────────────────────────────────────────────────────────────

struct MedicationView: View {
    let catId: Int64
    var onBack: (() -> Void)? = nil

    @StateObject private var viewModel: MedicationViewModel
    @Environment(\.dismiss) private var dismiss

    init(catId: Int64, onBack: (() -> Void)? = nil) {
        self.catId = catId
        self.onBack = onBack
        _viewModel = StateObject(wrappedValue: MedicationViewModel(catId: catId))
    }

    private func handleBack() {
        if let onBack { onBack() } else { dismiss() }
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                // ─── TopBar ────────────────────────────────────────────────
                MedicationTopBar(catName: viewModel.catName, onBack: handleBack)

                // ─── 리스트 / 빈 상태 ────────────────────────────────────────
                let hasData = !viewModel.activeMedications.isEmpty || !viewModel.inactiveMedications.isEmpty
                if hasData {
                    MedicationList(viewModel: viewModel)
                } else {
                    MedicationEmptyState()
                }
            }
            .background(MyCatColors.background)

            // ─── FAB ──────────────────────────────────────────────────────
            Button(action: { viewModel.onFabTap() }) {
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
        .onAppear { viewModel.load() }
        // ─── 입력 / 수정 Sheet ────────────────────────────────────────────
        .sheet(isPresented: $viewModel.showInputSheet) {
            MedicationInputSheet(
                editingItem: viewModel.editingItem,
                onDismiss: { viewModel.onDismissSheet() },
                onSave: { name, type, dosage, startDate, endDate, intervalDays, memo, alarmTimes in
                    viewModel.saveMedication(
                        name: name,
                        medicationType: type,
                        dosage: dosage,
                        startDate: startDate,
                        endDate: endDate,
                        intervalDays: intervalDays,
                        memo: memo,
                        alarmTimes: alarmTimes
                    )
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MedicationTopBar
// ─────────────────────────────────────────────────────────────────────────────

private struct MedicationTopBar: View {
    let catName: String
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(MyCatColors.onPrimary)
                    .frame(width: 44, height: 44)
            }

            VStack(alignment: .leading, spacing: 1) {
                Text("약 복용 관리")
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
// MARK: - MedicationEmptyState
// ─────────────────────────────────────────────────────────────────────────────

private struct MedicationEmptyState: View {
    var body: some View {
        Spacer()
        VStack(spacing: 12) {
            Text("💊")
                .font(.system(size: 48))
            Text("약 복용 기록이 없어요")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(MyCatColors.onBackground)
            Text("FAB 버튼을 눌러 추가해보세요")
                .font(.system(size: 13))
                .foregroundColor(MyCatColors.textMuted)
        }
        Spacer()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MedicationList
// ─────────────────────────────────────────────────────────────────────────────

private struct MedicationList: View {
    @ObservedObject var viewModel: MedicationViewModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 10) {
                // ── 복용 중 섹션 ───────────────────────────────────────────
                if !viewModel.activeMedications.isEmpty {
                    SectionHeader(title: "복용 중", emoji: "💊")
                        .padding(.top, 16)
                        .padding(.horizontal, 16)

                    ForEach(viewModel.activeMedications) { item in
                        MedicationCard(
                            item: item,
                            onEdit: { viewModel.onEditTap(item) },
                            onDelete: { viewModel.deleteMedication(id: item.id) },
                            onToggle: { viewModel.toggleActive(item: item) }
                        )
                        .padding(.horizontal, 16)
                    }
                }

                // ── 완료 섹션 ─────────────────────────────────────────────
                if !viewModel.inactiveMedications.isEmpty {
                    SectionHeader(title: "완료", emoji: "✅")
                        .padding(.top, viewModel.activeMedications.isEmpty ? 16 : 12)
                        .padding(.horizontal, 16)

                    ForEach(viewModel.inactiveMedications) { item in
                        MedicationCard(
                            item: item,
                            onEdit: { viewModel.onEditTap(item) },
                            onDelete: { viewModel.deleteMedication(id: item.id) },
                            onToggle: { viewModel.toggleActive(item: item) }
                        )
                        .padding(.horizontal, 16)
                    }
                }
            }
            .padding(.bottom, 88)  // FAB 영역 여백
        }
        .background(MyCatColors.background)
    }

    private func deleteMedication(id: Int64) {
        viewModel.deleteMedication(id: id)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - SectionHeader
// ─────────────────────────────────────────────────────────────────────────────

private struct SectionHeader: View {
    let title: String
    let emoji: String

    var body: some View {
        HStack(spacing: 6) {
            Text(emoji)
                .font(.system(size: 14))
            Text(title)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(MyCatColors.onBackground)
            Spacer()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MedicationCard  (Android MedicationItem 대응)
// ─────────────────────────────────────────────────────────────────────────────

private struct MedicationCard: View {
    let item: MedicationItem
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onToggle: () -> Void

    @State private var showDeleteAlert = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // ─ 헤더: 약 이름 + 타입 뱃지 + 수정/삭제 ─────────────────────
            HStack(spacing: 0) {
                Text(item.name)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)

                Spacer().frame(width: 6)

                MedicationTypeBadgeView(type: item.medicationType)

                Spacer()

                // 수정 버튼
                Button(action: onEdit) {
                    Image(systemName: "pencil")
                        .font(.system(size: 15))
                        .foregroundColor(MyCatColors.textMuted)
                        .frame(width: 32, height: 32)
                }

                // 삭제 버튼
                Button(action: { showDeleteAlert = true }) {
                    Image(systemName: "trash")
                        .font(.system(size: 15))
                        .foregroundColor(MyCatColors.textMuted)
                        .frame(width: 32, height: 32)
                }
            }

            Divider()
                .background(MyCatColors.border)
                .padding(.vertical, 8)

            // ─ 투약량 ─────────────────────────────────────────────────────
            if let dosage = item.dosage, !dosage.isEmpty {
                Text("투약량: \(dosage)")
                    .font(.system(size: 12))
                    .foregroundColor(MyCatColors.textMuted)
                Spacer().frame(height: 4)
            }

            // ─ 날짜 정보 ──────────────────────────────────────────────────
            HStack(spacing: 16) {
                // 시작일
                VStack(alignment: .leading, spacing: 2) {
                    Text("시작일")
                        .font(.system(size: 11))
                        .foregroundColor(MyCatColors.textMuted)
                    Text(MedicationViewModel.formatDate(item.startDate))
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                }

                // 종료일 (있을 때만)
                if let endDate = item.endDate {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("종료일")
                            .font(.system(size: 11))
                            .foregroundColor(MyCatColors.textMuted)
                        Text(MedicationViewModel.formatDate(endDate))
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(MyCatColors.onBackground)
                    }
                }

                // 복용 간격 (INTERVAL 타입만)
                if let days = item.intervalDays {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("복용 간격")
                            .font(.system(size: 11))
                            .foregroundColor(MyCatColors.textMuted)
                        Text("\(days)일마다")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(MyCatColors.onBackground)
                    }
                }
            }

            // ─ 메모 ──────────────────────────────────────────────────────
            if let memo = item.memo, !memo.isEmpty {
                Spacer().frame(height: 6)
                Text(memo)
                    .font(.system(size: 12))
                    .foregroundColor(MyCatColors.textMuted)
            }

            Spacer().frame(height: 10)

            // ─ 복용 중 / 완료 토글 버튼 ────────────────────────────────────
            Button(action: onToggle) {
                Text(item.isActive ? "복용 완료로 변경" : "복용 중으로 변경")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(item.isActive ? MyCatColors.textMuted : MyCatColors.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .background(Color.clear)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(
                                item.isActive ? MyCatColors.border : MyCatColors.primary,
                                lineWidth: 1
                            )
                    )
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(item.isActive ? Color.white : Color.white.opacity(0.6))
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        .alert("\(item.name)을 삭제할까요?", isPresented: $showDeleteAlert) {
            Button("삭제", role: .destructive) { onDelete() }
            Button("취소", role: .cancel) {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MedicationTypeBadgeView  (Android MedicationTypeBadge 대응)
// ─────────────────────────────────────────────────────────────────────────────

private struct MedicationTypeBadgeView: View {
    let type: MedicationTypeSwift

    private var color: Color {
        switch type.badgeColor {
        case .muted:     return MyCatColors.textMuted
        case .primary:   return MyCatColors.primary
        case .secondary: return MyCatColors.secondary
        case .success:   return MyCatColors.success
        }
    }

    var body: some View {
        Text(type.label)
            .font(.system(size: 10, weight: .bold))
            .foregroundColor(color)
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(color.opacity(0.15))
            .cornerRadius(20)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MedicationInputSheet  (Android MedicationInputDialog 대응)
// ─────────────────────────────────────────────────────────────────────────────

struct MedicationInputSheet: View {
    let editingItem: MedicationItem?
    let onDismiss: () -> Void
    let onSave: (String, MedicationTypeSwift, String, Int64, Int64?, Int?, String, [String]) -> Void

    // ─ State ─────────────────────────────────────────────────────────────
    @State private var nameInput: String
    @State private var selectedType: MedicationTypeSwift
    @State private var dosageInput: String
    @State private var startDate: Date
    @State private var endDate: Date
    @State private var showEndDate: Bool
    @State private var intervalDaysInput: String
    @State private var memoInput: String
    @State private var alarmTimes: [String]
    @State private var alarmHour: Int
    @State private var alarmMinute: Int

    @State private var isNameError: Bool = false
    @FocusState private var focusedField: Bool

    // ─ Init ──────────────────────────────────────────────────────────────
    init(
        editingItem: MedicationItem?,
        onDismiss: @escaping () -> Void,
        onSave: @escaping (String, MedicationTypeSwift, String, Int64, Int64?, Int?, String, [String]) -> Void
    ) {
        self.editingItem = editingItem
        self.onDismiss = onDismiss
        self.onSave = onSave

        _nameInput     = State(initialValue: editingItem?.name ?? "")
        _selectedType  = State(initialValue: editingItem?.medicationType ?? .daily)
        _dosageInput   = State(initialValue: editingItem?.dosage ?? "")
        _memoInput     = State(initialValue: editingItem?.memo ?? "")
        _alarmTimes    = State(initialValue: [])
        _intervalDaysInput = State(initialValue: editingItem?.intervalDays.map { String($0) } ?? "")

        if let startMs = editingItem?.startDate {
            _startDate = State(initialValue: Date(timeIntervalSince1970: Double(startMs) / 1000))
        } else {
            _startDate = State(initialValue: Date())
        }

        if let endMs = editingItem?.endDate {
            _endDate = State(initialValue: Date(timeIntervalSince1970: Double(endMs) / 1000))
            _showEndDate = State(initialValue: true)
        } else {
            _endDate = State(initialValue: Date())
            _showEndDate = State(initialValue: false)
        }

        // 현재 시각 기본값
        let cal = Calendar.current
        let now = Date()
        _alarmHour   = State(initialValue: cal.component(.hour, from: now))
        _alarmMinute = State(initialValue: cal.component(.minute, from: now))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // ─ 시트 헤더 ──────────────────────────────────────────────
                HStack {
                    Text(editingItem != nil ? "약 수정" : "약 추가")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(MyCatColors.textMuted)
                    }
                }

                // ─ 약 이름 ────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("약 이름 *")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    TextField("예: 항생제", text: $nameInput)
                        .focused($focusedField)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(isNameError ? Color.red : MyCatColors.border, lineWidth: 1)
                        )
                        .onChange(of: nameInput) { _ in isNameError = false }
                    if isNameError {
                        Text("약 이름을 입력해주세요")
                            .font(.system(size: 11))
                            .foregroundColor(.red)
                    }
                }

                // ─ 복용 타입 선택 ────────────────────────────────────────
                VStack(alignment: .leading, spacing: 8) {
                    Text("복용 타입")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)

                    HStack(spacing: 8) {
                        ForEach(MedicationTypeSwift.allCases, id: \.self) { type in
                            TypeChip(
                                label: type.label,
                                isSelected: selectedType == type,
                                onTap: { selectedType = type }
                            )
                        }
                    }
                }

                // ─ 투약량 ─────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("투약량 (선택, 예: 0.5ml)")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    TextField("0.5ml", text: $dosageInput)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(MyCatColors.border, lineWidth: 1)
                        )
                }

                // ─ 시작일 ─────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("시작일 *")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    DatePicker("", selection: $startDate, displayedComponents: .date)
                        .datePickerStyle(.compact)
                        .labelsHidden()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .tint(MyCatColors.primary)
                }

                // ─ 종료일 (ONCE 제외) ───────────────────────────────────
                if selectedType != .once {
                    VStack(alignment: .leading, spacing: 8) {
                        Toggle(isOn: $showEndDate) {
                            Text("종료일 설정")
                                .font(.system(size: 13))
                                .foregroundColor(MyCatColors.textMuted)
                        }
                        .tint(MyCatColors.primary)

                        if showEndDate {
                            DatePicker("", selection: $endDate, displayedComponents: .date)
                                .datePickerStyle(.compact)
                                .labelsHidden()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .tint(MyCatColors.primary)
                        }
                    }
                }

                // ─ 복용 간격 (INTERVAL만) ─────────────────────────────────
                if selectedType == .interval {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("복용 간격 (일)")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.textMuted)
                        TextField("예: 3", text: $intervalDaysInput)
                            .keyboardType(.numberPad)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 10)
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(MyCatColors.border, lineWidth: 1)
                            )
                    }
                }

                // ─ 메모 ──────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("메모 (선택)")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    TextField("예: 식후 30분", text: $memoInput)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(MyCatColors.border, lineWidth: 1)
                        )
                }

                // ─ 복용 알림 시간 (ONCE 제외) ────────────────────────────
                if selectedType != .once {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("복용 알림 시간")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.textMuted)

                        // 등록된 알림 시간 목록
                        ForEach(alarmTimes, id: \.self) { time in
                            HStack {
                                Text(time)
                                    .font(.system(size: 14))
                                    .foregroundColor(MyCatColors.onBackground)
                                Spacer()
                                Button(action: {
                                    alarmTimes.removeAll { $0 == time }
                                }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.system(size: 18))
                                        .foregroundColor(MyCatColors.textMuted)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(MyCatColors.surface)
                            .cornerRadius(8)
                        }

                        // 시간 선택 + 추가
                        HStack(spacing: 10) {
                            HStack(spacing: 4) {
                                // 시 선택
                                Picker("시", selection: $alarmHour) {
                                    ForEach(0..<24, id: \.self) { h in
                                        Text(String(format: "%02d", h)).tag(h)
                                    }
                                }
                                .pickerStyle(.wheel)
                                .frame(width: 60, height: 100)
                                .clipped()

                                Text(":")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(MyCatColors.onBackground)

                                // 분 선택
                                Picker("분", selection: $alarmMinute) {
                                    ForEach([0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55], id: \.self) { m in
                                        Text(String(format: "%02d", m)).tag(m)
                                    }
                                }
                                .pickerStyle(.wheel)
                                .frame(width: 60, height: 100)
                                .clipped()
                            }
                            .padding(.horizontal, 8)
                            .background(MyCatColors.surface)
                            .cornerRadius(8)

                            Button(action: addAlarmTime) {
                                Text("추가")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(MyCatColors.onPrimary)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(MyCatColors.primary)
                                    .cornerRadius(8)
                            }
                        }
                    }
                }

                // ─ 저장 / 취소 버튼 ─────────────────────────────────────
                HStack(spacing: 12) {
                    Button(action: onDismiss) {
                        Text("취소")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(MyCatColors.textMuted)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(MyCatColors.surface)
                            .cornerRadius(10)
                    }

                    Button(action: handleSave) {
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
        }
    }

    // MARK: - Alarm Time 추가

    private func addAlarmTime() {
        let formatted = String(format: "%02d:%02d", alarmHour, alarmMinute)
        guard !alarmTimes.contains(formatted) else { return }
        alarmTimes.append(formatted)
        alarmTimes.sort()
    }

    // MARK: - Save Handler

    private func handleSave() {
        let trimmedName = nameInput.trimmingCharacters(in: .whitespaces)
        guard !trimmedName.isEmpty else {
            isNameError = true
            return
        }

        let startMs = MedicationViewModel.toEpochMs(startDate)
        let endMs: Int64? = (selectedType != .once && showEndDate)
            ? MedicationViewModel.toEpochMs(endDate)
            : nil
        let interval: Int? = (selectedType == .interval)
            ? Int(intervalDaysInput)
            : nil

        onSave(
            trimmedName,
            selectedType,
            dosageInput,
            startMs,
            endMs,
            interval,
            memoInput,
            alarmTimes
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TypeChip  (Android FilterChip 대응)
// ─────────────────────────────────────────────────────────────────────────────

private struct TypeChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.system(size: 12, weight: isSelected ? .bold : .regular))
                .foregroundColor(isSelected ? MyCatColors.onPrimary : MyCatColors.onBackground)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? MyCatColors.primary : MyCatColors.surface)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(isSelected ? MyCatColors.primary : MyCatColors.border, lineWidth: 1)
                )
        }
    }
}
