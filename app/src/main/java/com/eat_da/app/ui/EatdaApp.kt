package com.eatda.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eatda.app.data.model.ScanMode
import com.eatda.app.ui.components.*
import com.eatda.app.ui.screens.*
import com.eatda.app.ui.theme.*
import com.eatda.app.util.HapticManager
import com.eatda.app.util.TtsService
import com.eatda.app.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EatdaApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = LocalContext.current
    val ttsPlaying by TtsService.isPlaying.collectAsStateWithLifecycle()

    // 시스템 뒤로가기 처리
    BackHandler(
        enabled = state.screen == Screen.RECIPE_DETAIL
    ) {
        vm.closeRecipe()
    }


    val colors = when {
        settings.colorBlindMode -> ColorBlindEatdaColors
        settings.a11yMode       -> A11yEatdaColors
        else                    -> DefaultEatdaColors
    }
    val sizes = buildSizes(settings)

    val appFontScale = when {
        settings.a11yMode -> 18f / 14f
        else -> when (settings.fontScale) {
            FontScale.NORMAL -> 1.00f
            FontScale.LARGE  -> 17f / 14f
            FontScale.XLARGE -> 20f / 14f
        }
    }
    val baseDensity = LocalDensity.current

    val scanSheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val voiceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val itemSheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * appFontScale,
        )
    ) {

        // 진동 + TTS 경고 처리
        LaunchedEffect(state.pendingHaptic) {
            state.pendingHaptic?.let { event ->
                if (settings.hapticEnabled) {
                    when (event) {
                        HapticEvent.ALLERGEN  -> HapticManager.allergenAlert(context)
                        HapticEvent.EXPIRY    -> HapticManager.expiryWarning(context)
                        HapticEvent.FRESHNESS -> HapticManager.freshnessAlert(context)
                    }
                }
                if (settings.ttsEnabled) {
                    val msg = when (event) {
                        HapticEvent.ALLERGEN  -> "알레르기 식재료가 감지됐습니다. 즉시 확인하세요."
                        HapticEvent.EXPIRY    -> "유통기한이 임박한 식재료가 있습니다."
                        HapticEvent.FRESHNESS -> "신선도가 낮은 식재료가 있습니다."
                    }
                    TtsService.speak(context, msg)
                }
                vm.clearHaptic()
            }
        }

        // 스캔 결과 TTS 읽기
        LaunchedEffect(state.pendingTts) {
            state.pendingTts?.let { text ->
                if (settings.ttsEnabled) TtsService.speak(context, text)
                vm.clearTts()
            }
        }

        // FAB을 바텀 네비 위에 오버레이하기 위해 Box로 감쌈
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Column(modifier = Modifier.fillMaxSize()) {
                EatdaTopBar(
                    colors = colors,
                    hasNotif = state.notifications.any { it.urgent },
                    onNotif = { vm.navigate(Screen.NOTIFICATIONS) },
                    onProfile = { vm.navigate(Screen.MYPAGE) },
                    profileActive = state.screen == Screen.MYPAGE,
                )
                AppScreenContent(
                    modifier = Modifier.weight(1f),
                    state = state,
                    colors = colors,
                    sizes = sizes,
                    ttsPlaying = ttsPlaying,
                    vm = vm,
                )

                EatdaBottomNav(
                    colors = colors,
                    sizes = sizes,
                    currentScreen = state.screen,
                    onNav = { screen ->
                        if (state.activeScanMode != null) vm.closeScan()
                        if (state.phoneScanMode != null) vm.closePhoneScan()

                        if (screen == Screen.RECIPES) {
                            vm.openRecipesFromNavigation()
                        } else {
                            vm.navigate(screen)
                        }
                    },
                    onScan = vm::openScanSheet,
                )

                Box(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.width(110.dp).height(4.dp)) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = DefaultEatdaColors.borderStrong.copy(alpha = 0.6f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                            )
                        }
                    }
                }
            }

            // 음성 어시스턴트 FAB — 스캔/음성 오버레이 열려있을 때는 숨김
            if (!state.voiceOverlayOpen && state.activeScanMode == null) {
                VoiceFab(
                    colors   = colors,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 76.dp),  // 바텀 네비 위
                    onClick  = vm::openVoice,
                )
            }
        }

        if (state.scanSheetOpen) {
            ScanBottomSheet(
                colors = colors,
                sheetState = scanSheetState,
                onPick = vm::startScan,
                onDismiss = vm::closeScanSheet,
            )
        }

        state.openItem?.let { item ->
            ItemDetailSheet(
                colors         = colors,
                item           = item,
                sheetState     = itemSheetState,
                onDismiss      = vm::closeItem,
                onUpdateExpiry = { newExpiry -> vm.updateItemExpiry(item.id, newExpiry) },
                onUpdateQty    = { newQty    -> vm.updateItemQty(item.id, newQty) },
                onRecipeRecommend = { foodName ->
                    vm.openRecipeRecommendations(foodName)
                },
            )
        }

        if (state.voiceOverlayOpen) {
            VoiceOverlaySheet(
                colors             = colors,
                sheetState         = voiceSheetState,
                inventory          = state.inventory,
                pendingDeleteItem  = state.pendingDeleteItem,
                lastVoiceResponse  = state.lastVoiceResponse,
                isAddMode          = state.isAddMode,
                addedItems         = state.addedItems,
                onCommand          = vm::handleVoiceCommand,
                onDismiss          = vm::closeVoice,
            )
        }

    } // CompositionLocalProvider
}

private fun buildSizes(settings: AppSettings): EatdaSizes {
    if (settings.a11yMode) return A11yEatdaSizes
    return when (settings.fontScale) {
        FontScale.NORMAL -> DefaultEatdaSizes
        FontScale.LARGE  -> EatdaSizes(fontBase = 17, fontH = 25, radius = 16.dp, radiusSm = 10.dp, touchTarget = 48.dp)
        FontScale.XLARGE -> EatdaSizes(fontBase = 20, fontH = 28, radius = 14.dp, radiusSm = 9.dp,  touchTarget = 52.dp)
    }
}

@Composable
private fun AppScreenContent(
    modifier: Modifier = Modifier,
    state: AppState,
    colors: EatdaColors,
    sizes: EatdaSizes,
    ttsPlaying: Boolean,
    vm: AppViewModel,
) {
    Box(modifier = modifier) {
        AnimatedContent(
            targetState = state.screen,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.HOME -> HomeScreen(
                    colors         = colors,
                    sizes          = sizes,
                    inventory      = state.inventory,
                    recipes = state.recipes,
                    simplifiedHome = state.settings.simplifiedHome,
                    onOpenItem     = vm::openItem,
                    onGoNotif      = { vm.navigate(Screen.NOTIFICATIONS) },
                    onGoRecipes    = { vm.navigate(Screen.RECIPES) },
                    onGoInv        = { vm.navigate(Screen.INVENTORY) },
                    onOpenVoice    = vm::openVoice,
                    marketArrivals  = state.marketArrivals,
                    onDismissMarket = vm::dismissMarketArrivals,
                    onConfirmMarket = vm::confirmMarketArrivals,
                )
                Screen.INVENTORY -> InventoryScreen(
                    colors = colors, sizes = sizes,
                    inventory = state.inventory,
                    onOpenItem = vm::openItem,
                )
                Screen.NOTIFICATIONS -> NotificationsScreen(
                    colors = colors,
                    sizes = sizes,
                    notifications = state.notifications,
                )
                Screen.RECIPES -> RecipesScreen(
                    colors = colors,
                    sizes = sizes,
                    inventory = state.inventory,
                    recipes = state.recipes,
                    onRecipeClick = { recipe -> vm.openRecipe(recipe) },
                    recipeFocusIngredient = state.recipeFocusIngredient,
                )
                Screen.RECIPE_DETAIL -> {
                    state.selectedRecipe?.let { recipe ->
                        RecipeDetailScreen(
                            colors = colors,
                            sizes = sizes,
                            recipe = recipe,
                            onBack = { vm.closeRecipe() },
                        )
                    }
                }
                Screen.MYPAGE -> MyPageScreen(
                    colors           = colors,
                    sizes            = sizes,
                    allergens        = state.allergens,
                    onRemoveAllergen = vm::removeAllergen,
                    onScanAllergen   = { vm.startScan(ScanMode.ALLERGEN) },
                    onGoHousehold    = { vm.navigate(Screen.HOUSEHOLD) },
                )
                Screen.SETTINGS -> SettingsScreen(
                    colors = colors, sizes = sizes,
                    settings = state.settings,
                    onUpdateSettings = vm::updateSettings,
                    onGoMyPage = { vm.navigate(Screen.MYPAGE) },
                )
                Screen.HOUSEHOLD -> HouseholdScreen(
                    colors            = colors,
                    household         = state.household,
                    members           = state.householdMembers,
                    myUserId          = state.myUserId,
                    onCreateHousehold = vm::createHousehold,
                    onJoinHousehold   = vm::joinHousehold,
                    onLeaveHousehold  = vm::leaveHousehold,
                    onBack            = { vm.navigate(Screen.MYPAGE) },
                )
            }
        }

        AnimatedVisibility(
            visible = state.activeScanMode != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            state.activeScanMode?.let { mode ->
                ScanView(
                    mode = mode,
                    colors = colors,
                    allergens = state.allergens,
                    scanResults = state.scanResults,
                    scanPhase = state.scanPhase,
                    onClose = vm::closeScan,
                    onConfirm = vm::confirmScan,
                )
            }
        }

        AnimatedVisibility(
            visible = state.phoneScanMode != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            state.phoneScanMode?.let { mode ->
                CameraScanView(
                    mode = mode,
                    colors = colors,
                    results = state.phoneScanResults,
                    onAddResults = vm::addPhoneScanResults,
                    onClose = vm::closePhoneScan,
                    onConfirm = vm::confirmPhoneScan,
                )
            }
        }

        // TTS 재생 중 인디케이터
        AnimatedVisibility(
            visible = ttsPlaying,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        ) {
            TtsPlayingBar(onStop = { TtsService.stop() })
        }

        AnimatedVisibility(
            visible = state.toast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        ) {
            state.toast?.let { msg ->
                EatdaToast(msg)
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2200)
                    vm.dismissToast()
                }
            }
        }
    }
}

@Composable
private fun TtsPlayingBar(onStop: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ttsWave")
    val heights = (0..2).map { i ->
        transition.animateFloat(
            initialValue = 4f, targetValue = 16f,
            animationSpec = infiniteRepeatable(
                tween(400, delayMillis = i * 120, easing = LinearEasing),
                RepeatMode.Reverse,
            ),
            label = "bar$i",
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF1F1B16))
            .clickable(onClick = onStop)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            heights.forEach { h ->
                val height by h
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(height.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.85f)),
                )
            }
        }
        Text("음성 재생 중", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        EatdaIcon(EatdaIcons.Close, tint = Color.White.copy(alpha = 0.6f), size = 13.dp)
    }
}
