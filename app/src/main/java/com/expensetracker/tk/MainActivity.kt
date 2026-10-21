package com.expensetracker.tk

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.screens.AddEditExpenseScreen
import com.expensetracker.tk.ui.screens.BorrowScreen
import com.expensetracker.tk.ui.screens.HomeScreen
import com.expensetracker.tk.ui.screens.InsightsScreen
import com.expensetracker.tk.ui.screens.LockScreen
import com.expensetracker.tk.ui.screens.OnboardingScreen
import com.expensetracker.tk.ui.screens.SettingsScreen
import com.expensetracker.tk.ui.screens.TransactionsScreen
import com.expensetracker.tk.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    lateinit var state: AppState
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        state = companionState ?: AppState(application as TrackerApp).also { companionState = it }
        when (intent?.action) {
            "com.expensetracker.tk.ADD" -> state.pendingUiAction.value = "add"
            "com.expensetracker.tk.INSIGHTS" -> state.pendingUiAction.value = "insights"
            "com.expensetracker.tk.VOICE" -> state.pendingUiAction.value = "voice"
            "com.expensetracker.tk.HISTORY" -> state.pendingUiAction.value = "history"
        }
        setContent {
            // observe theme so Settings changes apply instantly (was: read once at startup)
            val ui = state.ui.collectAsState().value
            val dark = when (ui.themeMode) {
                1 -> false
                2, 4 -> true
                else -> isSystemInDarkTheme()
            }
            LaunchedEffect(ui.themeMode, dark) {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !dark
            }
            ExpenseTrackerTheme(themeMode = ui.themeMode, accentIndex = ui.accentIndex) {
                Root()
            }
        }
    }

    companion object {
        private var companionState: AppState? = null
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun Root() {
    val activity = LocalContext.current as MainActivity
    val state = activity.state
    val ui = state.ui.collectAsState().value

    // first-run onboarding
    var onboarded = ui.onboarded
    if (!onboarded) {
        OnboardingScreen(state, onDone = { state.setOnboarded(true) })
        return
    }

    // ask notification permission once for new users (Android 13+)
    val notifPermission = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { state.prefs.notifPermissionAsked = true }
    LaunchedEffect(ui.onboarded) {
        if (ui.onboarded && !state.prefs.notifPermissionAsked) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                state.prefs.notifPermissionAsked = true
            }
        }
    }

    // Lock gate: default OFF. Only shows when the user enabled it in Settings and hasn't unlocked this session.
    var lockArmed by remember { mutableStateOf(state.prefs.lockEnabled && state.prefs.lockPinHash.isNotBlank()) }
    var showAdd by remember { mutableStateOf(false) }
    var editExpenseId by remember { mutableStateOf<String?>(null) }
    var editOrigin by remember { mutableStateOf<Rect?>(null) }
    var fabBounds by remember { mutableStateOf<Rect?>(null) }

    val tabs = listOf(
        Tab("home", "Home", Icons.Default.Home),
        Tab("txns", "History", Icons.AutoMirrored.Filled.ReceiptLong),
        Tab("insights", "Insights", Icons.Default.Insights),
        Tab("borrow", "Borrow", Icons.Default.Payments)
    )
    val nav = rememberNavController()
    val backstack by nav.currentBackStackEntryAsState()
    val currentRoute = backstack?.destination?.route
    val tabRoutes = tabs.map { it.route }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = currentRoute in tabRoutes,
                    enter = slideInVertically(tween(250)) { it } + fadeIn(),
                    exit = slideOutVertically(tween(200)) { it } + fadeOut()
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        BottomNavBar(tabs = tabs, currentRoute = currentRoute,
                            onSelect = { route ->
                                nav.navigate(route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            })
                        // centre-docked quick-add FAB, floating over the bar
                        AnimatedVisibility(
                            visible = !showAdd && editExpenseId == null,
                            enter = scaleIn(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 400f)
                            ) + fadeIn(),
                            exit = fadeOut(tween(150)),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            FloatingActionButton(
                                onClick = { editExpenseId = null; editOrigin = fabBounds; showAdd = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(54.dp)
                                    .offset(y = (-24).dp)
                                    .onGloballyPositioned { fabBounds = it.boundsInRoot() }
                            ) {
                                Icon(Icons.Default.Add, "Add expense")
                            }
                        }
                    }
                }
            },
        ) { padding ->
            LaunchedEffect(snackbarHostState) {
                UndoHost.show = { message, undo, dismissed ->
                    snackbarScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message, actionLabel = "Undo", duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) undo() else dismissed()
                    }
                }
            }

            // app shortcuts / quick tile deep actions
            LaunchedEffect(state.pendingUiAction.value) {
                when (state.pendingUiAction.value) {
                    "add" -> { editExpenseId = null; showAdd = true; state.pendingUiAction.value = null }
                    "insights" -> {
                        nav.navigate("insights") {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                        }
                        state.pendingUiAction.value = null
                    }
                    "history" -> {
                        nav.navigate("txns") {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                        }
                        state.pendingUiAction.value = null
                    }
                }
            }
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.padding(padding),
                enterTransition = {
                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(240))
                },
                exitTransition = {
                    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 5 } + fadeOut(tween(180))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(240))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(180))
                }
            ) {
                composable("home") {
                    HomeScreen(
                        state,
                        onAdd = { editExpenseId = null; editOrigin = fabBounds; showAdd = true },
                        onEdit = { id, origin -> editExpenseId = id; editOrigin = origin; showAdd = true },
                        onOpenSettings = { nav.navigate("settings") { launchSingleTop = true } }
                    )
                }
                composable("txns") { TransactionsScreen(state, onEdit = { id, origin -> editExpenseId = id; editOrigin = origin; showAdd = true }) }
                composable("insights") { InsightsScreen(state) }
                composable("borrow") { BorrowScreen(state) }
                composable("settings") {
                    SettingsScreen(state, onLockChanged = { armed -> lockArmed = armed && state.prefs.lockEnabled && state.prefs.lockPinHash.isNotBlank() })
                }
            }
        }

        // add / edit — morphs from the tapped row (container transform)
        AnimatedVisibility(
            visible = showAdd,
            enter = fadeIn(tween(30)),
            exit = fadeOut(tween(30))
        ) {
            AddEditExpenseScreen(
                state = state,
                editId = editExpenseId,
                origin = editOrigin,
                onDismiss = { showAdd = false; editExpenseId = null }
            )
        }

        // optional app lock — only when the user turned it on in Settings
        AnimatedVisibility(
            visible = lockArmed,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            LockScreen(
                state = state,
                onUnlocked = { lockArmed = false }
            )
        }
    }
}


/**
 * Screens call [post] from any (non-composable) callback to show a snackbar with an
 * Undo action. Root wires the actual snackbar host once per composition.
 */
object UndoHost {
    @Volatile
    var show: ((String, () -> Unit, () -> Unit) -> Unit)? = null

    fun post(message: String, undo: () -> Unit = {}, onDismissed: () -> Unit = {}) {
        show?.invoke(message, undo, onDismissed)
    }
}


/** Custom bottom bar with a spring-animated selection pill sliding between tabs. */
@Composable
private fun BottomNavBar(tabs: List<Tab>, currentRoute: String?, onSelect: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
        Column {
            BoxWithConstraints(Modifier.fillMaxWidth().height(66.dp)) {
                val slotW = maxWidth / 5
                val slot = when (currentRoute) {
                    "home" -> 0; "txns" -> 1; "insights" -> 3; "borrow" -> 4; else -> -1
                }
                val pillX by animateDpAsState(
                    targetValue = slotW * slot,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
                    label = "pill"
                )
                if (slot >= 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(15.dp),
                        modifier = Modifier
                            .offset(x = pillX + (slotW - 52.dp) / 2, y = 7.dp)
                            .size(52.dp, 30.dp)
                    ) {}
                }
                Row(Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, t ->
                        // centre gap BEFORE the 3rd tab so the FAB floats between History and Insights
                        if (index == 2) Spacer(Modifier.weight(1f))
                        val selected = currentRoute == t.route
                        val interaction = remember { MutableInteractionSource() }
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(interactionSource = interaction, indication = null) { onSelect(t.route) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                t.icon, t.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                t.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
