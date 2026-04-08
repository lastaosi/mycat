import SwiftUI

// MARK: - AppRoot
// 앱의 최상위 네비게이션 컨트롤러.
// Android AppNavHost와 동일한 역할:
//   Splash → Main 또는 ProfileRegister로 전환
//   이후 각 화면에서 NavigationStack으로 세부 화면 이동
struct AppRoot: View {
    @State private var currentRoute: AppRoute = .splash

    var body: some View {
        Group {
            switch currentRoute {
            case .splash:
                SplashView(
                    onNavigateToMain: {
                        withAnimation(.easeInOut(duration: 0.4)) {
                            currentRoute = .main
                        }
                    },
                    onNavigateToProfileRegister: {
                        withAnimation(.easeInOut(duration: 0.4)) {
                            currentRoute = .profileRegister
                        }
                    }
                )
            case .main:
                // MainView - 다음 단계에서 구현 예정
                MainTabView()
            case .profileRegister:
                ProfileRegisterView(onSaved: {
                        withAnimation(.easeInOut(duration: 0.4)) {
                            currentRoute = .main
                        }
                    })
            default:
                PlaceholderView(title: "준비 중") {
                    currentRoute = .main
                }
            }
        }
    }
}

// MARK: - 임시 화면 (각 화면 구현 전까지 사용)
 struct PlaceholderView: View {
    let title: String
    let onContinue: () -> Void

    var body: some View {
        ZStack {
            MyCatColors.background.ignoresSafeArea()
            VStack(spacing: 24) {
                Text("🐱")
                    .font(.system(size: 64))
                Text(title)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(MyCatColors.onBackground)
                Text("화면 구현 중입니다")
                    .font(.system(size: 14))
                    .foregroundColor(MyCatColors.textMuted)
                Button(action: onContinue) {
                    Text("계속하기")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(MyCatColors.onPrimary)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 12)
                        .background(MyCatColors.primary)
                        .cornerRadius(12)
                }
            }
        }
    }
    
   
}
