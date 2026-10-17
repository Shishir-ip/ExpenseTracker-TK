package com.expensetracker.tk

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
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

    // Lock gate: default OFF. Only shows when the user enabled it in Settings and hasn't unlocked this session.
    var lockArmed by remember { mutableStateOf(state.prefs.lockEnabled && state.prefs.lockPinHash.isNotBlank()) }
    var showAdd by remember { mutableStateOf(false) }
    var editExpenseId by remember { mutableStateOf<String?>(null) }

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
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            tabs.forEachIndexed { index, t ->
                                if (index == 2) {
                                    // reserved centre slot for the docked FAB
                                    Box(Modifier.weight(1f))
                                }
                                val selected = currentRoute == t.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            nav.navigate(t.route) {
                                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(t.icon, t.label) },
                                    label = { Text(t.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
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
                                onClick = { editExpenseId = null; showAdd = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(54.dp)
                                    .offset(y = (-24).dp)
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
                            message, actionLabel = "Undo", duration = SnackbarDuration.Short
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
                }
            }
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 12 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 12 } }
            ) {
                composable("home") {
                    HomeScreen(
                        state,
                        onAdd = { editExpenseId = null; showAdd = true },
                        onEdit = { id -> editExpenseId = id; showAdd = true },
                        onOpenSettings = { nav.navigate("settings") { launchSingleTop = true } }
                    )
                }
                composable("txns") { TransactionsScreen(state, onEdit = { id -> editExpenseId = id; showAdd = true }) }
                composable("insights") { InsightsScreen(state) }
                composable("borrow") { BorrowScreen(state) }
                composable("settings") {
                    SettingsScreen(state, onLockChanged = { armed -> lockArmed = armed && state.prefs.lockEnabled && state.prefs.lockPinHash.isNotBlank() })
                }
            }
        }

        // add / edit sheet
        AnimatedVisibility(
            visible = showAdd,
            enter = slideInVertically(tween(320)) { it } + fadeIn(),
            exit = slideOutVertically(tween(260)) { it } + fadeOut()
        ) {
            AddEditExpenseScreen(
                state = state,
                editId = editExpenseId,
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
