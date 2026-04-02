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

## 다음 작업 예정
- Main 화면 구현
- 고양이 상세/편집 화면
- 체중 기록 그래프
- 투약 알람 스케줄러
- 접종 기록 관리
