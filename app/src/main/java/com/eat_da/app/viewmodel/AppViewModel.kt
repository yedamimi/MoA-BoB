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
    val inventory: List<FoodItem> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipe: Recipe? = null,
    val scanResults: List<ScanResult> = emptyList(),
    val scanPhase: String = "idle",
    val firebaseConnected: Boolean = false,
    val pendingHaptic: HapticEvent? = null,
    val pendingTts: String? = null,
    val marketArrivals: List<FoodItem> = emptyList(),
    val pendingDeleteItem: FoodItem? = null,   // 음성 삭제 확인 대기 아이템
    val household: Household? = null,
    val householdMembers: List<HouseholdMember> = emptyList(),
    val myUserId: String = "",
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val rootDb = Firebase.database.reference
    private val db     = Firebase.database.reference.child("eatda")
    private val ctx    get() = getApplication<Application>()

    private var inventoryListener:    ValueEventListener? = null
    private var scanResultsListener:  ValueEventListener? = null
    private var scanStatusListener:   ValueEventListener? = null
    private var marketListener:       ChildEventListener? = null
    private var householdMemberListener: ValueEventListener? = null
    private var allergenHapticFired = false

    init {
        val userId = HouseholdManager.getOrCreateUserId(ctx)
        _state.update { it.copy(myUserId = userId) }

        // 저장된 가구 ID가 있으면 자동 로드
        HouseholdManager.getSavedHouseholdId(ctx)?.let { loadHousehold(it) }

        listenToInventory()
        listenToScanResults()
        listenToScanStatus()
        listenToMarketArrivals()
        loadRecipes()
    }

    // ── 레시피 로드 ───────────────────────────────────────────────────────────

    private fun loadRecipes() {
        viewModelScope.launch {
            val recipes = RecipeApiService.fetchRecipes()
            _state.update { it.copy(recipes = recipes) }
            println("레시피 개수: ${recipes.size}")
        }
    }

    // ── Firebase 리스너 ───────────────────────────────────────────────────────

    private fun listenToInventory() {
        inventoryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { stock ->
                    val count = stock.getValue(Int::class.java) ?: 0
                    if (count <= 0) return@mapNotNull null

                    val (displayName, category) = when (stock.key) {
                        "oi"        -> Pair("오이",    FoodCategory.VEGETABLE)
                        "apple"     -> Pair("사과",    FoodCategory.FRUIT)
                        "banana"    -> Pair("바나나",  FoodCategory.FRUIT)
                        "orange"    -> Pair("오렌지",  FoodCategory.FRUIT)
                        "broccoli"  -> Pair("브로콜리", FoodCategory.VEGETABLE)
                        "carrot"    -> Pair("당근",    FoodCategory.VEGETABLE)
                        "sandwich"  -> Pair("샌드위치", FoodCategory.GRAIN)
                        "pizza"     -> Pair("피자",    FoodCategory.GRAIN)
                        "donut"     -> Pair("도넛",    FoodCategory.GRAIN)
                        "cake"      -> Pair("케이크",  FoodCategory.GRAIN)
                        "hot dog"   -> Pair("핫도그",  FoodCategory.GRAIN)
                        "tofu"      -> Pair("두부",    FoodCategory.GRAIN)
                        else        -> Pair(stock.key ?: "", FoodCategory.GRAIN)
                    }

                    FoodItem(
                        id        = stock.key?.hashCode() ?: 0,
                        name      = displayName,
                        category  = category,
                        expiry    = LocalDate.now().plusDays(7),
                        qty       = "${count}개",
                        location  = "냉장고",
                        addedDays = 0,
                        isAllergen = false,
                    )
                }

                val notifs = buildNotifications(items, _state.value.settings, _state.value.allergens)
                _state.update { it.copy(inventory = items, notifications = notifs, firebaseConnected = true) }
            }

            override fun onCancelled(error: DatabaseError) {
                _state.update { it.copy(firebaseConnected = false) }
            }
        }
        Firebase.database.reference.child("stocks").addValueEventListener(inventoryListener!!)
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
                    "${names}${more}가 인식됐습니다"
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

    // ── 마켓 구매 감지 (eatda/inventory, source=="market") ────────────────────

    private fun listenToMarketArrivals() {
        marketListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val source = snapshot.child("source").getValue(String::class.java)
                if (source != "market") return

                val item = parseInventoryItem(snapshot) ?: return

                _state.update { s ->
                    if (s.marketArrivals.any { it.id == item.id }) s
                    else s.copy(marketArrivals = s.marketArrivals + item)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("inventory").addChildEventListener(marketListener!!)
    }

    fun dismissMarketArrivals() {
        _state.update { it.copy(marketArrivals = emptyList()) }
    }

    fun confirmMarketArrivals() {
        val arrivals = _state.value.marketArrivals

        arrivals.forEach { item ->
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

            val count = item.qty.filter { it.isDigit() }.toIntOrNull() ?: 1

            val stockRef = Firebase.database.reference.child("stocks").child(stockKey)
            stockRef.get().addOnSuccessListener { snapshot ->
                val currentCount = snapshot.getValue(Int::class.java) ?: 0
                stockRef.setValue(currentCount + count)
            }
        }

        _state.update { s ->
            s.copy(
                inventory      = s.inventory + s.marketArrivals,
                marketArrivals = emptyList(),
                toast          = "냉장고 재고가 업데이트됐어요 🧊",
            )
        }
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
                    id = "exp_dday_${item.id}", type = NotifType.EXPIRY_SOON,
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
    fun closeVoice() = _state.update { it.copy(voiceOverlayOpen = false) }

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

                // 구성원 실시간 구독
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
                _state.update { it.copy(pendingTts = msg) }
            }

            is VoiceCommand.SearchFood -> {
                val items = inv.filter { it.name == command.foodName }
                val msg = if (items.isEmpty()) {
                    "${command.foodName}은 냉장고에 없습니다."
                } else {
                    val total = items.sumOf { it.qty.filter { c -> c.isDigit() }.toIntOrNull() ?: 1 }
                    "네, ${command.foodName}이 ${total}개 등록되어 있습니다."
                }
                _state.update { it.copy(pendingTts = msg) }
            }

            is VoiceCommand.ExpiringSoon -> {
                val threshold = LocalDate.now().plusDays(3)
                val expiring  = inv
                    .filter { it.expiry != null && !it.expiry.isAfter(threshold) }
                    .sortedBy { it.expiry }
                val msg = when (expiring.size) {
                    0    -> "3일 이내 유통기한이 만료되는 식품은 없습니다."
                    1    -> "3일 이내 유통기한이 만료되는 식품은 ${expiring[0].name}입니다."
                    else -> {
                        val front = expiring.dropLast(1).joinToString(", ") { it.name }
                        "3일 이내 유통기한이 만료되는 식품은 ${front}과 ${expiring.last().name}입니다."
                    }
                }
                _state.update { it.copy(pendingTts = msg) }
            }

            is VoiceCommand.DeleteFood -> {
                val item = inv.firstOrNull { it.name == command.foodName }
                if (item != null) {
                    _state.update { it.copy(
                        pendingDeleteItem = item,
                        pendingTts        = "${item.name}을 삭제할까요?",
                    )}
                } else {
                    _state.update { it.copy(pendingTts = "${command.foodName}은 냉장고에 없습니다.") }
                }
            }

            is VoiceCommand.Confirm -> {
                val item = _state.value.pendingDeleteItem ?: return
                _state.update { s -> s.copy(
                    inventory         = s.inventory.filter { it.id != item.id },
                    pendingDeleteItem  = null,
                    pendingTts        = "${item.name}을 삭제했습니다.",
                    toast             = "${item.name} 삭제됨 🗑️",
                )}
            }

            is VoiceCommand.Cancel -> {
                _state.update { it.copy(
                    pendingDeleteItem = null,
                    pendingTts        = "취소했습니다.",
                )}
            }

            is VoiceCommand.Unknown -> {
                _state.update { it.copy(pendingTts = "죄송합니다. 다시 말씀해주세요.") }
            }
        }
    }

    // ── 이벤트 소비 ───────────────────────────────────────────────────────────

    fun clearHaptic() = _state.update { it.copy(pendingHaptic = null) }
    fun clearTts()    = _state.update { it.copy(pendingTts = null) }

    // ── 리소스 해제 ───────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        inventoryListener?.let  { Firebase.database.reference.child("stocks").removeEventListener(it) }
        scanResultsListener?.let { db.child("scan_results").removeEventListener(it) }
        scanStatusListener?.let  { db.child("scan_status").removeEventListener(it) }
        marketListener?.let      { db.child("inventory").removeEventListener(it) }
        _state.value.household?.id?.let { hid ->
            householdMemberListener?.let { HouseholdManager.removeListener(hid, it) }
        }
    }
}
