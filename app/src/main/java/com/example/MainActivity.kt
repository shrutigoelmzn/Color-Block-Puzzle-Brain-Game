package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.AppThemeMode
import com.example.domain.model.GameMode
import com.example.ui.achievements.AchievementsScreen
import com.example.ui.daily.DailyChallengeScreen
import com.example.ui.game.GameScreen
import com.example.ui.game.GameViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.stats.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.themes.ThemesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    GAME,
    THEMES,
    DAILY,
    ACHIEVEMENTS,
    STATS
}

class MainActivity : ComponentActivity() {

    private val appUpdateLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        com.example.update.InAppUpdateManager.handleActivityResult(result.resultCode)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.i("MainActivity", "POST_NOTIFICATIONS permission granted")
        } else {
            android.util.Log.d("MainActivity", "POST_NOTIFICATIONS permission denied or dismissed")
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (com.example.util.DeviceUtils.isEmulator()) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize In-App Update Manager
        com.example.update.InAppUpdateManager.initialize(applicationContext)

        // 2. Prioritize immediate UI composition and rendering
        setContent {
            val gameViewModel: GameViewModel = viewModel()
            val themeMode by gameViewModel.themeMode.collectAsState()
            val updateState by com.example.update.InAppUpdateManager.updateState.collectAsState()

            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainNavigation(gameViewModel = gameViewModel)

                        // Floating In-App Update Banner for downloaded / downloading states
                        com.example.ui.update.InAppUpdateFloatingBanner(
                            updateState = updateState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        )

                        // Dialog for available updates
                        val currentUpdate = updateState
                        if (currentUpdate is com.example.update.UpdateUIState.UpdateAvailable) {
                            com.example.ui.update.UpdateAvailableDialog(
                                updateState = currentUpdate,
                                launcher = appUpdateLauncher,
                                onDismiss = { com.example.update.InAppUpdateManager.dismissState() }
                            )
                        }
                    }
                }
            }
        }

        // 3. Initialize external SDKs in background coroutines with staggering to avoid Binder buffer contention (-ENOSPC)
        lifecycleScope.launch(Dispatchers.Default) {
            val isVirtual = com.example.util.DeviceUtils.isEmulator()
            if (isVirtual) {
                android.util.Log.i("MainActivity", "Running in emulator: skipping remote Google Play / Firebase SDK network initialization")
                return@launch
            }

            // Check for in-app updates in background
            delay(1200)
            try {
                com.example.update.InAppUpdateManager.checkForUpdate(this@MainActivity, isManualCheck = false)
            } catch (e: Throwable) {
                android.util.Log.d("MainActivity", "InAppUpdate check note: ${e.message}")
            }

            // Analytics & Crash reporting
            try {
                com.example.analytics.AnalyticsHelper.initialize(applicationContext)
                com.example.crashlytics.CrashReporter.log("MainActivity onCreate")
            } catch (e: Throwable) {
                android.util.Log.d("MainActivity", "Analytics init deferred: ${e.message}")
            }

            // Stagger next service
            delay(500)

            // Mobile Ads SDK (AdMob) - Must be invoked on the Main/UI thread with active Looper
            try {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    com.example.ads.AdManager.initialize(applicationContext)
                }
            } catch (e: Throwable) {
                android.util.Log.d("MainActivity", "AdManager init deferred: ${e.message}")
            }

            delay(400)

            // Google Play Games Services v2
            try {
                com.example.data.PlayGamesManager.initialize(applicationContext)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    com.example.data.PlayGamesManager.checkAuthentication(this@MainActivity)
                }
            } catch (e: Throwable) {
                android.util.Log.d("MainActivity", "PlayGames init deferred: ${e.message}")
            }

            delay(300)

            // Firebase Cloud Messaging (FCM) & Notification permissions
            try {
                com.example.notifications.FCMManager.initialize(applicationContext)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    requestNotificationPermissionIfNeeded()
                }
            } catch (e: Throwable) {
                android.util.Log.d("MainActivity", "FCM init deferred: ${e.message}")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.example.data.PlayGamesManager.setActivity(this)
    }

    override fun onResume() {
        super.onResume()
        com.example.update.InAppUpdateManager.onResume(this, appUpdateLauncher)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.data.PlayGamesManager.setActivity(null)
        com.example.update.InAppUpdateManager.unregisterListener()
    }
}

@Composable
fun MainNavigation(
    gameViewModel: GameViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = currentScreen != Screen.HOME && currentScreen != Screen.GAME) {
        currentScreen = Screen.HOME
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(
                viewModel = gameViewModel,
                onStartGame = { mode ->
                    gameViewModel.startNewGame(mode)
                    currentScreen = Screen.GAME
                },
                onNavigateThemes = { currentScreen = Screen.THEMES },
                onNavigateDaily = { currentScreen = Screen.DAILY },
                onNavigateAchievements = { currentScreen = Screen.ACHIEVEMENTS },
                onNavigateStats = { currentScreen = Screen.STATS }
            )

            Screen.GAME -> GameScreen(
                viewModel = gameViewModel,
                onNavigateBack = { currentScreen = Screen.HOME }
            )

            Screen.THEMES -> ThemesScreen(
                viewModel = gameViewModel,
                onNavigateBack = { currentScreen = Screen.HOME }
            )

            Screen.DAILY -> DailyChallengeScreen(
                viewModel = gameViewModel,
                onStartGame = { mode ->
                    gameViewModel.startNewGame(mode)
                    currentScreen = Screen.GAME
                },
                onNavigateBack = { currentScreen = Screen.HOME }
            )

            Screen.ACHIEVEMENTS -> AchievementsScreen(
                viewModel = gameViewModel,
                onNavigateBack = { currentScreen = Screen.HOME }
            )

            Screen.STATS -> StatsScreen(
                viewModel = gameViewModel,
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
