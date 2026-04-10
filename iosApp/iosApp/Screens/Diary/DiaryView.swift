import SwiftUI
import PhotosUI

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - DiaryView  (Android DiaryScreen 대응)
// ─────────────────────────────────────────────────────────────────────────────

struct DiaryView: View {
    let catId: Int64
    var onBack: (() -> Void)? = nil

    @StateObject private var viewModel: DiaryViewModel
    @Environment(\.dismiss) private var dismiss

    init(catId: Int64, onBack: (() -> Void)? = nil) {
        self.catId = catId
        self.onBack = onBack
        _viewModel = StateObject(wrappedValue: DiaryViewModel(catId: catId))
    }

    private func handleBack() {
        if let onBack { onBack() } else { dismiss() }
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                // ─── TopBar ────────────────────────────────────────────────
                DiaryTopBar(catName: viewModel.catName)

                // ─── 리스트 / 빈 상태 ────────────────────────────────────────
                if viewModel.diaries.isEmpty {
                    DiaryEmptyState()
                } else {
                    DiaryList(viewModel: viewModel)
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
            DiaryInputSheet(
                editingItem: viewModel.editingItem,
                onDismiss: { viewModel.onDismissSheet() },
                onSave: { title, content, mood, photoPath, dateMillis in
                    viewModel.saveDiary(
                        title: title,
                        content: content,
                        mood: mood,
                        photoPath: photoPath,
                        dateMillis: dateMillis
                    )
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - DiaryTopBar
// ─────────────────────────────────────────────────────────────────────────────

private struct DiaryTopBar: View {
    let catName: String
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        HStack(spacing: 4) {
            DrawerHamburgerButton(tint: MyCatColors.onPrimary)
                .environmentObject(drawerState)

            VStack(alignment: .leading, spacing: 1) {
                Text("다이어리")
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
// MARK: - DiaryEmptyState
// ─────────────────────────────────────────────────────────────────────────────

private struct DiaryEmptyState: View {
    var body: some View {
        Spacer()
        VStack(spacing: 12) {
            Text("📝")
                .font(.system(size: 48))
            Text("아직 다이어리가 없어요")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(MyCatColors.onBackground)
            Text("소중한 순간을 기록해보세요")
                .font(.system(size: 13))
                .foregroundColor(MyCatColors.textMuted)
        }
        Spacer()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - DiaryList
// ─────────────────────────────────────────────────────────────────────────────

private struct DiaryList: View {
    @ObservedObject var viewModel: DiaryViewModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 10) {
                ForEach(viewModel.diaries) { item in
                    DiaryCard(
                        item: item,
                        onEdit: { viewModel.onEditTap(item) },
                        onDelete: { viewModel.deleteDiary(id: item.id) }
                    )
                    .padding(.horizontal, 16)
                }
            }
            .padding(.vertical, 16)
            .padding(.bottom, 88)   // FAB 여백
        }
        .background(MyCatColors.background)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - DiaryCard  (Android DiaryItem 대응)
// ─────────────────────────────────────────────────────────────────────────────

private struct DiaryCard: View {
    let item: DiaryItem
    let onEdit: () -> Void
    let onDelete: () -> Void

    @State private var showDeleteAlert = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // ─ 헤더: 기분 이모지 + 날짜 + 수정/삭제 ────────────────────────
            HStack {
                HStack(spacing: 6) {
                    if let mood = item.mood {
                        Text(mood.emoji)
                            .font(.system(size: 20))
                    }
                    Text(DiaryViewModel.formatDate(item.createdAt))
                        .font(.system(size: 12))
                        .foregroundColor(MyCatColors.textMuted)
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

            Spacer().frame(height: 8)

            // ─ 제목 (있을 때만) ───────────────────────────────────────────
            if let title = item.title, !title.isEmpty {
                Text(title)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                Spacer().frame(height: 4)
            }

            // ─ 내용 (3줄 제한) ─────────────────────────────────────────────
            Text(item.content)
                .font(.system(size: 13))
                .foregroundColor(MyCatColors.onBackground.opacity(0.8))
                .lineLimit(3)

            // ─ 사진 (있을 때만) ───────────────────────────────────────────
            if let photoPath = item.photoPath, !photoPath.isEmpty {
                Spacer().frame(height: 8)
                DiaryPhotoView(photoPath: photoPath)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        .alert("다이어리 삭제", isPresented: $showDeleteAlert) {
            Button("삭제", role: .destructive) { onDelete() }
            Button("취소", role: .cancel) {}
        } message: {
            Text("이 기록을 삭제할까요?")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - DiaryPhotoView  (사진 표시 - 로컬 파일 경로 지원)
// ─────────────────────────────────────────────────────────────────────────────

private struct DiaryPhotoView: View {
    let photoPath: String

    var body: some View {
        let url = URL(fileURLWithPath: photoPath)
        if let uiImage = UIImage(contentsOfFile: resolvePhotoPath(photoPath)) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity)
                .frame(height: 160)
                .clipped()
                .cornerRadius(10)
        } else {
            // 이미지 로드 실패 시 placeholder
            RoundedRectangle(cornerRadius: 10)
                .fill(MyCatColors.surface)
                .frame(maxWidth: .infinity)
                .frame(height: 160)
                .overlay(
                    Image(systemName: "photo")
                        .font(.system(size: 28))
                        .foregroundColor(MyCatColors.textMuted)
                )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - DiaryInputSheet  (Android DiaryInputDialog 대응)
// ─────────────────────────────────────────────────────────────────────────────

struct DiaryInputSheet: View {
    let editingItem: DiaryItem?
    let onDismiss: () -> Void
    let onSave: (String, String, DiaryMoodSwift?, String?, Int64) -> Void

    // ─ State ─────────────────────────────────────────────────────────────
    @State private var titleInput: String
    @State private var contentInput: String
    @State private var selectedMood: DiaryMoodSwift?
    @State private var selectedDate: Date
    @State private var photoPath: String?

    // 사진 선택 관련
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var isLoadingPhoto: Bool = false

    // 유효성
    @State private var isContentError: Bool = false
    @FocusState private var isFocused: Bool

    // ─ Init ──────────────────────────────────────────────────────────────
    init(
        editingItem: DiaryItem?,
        onDismiss: @escaping () -> Void,
        onSave: @escaping (String, String, DiaryMoodSwift?, String?, Int64) -> Void
    ) {
        self.editingItem = editingItem
        self.onDismiss = onDismiss
        self.onSave = onSave

        _titleInput    = State(initialValue: editingItem?.title ?? "")
        _contentInput  = State(initialValue: editingItem?.content ?? "")
        _selectedMood  = State(initialValue: editingItem?.mood)
        _photoPath     = State(initialValue: editingItem?.photoPath)

        if let createdAt = editingItem?.createdAt {
            _selectedDate = State(initialValue: Date(timeIntervalSince1970: Double(createdAt) / 1000))
        } else {
            _selectedDate = State(initialValue: Date())
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // ─ 시트 헤더 ──────────────────────────────────────────────
                HStack {
                    Text(editingItem != nil ? "다이어리 수정" : "다이어리 작성")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(MyCatColors.onBackground)
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(MyCatColors.textMuted)
                    }
                }

                // ─ 날짜 선택 ───────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("날짜 *")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    DatePicker("", selection: $selectedDate, displayedComponents: .date)
                        .datePickerStyle(.compact)
                        .labelsHidden()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .tint(MyCatColors.primary)
                }

                // ─ 제목 (선택) ────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("제목 (선택)")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    TextField("오늘의 기록", text: $titleInput)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(MyCatColors.border, lineWidth: 1)
                        )
                }

                // ─ 내용 (필수) ────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 4) {
                    Text("내용 *")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)
                    ZStack(alignment: .topLeading) {
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(isContentError ? Color.red : MyCatColors.border, lineWidth: 1)
                        TextEditor(text: $contentInput)
                            .focused($isFocused)
                            .frame(height: 120)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 6)
                            .onChange(of: contentInput) { _ in isContentError = false }
                        if contentInput.isEmpty {
                            Text("오늘 고양이와 있었던 일을 기록해보세요")
                                .font(.system(size: 14))
                                .foregroundColor(.gray.opacity(0.5))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 14)
                                .allowsHitTesting(false)
                        }
                    }
                    .frame(height: 120)
                    if isContentError {
                        Text("내용을 입력해주세요")
                            .font(.system(size: 11))
                            .foregroundColor(.red)
                    }
                }

                // ─ 기분 선택 ──────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 8) {
                    Text("기분")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)

                    HStack(spacing: 8) {
                        ForEach(DiaryMoodSwift.allCases, id: \.self) { mood in
                            MoodButton(
                                mood: mood,
                                isSelected: selectedMood == mood,
                                onTap: {
                                    // 이미 선택된 기분을 탭하면 선택 해제
                                    selectedMood = (selectedMood == mood) ? nil : mood
                                }
                            )
                        }
                    }
                }

                // ─ 사진 선택 (PhotosPicker) ──────────────────────────────
                VStack(alignment: .leading, spacing: 8) {
                    Text("사진")
                        .font(.system(size: 13))
                        .foregroundColor(MyCatColors.textMuted)

                    if let path = photoPath, !path.isEmpty {
                        // 선택된 사진 미리보기
                        ZStack(alignment: .topTrailing) {
                            DiaryPhotoEditView(photoPath: path)

                            // 사진 제거 버튼
                            Button(action: { photoPath = nil }) {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.system(size: 22))
                                    .foregroundColor(.white)
                                    .shadow(radius: 2)
                            }
                            .padding(6)
                        }
                    } else {
                        // 사진 선택 버튼
                        PhotosPicker(
                            selection: $selectedPhotoItem,
                            matching: .images,
                            photoLibrary: .shared()
                        ) {
                            HStack {
                                Image(systemName: "photo.on.rectangle")
                                    .font(.system(size: 16))
                                Text(isLoadingPhoto ? "불러오는 중..." : "🖼️  사진 선택")
                                    .font(.system(size: 13))
                            }
                            .foregroundColor(MyCatColors.textMuted)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(MyCatColors.border, lineWidth: 1)
                            )
                        }
                        .disabled(isLoadingPhoto)
                    }
                }
                .onChange(of: selectedPhotoItem) { newItem in
                    guard let newItem else { return }
                    isLoadingPhoto = true
                    Task {
                        if let path = await savePhotoToDocuments(item: newItem) {
                            await MainActor.run {
                                photoPath = path
                                isLoadingPhoto = false
                            }
                        } else {
                            await MainActor.run { isLoadingPhoto = false }
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

    // MARK: - Save Handler

    private func handleSave() {
        let trimmedContent = contentInput.trimmingCharacters(in: .whitespaces)
        guard !trimmedContent.isEmpty else {
            isContentError = true
            return
        }

        let dateMillis = DiaryViewModel.toEpochMs(selectedDate)
        let trimmedTitle = titleInput.trimmingCharacters(in: .whitespaces)

        onSave(
            trimmedTitle,
            trimmedContent,
            selectedMood,
            photoPath,
            dateMillis
        )
    }

    // MARK: - Photo 저장 (Documents 폴더)

    /// PhotosPickerItem 을 Documents 폴더에 저장하고 파일 경로를 반환한다.
    /// Android 의 saveImageToInternalStorage() 에 대응.
    private func savePhotoToDocuments(item: PhotosPickerItem) async -> String? {
        guard let data = try? await item.loadTransferable(type: Data.self) else { return nil }
        guard let image = UIImage(data: data) else { return nil }

        // EXIF 방향 보정 후 JPEG 변환
        let fixedImage = fixOrientation(image)
        guard let jpegData = fixedImage.jpegData(compressionQuality: 0.85) else { return nil }

        let fileName = "diary_\(Int64(Date().timeIntervalSince1970 * 1000)).jpg"
        let documentsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let fileURL = documentsDir.appendingPathComponent(fileName)

        do {
            try jpegData.write(to: fileURL)
            return fileName
        } catch {
            return nil
        }
    }

    /// UIImage EXIF 방향 보정 (Android saveImageToInternalStorage 의 EXIF 처리에 대응)
    private func fixOrientation(_ image: UIImage) -> UIImage {
        guard image.imageOrientation != .up else { return image }
        UIGraphicsBeginImageContextWithOptions(image.size, false, image.scale)
        image.draw(in: CGRect(origin: .zero, size: image.size))
        let normalized = UIGraphicsGetImageFromCurrentImageContext() ?? image
        UIGraphicsEndImageContext()
        return normalized
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MoodButton  (Android 기분 이모지 버튼 대응)
// ─────────────────────────────────────────────────────────────────────────────

private struct MoodButton: View {
    let mood: DiaryMoodSwift
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(mood.emoji)
                .font(.system(size: 20))
                .frame(width: 40, height: 40)
                .background(isSelected ? MyCatColors.surface : MyCatColors.background)
                .clipShape(Circle())
                .overlay(
                    Circle()
                        .stroke(
                            isSelected ? MyCatColors.primary : MyCatColors.border,
                            lineWidth: isSelected ? 2 : 1
                        )
                )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - DiaryPhotoEditView  (입력 시트 내 사진 미리보기)
// ─────────────────────────────────────────────────────────────────────────────

private struct DiaryPhotoEditView: View {
    let photoPath: String

    var body: some View {
        let url = URL(fileURLWithPath: photoPath)
        if let uiImage = UIImage(contentsOfFile: resolvePhotoPath(photoPath)) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity)
                .frame(height: 120)
                .clipped()
                .cornerRadius(10)
        } else {
            RoundedRectangle(cornerRadius: 10)
                .fill(MyCatColors.surface)
                .frame(maxWidth: .infinity)
                .frame(height: 120)
                .overlay(
                    Image(systemName: "photo")
                        .font(.system(size: 24))
                        .foregroundColor(MyCatColors.textMuted)
                )
        }
    }
}
