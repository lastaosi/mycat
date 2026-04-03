# MyCat 작업 일지

## 2026-04-02

### 초기 세팅 (이전 커밋: `3e062a3`)
- KMP(Kotlin Multiplatform) 프로젝트 세팅
- SQLDelight DB 스키마 설계
  - `cat`, `breed`, `breed_monthly_guide`, `health_checklist`
  - `weight_record`, `medication`, `medication_alarm`, `medication_log`
  - `cat_diary`, `vaccination_record`

---

### Domain 레이어

#### Domain Model 정의
- `Cat` — 고양이 기본 정보 (이름, 생년월, 성별, 품종, 체중, 중성화 여부 등)
- `Breed` — 품종 정보 (한국어/영어명, 원산지, 체중 범위, 기대 수명, 흔한 질환)
- `HealthChecklist` — 월령별 건강 체크리스트 항목 (`VACCINE` / `CHECK` / `SURGERY`)
- `Medication` — 투약 정보 (`ONCE` / `DAILY` / `INTERVAL` / `PERIOD` 타입)
- `MedicationAlarm` — 투약 알람 (시각, 활성화 여부)
- `MedicationLog` — 투약 기록 (복약/건너뜀 여부)
- `WeightRecord` — 체중 기록 (고양이 ID, 체중(g), 기록 시각)
- `CatDiary` — 일기 (제목, 내용, 기분, 사진)
- `VaccinationRecord` — 접종 기록 (체크리스트 연동, 다음 접종일)

#### Repository 인터페이스 정의
- `CatRepository`, `BreedRepository`, `HealthChecklistRepository`
- `WeightRecordRepository`, `MedicationRepository`, `CatDiaryRepository`
- `VaccinationRecordRepository`

---

### Data 레이어

#### Repository 구현체 작성
- `asFlow() + mapToList(Dispatchers.IO)` 패턴으로 Flow 반환
- `withContext(Dispatchers.IO)` 패턴으로 suspend 함수 구현
- SQLDelight DB 타입(Long/Integer) → Domain 모델 타입(Int/Boolean) 변환 mapper 포함
- `Unit` 명시: withContext 블록 마지막이 DB 쿼리 호출인 경우 `Unit` 추가

| 구현체 | 담당 |
|--------|------|
| `CatRepositoryImpl` | 고양이 CRUD, 대표 고양이 설정 |
| `BreedRepositoryImpl` | 품종 조회/검색, 월령 가이드 |
| `HealthChecklistRepositoryImpl` | 월령별 헬스 체크리스트 |
| `WeightRecordRepositoryImpl` | 체중 기록 CRUD |
| `MedicationRepositoryImpl` | 투약 + 알람 + 로그 통합 관리 |
| `CatDiaryRepositoryImpl` | 일기 CRUD |
| `VaccinationRecordRepositoryImpl` | 접종 기록 CRUD |

#### SQLDelight .sq 파일 수정
- `CatDiary.sq`, `Medication.sq`, `MedicationAlarm.sq`, `MedicationLog.sq`, `WeightRecord.sq`, `VaccinationRecord.sq`
- `lastInsertRowId: SELECT last_insert_rowid();` 쿼리 추가

---

### Remote 레이어

#### Gemini API 연동 (`GeminiService`)
- Gemini 2.5 Flash Lite 모델 사용
- 고양이 사진(ByteArray) → Base64 인코딩 → API 요청
- 응답 JSON Regex 파싱 → `BreedRecognitionResult(breedName, confidence, description)`
- 이미지 전송 전 800px 리사이즈 + JPEG 80% 압축 처리 (UI 레이어)
- `expect fun encodeBase64(bytes: ByteArray): String` — Android/iOS 플랫폼별 구현

---

### DI (Koin)

#### `AppModule` 설정
- Repository 바인딩 (인터페이스 → 구현체, `single`)
- `HttpClient`, `GeminiService` 등록
- `CalculateAgeMonthUseCase` 등록 (`factory`)
- `CatTipRepository` 바인딩 추가

---

### Presentation 레이어

#### Navigation
- `NavRoutes` — sealed class 기반 라우트 정의 (`Splash`, `ProfileRegister`, `Main`)
- `AppNavHost` — Compose Navigation 설정, Splash → ProfileRegister / Main 분기

#### SplashScreen
- `SplashViewModel`: DB에 고양이 데이터 존재 여부로 첫 실행 판단
  - 고양이 없음 → `ProfileRegister`로 이동
  - 고양이 있음 → `Main`으로 이동

#### ProfileRegisterScreen (고양이 등록)
- `ProfileRegisterViewModel`: 등록 폼 상태 관리
  - 생년월 자동 포맷팅 (숫자 입력 → `YYYY-MM`)
  - 품종 실시간 검색 (1글자 이상 입력 시 DB 검색, 최대 5건 표시)
  - Gemini 품종 인식 후 DB 품종과 자동 매칭
  - 저장 전 유효성 검사 (사진, 이름, 생년월, 품종 필수)
- `ProfileRegisterScreen` / `ProfileRegisterContent` / `ProfileRegisterContentPreview` 분리
  - `Screen`: ViewModel 주입 + 이벤트 핸들러 위임
  - `Content`: 순수 UI 함수 (카메라/갤러리 런처 포함)
  - `Preview`: `showBackground = true`

---

### Main 화면 구현 + 체중 기록 화면

#### Main 화면
- `MainViewModel`: 대표 고양이 로드 → 월령 기반 오늘의 급여량·체중 범위 표시
  - 다가오는 예방접종 알람 (D-Day 표시, D-3 이내 긴급 처리)
  - 활성 투약 목록 (복용 타입별 레이블)
  - 최근 일기 미리보기 2건
  - 랜덤 고양이 팁 (`CatTipRepository.getRandomTip()`)
- `MainUiState`: `DrawerItem` 열거, `UpcomingAlarm`, `DiaryPreview` 미리보기 모델 포함
- `MainScreen` / `MainScrollContent` / `MainTopBar` / `MainDrawerContent` 분리

#### 체중 기록 화면
- `WeightViewModel`: 고양이 ID 기반 체중 히스토리 + 품종 평균 성장 데이터 로드
  - FAB → 체중 입력 다이얼로그 → `WeightRecord` DB 저장 (kg 입력 → g 변환)
- `WeightUiState`: `WeightTab`(내 고양이 추이 / 품종 평균 성장), `BreedAvgPoint` 포함
- `WeightScreen`: 탭 전환 + 체중 입력 다이얼로그 UI

#### CatTip
- `CatTip` 도메인 모델 + `CatTipRepository` 인터페이스
- `CatTipRepositoryImpl` 구현체
- `CatTip.sq` SQLDelight 스키마 추가

#### 기타
- `Theme.kt`: Material3 앱 테마 설정
- `ImageUtil.kt`: 이미지 리사이즈 유틸리티
- `AppNavHost` 업데이트: Main / Weight 화면 라우트 추가
- `DatabaseModule` 업데이트: CatTip DB 연결

---

### UseCase 도입 리팩토링

#### 신규 UseCase 추가
- `InsertCatUseCase` — `CatRepository.insert()` 래핑
- `SearchBreedUseCase` — 품종 검색 + 결과 개수 제한(maxResults=5) 캡슐화
- `RecognizeBreedUseCase` — Gemini 품종 인식 + DB 매칭 로직 통합
  - `RecognizeBreedResult(geminiRaw, confidence, matchedBreed)` 반환

#### ProfileRegisterViewModel 리팩토링
- `CatRepository` / `GeminiService` / `BreedRepository` 직접 의존 → UseCase 3개로 교체
- 디버그용 `getCount()` 로그, `getAllBreeds()` 필터 코드 제거
- 품종 검색 결과 limit을 `SearchBreedUseCase` 내부로 이전

#### SplashScreen 분리
- `SplashScreen` (ViewModel 주입 + 네비게이션 처리)와 `SplashContent` (순수 UI)로 분리
- `SplashContentPreview` 추가

#### AppModule 업데이트
- `CatTipRepository` 바인딩 추가
- `InsertCatUseCase`, `RecognizeBreedUseCase`, `SearchBreedUseCase` 등록

---

---

## 2026-04-03

### Presentation 레이어 — 신규 화면 구현

#### MedicationScreen (투약 관리)
- `MedicationViewModel`: 대표 고양이 기준 활성/비활성 투약 목록 로드
  - 투약 등록 (이름, 타입, 시작일, 종료일, 메모)
  - 알람 설정 (시각 추가/삭제, 알람별 AlarmManager 예약/취소)
  - 복약 기록 (`TAKEN` / `SKIPPED`) 저장
- `MedicationUiState`: 활성·비활성 투약, 선택 투약, 다이얼로그 표시 상태 포함

#### VaccinationScreen (접종 기록)
- `VaccinationViewModel`: 월령별 체크리스트 + 접종 기록 통합 조회
  - 접종 완료 처리 (완료일, 다음 접종일 입력)
  - 미접종/접종완료 항목 분류 표시
- `VaccinationUiState`: 체크리스트 + 접종 기록 매핑 상태 포함

#### DiaryScreen (일기)
- `DiaryViewModel`: 대표 고양이 일기 목록 + 작성/삭제
  - 기분 선택 (`HAPPY` / `NORMAL` / `SAD` / `SICK` / `ANGRY`)
  - 사진 첨부 (갤러리 선택 → URI 저장)
- `DiaryUiState`: 일기 목록, 작성 폼 상태 포함

#### HealthCheckScreen (건강 체크리스트)
- `HealthCheckViewModel`: 고양이 현재 월령 기준 체크리스트 항목 로드
  - 체크 완료 처리 (완료일 DB 저장)
  - 월령 범위별 그룹 표시
- `HealthCheckUiState`: 체크리스트 항목 + 카테고리별 그룹 상태 포함

#### NearbyVetScreen (근처 동물병원)
- `NearbyVetViewModel`: GPS 현재 위치 → Google Places API `SearchNearby` 호출
  - 동물병원 목록 (이름, 주소, 영업 여부, 평점) 표시
  - 위치 권한 요청 / 로딩 / 오류 상태 처리
- Google Maps Compose + FusedLocationProvider + Places SDK 연동

---

### 알람 / 백그라운드 스케줄러

#### AlarmManager 기반 정확 알람
- `MedicationAlarmScheduler`: 투약 알람 예약 (`setExactAndAllowWhileIdle`) / 취소
- `AlarmReceiver`: BroadcastReceiver — 알람 수신 후 `NotificationHelper`로 푸시 알림 발송
  - `ACTION_MEDICATION` 액션으로 투약 알림 처리
  - 매일 반복 알람의 경우 다음 날 동일 시각 재예약

#### WorkManager 기반 리마인더
- `WorkManagerScheduler`: 투약·접종 리마인더 워커 등록 (지연 실행)
- `MedicationReminderWorker`: 활성 투약 중 미복약 항목 확인 → 알림 발송
- `VaccinationReminderWorker`: D-7 이내 접종 예정 항목 확인 → 알림 발송

#### `NotificationHelper`
- 투약 알림 채널 (`medication_alarm`) / 리마인더 채널 (`reminder`) 생성
- Android 13+ 알림 권한 요청 흐름 포함

---

### 유틸리티

- `DateVisualTransformation`: 날짜 입력 자동 포맷 (`YYYYMMDD` → `YYYY-MM-DD`)
- `TimeVisualTransformation`: 시각 입력 자동 포맷 (`HHmm` → `HH:mm`)

---

### Main / Navigation 업데이트

- `MainDrawerContent` 개편: Medication / Vaccination / Diary / HealthCheck / NearbyVet 화면으로 네비게이션 연결
- `MainViewModel` / `MainUiState` 업데이트: 드로어 상태 통합
- `AppNavHost`: 5개 신규 화면 라우트 추가
- `AndroidManifest.xml`: AlarmReceiver 등록, 정확한 알람 권한(`SCHEDULE_EXACT_ALARM`), 위치 권한 추가
- `build.gradle.kts` / `libs.versions.toml`: Google Maps Compose, Places SDK, WorkManager 의존성 추가

---

## 다음 작업 예정
- 고양이 상세/편집 화면
- 체중 기록 그래프
- 투약 복약 기록 달력 뷰
- 일기 상세/편집 화면
