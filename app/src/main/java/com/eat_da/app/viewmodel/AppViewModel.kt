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
    private val ctx    get() = getApplication<Application>()

    private var inventoryListener:         ValueEventListener? = null
    private var scanResultsListener:       ValueEventListener? = null
    private var scanStatusListener:        ValueEventListener? = null
    private var arrivalsListener:          ChildEventListener? = null
    private var pendingDeliveryListener:   ValueEventListener? = null
    private var householdMemberListener:   ValueEventListener? = null
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
        listenToPendingDelivery()
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
                        val name      = child.child("name").getValue(String::class.java) ?: return@runCatching null
                        val catStr    = child.child("category").getValue(String::class.java) ?: "GRAIN"
                        val expiryStr = child.child("expiry").getValue(String::class.java) ?: LocalDate.now().toString()
                        val expiry    = runCatching { LocalDate.parse(expiryStr) }.getOrDefault(LocalDate.now())
                        val qty       = child.child("qty").getValue(String::class.java) ?: "1개"
                        val location  = child.child("location").getValue(String::class.java) ?: "냉장고"
                        val allergen  = child.child("isAllergen").getValue(Boolean::class.java) ?: false
                        FoodItem(
                            id         = child.key?.hashCode() ?: name.hashCode(),
                            name       = name,
                            category   = runCatching { FoodCategory.valueOf(catStr) }.getOrDefault(FoodCategory.GRAIN),
                            expiry     = expiry,
                            qty        = qty,
                            location   = location,
                            addedDays  = 0,
                            isAllergen = allergen,
                        )
                    }.getOrNull()
                }
                val webcam   = _state.value.webcamItems
                val combined = dbItems + webcam.filter { w -> dbItems.none { it.name == w.name } }
                val notifs   = buildNotifications(combined, _state.value.settings, _state.value.allergens)
                _state.update { it.copy(inventory = combined, notifications = notifs, firebaseConnected = true) }
            }
            override fun onCancelled(error: DatabaseError) {
                _state.update { it.copy(firebaseConnected = false) }
            }
        }
        rootDb.child("MoA-BoB").child("foodInventory").addValueEventListener(inventoryListener!!)
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

    private fun autoAddMarketItem(item: FoodItem, showToast: Boolean = true) {
        val key = "${item.name}_${System.currentTimeMillis()}"
        rootDb.child("MoA-BoB").child("foodInventory").child(key).setValue(
            mapOf(
                "name"       to item.name,
                "category"   to item.category.name,
                "expiry"     to item.expiry.toString(),
                "qty"        to item.qty,
                "location"   to item.location,
                "isAllergen" to item.isAllergen,
            )
        )
        if (showToast) _state.update { it.copy(toast = "${item.name} 냉장고에 추가됐어요 🧊") }
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
                            id         = key.hashCode(),
                            name       = name,
                            category   = runCatching { FoodCategory.valueOf(catStr) }.getOrDefault(FoodCategory.GRAIN),
                            expiry     = expiry,
                            qty        = o.optString("qty", "1개"),
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
                            id         = child.key?.hashCode() ?: name.hashCode(),
                            name       = name,
                            category   = runCatching { FoodCategory.valueOf(catStr) }.getOrDefault(FoodCategory.GRAIN),
                            expiry     = expiry,
                            qty        = child.child("qty").getValue(String::class.java) ?: "1개",
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
                        LocalDate.now().plusDays(DEFAULT_SHELF_DAYS[cmd.name] ?: 7L)
                    }
                    FoodItem(
                        id         = (System.currentTimeMillis() + cmd.name.hashCode()).toInt(),
                        name       = cmd.name,
                        qty        = cmd.qty,
                        category   = FoodCategory.GRAIN,
                        expiry     = expiry,
                        location   = "냉장고",
                        addedDays  = 0,
                        isAllergen = false,
                    )
                }
                val newInventory  = _state.value.inventory + newItems
                val newAddedItems = _state.value.addedItems + newItems
                val notifs        = buildNotifications(newInventory, _state.value.settings, _state.value.allergens)
                val names         = newItems.joinToString(", ") { "${it.name} ${it.qty}" }
                val msg           = "${names} 추가됐어요."
                _state.update { s -> s.copy(
                    inventory         = newInventory,
                    addedItems        = newAddedItems,
                    notifications     = notifs,
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
                    item == null        -> "${command.foodName}${command.foodName.josa("은", "는")} 냉장고에 등록되지 않았습니다."
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
                    val msg = if (command.qty != null)
                        "${item.name} ${command.qty}${item.name.josa("을", "를")} 삭제할까요?"
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
                val item = _state.value.pendingDeleteItem ?: return
                val msg = "${item.name}${item.name.josa("을", "를")} 삭제했습니다."
                _state.update { s -> s.copy(
                    inventory         = s.inventory.filter { it.id != item.id },
                    webcamItems       = s.webcamItems.filter { it.id != item.id },
                    pendingDeleteItem  = null,
                    pendingDeleteQty   = null,
                    pendingTts        = msg,
                    lastVoiceResponse = msg,
                    toast             = "${item.name} 삭제됨 🗑️",
                )}
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
        inventoryListener?.let         { rootDb.child("MoA-BoB").child("foodInventory").removeEventListener(it) }
        pendingDeliveryListener?.let   { rootDb.child("MoA-BoB").child("pendingDelivery").removeEventListener(it) }
        scanResultsListener?.let { db.child("scan_results").removeEventListener(it) }
        scanStatusListener?.let  { db.child("scan_status").removeEventListener(it) }
        arrivalsListener?.let    { db.child("inventory").removeEventListener(it) }
        _state.value.household?.id?.let { hid ->
            householdMemberListener?.let { HouseholdManager.removeListener(hid, it) }
        }
    }
}

// 받침 유무에 따라 한국어 조사 선택 ("이/가", "을/를", "은/는" 등)
private fun String.josa(받침있음: String, 받침없음: String): String {
    if (isEmpty()) return 받침있음
    val code = last().code
    return if (code in 0xAC00..0xD7A3 && (code - 0xAC00) % 28 != 0) 받침있음 else 받침없음
}
