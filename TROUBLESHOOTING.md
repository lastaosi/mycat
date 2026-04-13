# MyCat KMP 트러블슈팅 가이드

> Kotlin Multiplatform (Android / iOS) 개발 중 발생한 이슈 및 해결 방법  
> 작성일: 2026.04

---

## 목차

1. [빌드 / 환경](#1-빌드--환경)
2. [Koin DI](#2-koin-di)
3. [SQLDelight / DB](#3-sqldelight--db)
4. [카메라 / 사진](#4-카메라--사진)
5. [Compose UI](#5-compose-ui)
6. [알림 / AlarmManager](#6-알림--alarmmanager)
7. [아키텍처 / 설계](#7-아키텍처--설계)
8. [iOS KMP 연동](#8-ios-kmp-연동)
9. [보안](#9-보안)
10. [Gemini 품종 매칭 개선](#10-gemini-품종-매칭-개선)

---

## 1. 빌드 / 환경

### 1-1. Gradle android.useAndroidX 누락

- **문제:** iosArm64 빌드 시 AndroidX 의존성 감지 오류
- **해결:** `gradle.properties`에 추가

```properties
android.useAndroidX=true
```

---

### 1-2. Java / Gradle 버전 호환성

- **문제:** `keytool -list -v` 실행 시 `IllegalFormatConversionException` (Java 25 버그)
- **해결:** `./gradlew signingReport` 로 대체하거나 Java 21 고정

```properties
# gradle.properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
```

---

### 1-3. Vico 차트 버전 API 불일치

- **문제:** Vico 2.x API로 작성했는데 1.15.0과 맞지 않아 `rememberBottomAxis` 등 미참조
- **해결:** 1.x API로 전면 교체

```kotlin
// 1.x API 사용
ChartEntryModelProducer()
lineChart()
```

---

### 1-4. gradle.properties vs local.properties

- **문제:** `MAPS_API_KEY`를 `local.properties`에 넣었더니 `project.findProperty()`로 못 읽음
- **해결:** `gradle.properties`로 이동

```properties
# gradle.properties
MAPS_API_KEY=your_key_here
GEMINI_API_KEY=your_key_here
```

```kotlin
// build.gradle.kts
project.findProperty("MAPS_API_KEY") as String
```

---

## 2. Koin DI

### 2-1. koinViewModel import 경로

- **문제:** `org.koin.androidx.compose.koinViewModel` 미참조
- **해결:** KMP는 다른 패키지 사용

```kotlin
// KMP에서는 이것 사용
import org.koin.compose.viewmodel.koinViewModel
```

---

### 2-2. Context 주입 실패

- **문제:** ViewModel에 Context 직접 주입 시 Koin 에러 + 메모리 릭 위험
- **해결:** `AndroidViewModel` + `androidApplication()` 사용

```kotlin
// DatabaseModule.kt
viewModel { NearbyVetViewModel(androidApplication()) }

// ViewModel
class NearbyVetViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext
}
```

---

### 2-3. CalculateAgeMonthUseCase 중복 등록

- **문제:** `AppModule`에 두 번 등록되어 충돌
- **해결:** 하나 제거

---

### 2-4. Places.initialize() 초기화 순서

- **문제:** Koin 초기화 전에 Places 초기화 필요
- **해결:** `MyCatApplication.onCreate()`에서 Koin 시작 전에 배치

```kotlin
override fun onCreate() {
    super.onCreate()
    Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY) // 먼저
    startKoin { ... } // 나중에
    NotificationHelper.createChannels(this)
}
```

---

## 3. SQLDelight / DB

### 3-1. iOS SQLite DB 초기화 경로 문제

- **문제:** iOS에서 `NativeSqliteDriver`가 번들 복사 DB를 무시하고 새 빈 DB 생성
- **원인 1:** 복사 경로 불일치 (`Documents` → 드라이버는 `Library/Application Support/databases/` 사용)
- **원인 2:** `PRAGMA user_version = 0`인데 Schema version = 1이라 새 DB로 판단
- **해결:** 복사 경로를 `databases` 폴더로 맞추고 DB Browser에서 `user_version = 1` 설정

```kotlin
// DatabaseDriverFactory.ios.kt
val dbDir = "$appSupport/databases"
// DB Browser: PRAGMA user_version = 1 설정 후 번들 파일 교체
```

---

### 3-2. cat_tip 테이블 미생성

- **문제:** `CatTip.sq` 추가 후 DB 파일에 테이블 없음
- **해결:** Python 스크립트로 직접 INSERT 후 `assets/mycat.db` 교체

---

### 3-3. health_checklist 데이터 부족

- **문제:** 기존 25개 데이터로 화면 빈약
- **해결:** 50개로 확장, 타입 변환 후 재삽입

---

## 4. 카메라 / 사진

### 4-1. 사진 90도 회전 (Android)

- **문제:** `BitmapFactory.decodeStream`이 EXIF 방향 정보 무시
- **해결:** `ExifInterface`로 방향 읽어서 `Matrix.postRotate()` 적용

```kotlin
val exif = ExifInterface(inputStream)
val rotation = exif.getAttributeInt(
    ExifInterface.TAG_ORIENTATION,
    ExifInterface.ORIENTATION_NORMAL
)
val matrix = Matrix()
matrix.postRotate(rotation.toFloat())
Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
```

---

### 4-2. 사진 경로 영구 저장 (Android)

- **문제:** `externalCacheDir`은 시스템이 삭제할 수 있는 임시 폴더
- **해결:** `context.filesDir`로 영구 복사

---

### 4-3. iOS 사진 경로 재가동 시 소실

- **문제:** 앱 재가동 시 절대 경로의 UUID가 바뀌어 사진 로드 실패
- **해결:** 파일명만 DB에 저장하고 불러올 때 현재 경로로 복원

```swift
// 저장 시 파일명만 반환
return fileName  // url.path 대신

// 불러올 때
func resolvePhotoPath(_ fileName: String) -> String {
    if fileName.hasPrefix("/") { return fileName } // 하위 호환
    let docs = FileManager.default.urls(
        for: .documentDirectory,
        in: .userDomainMask
    )[0]
    return docs.appendingPathComponent(fileName).path
}
```

---

## 5. Compose UI

### 5-1. tabIndicatorOffset 미참조

- **해결:** import 추가

```kotlin
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
```

---

### 5-2. TipBannerCard 미표시

- **문제:** `MainScrollContent`에서 중복 호출 + `refreshTip()` 초기 미호출
- **해결:** 중복 제거 + `loadData()` 완료 후 `refreshTip()` 호출

---

### 5-3. DatePicker 미표시

- **문제:** `readOnly` 필드에 `clickable`이 동작 안 함
- **해결:** `Box`로 감싸고 `enabled = false` + `Box clickable` 조합으로 처리

```kotlin
Box {
    OutlinedTextField(enabled = false, ...)
    Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
}
```

---

### 5-4. AlertDialog 내부 UI 배치 오류

- **문제:** 알람 시간 입력 UI가 `AlertDialog` 닫는 괄호 밖에 위치해서 렌더링 안 됨
- **해결:** `text = { }` 블록 안으로 이동

---

## 6. 알림 / AlarmManager

### 6-1. 알림 미발송

- **문제:** `BroadcastReceiver`의 context가 앱 컨텍스트가 아님
- **해결:** `context → context.applicationContext`로 변경

```kotlin
NotificationHelper.showMedicationNotification(
    context = context.applicationContext
)
```

---

### 6-2. 알림 채널 미생성

- **문제:** `NotificationHelper.createChannels()` 미호출로 채널 없음
- **해결:** `MyCatApplication.onCreate()`에 추가

```kotlin
NotificationHelper.createChannels(this)
```

---

### 6-3. 알람 시간 입력 안 됨

- **문제:** 알람 시간 입력 UI가 다이얼로그 밖에 있어서 `alarmTimes`가 빈값
- **해결:** `TimeVisualTransformation` 적용 + 다이얼로그 `text` 블록 안으로 이동

---

## 7. 아키텍처 / 설계

### 7-1. 첫 고양이 등록 시 대표 설정 누락

- **문제:** 첫 등록 시 `isRepresentative = false`라 `getRepresentativeCat()` null 반환
- **해결:** `catRepository.getCount() == 0L`이면 자동으로 대표 설정

```kotlin
val isRepresentative = catRepository.getCount() == 0L
```

---

### 7-2. BreedAvgPoint 레이어 위치

- **문제:** `presentation` 레이어에 있던 `BreedAvgPoint`를 Domain UseCase에서 참조 불가
- **해결:** `domain/model/BreedAvgPoint.kt`로 이동

---

### 7-3. Navigation 라우트 미등록

- **문제:** `weight_graph/1` 등 라우트가 NavGraph에 없어서 런타임 크래시
- **해결:** `AppNavHost`에 각 화면 `composable` 등록 확인

---

### 7-4. DiaryViewModel 함수명 오타

- **문제:** `onAction`이 `DairyAction`으로 오타
- **해결:** `DiaryAction`으로 수정

---

## 8. iOS KMP 연동

### 8-1. Kotlin 타입 → Swift 타입 변환

```swift
// KotlinLong → Int64
self?.catId = catId.int64Value

// KotlinDouble → Double
let value = kotlinDouble.doubleValue

// KotlinInt → Int
breedId: KotlinInt(value: Int32(id))

// ByteArray → KotlinByteArray
let bytes = KotlinByteArray(size: Int32(data.count))
for (i, byte) in data.enumerated() {
    bytes.set(index: Int32(i), value: Int8(bitPattern: byte))
}
```

---

### 8-2. iOS 드로어에서 dismiss() 미동작

- **문제:** `selectedItem` 스위치 방식이라 `NavigationStack dismiss()`가 무효
- **해결:** `DrawerState`에 `goHome()` 추가 후 `@EnvironmentObject`로 접근

```swift
class DrawerState: ObservableObject {
    @Published var selectedItem: DrawerMenuItem = .home
    func goHome() { selectedItem = .home }
}

// 각 화면에서
@EnvironmentObject var drawerState: DrawerState
onBack: { drawerState.goHome() }
```

---

### 8-3. Swift AxisValueLabel 문법 오류

- **문제:** `AxisMarks` 클로저 내부에서 `value` 파라미터 사용 불가
- **해결:** `if let` 조건을 `AxisValueLabel` 바깥으로 이동

```swift
// 올바른 방법
AxisMarks { value in
    if let date = value.as(Date.self) {
        AxisValueLabel { Text(...) }
    }
    AxisGridLine()
}
```

---

### 8-4. Gemini API Key iOS 설정

```
// Config.xcconfig
GEMINI_API_KEY = your_key

// Info.plist
<key>GEMINI_API_KEY</key>
<string>$(GEMINI_API_KEY)</string>
```

```kotlin
// ApiKeyProvider.ios.kt
NSBundle.mainBundle.objectForInfoDictionaryKey("GEMINI_API_KEY") as? String
```

---

### 8-5. 품종 인식 결과 iOS에서 breedId null

- **문제:** `ProfileKotlinViewModel` `onResult` 콜백에 `breedId`가 빠져 있음
- **해결:** 콜백 시그니처에 `breedId: Int?` 추가

```kotlin
// 변경 전
onResult: (breedName: String, confidence: Double) -> Unit

// 변경 후
onResult: (breedName: String, breedId: Int?, confidence: Double) -> Unit
```

---

## 9. 보안

### 9-1. API 키 .gitignore 필수 등록

```
gradle.properties
local.properties
*.keystore
iosApp/Configuration/Config.xcconfig
google-services.json
```

---

### 9-2. Config.xcconfig GitHub 노출 시 조치

- **문제:** `Config.xcconfig`가 `.gitignore`에 등록 안 돼 GitHub에 노출
- **해결:** 즉시 키 재발급 + `.gitignore` 등록 + git 추적 제거

```bash
git rm --cached iosApp/Configuration/Config.xcconfig
echo 'iosApp/Configuration/Config.xcconfig' >> .gitignore
git commit -m "Remove Config.xcconfig from tracking"
git push
```

---

## 10. Gemini 품종 매칭 개선

### 10-1. Gemini 반환 품종명과 DB 불일치

- **문제:** "브리티시 쇼트헤어" 등 품종명이 DB와 정확히 일치하지 않으면 매칭 실패
- **해결:** 3단계 fuzzy 매칭 로직 구현

```kotlin
private suspend fun findBestMatch(breedName: String): Breed? {
    // 1단계: 전체 이름으로 검색
    val fullMatch = breedRepository.searchBreeds(breedName).first().firstOrNull()
    if (fullMatch != null) return fullMatch

    // 2단계: 단어별 분리 후 순차 검색 (긴 단어 우선)
    val words = breedName.split(" ")
        .filter { it.length >= 2 }
        .sortedByDescending { it.length }
    for (word in words) {
        val match = breedRepository.searchBreeds(word).first().firstOrNull()
        if (match != null) return match
    }

    // 3단계: 앞 2글자로 검색
    return breedRepository.searchBreeds(breedName.take(2)).first().firstOrNull()
}
```

---

*MyCat Project — 이정훈 © 2026*
