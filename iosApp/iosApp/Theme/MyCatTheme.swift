import SwiftUI

// MARK: - 앱 컬러 팔레트
enum MyCatColors {
    // Primary: #FF8C42
    static let primary      = Color(r: 255, g: 140, b: 66)
    // Background: #FFF8F3
    static let background   = Color(r: 255, g: 248, b: 243)
    // Surface: #FFF0E6
    static let surface      = Color(r: 255, g: 240, b: 230)
    // OnPrimary: #FFFFFF
    static let onPrimary    = Color.white
    // Secondary: #A0522D
    static let secondary    = Color(r: 160, g: 82, b: 45)
    // TextMuted: #9E9E9E
    static let textMuted    = Color(r: 158, g: 158, b: 158)
    // Success: #4CAF50
    static let success      = Color(r: 76, g: 175, b: 80)
    // Border: #E0E0E0
    static let border       = Color(r: 224, g: 224, b: 224)
    // OnBackground: ~#1A1A1A
    static let onBackground = Color(r: 26, g: 26, b: 26)
}

private extension Color {
    init(r: Double, g: Double, b: Double, a: Double = 1.0) {
        self.init(red: r / 255, green: g / 255, blue: b / 255, opacity: a)
    }
}

func resolvePhotoPath(_ fileName: String) -> String {
    // 이미 절대 경로면 그대로 반환 (하위 호환)
    if fileName.hasPrefix("/") { return fileName }
    let documents = FileManager.default.urls(
        for: .documentDirectory,
        in: .userDomainMask
    )[0]
    let fullPath = documents.appendingPathComponent(fileName).path
    print("resolvePhotoPath: \(fileName) → \(fullPath)")
      print("파일 존재: \(FileManager.default.fileExists(atPath: fullPath))")
    return documents.appendingPathComponent(fileName).path
}
