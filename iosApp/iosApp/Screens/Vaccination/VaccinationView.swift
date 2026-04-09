import SwiftUI

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - VaccinationView  (Android VaccinationScreen 대응)
// ─────────────────────────────────────────────────────────────────────────────

struct VaccinationView: View {
    let catId: Int64
    var onBack: (() -> Void)? = nil

    @StateObject private var viewModel: VaccinationViewModel
    @Environment(\.dismiss) private var dismiss

    init(catId: Int64, onBack: (() -> Void)? = nil) {
        self.catId = catId
        self.onBack = onBack
        _viewModel = StateObject(wrappedValue: VaccinationViewModel(catId: catId))
    }

    private func handleBack() {
        if let onBack { onBack() } else { dismiss() }
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                // ─── TopBar ───────────────────────────────────────────────
                VaccinationTopBar(
                    catName: viewModel.catName,
                    onBack: handleBack
                )

                // ─── 리스트 / 빈 상태 ──────────────────────────────────────
                if viewModel.records.isEmpty {
                    VaccinationEmptyState()
                } else {
                    VaccinationList(viewModel: viewModel)
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
                    .shadow(
                        color: MyCatColors.primary.opacity(0.4),
                        radius: 6, x: 0, y: 3
                    )
            }
            .padding(.trailing, 20)
            .padding(.bottom, 28)
        }
        .navigationBarHidden(true)
        .onAppear { viewModel.load() }
        // ─── 입력 / 수정 Sheet ─────────────────────────────────────────────
        .sheet(isPresented: $viewModel.showInputSheet) {
            VaccinationInputSheet(
                editingItem: viewModel.editingItem,
                onDismiss: { viewModel.onDismissSheet() },
                onSave: { title, vaccinatedAt, nextDueAt, memo, isNotif in
                    viewModel.saveVaccination(
                        title: title,
                        vaccinatedAt: vaccinatedAt,
                        nextDueAt: nextDueAt,
                        memo: memo,
                        isNotificationEnabled: isNotif
                    )
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - VaccinationTopBar
// ─────────────────────────────────────────────────────────────────────────────

private struct VaccinationTopBar: View {
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
                Text("예방접종")
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
// MARK: - VaccinationEmptyState
// ─────────────────────────────────────────────────────────────────────────────

private struct VaccinationEmptyState: View {
    var body: some View {
        Spacer()
        VStack(spacing: 12) {
            Text("💉")
                .font(.system(size: 48))
            Text("접종 기록이 없어요")
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
// MARK: - VaccinationList
// ─────────────────────────────────────────────────────────────────────────────

private struct VaccinationList: View {
    @ObservedObject var viewModel: VaccinationViewModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 10) {
                ForEach(viewModel.records) { item in
                    VaccinationRecordCard(
                        item: item,
                        onEdit: { viewModel.onEditTap(item) },
                        onDelete: { viewModel.deleteVaccination(id: item.id) }
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
// MARK: - VaccinationRecordCard  (Android VaccinationRecordItem 대응)
// ─────────────────────────────────────────────────────────────────────────────

private struct VaccinationRecordCard: View {
    let item: VaccinationItem
    let onEdit: () -> Void
    let onDelete: () -> Void

    @State private var showDeleteAlert = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // ─ 헤더 행: 접종명 + 알림뱃지 + 수정/삭제 ─────────────────────
            HStack(spacing: 0) {
                Text("💉")
                    .font(.system(size: 16))

                Spacer().frame(width: 8)

                Text(item.title)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)

                if item.isNotificationEnabled {
                    Spacer().frame(width: 6)
                    Text("알림")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(MyCatColors.primary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(MyCatColors.primary.opacity(0.15))
                        .cornerRadius(20)
                }

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

            // ─ 날짜 정보 행 ──────────────────────────────────────────────
            HStack(spacing: 16) {
                // 접종일
                VStack(alignment: .leading, spacing: 2) {
                    Text("접종일")
                        .font(.system(size: 11))
                        .foregroundColor(MyCatColors.textMuted)
                    Text(VaccinationViewModel.formatDate(item.vaccinatedAt))
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                }

                // 다음 예정일 (있을 때만)
                if let nextDue = item.nextDueAt {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("다음 예정일")
                            .font(.system(size: 11))
                            .foregroundColor(MyCatColors.textMuted)
                        Text(
                            "\(VaccinationViewModel.formatDate(nextDue)) (\(VaccinationViewModel.dDayLabel(nextDueAt: nextDue)))"
                        )
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(VaccinationViewModel.dDayColor(nextDueAt: nextDue))
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
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        // ─ 삭제 확인 Alert ────────────────────────────────────────────────
        .alert("\(item.title) 기록을 삭제할까요?", isPresented: $showDeleteAlert) {
            Button("삭제", role: .destructive) { onDelete() }
            Button("취소", role: .cancel) {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - VaccinationInputSheet  (Android VaccinationInputDialog 대응)
// ─────────────────────────────────────────────────────────────────────────────

struct VaccinationInputSheet: View {
    let editingItem: VaccinationItem?
    let onDismiss: () -> Void
    let onSave: (String, Int64, Int64?, String, Bool) -> Void

    // ─ State ─────────────────────────────────────────────────────────────
    @State private var titleInput: String
    @State private var vaccinatedDate: Date
    @State private var nextDueDate: Date?
    @State private var memoInput: String
    @State private var isNotifEnabled: Bool
    @State private var showNextDuePicker: Bool
    @State private var isTitleError: Bool = false
    @State private var isDateError: Bool = false

    // ─ Init ──────────────────────────────────────────────────────────────
    init(
        editingItem: VaccinationItem?,
        onDismiss: @escaping () -> Void,
        onSave: @escaping (String, Int64, Int64?, String, Bool) -> Void
    ) {
        self.editingItem = editingItem
        self.onDismiss = onDismiss
        self.onSave = onSave

        _titleInput       = State(initialValue: editingItem?.title ?? "")
        _memoInput        = State(initialValue: editingItem?.memo ?? "")
        _isNotifEnabled   = State(initialValue: editingItem?.isNotificationEnabled ?? true)

        if let vaccAt = editingItem?.vaccinatedAt {
            _vaccinatedDate = State(initialValue: Date(timeIntervalSince1970: Double(vaccAt) / 1000))
        } else {
            _vaccinatedDate = State(initialValue: Date())
        }

        if let nextMs = editingItem?.nextDueAt {
            _nextDueDate = State(initialValue: Date(timeIntervalSince1970: Double(nextMs) / 1000))
            _showNextDuePicker = State(initialValue: true)
        } else {
            _nextDueDate = State(initialValue: nil)
            _showNextDuePicker = State(initialValue: false)
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // ─ 타이틀 ─────────────────────────────────────────────────
                HStack {
                    Text(editingItem != nil ? "접종 기록 수정" : "접종 기록 추가")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(MyCatColors.textMuted)
                    }
                }

                // ─ 접종명 ─────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("접종명 *")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    TextField("예: 3차 종합백신", text: $titleInput)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(isTitleError ? Color.red : MyCatColors.border, lineWidth: 1)
                        )
                        .onChange(of: titleInput) { _ in isTitleError = false }
                    if isTitleError {
                        Text("접종명을 입력해주세요")
                            .font(.system(size: 11))
                            .foregroundColor(.red)
                    }
                }

                // ─ 접종일 ─────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("접종일 *")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    DatePicker(
                        "",
                        selection: $vaccinatedDate,
                        in: ...Date(),
                        displayedComponents: .date
                    )
                    .datePickerStyle(.compact)
                    .labelsHidden()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .tint(MyCatColors.primary)
                    .onChange(of: vaccinatedDate) { _ in isDateError = false }
                    if isDateError {
                        Text("접종일을 선택해주세요")
                            .font(.system(size: 11))
                            .foregroundColor(.red)
                    }
                }

                // ─ 다음 예정일 (선택) ──────────────────────────────────────
                VStack(alignment: .leading, spacing: 8) {
                    Toggle(isOn: $showNextDuePicker) {
                        Text("다음 예정일 설정")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.textMuted)
                    }
                    .tint(MyCatColors.primary)
                    .onChange(of: showNextDuePicker) { enabled in
                        nextDueDate = enabled ? Date() : nil
                    }

                    if showNextDuePicker {
                        let nextBinding = Binding<Date>(
                            get: { nextDueDate ?? Date() },
                            set: { nextDueDate = $0 }
                        )
                        DatePicker(
                            "",
                            selection: nextBinding,
                            displayedComponents: .date
                        )
                        .datePickerStyle(.compact)
                        .labelsHidden()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .tint(MyCatColors.primary)
                    }
                }

                // ─ 메모 ──────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("메모 (선택)")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    TextField("이상 반응, 주사 부위 등", text: $memoInput)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(MyCatColors.border, lineWidth: 1)
                        )
                }

                // ─ 알림 토글 ──────────────────────────────────────────────
                Toggle(isOn: $isNotifEnabled) {
                    Text("다음 예정일 알림")
                        .font(.system(size: 14))
                        .foregroundColor(MyCatColors.onBackground)
                }
                .tint(MyCatColors.primary)

                // ─ 저장 / 취소 버튼 ───────────────────────────────────────
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

    // MARK: - Save Handler

    private func handleSave() {
        guard !titleInput.trimmingCharacters(in: .whitespaces).isEmpty else {
            isTitleError = true
            return
        }

        let vaccinatedMs = Int64(vaccinatedDate.timeIntervalSince1970 * 1000)
        let nextDueMs: Int64? = showNextDuePicker
            ? Int64((nextDueDate ?? Date()).timeIntervalSince1970 * 1000)
            : nil

        onSave(titleInput, vaccinatedMs, nextDueMs, memoInput, isNotifEnabled)
    }
}
