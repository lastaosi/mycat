import SwiftUI
import ComposeApp

// ═══════════════════════════════════════════════════════════════════
// MARK: - DrawerState  (EnvironmentObject — 햄버거 버튼에서 공유)
// ═══════════════════════════════════════════════════════════════════
class DrawerState: ObservableObject {
    @Published var isOpen: Bool = false

    func toggle() {
        withAnimation(.easeInOut(duration: 0.28)) { isOpen.toggle() }
    }
    func close() {
        withAnimation(.easeInOut(duration: 0.28)) { isOpen = false }
    }
    func open() {
        withAnimation(.easeInOut(duration: 0.28)) { isOpen = true }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - DrawerMenuItem  (Android DrawerItem 대응)
// ═══════════════════════════════════════════════════════════════════
enum DrawerMenuItem: CaseIterable, Hashable {
    case home
    case careGuide
    case weight
    case healthCheck
    case vaccination
    case medication
    case diary
    case vetMap

    var label: String {
        switch self {
        case .home:        return "홈"
        case .careGuide:   return "케어 가이드"
        case .weight:      return "체중 기록"
        case .healthCheck: return "건강 체크리스트"
        case .vaccination: return "예방접종"
        case .medication:  return "약 복용 관리"
        case .diary:       return "다이어리"
        case .vetMap:      return "근처 동물병원"
        }
    }

    var emoji: String {
        switch self {
        case .home:        return "🏠"
        case .careGuide:   return "📋"
        case .weight:      return "⚖️"
        case .healthCheck: return "❤️"
        case .vaccination: return "💉"
        case .medication:  return "💊"
        case .diary:       return "📝"
        case .vetMap:      return "🏥"
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - DrawerHamburgerButton  (각 화면 툴바에서 재사용)
// ═══════════════════════════════════════════════════════════════════
struct DrawerHamburgerButton: View {
    @EnvironmentObject var drawerState: DrawerState

    var body: some View {
        Button {
            drawerState.toggle()
        } label: {
            Image(systemName: "line.3.horizontal")
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(MyCatColors.primary)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - MainDrawerView  (Android MainScreen + ModalNavigationDrawer 대응)
// ═══════════════════════════════════════════════════════════════════
struct MainDrawerView: View {

    @StateObject private var drawerVM    = MainDrawerViewModel()
    @StateObject private var drawerState = DrawerState()
    @State private var selectedItem: DrawerMenuItem = .home
    @State private var showProfileRegister = false
    @State private var showProfileEdit     = false

    private let drawerWidth: CGFloat = 285

    // MARK: body
    var body: some View {
        ZStack(alignment: .leading) {

            // ── 1. 메인 콘텐츠 ─────────────────────────────────────
            mainContent
                .environmentObject(drawerState)
                .disabled(drawerState.isOpen)   // 드로어 열릴 때 백그라운드 인터랙션 차단

            // ── 2. 딤 오버레이 ─────────────────────────────────────
            if drawerState.isOpen {
                Color.black.opacity(0.45)
                    .ignoresSafeArea()
                    .onTapGesture { drawerState.close() }
                    .zIndex(1)
                    .transition(.opacity)
            }

            // ── 3. 드로어 패널 ─────────────────────────────────────
            DrawerPanelView(
                drawerVM:       drawerVM,
                selectedItem:   selectedItem,
                onItemSelected: { item in
                    selectedItem = item
                    drawerState.close()
                },
                onAddCat: {
                    drawerState.close()
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.32) {
                        showProfileRegister = true
                    }
                },
                onEditProfile: {
                    drawerState.close()
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.32) {
                        showProfileEdit = true
                    }
                }
            )
            .frame(width: drawerWidth)
            .offset(x: drawerState.isOpen ? 0 : -drawerWidth)
            .zIndex(2)
        }
        // ── 스와이프 제스처 (왼쪽 엣지에서 오른쪽으로 → 열기)
        .gesture(
            DragGesture(minimumDistance: 20, coordinateSpace: .local)
                .onEnded { value in
                    if !drawerState.isOpen,
                       value.startLocation.x < 40,
                       value.translation.width > 80 {
                        drawerState.open()
                    } else if drawerState.isOpen,
                              value.translation.width < -80 {
                        drawerState.close()
                    }
                }
        )
        // ── 프로필 등록 시트
        .sheet(isPresented: $showProfileRegister) {
            ProfileRegisterView(onSaved: { showProfileRegister = false })
        }
        // ── 프로필 수정 시트
        .sheet(isPresented: $showProfileEdit) {
            if let cat = drawerVM.representativeCat {
                ProfileRegisterView(
                    onSaved: { showProfileEdit = false },
                    catId: cat.id
                )
            }
        }
        .onAppear { drawerVM.load() }
    }

    // MARK: - 메뉴 항목별 콘텐츠 스위처
    @ViewBuilder
    private var mainContent: some View {
        let catId = drawerVM.currentCatId
        switch selectedItem {
        case .home:
            HomeView()

        case .careGuide:
            NavigationStack {
                PlaceholderView(title: "케어 가이드") {}
                    .navigationTitle("케어 가이드")
                    .navigationBarTitleDisplayMode(.large)
                    .background(MyCatColors.background)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            DrawerHamburgerButton()
                        }
                    }
            }
            .environmentObject(drawerState)

        case .weight:
            NavigationStack {
                WeightView(catId: catId)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            DrawerHamburgerButton()
                        }
                    }
            }
            .environmentObject(drawerState)

        case .healthCheck:
            HealthCheckView()
                .environmentObject(drawerState)

        case .vaccination:
            NavigationStack {
                VaccinationView(catId: catId)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            DrawerHamburgerButton()
                        }
                    }
            }
            .environmentObject(drawerState)

        case .medication:
            NavigationStack {
                MedicationView(catId: catId)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            DrawerHamburgerButton()
                        }
                    }
            }
            .environmentObject(drawerState)

        case .diary:
            NavigationStack {
                DiaryView(catId: catId)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            DrawerHamburgerButton()
                        }
                    }
            }
            .environmentObject(drawerState)

        case .vetMap:
            NavigationStack {
                PlaceholderView(title: "근처 동물병원") {}
                    .navigationTitle("근처 동물병원")
                    .navigationBarTitleDisplayMode(.large)
                    .background(MyCatColors.background)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            DrawerHamburgerButton()
                        }
                    }
            }
            .environmentObject(drawerState)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - DrawerPanelView  (Android ModalDrawerSheet 대응)
// ═══════════════════════════════════════════════════════════════════
struct DrawerPanelView: View {

    @ObservedObject var drawerVM: MainDrawerViewModel
    let selectedItem:   DrawerMenuItem
    let onItemSelected: (DrawerMenuItem) -> Void
    let onAddCat:       () -> Void
    let onEditProfile:  () -> Void

    var body: some View {
        VStack(spacing: 0) {

            // ── 헤더 (고양이 프로필)
            DrawerHeaderView(
                drawerVM:      drawerVM,
                onAddCat:      onAddCat,
                onEditProfile: onEditProfile
            )

            // ── 구분선
            Divider()
                .background(MyCatColors.border)

            Spacer().frame(height: 8)

            // ── 메뉴 목록
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    ForEach(DrawerMenuItem.allCases, id: \.self) { item in
                        DrawerMenuItemView(
                            item:       item,
                            isSelected: selectedItem == item,
                            onTap:      { onItemSelected(item) }
                        )
                    }
                }
                .padding(.bottom, 8)
            }

            Spacer()

            // ── 하단 버전 정보
            Text("MY Cat v1.0.0")
                .font(.system(size: 11))
                .foregroundColor(MyCatColors.textMuted)
                .padding(.bottom, 32)
        }
        .frame(maxHeight: .infinity)
        .background(MyCatColors.background)
        .ignoresSafeArea(edges: .vertical)
        .shadow(color: .black.opacity(0.14), radius: 10, x: 5, y: 0)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - DrawerHeaderView  (Android DrawerHeader 대응)
// ═══════════════════════════════════════════════════════════════════
struct DrawerHeaderView: View {

    @ObservedObject var drawerVM: MainDrawerViewModel
    let onAddCat:      () -> Void
    let onEditProfile: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {

            // ── 대표 고양이 행 ────────────────────────────────────
            HStack(alignment: .center, spacing: 12) {

                // 프로필 사진
                ZStack {
                    Circle()
                        .fill(MyCatColors.border)
                        .frame(width: 64, height: 64)

                    if let photoPath = drawerVM.representativeCat?.photoPath,
                       let uiImage = UIImage(contentsOfFile: photoPath) {
                        Image(uiImage: uiImage)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 64, height: 64)
                            .clipShape(Circle())
                    } else {
                        Text("🐱")
                            .font(.system(size: 28))
                    }
                }

                // 이름 + 품종
                VStack(alignment: .leading, spacing: 3) {
                    if let cat = drawerVM.representativeCat {
                        Text(cat.name)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(MyCatColors.onBackground)
                            .lineLimit(1)
                        Text(cat.breedName)
                            .font(.system(size: 12))
                            .foregroundColor(MyCatColors.textMuted)
                            .lineLimit(1)
                    } else {
                        Text("고양이를 등록해주세요")
                            .font(.system(size: 14))
                            .foregroundColor(MyCatColors.textMuted)
                    }
                }

                Spacer()

                // 수정 아이콘 버튼
                Button(action: onEditProfile) {
                    Image(systemName: "pencil")
                        .font(.system(size: 14))
                        .foregroundColor(MyCatColors.textMuted)
                        .frame(width: 28, height: 28)
                }
            }

            // ── 멀티캣 전환 (2마리 이상일 때) ────────────────────
            if drawerVM.allCats.count > 1 {
                Spacer().frame(height: 12)
                Divider().background(MyCatColors.border)
                Spacer().frame(height: 8)

                Text("고양이 전환")
                    .font(.system(size: 11))
                    .foregroundColor(MyCatColors.textMuted)

                Spacer().frame(height: 6)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(drawerVM.allCats, id: \.id) { cat in
                            CatSwitchChip(
                                cat:   cat,
                                onTap: { drawerVM.selectCat(catId: cat.id) }
                            )
                        }
                    }
                }
            }

            // ── 구분 + 추가 버튼 ─────────────────────────────────
            Spacer().frame(height: 8)
            Divider().background(MyCatColors.border)
            Spacer().frame(height: 4)

            Button(action: onAddCat) {
                Text("+ 고양이 추가")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(MyCatColors.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 56)      // Safe Area 여백 (상태바)
        .padding(.bottom, 16)
        .background(MyCatColors.surface)
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - CatSwitchChip  (Android DrawerHeader 내 고양이 전환 칩)
// ═══════════════════════════════════════════════════════════════════
struct CatSwitchChip: View {

    let cat:   CatSummaryIos
    let onTap: () -> Void

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                // 배경 원
                Circle()
                    .fill(cat.isRepresentative ? MyCatColors.primary.opacity(0.15) : MyCatColors.border)
                    .frame(width: 44, height: 44)

                // 테두리 (대표 고양이만)
                if cat.isRepresentative {
                    Circle()
                        .stroke(MyCatColors.primary, lineWidth: 2)
                        .frame(width: 44, height: 44)
                }

                // 프로필 사진 or 이모지
                if let photoPath = cat.photoPath,
                   let uiImage = UIImage(contentsOfFile: photoPath) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 44, height: 44)
                        .clipShape(Circle())
                } else {
                    Text("🐱")
                        .font(.system(size: 20))
                }
            }

            Text(cat.name)
                .font(.system(size: 10,
                              weight: cat.isRepresentative ? .bold : .regular))
                .foregroundColor(cat.isRepresentative
                                 ? MyCatColors.primary
                                 : MyCatColors.textMuted)
                .lineLimit(1)
        }
        .frame(width: 52)
        .onTapGesture { onTap() }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MARK: - DrawerMenuItemView  (Android DrawerMenuItem 대응)
// ═══════════════════════════════════════════════════════════════════
struct DrawerMenuItemView: View {

    let item:       DrawerMenuItem
    let isSelected: Bool
    let onTap:      () -> Void

    var body: some View {
        let bgColor    = isSelected ? MyCatColors.surface      : MyCatColors.background
        let textColor  = isSelected ? MyCatColors.primary      : MyCatColors.onBackground
        let fontWeight: Font.Weight = isSelected ? .bold       : .regular

        HStack(spacing: 0) {
            // 선택 인디케이터 바
            RoundedRectangle(cornerRadius: 2)
                .fill(isSelected ? MyCatColors.primary : Color.clear)
                .frame(width: 3, height: 20)

            Spacer().frame(width: 10)

            // 이모지
            Text(item.emoji)
                .font(.system(size: 16))
                .frame(width: 22, height: 22)

            Spacer().frame(width: 10)

            // 라벨
            Text(item.label)
                .font(.system(size: 14, weight: fontWeight))
                .foregroundColor(textColor)

            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 13)
        .background(bgColor)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .padding(.horizontal, 12)
        .padding(.vertical, 2)
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
    }
}
