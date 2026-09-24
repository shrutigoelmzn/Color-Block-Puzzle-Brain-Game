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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

enum class Screen {
    HOME,
    GAME,
    THEMES,
    DAILY,
    ACHIEVEMENTS,
    STATS
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.i("MainActivity", "POST_NOTIFICATIONS permission granted")
        } else {
            android.util.Log.w("MainActivity", "POST_NOTIFICATIONS permission denied")
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase Analytics & Crashlytics
        com.example.analytics.AnalyticsHelper.initialize(this)
        com.example.crashlytics.CrashReporter.log("MainActivity onCreate")

        // Initialize Firebase Cloud Messaging (FCM)
        com.example.notifications.FCMManager.initialize(this)
        requestNotificationPermissionIfNeeded()

        // Initialize Google Play Games Services v2
        com.example.data.PlayGamesManager.initialize(this)
        com.example.data.PlayGamesManager.checkAuthentication(this)

        // Initialize Google Mobile Ads SDK (AdMob)
        com.example.ads.AdManager.initialize(this)

        setContent {
            val gameViewModel: GameViewModel = viewModel()
            val themeMode by gameViewModel.themeMode.collectAsState()
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
                    MainNavigation(gameViewModel = gameViewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.example.data.PlayGamesManager.setActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.data.PlayGamesManager.setActivity(null)
    }
}

@Composable
fun MainNavigation(
    gameViewModel: GameViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = currentScreen != Screen.HOME) {
        if (currentScreen == Screen.GAME) {
            gameViewModel.pauseGame()
        }
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
