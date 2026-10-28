# 🍚 MoA-BoB (모아밥)

> **AI 기반 식재료 관리 및 활용 서비스**
> 냉장고 속 식재료를 AI로 인식·관리하고, 유통기한과 신선도를 알려주며, 남은 재료로 만들 수 있는 레시피까지 추천하는 **배리어프리 Android 앱**입니다.


---

## 📌 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [접근성 (배리어프리)](#-접근성-배리어프리)
- [시스템 구성](#-시스템-구성)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [Firebase 데이터 구조](#-firebase-데이터-구조)
- [AI 신선도 판별 모델](#-ai-신선도-판별-모델)
- [시작하기](#-시작하기)

---

## 💡 프로젝트 소개

냉장고에 무엇이 있는지, 언제까지 먹어야 하는지 잊어버려 버려지는 음식이 많습니다. 특히 시각·고령 사용자는 작은 글씨의 유통기한을 확인하기 어렵습니다.

**모아밥**은 카메라와 AI로 식재료를 자동 등록하고, 유통기한·신선도·알레르기 정보를 **화면, 음성(TTS), 진동(햅틱)** 으로 알려 주어 누구나 쉽게 식재료를 관리하고 음식물 쓰레기를 줄일 수 있도록 돕습니다.

---

## ✨ 주요 기능

### 📷 AI 스캔 (입고 · 출고 · 신선도 · 알레르기)
| 모드 | 설명 |
|---|---|
| **입고 (IN)** | 식재료를 카메라에 비추면 품목을 인식해 재고에 등록 |
| **출고 (OUT)** | 꺼낸 식재료를 인식해 재고에서 차감 |
| **신선도 (FRESH)** | AI 모델로 신선도 점수(0~100)와 상태(신선 / 주의 / 부패)를 판별 |
| **알레르기 (ALLERGEN)** | 등록된 알레르기 식품이 감지되면 강한 진동과 경고 표시 |

- **외부 스캔 장치(웹캠 + AI 모델)** 와 Firebase로 실시간 연동 — 앱이 스캔 모드를 지정하면 장치가 결과를 올려 줍니다.
- **스마트폰 카메라 스캔** 도 지원 (CameraX + ML Kit Image Labeling)
- **유통기한 OCR**: Google Cloud Vision / ML Kit 한국어 텍스트 인식으로 포장지의 날짜(`2027.10.15`, `27.10.15`, `20271015` 등)를 자동 추출
- 유통기한이 없으면 식약처 기준 품목별 기본 보관 기간으로 자동 설정

### 🧊 재고 관리
- 카테고리(채소·과일·육류·해산물·유제품·곡물·음료·소스·가공식품)별 색상 구분
- 유통기한 상태 표시: `D-n` / `내일까지` / `오늘까지` / `n일 경과`
- 상세 시트에서 유통기한·수량 수정, 삭제
- **🥄 짜투리 표시**: 일부만 사용하고 남은 식재료를 표시해 먼저 소진하도록 유도
- **맞춤 보관 가이드**: 품목별 보관 방법 안내

### 🍳 레시피 추천
- **식품안전나라 조리식품 레시피 API(COOKRCP01)** 연동
- 보유 재료 · 유통기한 임박 재료 우선 추천
- 레시피 상세: 재료, 조리 과정, 영양 정보(열량·탄수화물·단백질·지방·나트륨), 음식 사진

### 🔔 알림
- 유통기한 D-Day / D-2 / D-5 / D-7 알림 (항목별 on/off)
- 신선도 저하, 알레르기 식품 경고, 레시피 팁 알림

### 🎙️ 음성 명령
자연어 음성으로 앱을 조작할 수 있습니다.

| 예시 발화 | 동작 |
|---|---|
| "우유 유통기한 언제야?" | 유통기한 조회 |
| "냉장고에 우유 있어?" | 재고 여부 확인 |
| "유통기한 하루 남은 거 있어?" | 임박 식품 조회 |
| "우유 2개 삭제해줘" → "응" | 재고 삭제 (확인 후) |
| "재고 추가 모드 열어줘" | 음성 추가 모드 진입 |
| "사과 1개 우유 1개 12월 5일" | 여러 품목 한 번에 추가 |
| "끝" / "이전으로" | 모드 종료 · 닫기 |

- 동의어·별칭, 한글 수량 표현("두 개", "셋") 인식

### 👨‍👩‍👧 공동 냉장고 (가구 공유)
- 가구 생성 후 **6자리 초대 코드** 로 가족·룸메이트 초대
- 구성원 목록 및 공유 재고 실시간 동기화

### 🛒 싱싱마켓 연동
- 앱 내 WebView 마켓에서 장보기
- 주문한 상품이 도착하면 **자동으로 재고에 입고** (배송 도착 알림 시트)

---

## ♿ 접근성 (배리어프리)

모아밥은 시각 장애인, 고령자, 색각 이상 사용자 모두를 고려해 설계했습니다.

| 기능 | 설명 |
|---|---|
| **TTS 음성 안내** | Google Cloud Text-to-Speech (오프라인 시 Android 기본 TTS로 폴백) |
| **STT 음성 명령** | Android SpeechRecognizer 기반 음성 조작 |
| **햅틱 피드백** | 알레르기 경고(강하게 3회) · 유통기한 D-1(짧게 2회) · 신선도 경고(길게 1회) |
| **글자 크기** | 기본 / 크게 / 최대 3단계 |
| **색각 보정 모드** | 색상만으로 정보를 구분하지 않도록 보정 |
| **접근성 모드 · 간소화 홈** | 핵심 정보만 크게 보여 주는 단순 화면 |

---

## 🏗️ 시스템 구성

```
┌──────────────────────┐        ┌───────────────────────────┐
│   스캔 장치 (웹캠)     │        │      Android 앱 (잇다)      │
│  EfficientNet-B0     │        │  Jetpack Compose + MVVM   │
│  품목·신선도 판별      │        │                           │
└─────────┬────────────┘        └───────┬───────────────────┘
          │  scan_results / inventory     │  current_mode / confirm_scan
          ▼                               ▼
     ┌──────────────────────────────────────────┐
     │       Firebase Realtime Database          │
     └──────────────────────────────────────────┘
                         ▲
                         │ pendingDelivery
                ┌────────┴────────┐
                │  싱싱마켓 (Web)   │
                └─────────────────┘

외부 API: 식품안전나라 레시피 API · Google Cloud Vision (OCR) · Google Cloud TTS
```

1. 앱에서 스캔 모드를 선택하면 `current_mode`가 갱신됩니다.
2. 스캔 장치가 식재료를 인식해 `scan_results`, `scan_status`에 결과를 기록합니다.
3. 앱은 리스너로 결과를 실시간 수신하고, 사용자가 확인하면 `confirm_scan`으로 재고에 반영합니다.

---

## 🛠️ 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM, Kotlin Coroutines, StateFlow |
| Backend | Firebase Realtime Database |
| Camera / Vision | CameraX, ML Kit (Image Labeling, Text Recognition – Korean), Google Cloud Vision API |
| On-device ML | TensorFlow Lite Task Vision |
| Voice | Android SpeechRecognizer (STT), Google Cloud TTS |
| Image | Coil 3 |
| Web | AndroidX WebKit (`WebViewAssetLoader`) |
| Open API | 식품안전나라 조리식품 레시피 DB (COOKRCP01) |
| AI Model | EfficientNet-B0 멀티태스크 신선도 분류 |

---

## 📁 프로젝트 구조

```
app/src/main/
├── assets/
│   └── shop.html                  # 싱싱마켓 웹 페이지
└── java/com/eat_da/app/
    ├── MainActivity.kt
    ├── data/
    │   ├── api/RecipeApiService.kt    # 식품안전나라 레시피 API
    │   ├── model/Models.kt            # FoodItem, Recipe, ScanResult 등
    │   ├── model/MockData.kt
    │   └── FoodCategoryMapper.kt      # 식품명 → 카테고리 자동 분류
    ├── viewmodel/
    │   └── AppViewModel.kt            # 앱 상태 · Firebase 연동 · 음성 명령 처리
    ├── ui/
    │   ├── EatdaApp.kt                # 루트 화면 · 내비게이션
    │   ├── screens/                   # 홈, 재고, 알림, 레시피, 레시피 상세,
    │   │                              # 마이페이지, 설정, 공동 냉장고
    │   ├── components/                # 스캔 시트/뷰, 재고 상세, 음성 오버레이,
    │   │                              # 알레르기 경고, 마켓 도착 시트 등
    │   ├── shop/ShopWebViewActivity.kt
    │   └── theme/                     # 색상 · 타이포 · 테마
    └── util/
        ├── CameraLabelerService.kt    # ML Kit 이미지 라벨링
        ├── VisionService.kt           # Cloud Vision 유통기한 OCR
        ├── TessaractOcrService.kt     # ML Kit 유통기한 OCR
        ├── SttService.kt / TtsService.kt
        ├── VoiceCommandParser.kt      # 음성 → 앱 명령 파싱
        ├── HapticManager.kt
        └── HouseholdManager.kt        # 공동 냉장고 (초대 코드)
```

---

## 🗄️ Firebase 데이터 구조

```
(root)
├── current_mode              # 스캔 장치 모드: IN / OUT / fresh_scan / ALLERGEN / IDLE
├── MoA-BoB/
│   ├── foodInventory/<id>    # name, category, expiry, qty, location,
│   │                         # freshness, freshnessClass, freshnessConf,
│   │                         # isAllergen, hasRemnant, addedDays
│   └── pendingDelivery       # 싱싱마켓 배송 도착 대기 상품
├── eatda/
│   ├── scan_results          # 스캔 장치가 올린 인식 결과
│   ├── scan_status           # idle / scanning / ...
│   ├── confirm_scan          # 사용자가 확정한 스캔 모드
│   ├── inventory             # market / webcam 입고 이벤트
│   └── allergens             # 사용자 알레르기 목록
├── households/<householdId>  # name, ownerId, inviteCode, members/, inventory/
└── inviteCodes/<code>        # → householdId
```

---

## 🧠 AI 신선도 판별 모델

**EfficientNet-B0 기반 멀티태스크 분류 모델** 로 식재료의 품목과 신선도를 동시에 판별합니다.

| 항목 | 결과 |
|---|---|
| Loss | 0.2758 → **0.0326** (꾸준히 감소) |
| Validation Accuracy | 97.7% → **98.5%** (4 epoch에서 수렴) |
| Overfitting | 없음 (5~10 epoch 구간 안정) |

앱에서의 신선도 판단 기준:

| 신선도 점수 | 상태 |
|---|---|
| 80 이상 | 🟢 신선 (FRESH) |
| 60 ~ 79 | 🟡 주의 (ATTENTION) |
| 60 미만 | 🔴 부패 (ROTTEN) |

---

## 🚀 시작하기

### 요구 사항
- Android Studio (AGP 9.x 지원 버전)
- JDK 17 이상
- Firebase 프로젝트 (Realtime Database 사용)

### 1. 저장소 클론
```bash
git clone https://github.com/yedamimi/moa-bob.git
```

### 2. Firebase 설정
Firebase 콘솔에서 Android 앱(`com.eatda.app`)을 등록하고 발급받은 `google-services.json`을 `app/` 폴더에 넣습니다.
Firebase SDK는 이 파일로 초기화되므로 DB URL을 코드에 직접 넣지 않습니다.

### 3. API 키 설정
앱은 아래 `BuildConfig` 값을 사용합니다. `local.properties` 등에 키를 저장하고 `app/build.gradle.kts`의 `buildConfigField`로 주입하세요. **키는 저장소에 커밋하지 마세요.**

| BuildConfig 필드 | 용도 |
|---|---|
| `EATDA_API_KEY` | 식품안전나라 Open API 인증키 |
| `EATDA_VISION_KEY` | Google Cloud Vision API 키 (유통기한 OCR) |
| `EATDA_TTS_KEY` | Google Cloud Text-to-Speech API 키 |

### 4. 빌드 및 실행
Android Studio에서 프로젝트를 열고 Gradle Sync 후 실행합니다.

### 필요 권한
- `INTERNET` — Firebase, 외부 API
- `RECORD_AUDIO` — 음성 명령
- `VIBRATE` — 햅틱 피드백

