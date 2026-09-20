package com.eatda.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import androidx.lifecycle.viewModelScope
import com.eatda.app.data.api.RecipeApiService
import kotlinx.coroutines.launch

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
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val rootDb = Firebase.database.reference
    private val db     = Firebase.database.reference.child("eatda")

    // ⭐ 실제 식재료 재고
    private val foodInventoryDb =
        Firebase.database.reference.child("MoA-BoB").child("foodInventory")

    private val ctx    get() = getApplication<Application>()

    // ── Firebase Inventory 저장 ───────────────────────────────────────────

    /**
     * FoodItem 하나를 Firebase의 eatda/inventory에 저장
     */
    /**
     * 식품명을 Firebase inventory의 고정 key로 변환
     */
    private fun getInventoryKey(name: String): String {
        return when {
            name.contains("우유") -> "milk"
            name.contains("바나나") -> "banana"
            name.contains("사과") -> "apple"
            name.contains("오이") -> "cucumber"
            name.contains("양파") -> "onion"
            name.contains("애호박") -> "zucchini"
            name.contains("브로콜리") -> "broccoli"
            name.contains("딸기") -> "strawberry"
            name.contains("계란") -> "egg"
            name.contains("두부") -> "tofu"

            else -> name.trim()
                .lowercase()
                .replace(" ", "_")
        }
    }

    /**
     * Firebase inventory에 표시할 대표 식품명으로 변환
     */
    private fun getInventoryName(name: String): String {
        return when {
            name.contains("우유") -> "우유"
            name.contains("바나나") -> "바나나"
            name.contains("사과") -> "사과"
            name.contains("오이") -> "오이"
            name.contains("양파") -> "양파"
            name.contains("애호박") -> "애호박"
            name.contains("브로콜리") -> "브로콜리"
            name.contains("딸기") -> "딸기"
            name.contains("계란") -> "계란"
            name.contains("두부") -> "두부"

            else -> name.trim()
        }
    }

    /**
     * FoodItem의 qty(String)를 숫자로 변환
     *
     * 예:
     * "1개"  -> 1
     * "2개"  -> 2
     * "10개" -> 10
     */
    private fun parseQuantity(qty: String): Int {
        return Regex("""\d+""")
            .find(qty)
            ?.value
            ?.toIntOrNull()
            ?: 1
    }

    /**
     * FoodItem 하나를 Firebase의 MoA-BoB/foodInventory에 저장
     *
     * 식품 하나당 Firebase 항목 하나를 사용하며,
     * 기존 식품이 있으면 수량을 합산하고
     * 소비기한은 더 빠른 날짜를 유지한다.
     */
    private fun saveInventoryItem(item: FoodItem) {

        val inventoryRef = foodInventoryDb
            .child(item.name)

        inventoryRef.get().addOnSuccessListener { snapshot ->

            if (snapshot.exists()) {

                // 기존 수량
                val currentQtyText =
                    snapshot.child("qty")
                        .getValue(String::class.java)
                        ?: "0개"

                val currentQty = parseQuantity(currentQtyText)
                val newQty = currentQty + parseQuantity(item.qty)

                // 기존 소비기한
                val currentExpiry =
                    snapshot.child("expiry")
                        .getValue(String::class.java)
                        ?.let {
                            runCatching {
                                LocalDate.parse(it)
                            }.getOrNull()
                        }

                // 더 빠른 소비기한 유지
                val newExpiry =
                    if (currentExpiry != null) {
                        minOf(currentExpiry, item.expiry)
                    } else {
                        item.expiry
                    }

                val updates = mapOf(
                    "qty" to "${newQty}개",
                    "expiry" to newExpiry.toString()
                )

                inventoryRef.updateChildren(updates)

            } else {

                // 새로운 식품
                val data = mapOf(
                    "name" to item.name,
                    "category" to item.category.name,
                    "expiry" to item.expiry.toString(),
                    "qty" to item.qty,
                    "freshness" to item.freshness,
                    "location" to item.location,
                    "addedDays" to item.addedDays,
                    "isAllergen" to item.isAllergen,
                    "source" to "app",
                    "addedAt" to LocalDate.now().toString(),
                    "fromShop" to false
                )

                inventoryRef.setValue(data)
            }
        }
    }
    /**
     * FoodItem을 Firebase의 MoA-BoB/foodInventory에서 삭제
     */
    private fun deleteInventoryItem(item: FoodItem) {
        foodInventoryDb
            .child(item.name)
            .removeValue()
    }
    private var inventoryListener:       ValueEventListener? = null
    private var scanResultsListener:     ValueEventListener? = null
    private var scanStatusListener:      ValueEventListener? = null
    private var arrivalsListener:        ChildEventListener? = null
    private var householdMemberListener: ValueEventListener? = null
    private var allergenHapticFired = false

    private val listenerStartTime = System.currentTimeMillis()

    init {
        val userId = HouseholdManager.getOrCreateUserId(ctx)
        _state.update { it.copy(myUserId = userId) }

        HouseholdManager.getSavedHouseholdId(ctx)?.let { loadHousehold(it) }

        listenToInventory()
        listenToScanResults()
        listenToScanStatus()
        listenToArrivals()
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

                // MoA-BoB/foodInventory의 각 자식이 FoodItem 하나
                val inventoryItems = snapshot.children.mapNotNull { itemSnapshot ->
                    parseInventoryItem(itemSnapshot)
                }

                // 웹캠에서 현재 인식된 식재료
                val webcam = _state.value.webcamItems

                // Firebase 재고 + 웹캠 재고
                val combined =
                    inventoryItems +
                            webcam.filter { w ->
                                inventoryItems.none { it.name == w.name }
                            }

                // 유통기한 등에 따른 알림 생성
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

        // ⭐ 새 재고 저장소
        foodInventoryDb.addValueEventListener(inventoryListener!!)
    }

    private fun listenToScanResults() {
        scanResultsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val results = snapshot.children.mapNotNull { parseScanResult(it) }
                val mode = _state.value.activeScanMode
                val hasAllergen = results.any { r ->
                    r.isAllergen && _state.value.allergens.any { a -> a.name == r.name }
                }
                val haptic = if (
                    hasAllergen && !allergenHapticFired &&
                    (mode == ScanMode.IN || mode == ScanMode.OUT) &&
                    _state.value.settings.hapticEnabled
                ) {
                    allergenHapticFired = true
                    HapticEvent.ALLERGEN
                } else null

                val ttsText = if (results.isNotEmpty()) {
                    val names = results.take(3).joinToString(", ") { it.name }
                    val more  = if (results.size > 3) " 외 ${results.size - 3}개" else ""
                    "$names${more}가 인식됐습니다"
                } else null

                _state.update { s ->
                    s.copy(
                        scanResults   = results,
                        scanPhase     = if (results.isNotEmpty()) "detected" else s.scanPhase,
                        pendingHaptic = haptic ?: s.pendingHaptic,
                        pendingTts    = ttsText ?: s.pendingTts,
                    )
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("scan_results").addValueEventListener(scanResultsListener!!)
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

    private fun listenToArrivals() {
        arrivalsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                when (snapshot.child("source").getValue(String::class.java)) {
                    "market" -> {
                        val item = parseInventoryItem(snapshot) ?: return
                        autoAddMarketItem(item)
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

    private fun autoAddMarketItem(item: FoodItem) {
        val stockKey = when (item.name) {
            "오이"    -> "oi"
            "사과"    -> "apple"
            "바나나"  -> "banana"
            "오렌지"  -> "orange"
            "브로콜리" -> "broccoli"
            "당근"    -> "carrot"
            "샌드위치" -> "sandwich"
            "피자"    -> "pizza"
            "도넛"    -> "donut"
            "케이크"  -> "cake"
            "핫도그"  -> "hot dog"
            "두부"    -> "tofu"
            else      -> item.name
        }
        val count    = item.qty.filter { it.isDigit() }.toIntOrNull() ?: 1
        val stockRef = Firebase.database.reference.child("stocks").child(stockKey)
        stockRef.get().addOnSuccessListener { snapshot ->
            val current = snapshot.getValue(Int::class.java) ?: 0
            stockRef.setValue(current + count)
        }
    }

    private fun addWebcamItem(item: FoodItem) {
        _state.update { s ->
            if (s.webcamItems.any { it.name == item.name && it.expiry == item.expiry }) return@update s
            val newWebcam  = s.webcamItems + item
            val stocksOnly = s.inventory.filter { inv -> s.webcamItems.none { it.name == inv.name } }
            val combined   = stocksOnly + newWebcam.filter { w -> stocksOnly.none { it.name == w.name } }
            val notifs     = buildNotifications(combined, s.settings, s.allergens)
            s.copy(webcamItems = newWebcam, inventory = combined, notifications = notifs,
                toast = "${item.name} 웹캠으로 인식됐어요 📷")
        }
    }

    fun dismissMarketArrivals() {}
    fun confirmMarketArrivals() {}

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
                .filter { item -> allergens.any { a -> a.name == item.name } }
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
        val name       = snap.child("name").getValue(String::class.java) ?: return@runCatching null
        val catStr     = snap.child("category").getValue(String::class.java) ?: "GRAIN"
        val category   = runCatching { FoodCategory.valueOf(catStr) }.getOrDefault(FoodCategory.GRAIN)
        val expiryStr  = snap.child("expiry").getValue(String::class.java) ?: LocalDate.now().toString()
        val expiry     = runCatching { LocalDate.parse(expiryStr) }.getOrDefault(LocalDate.now())
        val qty        = snap.child("qty").getValue(String::class.java) ?: "1개"
        val freshness  = snap.child("freshness").getValue(Int::class.java)
        val location   = snap.child("location").getValue(String::class.java) ?: "냉장 1칸"
        val addedDays  = snap.child("addedDays").getValue(Int::class.java) ?: 0
        val isAllergen = snap.child("isAllergen").getValue(Boolean::class.java) ?: false
        FoodItem(
            id = snap.key?.hashCode() ?: 0,
            name = name, category = category, expiry = expiry,
            qty = qty, freshness = freshness, location = location,
            addedDays = addedDays, isAllergen = isAllergen,
        )
    }.getOrNull()

    private fun parseScanResult(snap: DataSnapshot): ScanResult? = runCatching {
        val id         = snap.key ?: return@runCatching null
        val name       = snap.child("name").getValue(String::class.java) ?: return@runCatching null
        val confidence = snap.child("confidence").getValue(Double::class.java)
        val isAllergen = snap.child("isAllergen").getValue(Boolean::class.java) ?: false
        val qty        = snap.child("qty").getValue(String::class.java) ?: ""
        ScanResult(id = id, name = name, confidence = confidence, isAllergen = isAllergen, qty = qty)
    }.getOrNull()

    // ── 화면 / 시트 제어 ──────────────────────────────────────────────────────

    fun navigate(screen: Screen) = _state.update { it.copy(screen = screen) }

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

    fun startScan(mode: ScanMode) {
        allergenHapticFired = false
        rootDb.child("current_mode").setValue(mode.name)
        db.child("scan_results").removeValue()
        db.child("scan_status").setValue("scanning")
        _state.update { it.copy(scanSheetOpen = false, activeScanMode = mode, scanResults = emptyList(), scanPhase = "scanning") }
    }

    fun closeScan() {
        rootDb.child("current_mode").setValue("IDLE")
        db.child("scan_status").setValue("idle")
        _state.update { it.copy(activeScanMode = null, scanResults = emptyList(), scanPhase = "idle") }
    }

    fun confirmScan(items: List<ScanResult>) {
        val mode = _state.value.activeScanMode
        db.child("confirm_scan").setValue(mode?.name ?: "IDLE")
        rootDb.child("current_mode").setValue("IDLE")
        db.child("scan_status").setValue("idle")
        val msg = when (mode) {
            ScanMode.IN       -> "${items.size}개 항목이 재고에 추가됐습니다"
            ScanMode.OUT      -> "${items.size}개 항목이 출고됐습니다"
            ScanMode.FRESH    -> "신선도 분석 결과를 확인했습니다"
            ScanMode.ALLERGEN -> "알레르기 식재료가 등록되었습니다"
            null              -> ""
        }
        _state.update { it.copy(activeScanMode = null, toast = msg, scanResults = emptyList(), scanPhase = "idle") }
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
            ScanMode.ALLERGEN -> "알레르기 식재료가 등록되었습니다"
            null              -> ""
        }
        _state.update { it.copy(phoneScanMode = null, phoneScanResults = emptyList(), toast = msg) }
    }

    fun openItem(item: FoodItem) = _state.update { it.copy(openItem = item) }
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
        db.child("allergens").child(id).removeValue()
        _state.update { s ->
            val newAllergens = s.allergens.filter { a -> a.id != id }
            val notifs = buildNotifications(s.inventory, s.settings, newAllergens)
            s.copy(allergens = newAllergens, notifications = notifs)
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        val notifs = buildNotifications(_state.value.inventory, newSettings, _state.value.allergens)
        _state.update { it.copy(settings = newSettings, notifications = notifs) }
    }

    // ── 재고 항목 수정 ────────────────────────────────────────────────────────

    fun updateItemExpiry(itemId: Int, newExpiry: String) {
        val newDate = LocalDate.parse(newExpiry)

        // 현재 재고에서 해당 FoodItem 찾기
        val item = _state.value.inventory.firstOrNull { it.id == itemId }
            ?: return

        // ⭐ MoA-BoB/foodInventory의 유통기한 수정
        foodInventoryDb
            .child(item.name)
            .child("expiry")
            .setValue(newDate.toString())

        // 앱 화면의 상태도 즉시 변경
        _state.update { s ->
            s.copy(
                inventory = s.inventory.map { item ->
                    if (item.id == itemId) item.copy(expiry = newDate) else item
                },
                openItem = s.openItem?.let {
                    if (it.id == itemId) it.copy(expiry = newDate) else it
                },
                toast = "유통기한이 수정됐어요 ✏️",
            )
        }
    }
    fun updateItemQty(itemId: Int, newQty: String) {
        // 현재 재고에서 해당 FoodItem 찾기
        val item = _state.value.inventory.firstOrNull { it.id == itemId }
            ?: return

        // ⭐ MoA-BoB/foodInventory의 해당 식품 수량 수정
        foodInventoryDb
            .child(item.name)
            .child("qty")
            .setValue(newQty)

        // 앱 화면의 상태도 즉시 변경
        _state.update { s ->
            s.copy(
                inventory = s.inventory.map { item ->
                    if (item.id == itemId) item.copy(qty = newQty) else item
                },
                openItem = s.openItem?.let {
                    if (it.id == itemId) it.copy(qty = newQty) else it
                },
                toast = "개수가 수정됐어요 ✏️",
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
                    isAddMode  = true,
                    addedItems = emptyList(),
                    pendingTts = msg,
                    lastVoiceResponse = msg,
                )}
            }

            // ── 재고 추가 모드 종료 ───────────────────────────────────────────
            VoiceCommand.ExitAddMode -> {
                val count = _state.value.addedItems.size
                val msg   = if (count > 0) "총 ${count}개 항목이 추가됐습니다." else "추가 모드를 종료합니다."
                _state.update { it.copy(
                    isAddMode  = false,
                    addedItems = emptyList(),
                    pendingTts = msg,
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
                        LocalDate.now().plusDays(
                            DEFAULT_SHELF_DAYS[cmd.name] ?: 7L
                        )
                    }

                    val newItem = FoodItem(
                        id         = (System.currentTimeMillis() + cmd.name.hashCode()).toInt(),
                        name       = cmd.name,
                        qty        = cmd.qty,
                        category   = FoodCategory.GRAIN,
                        expiry     = expiry,
                        location   = "냉장고",
                        addedDays  = 0,
                        isAllergen = false,
                    )

                    // ⭐ Firebase inventory에 저장
                    saveInventoryItem(newItem)

                    // map에서 이 FoodItem을 반환
                    newItem
                }

                val newInventory  = _state.value.inventory + newItems
                val newAddedItems = _state.value.addedItems + newItems

                val notifs = buildNotifications(
                    newInventory,
                    _state.value.settings,
                    _state.value.allergens
                )

                val names = newItems.joinToString(", ") {
                    "${it.name} ${it.qty}"
                }

                val msg = "${names} 추가됐어요."

                _state.update { s ->
                    s.copy(
                        inventory         = newInventory,
                        addedItems        = newAddedItems,
                        notifications     = notifs,
                        pendingTts        = msg,
                        lastVoiceResponse = msg,
                    )
                }
            }

            // ── 음성으로 재고 추가 ────────────────────────────────────────────
            is VoiceCommand.AddFood -> {
                val expiry = if (command.expiry != null) {
                    runCatching { LocalDate.parse(command.expiry) }.getOrNull()
                        ?: LocalDate.now().plusDays(7)
                } else {
                    // 식약처 로컬 기본값 — expiry 없이 말한 경우 (신선식품)
                    val days = DEFAULT_SHELF_DAYS[command.name] ?: 7L
                    LocalDate.now().plusDays(days)
                }

                val newItem = FoodItem(
                    id        = System.currentTimeMillis().toInt(),
                    name      = command.name,
                    qty       = command.qty,
                    category  = FoodCategory.GRAIN,
                    expiry    = expiry,
                    location  = "냉장고",
                    addedDays = 0,
                    isAllergen = false,
                )

                // ⭐ Firebase inventory에 저장
                saveInventoryItem(newItem)

                val newAddedItems = _state.value.addedItems + newItem
                val newInventory  = _state.value.inventory + newItem
                val notifs        = buildNotifications(newInventory, _state.value.settings, _state.value.allergens)

                val expiryText  = "${expiry.monthValue}월 ${expiry.dayOfMonth}일"
                val sourceLabel = if (command.expiry == null) " (기본값)" else ""
                val msg = "${command.name} ${command.qty}, 소비기한 $expiryText${sourceLabel} 추가됐어요."

                _state.update { s -> s.copy(
                    inventory         = newInventory,
                    addedItems        = newAddedItems,
                    notifications     = notifs,
                    pendingTts        = msg,
                    lastVoiceResponse = msg,
                )}
            }

            // ── 유통기한 조회 ─────────────────────────────────────────────────
            is VoiceCommand.ExpiryQuery -> {
                val item = inv.firstOrNull { it.name == command.foodName }
                val msg = when {
                    item == null        -> "${command.foodName}은 냉장고에 등록되지 않았습니다."
                    item.expiry == null -> "${item.name}의 유통기한 정보가 없습니다."
                    else -> {
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
                }
                _state.update { it.copy(pendingTts = msg, lastVoiceResponse = msg) }
            }

            is VoiceCommand.SearchFood -> {
                val items = inv.filter { it.name == command.foodName }
                val msg = if (items.isEmpty()) {
                    "아니요, 냉장고에 ${command.foodName}이 없습니다."
                } else {
                    val qtyText = items.firstOrNull()?.qty ?: "1개"
                    "네, 냉장고에 ${command.foodName}이 ${qtyText} 있습니다."
                }
                _state.update { it.copy(pendingTts = msg, lastVoiceResponse = msg) }
            }

            is VoiceCommand.ExpiringSoon -> {
                val days      = command.days
                val threshold = LocalDate.now().plusDays(days.toLong())
                val expiring  = inv
                    .filter { it.expiry != null && !it.expiry.isAfter(threshold) }
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
                    val msg = if (command.qty != null) {
                        "${item.name} ${command.qty}개를 삭제할까요?"
                    } else {
                        "${item.name}을 삭제할까요?"
                    }

                    _state.update {
                        it.copy(
                            pendingDeleteItem = item,
                            pendingDeleteQty = command.qty,
                            pendingTts = msg,
                            lastVoiceResponse = msg
                        )
                    }
                } else {
                    val msg = "${command.foodName}은 냉장고에 없습니다."
                    _state.update {
                        it.copy(
                            pendingTts = msg,
                            lastVoiceResponse = msg
                        )
                    }
                }
            }
            VoiceCommand.Confirm -> {
                val state = _state.value
                val item = state.pendingDeleteItem ?: return
                val deleteQty = state.pendingDeleteQty

                // 현재 재고 수량 숫자로 변환
                val currentQty = item.qty
                    .filter { it.isDigit() }
                    .toIntOrNull() ?: 1

                // 수량을 지정하지 않았으면 전체 삭제
                if (deleteQty == null) {
                    deleteInventoryItem(item)

                    val msg = "${item.name}을 삭제했습니다."

                    _state.update { s ->
                        s.copy(
                            inventory = s.inventory.filter { it.id != item.id },
                            webcamItems = s.webcamItems.filter { it.id != item.id },
                            pendingDeleteItem = null,
                            pendingDeleteQty = null,
                            pendingTts = msg,
                            lastVoiceResponse = msg,
                            toast = "${item.name} 삭제됨 🗑️",
                        )
                    }

                    return
                }

                // 삭제 후 남는 수량 계산
                val remainingQty = currentQty - deleteQty

                if (remainingQty <= 0) {
                    // 전부 삭제되는 경우 → Firebase 항목 자체 삭제
                    deleteInventoryItem(item)

                    val msg = "${item.name} ${currentQty}개를 삭제했습니다."

                    _state.update { s ->
                        s.copy(
                            inventory = s.inventory.filter { it.id != item.id },
                            webcamItems = s.webcamItems.filter { it.id != item.id },
                            pendingDeleteItem = null,
                            pendingDeleteQty = null,
                            pendingTts = msg,
                            lastVoiceResponse = msg,
                            toast = "${item.name} 삭제됨 🗑️",
                        )
                    }
                } else {
                    // 일부만 삭제 → 남은 수량으로 Firebase 업데이트
                    val newQty = "${remainingQty}개"

                    foodInventoryDb
                        .child(item.name)
                        .child("qty")
                        .setValue(newQty)

                    val updatedItem = item.copy(qty = newQty)

                    val msg = "${item.name} ${deleteQty}개를 삭제했습니다. ${remainingQty}개 남았습니다."

                    _state.update { s ->
                        s.copy(
                            inventory = s.inventory.map {
                                if (it.id == item.id) updatedItem else it
                            },
                            webcamItems = s.webcamItems.map {
                                if (it.id == item.id) updatedItem else it
                            },
                            pendingDeleteItem = null,
                            pendingDeleteQty = null,
                            pendingTts = msg,
                            lastVoiceResponse = msg,
                            toast = "${item.name} ${deleteQty}개 삭제됨",
                        )
                    }
                }
            }
            VoiceCommand.Cancel -> {
                val msg = "취소했습니다."
                _state.update { it.copy(pendingDeleteItem = null, pendingTts = msg, lastVoiceResponse = msg) }
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
        inventoryListener?.let  { Firebase.database.reference.child("stocks").removeEventListener(it) }
        scanResultsListener?.let { db.child("scan_results").removeEventListener(it) }
        scanStatusListener?.let  { db.child("scan_status").removeEventListener(it) }
        arrivalsListener?.let    { db.child("inventory").removeEventListener(it) }
        _state.value.household?.id?.let { hid ->
            householdMemberListener?.let { HouseholdManager.removeListener(hid, it) }
        }
    }
}
