import SwiftUI
import PhotosUI

struct ProfileRegisterView: View {
    let onSaved: () -> Void
    var catId: Int64? = nil

    @StateObject private var viewModel = ProfileViewModel()
    @State private var selectedPhoto: PhotosPickerItem? = nil
    @State private var showGenderPicker = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {

                    // 사진 등록
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        ZStack {
                            Circle()
                                .fill(MyCatColors.surface)
                                .frame(width: 120, height: 120)
                            if let photoPath = viewModel.photoPath,
                               let uiImage = UIImage(contentsOfFile: photoPath) {
                                Image(uiImage: uiImage)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 120, height: 120)
                                    .clipShape(Circle())
                            } else {
                                VStack(spacing: 4) {
                                    Text("🐱").font(.system(size: 40))
                                    Text("사진 등록")
                                        .font(.system(size: 12))
                                        .foregroundColor(MyCatColors.textMuted)
                                }
                            }
                        }
                    }
                    .onChange(of: selectedPhoto) { _, item in
                        Task {
                            if let data = try? await item?.loadTransferable(type: Data.self) {
                                // 이미지 저장
                                if let path = saveImageToDocuments(data: data) {
                                    await MainActor.run {
                                        viewModel.photoPath = path
                                    }
                                }
                            }
                        }
                    }

                    // Gemini 품종 인식 버튼
                    Button(action: {
                        if let photoPath = viewModel.photoPath,
                           let data = try? Data(contentsOf: URL(fileURLWithPath: photoPath)) {
                            viewModel.recognizeBreed(imageData: data)
                        }
                    }) {
                        HStack {
                            if viewModel.isLoading {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Text("🔍 품종 자동 인식")
                            }
                        }
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(viewModel.photoPath != nil ? MyCatColors.primary : MyCatColors.border)
                        .cornerRadius(8)
                    }
                    .disabled(viewModel.photoPath == nil || viewModel.isLoading)

                    if !viewModel.geminiResult.isEmpty {
                        Text("인식 결과: \(viewModel.geminiResult)")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.primary)
                    }

                    // 이름
                    VStack(alignment: .leading, spacing: 4) {
                        Text("이름 *")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.textMuted)
                        TextField("이름을 입력해주세요", text: $viewModel.name)
                            .textFieldStyle(.roundedBorder)
                    }

                    // 생년월
                    VStack(alignment: .leading, spacing: 4) {
                        Text("생년월 * (예: 202303)")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.textMuted)
                        TextField("YYYYMM", text: $viewModel.birthDate)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.numberPad)
                            .onChange(of: viewModel.birthDate) { _, value in
                                // 숫자만, 최대 6자리
                                let filtered = value.filter { $0.isNumber }
                                if filtered.count <= 6 {
                                    viewModel.birthDate = filtered
                                }
                            }
                    }

                    // 성별
                    VStack(alignment: .leading, spacing: 8) {
                        Text("성별")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.textMuted)
                        HStack(spacing: 8) {
                            ForEach(["MALE", "FEMALE", "UNKNOWN"], id: \.self) { g in
                                Button(action: { viewModel.gender = g }) {
                                    Text(g == "MALE" ? "수컷" : g == "FEMALE" ? "암컷" : "모름")
                                        .font(.system(size: 14))
                                        .foregroundColor(viewModel.gender == g ? .white : MyCatColors.onBackground)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 8)
                                        .background(viewModel.gender == g ? MyCatColors.primary : MyCatColors.surface)
                                        .cornerRadius(20)
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    // 품종 검색
                    VStack(alignment: .leading, spacing: 4) {
                        Text("품종")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.textMuted)

                        // 검색 결과
                        if !viewModel.breedSearchResults.isEmpty {
                            VStack(spacing: 0) {
                                ForEach(viewModel.breedSearchResults, id: \.0) { id, name in
                                    Button(action: { viewModel.selectBreed(id: id, name: name) }) {
                                        HStack {
                                            Text(name)
                                                .font(.system(size: 15))
                                                .foregroundColor(MyCatColors.onBackground)
                                            Spacer()
                                        }
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 12)
                                    }
                                    Divider()
                                }
                            }
                            .background(Color.white)
                            .cornerRadius(8)
                            .shadow(radius: 4)
                        }

                        TextField("품종 검색 (예: 코리안, 페르시안)", text: $viewModel.breedNameCustom)
                            .textFieldStyle(.roundedBorder)
                            .onChange(of: viewModel.breedNameCustom) { _, value in
                                viewModel.searchBreeds(keyword: value)
                            }
                    }

                    // 중성화
                    Toggle("중성화", isOn: $viewModel.isNeutered)
                        .tint(MyCatColors.primary)

                    // 메모
                    VStack(alignment: .leading, spacing: 4) {
                        Text("메모")
                            .font(.system(size: 13))
                            .foregroundColor(MyCatColors.textMuted)
                        TextEditor(text: $viewModel.memo)
                            .frame(height: 100)
                            .padding(4)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(MyCatColors.border, lineWidth: 1)
                            )
                    }

                    // 저장 버튼
                    Button(action: { viewModel.save() }) {
                        HStack {
                            if viewModel.isLoading {
                                ProgressView().tint(.white)
                            } else {
                                Text(catId != nil ? "수정하기" : "등록하기")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(MyCatColors.primary)
                        .cornerRadius(12)
                    }
                    .disabled(viewModel.isLoading)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 24)
            }
            .background(MyCatColors.background)
            .navigationTitle(catId != nil ? "프로필 수정" : "고양이 등록")
            .navigationBarTitleDisplayMode(.large)
            .alert("오류", isPresented: .constant(viewModel.errorMessage != nil)) {
                Button("확인") { viewModel.errorMessage = nil }
            } message: {
                Text(viewModel.errorMessage ?? "")
            }
            .onChange(of: viewModel.isSaved) { _, saved in
                if saved { onSaved() }
            }
        }
        .onAppear {
            viewModel.catId = catId
        }
    }

    // 이미지 Documents에 저장
    private func saveImageToDocuments(data: Data) -> String? {
        let fileName = "cat_photo_\(Date().timeIntervalSince1970).jpg"
        let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(fileName)
        try? data.write(to: url)
        return url.path
    }
    
}
