package com.eatda.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.eatda.app.data.allergenMatches
import com.eatda.app.data.defaultAllergens
import com.eatda.app.data.model.*
import com.eatda.app.util.Household
import com.eatda.app.util.HouseholdManager
import com.eatda.app.util.HouseholdMember
import com.eatda.app.util.VoiceCommand
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.lifecycle.viewModelScope
import com.eatda.app.data.api.RecipeApiService
import kotlinx.coroutines.launch
import com.eatda.app.data.FoodCategoryMapper
import com.eatda.app.data.ReceiptEntry
import com.eatda.app.util.parseQty

// ── 열거형 ────────────────────────────────────────────────────────────────────

enum class Screen {
    HOME, INVENTORY, NOTIFICATIONS, RECIPES, RECIPE_DETAIL, MYPAGE, SETTINGS, HOUSEHOLD
}

enum class FontScale(val label: String) {
    NORMAL("기본"), LARGE("크게"), XLARGE("최대")
}

enum class HapticEvent { ALLERGEN, EXPIRY, FRESHNESS }

// ── 설정 ──────────────────────────────────────────────────────────────────────

data class AppSettings(
    val a11yMode: Boolean = false,
    val ttsEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val colorBlindMode: Boolean = false,
    val fontScale: FontScale = FontScale.NORMAL,
    val simplifiedHome: Boolean = false,
    val notifAllergen: Boolean = true,
    val notifExpiryDDay: Boolean = true,
    val notifExpiryD2: Boolean = true,
    val notifExpiryD5: Boolean = true,
    val notifExpiryD7: Boolean = true,
    val notifRecipe: Boolean = true,
)

// ── 식약처 로컬 기본값 (expiry 없을 때 사용) ──────────────────────────────────
private val DEFAULT_SHELF_DAYS: Map<String, Long> = mapOf(
    "사과" to 30L, "바나나" to 7L, "오렌지" to 21L, "포도" to 7L, "딸기" to 5L,
    "수박" to 7L, "참외" to 7L, "복숭아" to 5L, "배" to 30L, "귤" to 14L,
    "당근" to 21L, "오이" to 7L, "감자" to 30L, "고구마" to 30L, "양파" to 30L,
    "마늘" to 30L, "브로콜리" to 7L, "시금치" to 3L, "상추" to 3L, "토마토" to 7L,
    "계란" to 30L, "우유" to 7L, "두부" to 5L, "돼지고기" to 3L, "닭고기" to 2L,
    "소고기" to 3L, "생선" to 2L,
)

// 이 시간 안에 연달아 들어온 스캔 결과는 한 문장으로 읽음
private const val SCAN_TTS_DEBOUNCE_MS = 800L

// ── 상태 ──────────────────────────────────────────────────────────────────────

data class AppState(
    val screen: Screen = Screen.HOME,
    val scanSheetOpen: Boolean = false,
    val activeScanMode: ScanMode? = null,
    val phoneScanMode: ScanMode? = null,
    val phoneScanResults: List<ScanResult> = emptyList(),
    val openItem: FoodItem? = null,
    val voiceOverlayOpen: Boolean = false,
    val allergens: List<Allergen> = defaultAllergens,
    val toast: String? = null,
    val settings: AppSettings = AppSettings(),
    val inventory: List<FoodItem> = emptyList(),        // stocks + webcam 병합
    val webcamItems: List<FoodItem> = emptyList(),      // 웹캠 인식 아이템 (소비기한 포함)
    val notifications: List<AppNotification> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val recipeFocusIngredient: String? = null,
    val inventoryGroupName: String? = null,          // 재고 화면에서 보고 있는 식재료 그룹 (null이면 전체 목록)
    val streamUrl: String? = null,                   // scan.py 웹캠 MJPEG 스트림 주소 (스크립트 꺼지면 null)
    val scanHint: String? = null,                    // scan.py가 보내는 한글 안내 문구 (스캔 화면 상태 바)
    val freshnessChecks: Map<String, FreshnessCheck> = emptyMap(), // 식재료 이름별 최근 신선도 확인 기록
    val selectedRecipe: Recipe? = null,
    val scanResults: List<ScanResult> = emptyList(),
    val scanPhase: String = "idle",
    val firebaseConnected: Boolean = false,
    val pendingHaptic: HapticEvent? = null,
    val pendingTts: String? = null,
    val lastVoiceResponse: String? = null,
    val marketArrivals: List<FoodItem> = emptyList(),
    val pendingDeleteItem: FoodItem? = null,
    val pendingDeleteQty: Int? = null,
    val household: Household? = null,
    val householdMembers: List<HouseholdMember> = emptyList(),
    val myUserId: String = "",
    // ── 음성 재고 추가 모드 ──────────────────────────────────────────────────
    val isAddMode: Boolean = false,           // "재고 추가 모드 열어줘" 로 진입
    val addedItems: List<FoodItem> = emptyList(), // 이번 세션에 추가한 항목 (오버레이 표시용)
    // ── 영수증 스캔 ─────────────────────────────────────────────────────────
    val receiptScanOpen: Boolean = false,
)

// ── qty 파싱 헬퍼 (숫자/문자열 둘 다 대응) ─────────────────────────────────────

// Firebase qty 필드가 숫자(3)든 문자열("3개")이든 정수만 뽑아냄
// "1/2통"처럼 정수가 아닌 문자열은 default
private fun DataSnapshot.qtyNumber(default: Int = 0): Int =
    when (val v = child("qty").value) {
        is Number -> v.toInt()
        is String -> parseQty(v)?.amount ?: default
        else      -> default
    }

// "3개" 형태의 표시용 문자열 생성. unit 필드가 있으면 우선 사용
private fun DataSnapshot.qtyText(defaultUnit: String = "개"): String =
    formatQty(
        value = child("qty").value,
        unitField = child("unit").getValue(String::class.java),
        defaultUnit = defaultUnit,
    )

// qty 값(숫자 또는 문자열)을 "3개" 형태로 변환
// 정수가 아닌 문자열("1/2통")은 그대로 표시하고, 숫자가 없으면 1로 간주
private fun formatQty(value: Any?, unitField: String?, defaultUnit: String = "개"): String {
    val unit = unitField?.takeIf { it.isNotBlank() } ?: defaultUnit
    return when (value) {
        is Number -> "${value.toInt()}$unit"
        is String -> {
            val parsed = parseQty(value, defaultUnit = unit)
            when {
                parsed != null              -> "${parsed.amount}${parsed.unit}"
                value.none { it.isDigit() } -> "1$unit"
                else                        -> value.trim()
            }
        }
        else -> "1$unit"
    }
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val rootDb = Firebase.database.reference
    private val db     = Firebase.database.reference.child("eatda")
    private val ctx    get() = getApplication<Application>()

    private var inventoryListener:         ValueEventListener? = null
    private var scanResultsListener:       ValueEventListener? = null
    private var scanStatusListener:        ValueEventListener? = null
    private var arrivalsListener:          ChildEventListener? = null
    private var pendingDeliveryListener:   ValueEventListener? = null
    private var householdMemberListener:   ValueEventListener? = null
    private var streamUrlListener:         ValueEventListener? = null
    private var scanHintListener:          ValueEventListener? = null
    private var allergensListener:         ValueEventListener? = null
    private var freshnessChecksListener:   ValueEventListener? = null
    private var allergenHapticFired = false

    // 유통기한 촬영으로 소비기한을 옮긴 스캔 결과 → 현재 재고 키 (다시 촬영할 때 사용)
    private val ocrItemIds = mutableMapOf<String, String>()

    // 스캔 결과 음성 안내 묶음 (announceScanResults)
    private val pendingScanNames = mutableListOf<String>()
    private var scanTtsJob: kotlinx.coroutines.Job? = null

    private val listenerStartTime = System.currentTimeMillis()

    init {
        val userId = HouseholdManager.getOrCreateUserId(ctx)
        _state.update { it.copy(myUserId = userId) }

        HouseholdManager.getSavedHouseholdId(ctx)?.let { loadHousehold(it) }

        listenToInventory()
        listenToScanResults()
        listenToScanStatus()
        listenToArrivals()
        listenToPendingDelivery()
        listenToStreamUrl()
        listenToScanHint()
        listenToAllergens()
        listenToFreshnessChecks()
        loadRecipes()
    }

    // ── 레시피 로드 ───────────────────────────────────────────────────────────

    private fun loadRecipes() {
        viewModelScope.launch {
            val recipes = RecipeApiService.fetchRecipes()
            _state.update { it.copy(recipes = recipes) }
        }
    }

    // ── Firebase 리스너 ───────────────────────────────────────────────────────

    private fun listenToInventory() {
        inventoryListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val dbItems = snapshot.children.mapNotNull { child ->
                    runCatching {
                        val name = child.child("name")
                            .getValue(String::class.java)
                            ?: return@runCatching null

                        val catStr = child.child("category")
                            .getValue(String::class.java) ?: "GRAIN"

                        val expiryStr = child.child("expiry")
                            .getValue(String::class.java)
                            ?: LocalDate.now().toString()

                        val expiry = runCatching {
                            LocalDate.parse(expiryStr)
                        }.getOrDefault(LocalDate.now())

                        val location = child.child("location")
                            .getValue(String::class.java) ?: "냉장고"

                        val allergen = child.child("isAllergen")
                            .getValue(Boolean::class.java) ?: false

                        val itemId = child.key ?: return@runCatching null
                        val remnant = child.child("hasRemnant")
                            .getValue(Boolean::class.java) ?: false

                        FoodItem(
                            id = itemId,
                            name = name,
                            category = runCatching {
                                FoodCategory.valueOf(catStr)
                            }.getOrDefault(FoodCategory.GRAIN),
                            expiry = expiry,
                            qty = child.qtyText(),
                            location = location,
                            addedDays = 0,
                            isAllergen = allergen,
                            hasRemnant = remnant,
                            freshness = (child.child("freshness").value as? Number)?.toInt(),
                            freshnessClass = child.child("freshnessClass")
                                .getValue(String::class.java),
                            freshnessConf = (child.child("freshnessConf").value as? Number)?.toDouble(),
                        )
                    }.getOrNull()
                }

                val webcam = _state.value.webcamItems

                val combined = (dbItems + webcam.filter { w ->
                    dbItems.none { it.name == w.name }
                }).withAllergenFlags(_state.value.allergens)

                val notifs = buildNotifications(
                    combined,
                    _state.value.settings,
                    _state.value.allergens
                )

                _state.update {
                    it.copy(
                        inventory = combined,
                        notifications = notifs,
                        firebaseConnected = true
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _state.update {
                    it.copy(firebaseConnected = false)
                }
            }
        }

        rootDb.child("MoA-BoB")
            .child("foodInventory")
            .addValueEventListener(inventoryListener!!)
    }


    private fun listenToScanResults() {
        scanResultsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 최근에 인식한 항목이 위로 오도록 역순
                val results = snapshot.children.mapNotNull { parseScanResult(it) }.reversed()
                val mode = _state.value.activeScanMode

                // 연속 스캔 중 결과가 계속 쌓이므로 새로 들어온 항목만 알림/음성 대상
                val prevIds = _state.value.scanResults.map { it.id }.toSet()
                val newResults = results.filter { it.id !in prevIds }

                // 알레르기 여부는 사용자가 등록한 알레르기 목록으로 판단 (알류 → 계란 등 포함)
                val hasAllergen = newResults.any { r ->
                    _state.value.allergens.any { a -> allergenMatches(a.name, r.name) }
                }
                val haptic = if (
                    hasAllergen && !allergenHapticFired &&
                    (mode == ScanMode.IN || mode == ScanMode.OUT) &&
                    _state.value.settings.hapticEnabled
                ) {
                    allergenHapticFired = true
                    HapticEvent.ALLERGEN
                } else null

                // 인식된 순서대로 읽도록 다시 시간순으로
                if (newResults.isNotEmpty()) announceScanResults(newResults.reversed().map { it.name })

                _state.update { s ->
                    s.copy(
                        scanResults   = results,
                        scanPhase     = if (results.isNotEmpty()) "detected" else s.scanPhase,
                        pendingHaptic = haptic ?: s.pendingHaptic,
                    )
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("scan_results").addValueEventListener(scanResultsListener!!)
    }

    // scan.py는 동시에 인식한 식재료도 한 건씩 따로 올리므로,
    // 짧은 시간 안에 들어온 결과를 모아 한 문장으로 읽음 (앞 문장이 뒤 문장에 덮이지 않도록)
    private fun announceScanResults(names: List<String>) {
        pendingScanNames += names
        scanTtsJob?.cancel()
        scanTtsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(SCAN_TTS_DEBOUNCE_MS)
            val batch = pendingScanNames.toList()
            pendingScanNames.clear()
            if (batch.isEmpty()) return@launch

            // 같은 식재료를 여러 개 인식하면 "귤 2개"처럼 묶음
            val labels = batch.groupingBy { it }.eachCount()
                .map { (name, count) -> if (count > 1) "$name ${count}개" else name }
            val more    = if (labels.size > 3) " 외 ${labels.size - 3}개" else ""
            val subject = labels.take(3).joinToString(", ") + more
            val verb = when (_state.value.activeScanMode) {
                ScanMode.IN  -> "입고됐습니다"
                ScanMode.OUT -> "출고됐습니다"
                else         -> "인식됐습니다"
            }
            val msg = "$subject${subject.josa("이", "가")} $verb"
            _state.update { it.copy(pendingTts = msg) }
        }
    }

    private fun listenToScanStatus() {
        scanStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val phase = snapshot.getValue(String::class.java) ?: "idle"
                _state.update { it.copy(scanPhase = phase) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("scan_status").addValueEventListener(scanStatusListener!!)
    }

    // scan.py가 진행 상황을 한글로 eatda/scan_hint에 기록함
    private fun listenToScanHint() {
        scanHintListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hint = snapshot.getValue(String::class.java)?.takeIf { it.isNotBlank() }
                _state.update { it.copy(scanHint = hint) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("scan_hint").addValueEventListener(scanHintListener!!)
    }

    // scan.py가 신선도 확인 때 MoA-BoB/freshnessChecks/{이름}에 기록함
    private fun listenToFreshnessChecks() {
        freshnessChecksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val checks = snapshot.children.mapNotNull { child ->
                    val name      = child.key ?: return@mapNotNull null
                    val score     = (child.child("score").value as? Number)?.toInt() ?: return@mapNotNull null
                    val checkedAt = (child.child("checkedAt").value as? Number)?.toLong() ?: return@mapNotNull null
                    name to FreshnessCheck(score = score, checkedAt = checkedAt)
                }.toMap()
                _state.update { it.copy(freshnessChecks = checks) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootDb.child("MoA-BoB").child("freshnessChecks").addValueEventListener(freshnessChecksListener!!)
    }

    // Firebase에 저장된 알레르기 목록이 있으면 사용, 없으면 기본값(defaultAllergens) 유지
    private fun listenToAllergens() {
        allergensListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val list = snapshot.children.mapNotNull { child ->
                    val id   = child.key ?: return@mapNotNull null
                    val name = child.child("name").getValue(String::class.java) ?: return@mapNotNull null
                    Allergen(id = id, name = name, addedAt = child.child("addedAt").getValue(String::class.java) ?: "")
                }
                _state.update { it.withAllergens(list) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("allergens").addValueEventListener(allergensListener!!)
    }

    // scan.py가 시작할 때 자기 PC 주소를 eatda/stream_url에 기록함
    private fun listenToStreamUrl() {
        streamUrlListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val url = snapshot.getValue(String::class.java)?.takeIf { it.isNotBlank() }
                _state.update { it.copy(streamUrl = url) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("stream_url").addValueEventListener(streamUrlListener!!)
    }

    private fun listenToArrivals() {
        arrivalsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                when (snapshot.child("source").getValue(String::class.java)) {
                    "market" -> {
                        val item = parseInventoryItem(snapshot) ?: return
                        autoAddMarketItem(item)
                        snapshot.ref.removeValue()
                    }
                    "webcam" -> {
                        val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        if (ts < listenerStartTime) return
                        val item = parseInventoryItem(snapshot) ?: return
                        addWebcamItem(item)
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("inventory").addChildEventListener(arrivalsListener!!)
    }

    private fun autoAddMarketItem(item: FoodItem, showToast: Boolean = true) {
        val inventoryRef = rootDb.child("MoA-BoB").child("foodInventory")

        // Firebase qty는 정수만 저장 가능 → "1/2통" 같은 값은 1로 올려 저장
        val parsed = parseQty(item.qty)
        val qty    = parsed?.amount?.coerceAtLeast(1) ?: 1
        val unit   = parsed?.unit ?: item.qty.filter { it.isLetter() }.ifBlank { "개" }
        val expiry = item.expiry.toString()

        inventoryRef.get().addOnSuccessListener { snapshot ->
            // 같은 이름 + 같은 유통기한의 재고 찾기
            val existing = snapshot.children.firstOrNull {
                it.child("name").getValue(String::class.java) == item.name &&
                        it.child("expiry").getValue(String::class.java) == expiry
            }

            if (existing != null) {
                // 기존 재고가 있으면 수량 합치기
                existing.ref.child("qty").setValue(existing.qtyNumber() + qty)
            } else {
                // 없으면 새로운 Firebase key 생성
                val key = inventoryRef.push().key ?: return@addOnSuccessListener
                inventoryRef.child(key).setValue(
                    mapOf(
                        "name"       to item.name,
                        "category"   to item.category.name,
                        "expiry"     to expiry,
                        "qty"        to qty,
                        "unit"       to unit,
                        "location"   to item.location,
                        "isAllergen" to item.isAllergen,
                    )
                )
            }

            if (showToast) _state.update { it.copy(toast = "${item.name} 냉장고에 추가됐어요 🧊") }
        }
    }


    private fun addWebcamItem(item: FoodItem) {
        _state.update { s ->
            if (s.webcamItems.any { it.name == item.name && it.expiry == item.expiry }) {
                return@update s
            }

            val newWebcam = s.webcamItems + item
            val stocksOnly = s.inventory.filter { inv ->
                s.webcamItems.none { it.name == inv.name }
            }
            val combined = (stocksOnly + newWebcam.filter { w ->
                stocksOnly.none { it.name == w.name }
            }).withAllergenFlags(s.allergens)
            val notifs = buildNotifications(combined, s.settings, s.allergens)

            s.copy(
                webcamItems = newWebcam,
                inventory = combined,
                notifications = notifs,
                toast = "${item.name} 웹캠으로 인식됐어요 📷"
            )
        }
    }

    fun checkPendingDelivery() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val conn = java.net.URL(
                    "https://eat-da-bd161-default-rtdb.asia-southeast1.firebasedatabase.app/MoA-BoB/pendingDelivery.json"
                ).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5_000
                conn.readTimeout    = 5_000
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                if (body == "null") return@launch
                val root  = org.json.JSONObject(body)
                val items = root.keys().asSequence().mapNotNull { key ->
                    runCatching {
                        val o      = root.getJSONObject(key)
                        val name   = o.optString("name").takeIf { it.isNotEmpty() } ?: return@runCatching null
                        val catStr = o.optString("category", "GRAIN")
                        val expStr = o.optString("expiry", LocalDate.now().toString())
                        val expiry = runCatching { LocalDate.parse(expStr) }.getOrDefault(LocalDate.now())
                        FoodItem(
                            id         = key,
                            name       = name,
                            category   = runCatching { FoodCategory.valueOf(catStr) }.getOrDefault(FoodCategory.GRAIN),
                            expiry     = expiry,
                            qty        = formatQty(
                                value     = o.opt("qty"),
                                unitField = o.optString("unit").takeIf { it.isNotEmpty() },
                            ),
                            location   = o.optString("location", "냉장 1칸"),
                            addedDays  = 0,
                            isAllergen = o.optBoolean("isAllergen", false),
                        )
                    }.getOrNull()
                }.toList()
                if (items.isNotEmpty()) _state.update { it.copy(marketArrivals = items) }
            } catch (_: Exception) { }
        }
    }

    private fun listenToPendingDelivery() {
        val ref = rootDb.child("MoA-BoB").child("pendingDelivery")
        pendingDeliveryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    runCatching {
                        val name      = child.child("name").getValue(String::class.java) ?: return@runCatching null
                        val catStr    = child.child("category").getValue(String::class.java) ?: "GRAIN"
                        val expiryStr = child.child("expiry").getValue(String::class.java) ?: LocalDate.now().toString()
                        val expiry    = runCatching { LocalDate.parse(expiryStr) }.getOrDefault(LocalDate.now())
                        FoodItem(
                            id         = child.key ?: return@runCatching null,
                            name       = name,
                            category   = runCatching { FoodCategory.valueOf(catStr) }.getOrDefault(FoodCategory.GRAIN),
                            expiry     = expiry,
                            qty        = child.qtyText(),
                            location   = child.child("location").getValue(String::class.java) ?: "냉장 1칸",
                            addedDays  = 0,
                            isAllergen = child.child("isAllergen").getValue(Boolean::class.java) ?: false,
                        )
                    }.getOrNull()
                }
                _state.update { it.copy(marketArrivals = items) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(pendingDeliveryListener!!)
    }

    fun dismissMarketArrivals() {
        _state.update { it.copy(marketArrivals = emptyList()) }
    }

    fun confirmMarketArrivals() {
        val items = _state.value.marketArrivals
        items.forEach { autoAddMarketItem(it, showToast = false) }
        rootDb.child("MoA-BoB").child("pendingDelivery").removeValue()
        val names  = items.take(3).joinToString(", ") { it.name }
        val suffix = if (items.size > 3) " 외 ${items.size - 3}개" else ""
        _state.update { it.copy(marketArrivals = emptyList(), toast = "${names}${suffix} 냉장고에 추가됐어요 🧊") }
    }

    // ── 알림 생성 ─────────────────────────────────────────────────────────────

    private fun buildNotifications(
        inventory: List<FoodItem>,
        settings: AppSettings,
        allergens: List<Allergen>,
    ): List<AppNotification> {
        val notifs = mutableListOf<AppNotification>()

        inventory.sortedBy { it.daysLeft() }.forEach { item ->
            val days = item.daysLeft()
            val notif = when {
                days <= 0 && settings.notifExpiryDDay -> AppNotification(
                    id = "exp_d0_${item.id}", type = NotifType.EXPIRY_SOON,
                    title = "${item.name} 유통기한 만료",
                    body = "${item.location}에 보관 중입니다.", time = "오늘", urgent = true,
                )
                days <= 2 && settings.notifExpiryD2 -> AppNotification(
                    id = "exp_d2_${item.id}", type = NotifType.EXPIRY_SOON,
                    title = "${item.name} D-${days}",
                    body = "레시피 추천을 받아보세요.", time = "오늘", urgent = true,
                )
                days <= 5 && settings.notifExpiryD5 -> AppNotification(
                    id = "exp_d5_${item.id}", type = NotifType.EXPIRY_SOON,
                    title = "${item.name} D-${days}",
                    body = "${item.location}에 보관 중입니다.", time = "오늘", urgent = false,
                )
                days <= 7 && settings.notifExpiryD7 -> AppNotification(
                    id = "exp_d7_${item.id}", type = NotifType.EXPIRY_SOON,
                    title = "${item.name} D-${days}",
                    body = "${item.location}에 보관 중입니다.", time = "오늘", urgent = false,
                )
                else -> null
            }
            notif?.let { notifs.add(it) }
        }

        if (settings.notifAllergen) {
            inventory
                .filter { item -> allergens.any { a -> allergenMatches(a.name, item.name) } }
                .forEach { item ->
                    notifs.add(AppNotification(
                        id = "allergen_${item.id}", type = NotifType.EXPIRY_SOON,
                        title = "${item.name} 알레르기 주의",
                        body = "알레르기 유발 식재료가 재고에 있습니다.", time = "오늘", urgent = true,
                    ))
                }
        }

        return notifs.sortedByDescending { it.urgent }
    }

    // ── Firebase 파싱 ─────────────────────────────────────────────────────────

    private fun parseInventoryItem(snap: DataSnapshot): FoodItem? = runCatching {
        val name           = snap.child("name").getValue(String::class.java) ?: return@runCatching null
        val catStr         = snap.child("category").getValue(String::class.java) ?: "GRAIN"
        val category       = runCatching { FoodCategory.valueOf(catStr) }.getOrDefault(FoodCategory.GRAIN)
        val expiryStr      = snap.child("expiry").getValue(String::class.java) ?: LocalDate.now().toString()
        val expiry         = runCatching { LocalDate.parse(expiryStr) }.getOrDefault(LocalDate.now())
        val freshness      = (snap.child("freshness").value as? Number)?.toInt()
        val freshnessClass = snap.child("freshnessClass").getValue(String::class.java)
        val freshnessConf  = (snap.child("freshnessConf").value as? Number)?.toDouble()
        val location       = snap.child("location").getValue(String::class.java) ?: "냉장 1칸"
        val addedDays      = (snap.child("addedDays").value as? Number)?.toInt() ?: 0
        val isAllergen     = snap.child("isAllergen").getValue(Boolean::class.java) ?: false

        FoodItem(
            id             = snap.key ?: return@runCatching null,
            name           = name,
            category       = category,
            expiry         = expiry,
            qty            = snap.qtyText(),
            freshness      = freshness,
            freshnessClass = freshnessClass,
            freshnessConf  = freshnessConf,
            location       = location,
            addedDays      = addedDays,
            isAllergen     = isAllergen,
        )
    }.getOrNull()

    private fun parseScanResult(snap: DataSnapshot): ScanResult? = runCatching {
        val id         = snap.key ?: return@runCatching null
        val name       = snap.child("name").getValue(String::class.java) ?: return@runCatching null
        val confidence = (snap.child("confidence").value as? Number)?.toDouble()
        val isAllergen = snap.child("isAllergen").getValue(Boolean::class.java) ?: false
        val qty        = snap.child("qty").value?.toString() ?: ""
        val category   = snap.child("category").getValue(String::class.java)
            ?.let { runCatching { FoodCategory.valueOf(it) }.getOrNull() }
        val expiry     = snap.child("expiry").getValue(String::class.java)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val freshness  = (snap.child("freshness").value as? Number)?.toInt()?.takeIf { it >= 0 }
        ScanResult(
            id              = id,
            name            = name,
            confidence      = confidence,
            suggestedExpiry = expiry,
            category        = category,
            freshness       = freshness,
            // 앱 공통 기준 (70% 이상 신선 / 30% 이하 부패 / 그 사이 보통)
            freshnessStatus = freshnessStatusOf(freshness),
            isAllergen      = isAllergen,
            qty             = qty,
            itemId          = snap.child("itemId").getValue(String::class.java),
        )
    }.getOrNull()

    // ── 화면 / 시트 제어 ──────────────────────────────────────────────────────

    // 다른 화면으로 이동하면 재고 그룹 화면은 닫고 전체 목록부터 보여줌
    fun navigate(screen: Screen) = _state.update { it.copy(screen = screen, inventoryGroupName = null) }

    fun openInventoryGroup(foodName: String?) = _state.update { it.copy(inventoryGroupName = foodName) }

    fun openScanSheet()  = _state.update { it.copy(scanSheetOpen = true) }
    fun closeScanSheet() = _state.update { it.copy(scanSheetOpen = false) }

    fun openRecipe(recipe: Recipe) = _state.update { it.copy(selectedRecipe = recipe, screen = Screen.RECIPE_DETAIL) }
    fun closeRecipe()              = _state.update { it.copy(selectedRecipe = null, screen = Screen.RECIPES) }

    fun openRecipeRecommendations(foodName: String) {
        _state.update { it.copy(openItem = null, recipeFocusIngredient = foodName, screen = Screen.RECIPES) }
    }

    fun openRecipesFromNavigation() {
        _state.update { it.copy(recipeFocusIngredient = null, screen = Screen.RECIPES) }
    }

    // 재고 그룹 화면에서 레시피 추천으로 들어온 경우 → 보던 그룹 화면으로 복귀
    // 그 외 → 홈으로
    fun backFromRecipes() {
        _state.update {
            if (it.recipeFocusIngredient != null) {
                it.copy(screen = Screen.INVENTORY, recipeFocusIngredient = null)
            } else {
                it.copy(screen = Screen.HOME, inventoryGroupName = null)
            }
        }
    }

    // 스캔 시트에서 시작 → 신선도 확인 대상 없음 (보이는 식재료 전부 기록)
    fun startScan(mode: ScanMode) {
        db.child("fresh_target").removeValue()
        beginScan(mode)
    }

    // 재고 화면 "신선도 확인" 버튼 → 해당 식재료만 기록
    // scan.py가 모드 변경 시 대상을 읽으므로 current_mode보다 먼저 기록
    fun startFreshCheck(foodName: String) {
        db.child("fresh_target").setValue(foodName)
        beginScan(ScanMode.FRESH)
    }

    private fun beginScan(mode: ScanMode) {
        allergenHapticFired = false
        ocrItemIds.clear()
        val firebaseMode = when (mode) {
            ScanMode.FRESH -> "fresh_scan"
            else           -> mode.name
        }
        rootDb.child("current_mode").setValue(firebaseMode)
        db.child("scan_results").removeValue()
        db.child("scan_status").setValue("scanning")
        _state.update { it.copy(scanSheetOpen = false, activeScanMode = mode, scanResults = emptyList(), scanPhase = "scanning") }
    }

    fun closeScan() {
        db.child("fresh_target").removeValue()
        rootDb.child("current_mode").setValue("IDLE")
        db.child("scan_status").setValue("idle")
        _state.update { it.copy(activeScanMode = null, scanResults = emptyList(), scanPhase = "idle") }
    }

    fun confirmScan(items: List<ScanResult>) {
        val mode = _state.value.activeScanMode
        db.child("fresh_target").removeValue()
        rootDb.child("current_mode").setValue("IDLE")
        db.child("scan_status").setValue("idle")
        val msg = when (mode) {
            ScanMode.IN       -> "${items.size}개 항목이 재고에 추가됐습니다"
            ScanMode.OUT      -> "${items.size}개 항목이 출고됐습니다"
            ScanMode.FRESH    -> "신선도 분석 결과를 확인했습니다"
            null              -> ""
        }
        _state.update { it.copy(activeScanMode = null, toast = msg, scanResults = emptyList(), scanPhase = "idle") }
    }

    // 입고 화면 "유통기한 촬영"으로 읽은 날짜를 재고에 반영
    //
    // scan.py는 같은 이름·같은 추정 소비기한이면 한 재고에 수량을 합치므로,
    // 스캔한 1개만 새 소비기한으로 옮김 (남은 수량은 원래 소비기한 유지)
    // 새 소비기한과 같은 재고가 이미 있으면 그쪽 수량에 합침
    fun applyOcrExpiry(result: ScanResult, newDate: LocalDate) {
        val itemId = ocrItemIds[result.id] ?: result.itemId ?: run {
            _state.update { it.copy(toast = "재고를 찾지 못해 소비기한을 저장하지 못했어요") }
            return
        }
        val newExpiry    = newDate.toString()
        val inventoryRef = rootDb.child("MoA-BoB").child("foodInventory")

        inventoryRef.get().addOnSuccessListener { snapshot ->
            val current = snapshot.child(itemId)
            if (!current.exists()) {
                _state.update { it.copy(toast = "재고를 찾지 못해 소비기한을 저장하지 못했어요") }
                return@addOnSuccessListener
            }
            if (current.child("expiry").getValue(String::class.java) == newExpiry) return@addOnSuccessListener

            val name = current.child("name").getValue(String::class.java) ?: return@addOnSuccessListener
            val qty  = current.qtyNumber()
            val existing = snapshot.children.firstOrNull {
                it.key != itemId &&
                        it.child("name").getValue(String::class.java) == name &&
                        it.child("expiry").getValue(String::class.java) == newExpiry
            }

            val targetKey: String = when {
                // 1개뿐 + 새 소비기한 재고 있음 → 합치고 원래 재고 삭제
                qty <= 1 && existing != null -> {
                    existing.ref.child("qty").setValue(existing.qtyNumber() + 1)
                    current.ref.removeValue()
                    existing.key!!
                }
                // 1개뿐 → 소비기한만 수정
                qty <= 1 -> {
                    current.ref.child("expiry").setValue(newExpiry)
                    itemId
                }
                // 여러 개 → 1개만 떼어 새 소비기한 재고로
                else -> {
                    current.ref.child("qty").setValue(qty - 1)
                    if (existing != null) {
                        existing.ref.child("qty").setValue(existing.qtyNumber() + 1)
                        existing.key!!
                    } else {
                        val key = inventoryRef.push().key ?: return@addOnSuccessListener
                        @Suppress("UNCHECKED_CAST")
                        val data = HashMap(current.value as? Map<String, Any?> ?: emptyMap())
                        data["qty"]    = 1
                        data["expiry"] = newExpiry
                        inventoryRef.child(key).setValue(data)
                        key
                    }
                }
            }

            ocrItemIds[result.id] = targetKey
            val label = newDate.format(DateTimeFormatter.ofPattern("M월 d일"))
            _state.update { it.copy(toast = "${name} 소비기한을 ${label}로 저장했어요 ✏️") }
        }.addOnFailureListener {
            _state.update { it.copy(toast = "소비기한 저장에 실패했어요") }
        }
    }

    // 결과 목록만 비우고 scan.py는 계속 스캔 (연속 스캔 모드)
    fun rescan() {
        allergenHapticFired = false
        ocrItemIds.clear()
        db.child("scan_results").removeValue()
        _state.update { it.copy(scanResults = emptyList(), scanPhase = "scanning") }
    }

    // ── 영수증 스캔 ─────────────────────────────────────────────────────────

    fun openReceiptScan()  = _state.update { it.copy(scanSheetOpen = false, receiptScanOpen = true) }
    fun closeReceiptScan() = _state.update { it.copy(receiptScanOpen = false) }

    // 확인 화면에서 고른 항목을 재고에 추가 (같은 이름·소비기한 재고가 있으면 수량 합침)
    fun confirmReceipt(entries: List<ReceiptEntry>) {
        val now = System.currentTimeMillis()
        val items = entries
            .filter { it.checked && it.name.isNotBlank() }
            .map { entry ->
                val name = entry.name.trim()
                FoodItem(
                    id        = "receipt_${now}_${entry.id}",
                    name      = name,
                    category  = FoodCategoryMapper.getCategory(name),
                    expiry    = entry.expiry,
                    qty       = "${entry.qty}개",
                    location  = "냉장고",
                    addedDays = 0,
                )
            }
        if (items.isEmpty()) {
            _state.update { it.copy(receiptScanOpen = false) }
            return
        }

        items.forEach { autoAddMarketItem(it, showToast = false) }

        val names   = items.map { it.name }
        val more    = if (names.size > 3) " 외 ${names.size - 3}개" else ""
        val subject = names.take(3).joinToString(", ") + more
        val msg     = "$subject${subject.josa("이", "가")} 입고됐습니다"
        _state.update {
            it.copy(
                receiptScanOpen = false,
                toast           = "영수증으로 ${items.size}개 입고했어요 🧾",
                pendingTts      = msg,
            )
        }
    }

    fun openPhoneScan(mode: ScanMode) =
        _state.update { it.copy(scanSheetOpen = false, phoneScanMode = mode, phoneScanResults = emptyList()) }

    fun closePhoneScan() =
        _state.update { it.copy(phoneScanMode = null, phoneScanResults = emptyList()) }

    fun addPhoneScanResults(newResults: List<ScanResult>) =
        _state.update { it.copy(phoneScanResults = it.phoneScanResults + newResults) }

    fun confirmPhoneScan(items: List<ScanResult>) {
        val mode = _state.value.phoneScanMode
        val msg = when (mode) {
            ScanMode.IN       -> "${items.size}개 항목이 재고에 추가됐습니다"
            ScanMode.OUT      -> "${items.size}개 항목이 출고됐습니다"
            ScanMode.FRESH    -> "신선도 분석 결과를 확인했습니다"
            null              -> ""
        }
        _state.update { it.copy(phoneScanMode = null, phoneScanResults = emptyList(), toast = msg) }
    }

    fun openItem(item: FoodItem) = _state.update { it.copy(openItem = item.withAllergenFlag(it.allergens)) }
    fun closeItem()              = _state.update { it.copy(openItem = null) }

    fun openVoice()  = _state.update { it.copy(voiceOverlayOpen = true) }
    fun closeVoice() = _state.update { it.copy(voiceOverlayOpen = false, lastVoiceResponse = null, isAddMode = false, addedItems = emptyList()) }

    // ── 공동 냉장고 ───────────────────────────────────────────────────────────

    fun createHousehold(name: String, myName: String) {
        HouseholdManager.createHousehold(
            context       = ctx,
            householdName = name,
            myName        = myName,
            onSuccess     = { householdId, _ -> loadHousehold(householdId) },
            onError       = { msg -> _state.update { it.copy(toast = msg) } },
        )
    }

    fun joinHousehold(code: String, myName: String) {
        HouseholdManager.joinHousehold(
            context     = ctx,
            inviteCode  = code,
            myName      = myName,
            onSuccess   = { householdId -> loadHousehold(householdId) },
            onError     = { msg -> _state.update { it.copy(toast = msg) } },
        )
    }

    fun leaveHousehold() {
        val householdId = _state.value.household?.id ?: return
        householdMemberListener?.let { HouseholdManager.removeListener(householdId, it) }
        HouseholdManager.leaveHousehold(ctx, householdId) {
            _state.update { it.copy(household = null, householdMembers = emptyList(), toast = "공동 냉장고에서 나갔어요") }
        }
    }

    private fun loadHousehold(householdId: String) {
        Firebase.database.reference.child("households").child(householdId).get()
            .addOnSuccessListener { snap ->
                val name       = snap.child("name").getValue(String::class.java) ?: return@addOnSuccessListener
                val ownerId    = snap.child("ownerId").getValue(String::class.java) ?: ""
                val inviteCode = snap.child("inviteCode").getValue(String::class.java) ?: ""
                val household  = Household(id = householdId, name = name, inviteCode = inviteCode, ownerId = ownerId)
                _state.update { it.copy(household = household) }

                householdMemberListener?.let { HouseholdManager.removeListener(householdId, it) }
                householdMemberListener = HouseholdManager.listenToMembers(householdId) { members ->
                    _state.update { it.copy(householdMembers = members) }
                }
            }
    }

    fun dismissToast() = _state.update { it.copy(toast = null) }

    fun removeAllergen(id: String) {
        val newAllergens = _state.value.allergens.filter { a -> a.id != id }
        saveAllergens(newAllergens)
        _state.update { it.withAllergens(newAllergens) }
    }

    // 마이페이지에서 직접 입력하거나 대표 항목을 눌러 등록
    fun addAllergen(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (_state.value.allergens.any { it.name == trimmed }) {
            _state.update { it.copy(toast = "${trimmed}${trimmed.josa("은", "는")} 이미 등록돼 있어요") }
            return
        }
        addAllergens(listOf(trimmed))
    }

    private fun addAllergens(names: List<String>) {
        val current  = _state.value.allergens
        val existing = current.map { it.name }.toSet()
        val today    = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        val now      = System.currentTimeMillis()
        val added = names.distinct()
            .filter { it !in existing }
            .mapIndexed { i, name -> Allergen(id = "a_${now}_$i", name = name, addedAt = today) }
        if (added.isEmpty()) return

        val newAllergens = current + added
        saveAllergens(newAllergens)
        _state.update { it.withAllergens(newAllergens) }
    }

    // 알레르기 목록이 바뀌면 재고 알레르기 표시와 알림도 함께 갱신
    private fun AppState.withAllergens(list: List<Allergen>): AppState {
        val flagged = inventory.withAllergenFlags(list)
        return copy(
            allergens     = list,
            inventory     = flagged,
            webcamItems   = webcamItems.withAllergenFlags(list),
            openItem      = openItem?.withAllergenFlag(list),
            notifications = buildNotifications(flagged, settings, list),
        )
    }

    // 기본 알레르기(우유, 계란)도 함께 저장되도록 목록 전체를 씀
    private fun saveAllergens(list: List<Allergen>) {
        db.child("allergens").setValue(
            list.associate { it.id to mapOf("name" to it.name, "addedAt" to it.addedAt) }
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        val notifs = buildNotifications(_state.value.inventory, newSettings, _state.value.allergens)
        _state.update { it.copy(settings = newSettings, notifications = notifs) }
    }

    // ── 재고 항목 수정 ────────────────────────────────────────────────────────

    fun updateItemExpiry(itemId: String, newExpiry: String) {
        val newDate = runCatching { LocalDate.parse(newExpiry) }.getOrNull() ?: run {
            _state.update { it.copy(toast = "날짜 형식이 올바르지 않아요") }
            return
        }
        val inventoryRef = rootDb.child("MoA-BoB").child("foodInventory")

        inventoryRef.get().addOnSuccessListener { snapshot ->
            val current = snapshot.child(itemId)
            if (!current.exists()) return@addOnSuccessListener

            val currentName = current.child("name").getValue(String::class.java) ?: return@addOnSuccessListener
            val currentQty  = current.qtyNumber()

            // 같은 이름 + 새로운 유통기한을 가진 기존 재고 찾기
            val existing = snapshot.children.firstOrNull {
                it.key != itemId &&
                        it.child("name").getValue(String::class.java) == currentName &&
                        it.child("expiry").getValue(String::class.java) == newExpiry
            }

            if (existing != null) {
                // 이미 같은 이름 + 같은 유통기한이 있으면 수량 합치기
                existing.ref.child("qty").setValue(existing.qtyNumber() + currentQty)
                    .addOnSuccessListener { current.ref.removeValue() }
            } else {
                // 같은 재고가 없으면 유통기한만 수정
                current.ref.child("expiry").setValue(newExpiry)
            }

            _state.update { s ->
                val updatedInventory = if (existing != null) {
                    s.inventory.filter { it.id != itemId }
                } else {
                    s.inventory.map { if (it.id == itemId) it.copy(expiry = newDate) else it }
                }
                s.copy(
                    inventory = updatedInventory,
                    openItem  = if (existing != null) null
                    else s.openItem?.let { if (it.id == itemId) it.copy(expiry = newDate) else it },
                    toast     = if (existing != null) "${currentName} 재고가 합쳐졌어요 ✏️"
                    else "유통기한이 수정됐어요 ✏️",
                )
            }
        }
    }

    fun updateItemQty(itemId: String, newQty: String) {
        val inventoryRef = rootDb.child("MoA-BoB").child("foodInventory")

        // 형식이 잘못된 값을 0으로 보고 재고를 지워버리지 않도록 먼저 검사
        val parsed = parseQty(newQty) ?: run {
            _state.update { it.copy(toast = "수량 형식이 올바르지 않아요") }
            return
        }
        val qty  = parsed.amount
        val unit = parsed.unit
        val itemRef = inventoryRef.child(itemId)

        if (qty <= 0) {
            itemRef.removeValue()
            _state.update { s ->
                s.copy(
                    inventory = s.inventory.filter { it.id != itemId },
                    openItem  = if (s.openItem?.id == itemId) null else s.openItem,
                    toast     = "재고가 삭제됐어요 🗑️",
                )
            }
            return
        }

        itemRef.updateChildren(mapOf("qty" to qty, "unit" to unit))
        _state.update { s ->
            s.copy(
                inventory = s.inventory.map { if (it.id == itemId) it.copy(qty = "$qty$unit") else it },
                openItem  = s.openItem?.let { if (it.id == itemId) it.copy(qty = "$qty$unit") else it },
                toast     = "개수가 수정됐어요 ✏️",
            )
        }
    }

    fun deleteInventoryItem(itemId: String) {
        rootDb.child("MoA-BoB").child("foodInventory").child(itemId).removeValue()
            .addOnSuccessListener {
                _state.update { s ->
                    s.copy(
                        inventory = s.inventory.filter { it.id != itemId },
                        openItem  = if (s.openItem?.id == itemId) null else s.openItem,
                        toast     = "재고가 삭제됐어요 🗑️",
                    )
                }
            }
            .addOnFailureListener {
                _state.update { it.copy(toast = "재고 삭제에 실패했어요.") }
            }
    }

    fun toggleRemnant(itemId: String) {
        val current = _state.value.inventory.firstOrNull { it.id == itemId } ?: return
        val newValue = !current.hasRemnant

        // 웹캠 인식 항목은 foodInventory에 없으므로 화면에서만 표시하고,
        // 나머지는 Firebase hasRemnant 필드에 저장해 앱을 다시 켜도 유지
        val isWebcamOnly = _state.value.webcamItems.any { it.id == itemId }
        if (!isWebcamOnly) {
            rootDb.child("MoA-BoB").child("foodInventory").child(itemId)
                .child("hasRemnant").setValue(newValue)
                .addOnFailureListener {
                    _state.update { it.copy(toast = "자투리 저장에 실패했어요.") }
                }
        }

        _state.update { s ->
            s.copy(
                inventory = s.inventory.map {
                    if (it.id == itemId) it.copy(hasRemnant = newValue) else it
                },
                webcamItems = s.webcamItems.map {
                    if (it.id == itemId) it.copy(hasRemnant = newValue) else it
                },
                openItem = s.openItem?.let {
                    if (it.id == itemId) it.copy(hasRemnant = newValue) else it
                },
                toast = if (newValue) "자투리로 표시됐어요 🥄" else "자투리 표시가 해제됐어요",
            )
        }
    }

    // ── 음성 명령 처리 ────────────────────────────────────────────────────────

    fun handleVoiceCommand(command: VoiceCommand) {
        val inv = _state.value.inventory
        when (command) {

            // ── 재고 추가 모드 진입 ───────────────────────────────────────────
            VoiceCommand.EnterAddMode -> {
                val msg = "재고 추가 모드입니다. 추가할 식재료를 말씀하세요."
                _state.update { it.copy(
                    isAddMode         = true,
                    addedItems        = emptyList(),
                    pendingTts        = msg,
                    lastVoiceResponse = msg,
                )}
            }

            // ── 재고 추가 모드 종료 ───────────────────────────────────────────
            VoiceCommand.ExitAddMode -> {
                val count = _state.value.addedItems.size
                val msg   = if (count > 0) "총 ${count}개 항목이 추가됐습니다." else "추가 모드를 종료합니다."
                _state.update { it.copy(
                    isAddMode         = false,
                    addedItems        = emptyList(),
                    pendingTts        = msg,
                    lastVoiceResponse = msg,
                )}
            }

            // ── 음성으로 재고 여러 개 한 번에 추가 ───────────────────────────
            is VoiceCommand.AddFoods -> {
                val newItems = command.items.map { cmd ->
                    val expiry = if (cmd.expiry != null) {
                        runCatching { LocalDate.parse(cmd.expiry) }.getOrNull()
                            ?: LocalDate.now().plusDays(7)
                    } else {
                        LocalDate.now().plusDays(DEFAULT_SHELF_DAYS[cmd.name] ?: 7L)
                    }
                    FoodItem(
                        id         = "${System.currentTimeMillis()}_${cmd.name.hashCode()}",
                        name       = cmd.name,
                        qty        = cmd.qty,
                        category   = FoodCategoryMapper.getCategory(cmd.name),
                        expiry     = expiry,
                        location   = "냉장고",
                        addedDays  = 0,
                        isAllergen = false,
                    )
                }
                newItems.forEach { autoAddMarketItem(it, showToast = false) }
                val names = newItems.joinToString(", ") { "${it.name} ${it.qty}" }
                val msg   = "${names} 추가됐어요."
                _state.update { s -> s.copy(
                    addedItems        = s.addedItems + newItems,
                    pendingTts        = msg,
                    lastVoiceResponse = msg,
                )}
            }

            // ── 음성으로 재고 추가 ────────────────────────────────────────────
            is VoiceCommand.AddFood -> {
                val expiry = if (command.expiry != null) {
                    runCatching { LocalDate.parse(command.expiry) }.getOrNull()
                        ?: LocalDate.now().plusDays(7)
                } else {
                    // 식약처 로컬 기본값 — expiry 없이 말한 경우 (신선식품)
                    LocalDate.now().plusDays(DEFAULT_SHELF_DAYS[command.name] ?: 7L)
                }

                val newItem = FoodItem(
                    id         = System.currentTimeMillis().toString(),
                    name       = command.name,
                    qty        = command.qty,
                    category   = FoodCategoryMapper.getCategory(command.name),
                    expiry     = expiry,
                    location   = "냉장고",
                    addedDays  = 0,
                    isAllergen = false,
                )

                autoAddMarketItem(newItem, showToast = false)

                val expiryText  = "${expiry.monthValue}월 ${expiry.dayOfMonth}일"
                val sourceLabel = if (command.expiry == null) " (기본값)" else ""
                val msg = "${command.name} ${command.qty}, 소비기한 $expiryText${sourceLabel} 추가됐어요."

                _state.update { s -> s.copy(
                    addedItems        = s.addedItems + newItem,
                    pendingTts        = msg,
                    lastVoiceResponse = msg,
                )}
            }

            // ── 유통기한 조회 ─────────────────────────────────────────────────
            is VoiceCommand.ExpiryQuery -> {
                val item = inv.firstOrNull { it.name == command.foodName }
                val msg = if (item == null) {
                    "${command.foodName}${command.foodName.josa("은", "는")} 냉장고에 등록되지 않았습니다."
                } else {
                    val d    = item.expiry
                    val days = item.daysLeft()
                    buildString {
                        append("${item.name}의 유통기한은 ${d.monthValue}월 ${d.dayOfMonth}일입니다.")
                        when {
                            days <= 0 -> append(" 이미 만료됐습니다.")
                            days == 1 -> append(" 내일 만료됩니다.")
                            days <= 3 -> append(" ${days}일 남았습니다.")
                        }
                    }
                }
                _state.update { it.copy(pendingTts = msg, lastVoiceResponse = msg) }
            }

            is VoiceCommand.SearchFood -> {
                val items = inv.filter { it.name == command.foodName }
                val msg = if (items.isEmpty()) {
                    "아니요, 냉장고에 ${command.foodName}${command.foodName.josa("이", "가")} 없습니다."
                } else {
                    val qtyText = items.firstOrNull()?.qty ?: "1개"
                    "네, 냉장고에 ${command.foodName}${command.foodName.josa("이", "가")} ${qtyText} 있습니다."
                }
                _state.update { it.copy(pendingTts = msg, lastVoiceResponse = msg) }
            }

            is VoiceCommand.ExpiringSoon -> {
                val days      = command.days
                val threshold = LocalDate.now().plusDays(days.toLong())
                val expiring  = inv
                    .filter { !it.expiry.isAfter(threshold) }
                    .sortedBy { it.expiry }
                val dayLabel = when (days) {
                    1    -> "하루"
                    2    -> "이틀"
                    3    -> "사흘"
                    4    -> "나흘"
                    5    -> "닷새"
                    7    -> "일주일"
                    else -> "${days}일"
                }
                val msg = when (expiring.size) {
                    0    -> "네, $dayLabel 이내 유통기한이 만료되는 식품은 없습니다."
                    1    -> "네, 유통기한 $dayLabel 남은 제품은 ${expiring[0].name}입니다."
                    else -> {
                        val names = expiring.joinToString(", ") { it.name }
                        "네, 유통기한 $dayLabel 남은 제품은 ${names}입니다."
                    }
                }
                _state.update { it.copy(pendingTts = msg, lastVoiceResponse = msg) }
            }

            is VoiceCommand.DeleteFood -> {
                val item = inv.firstOrNull { it.name == command.foodName }
                if (item != null) {
                    val msg = if (command.qty != null)
                        "${item.name} ${command.qty}개${item.name.josa("을", "를")} 삭제할까요?"
                    else
                        "${item.name}${item.name.josa("을", "를")} 삭제할까요?"
                    _state.update { it.copy(
                        pendingDeleteItem = item,
                        pendingDeleteQty  = command.qty,
                        pendingTts        = msg,
                        lastVoiceResponse = msg,
                    )}
                } else {
                    val msg = "${command.foodName}${command.foodName.josa("은", "는")} 냉장고에 없습니다."
                    _state.update { it.copy(pendingTts = msg, lastVoiceResponse = msg) }
                }
            }

            VoiceCommand.Confirm -> {
                val item      = _state.value.pendingDeleteItem ?: return
                val deleteQty = _state.value.pendingDeleteQty ?: 1
                val inventoryRef = rootDb.child("MoA-BoB").child("foodInventory")

                inventoryRef.get().addOnSuccessListener { snapshot ->
                    // 같은 이름의 재고를 유통기한 빠른 순서로 정렬
                    val matchingItems = snapshot.children
                        .filter { it.child("name").getValue(String::class.java) == item.name }
                        .sortedBy {
                            val expiry = it.child("expiry").getValue(String::class.java)
                            runCatching { LocalDate.parse(expiry) }.getOrDefault(LocalDate.MAX)
                        }

                    var remainingToDelete = deleteQty

                    // 유통기한이 빠른 재고부터 차례대로 소비
                    matchingItems.forEach { stock ->
                        if (remainingToDelete <= 0) return@forEach
                        val stockQty = stock.qtyNumber()
                        if (stockQty <= remainingToDelete) {
                            remainingToDelete -= stockQty
                            stock.ref.removeValue()
                        } else {
                            stock.ref.child("qty").setValue(stockQty - remainingToDelete)
                            remainingToDelete = 0
                        }
                    }

                    val actuallyDeleted = deleteQty - remainingToDelete
                    val msg = if (actuallyDeleted > 0) {
                        "${item.name} ${actuallyDeleted}개${item.name.josa("을", "를")} 삭제했습니다."
                    } else {
                        "${item.name}${item.name.josa("은", "는")} 냉장고에 없습니다."
                    }

                    _state.update { s ->
                        s.copy(
                            pendingDeleteItem = null,
                            pendingDeleteQty  = null,
                            pendingTts        = msg,
                            lastVoiceResponse = msg,
                            toast             = if (actuallyDeleted > 0) "${item.name} ${actuallyDeleted}개 삭제됨 🗑️" else null,
                        )
                    }
                }
            }

            VoiceCommand.Cancel -> {
                val msg = "취소했습니다."
                _state.update { it.copy(
                    pendingDeleteItem = null,
                    pendingDeleteQty  = null,
                    pendingTts        = msg,
                    lastVoiceResponse = msg,
                )}
            }

            // ── 이전 (음성 초기 화면으로 복귀) ──────────────────────────────────
            VoiceCommand.Back -> {
                _state.update { it.copy(
                    lastVoiceResponse = null,
                    isAddMode         = false,
                    addedItems        = emptyList(),
                )}
            }

            is VoiceCommand.Unknown -> {
                val msg = "죄송합니다. 다시 말씀해주세요."
                _state.update { it.copy(pendingTts = msg, lastVoiceResponse = msg) }
            }
        }
    }

    // ── 이벤트 소비 ───────────────────────────────────────────────────────────

    fun clearHaptic() = _state.update { it.copy(pendingHaptic = null) }
    fun clearTts()    = _state.update { it.copy(pendingTts = null) }
    fun clearVoiceResponse() = _state.update { it.copy(lastVoiceResponse = null) }

    // ── 리소스 해제 ───────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        inventoryListener?.let        { rootDb.child("MoA-BoB").child("foodInventory").removeEventListener(it) }
        pendingDeliveryListener?.let  { rootDb.child("MoA-BoB").child("pendingDelivery").removeEventListener(it) }
        scanResultsListener?.let      { db.child("scan_results").removeEventListener(it) }
        scanStatusListener?.let       { db.child("scan_status").removeEventListener(it) }
        arrivalsListener?.let         { db.child("inventory").removeEventListener(it) }
        streamUrlListener?.let        { db.child("stream_url").removeEventListener(it) }
        scanHintListener?.let         { db.child("scan_hint").removeEventListener(it) }
        allergensListener?.let        { db.child("allergens").removeEventListener(it) }
        freshnessChecksListener?.let  { rootDb.child("MoA-BoB").child("freshnessChecks").removeEventListener(it) }
        _state.value.household?.id?.let { hid ->
            householdMemberListener?.let { HouseholdManager.removeListener(hid, it) }
        }
    }
}

// 등록한 알레르기 목록 기준으로 재고의 알레르기 표시(isAllergen)를 다시 계산
// (Firebase에 저장된 isAllergen 값 대신 사용자의 알레르기 목록을 기준으로 함)
private fun FoodItem.withAllergenFlag(allergens: List<Allergen>): FoodItem =
    copy(isAllergen = allergens.any { a -> allergenMatches(a.name, name) })

private fun List<FoodItem>.withAllergenFlags(allergens: List<Allergen>): List<FoodItem> =
    map { it.withAllergenFlag(allergens) }

// 받침 유무에 따라 한국어 조사 선택 ("이/가", "을/를", "은/는" 등)
private fun String.josa(받침있음: String, 받침없음: String): String {
    if (isEmpty()) return 받침있음
    val code = last().code
    return if (code in 0xAC00..0xD7A3 && (code - 0xAC00) % 28 != 0) 받침있음 else 받침없음
}
