import SwiftUI

struct SplashView: View {
    let onNavigateToMain: () -> Void
    let onNavigateToProfileRegister: () -> Void

    @StateObject private var viewModel = SplashViewModel()

    // walk 애니메이션
    @State private var walkOffsetX: CGFloat = -400
    @State private var walkGoingRight = true

    // run 애니메이션
    @State private var runOffsetX: CGFloat = 0
    @State private var runOffsetY: CGFloat = 0
    @State private var runDirX: CGFloat = 1
    @State private var runDirY: CGFloat = 1
    @State private var runFacingRight = true

    private let bgColor = Color(red: 1.0, green: 0.973, blue: 0.941) // #FFF8F0
    private let titleColor = Color(red: 0.545, green: 0.369, blue: 0.235) // #8B5E3C
    private let hintColor = Color(red: 0.667, green: 0.533, blue: 0.400) // #AA8866

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height

            ZStack {
                // 배경
                bgColor.ignoresSafeArea()

                // 1. sit — 하단 고정
                Image("byeori_sit")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 200, height: 200)
                    .position(x: w / 2, y: h - 130)

                // 2. roll — 중앙에서 위로 고정
                Image("byeori_roll")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 180, height: 180)
                    .position(x: w / 2, y: h / 2 - 120)

                // 3. walk — 좌우 왕복
                Image("byeori_walk")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 130, height: 130)
                    .scaleEffect(x: walkGoingRight ? 1 : -1, y: 1)
                    .position(
                        x: w / 2 + walkOffsetX,
                        y: h / 2 + 180
                    )

                // 4. run — 랜덤 튕기기
                Image("byeori_run")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 130, height: 130)
                    .scaleEffect(x: runFacingRight ? 1 : -1, y: 1)
                    .position(
                        x: w / 2 + runOffsetX,
                        y: h / 2 + runOffsetY
                    )

                // 5. 로고 텍스트
                Text("My Cat")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(titleColor)
                    .position(x: w / 2, y: 140)

                // 6. 로딩 텍스트
                Text("초기 데이터를 생성중입니다..")
                    .font(.system(size: 13))
                    .foregroundColor(hintColor)
                    .position(x: w / 2, y: h - 30)
            }
            .onAppear {
                startWalkLoop()
                startRunLoop()
                viewModel.start()
            }
            .onChange(of: viewModel.destination) { _, dest in
                switch dest {
                case .main:            onNavigateToMain()
                case .profileRegister: onNavigateToProfileRegister()
                case .loading:         break
                }
            }
        }
    }

    // MARK: - Walk 애니메이션 (좌우 왕복 2초)
    private func startWalkLoop() {
        Task { @MainActor in
            while true {
                let target: CGFloat = walkGoingRight ? 400 : -400
                withAnimation(.linear(duration: 3.0)) {
                    walkOffsetX = target
                }
                try? await Task.sleep(nanoseconds: 2_080_000_000)
                walkGoingRight.toggle()
            }
        }
    }

    // MARK: - Run 애니메이션 (랜덤 튕기기)
    private func startRunLoop() {
        Task { @MainActor in
            while true {
                let nextX = runOffsetX + runDirX * 4
                let nextY = runOffsetY + runDirY * 4

                if abs(nextX) >= 380 {
                    runDirX *= -1
                    runFacingRight = runDirX > 0
                }
                if abs(nextY) >= 300 {
                    runDirY *= -1
                }

                withAnimation(.linear(duration: 0.016)) {
                    runOffsetX += runDirX * 4
                    runOffsetY += runDirY * 4
                }

                try? await Task.sleep(nanoseconds: 16_000_000) // ~60fps
            }
        }
    }
}

#Preview {
    SplashView(
        onNavigateToMain: {},
        onNavigateToProfileRegister: {}
    )
}
