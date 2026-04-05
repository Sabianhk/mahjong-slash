package com.mahjongslash

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mahjongslash.data.PreferencesManager
import com.mahjongslash.game.audio.AudioManager
import com.mahjongslash.ui.components.TutorialOverlay
import com.mahjongslash.ui.screens.*
import com.mahjongslash.ui.theme.MahjongSlashTheme
import com.mahjongslash.game.engine.MemoryDifficulty
import com.mahjongslash.viewmodel.GameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        // Full immersive mode — hide system bars, reveal on swipe
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val preferencesManager = PreferencesManager(applicationContext)
        val musicManager = AudioManager(applicationContext)

        setContent {
            MahjongSlashTheme {
                val navController = rememberNavController()

                // Switch music based on current screen
                // Music files are optional — resolved by name so build works without them
                val menuMusicRes = remember {
                    applicationContext.resources.getIdentifier("music_menu", "raw", packageName)
                }
                val gameMusicRes = remember {
                    applicationContext.resources.getIdentifier("music_gameplay", "raw", packageName)
                }
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        val route = entry.destination.route ?: return@collect
                        when {
                            route == "game" && gameMusicRes != 0 -> {
                                musicManager.startMusic(gameMusicRes, volume = 0.25f)
                            }
                            route in listOf("menu", "splash", "scores", "settings", "memory_pick") ||
                                route.startsWith("gameover") || route.startsWith("memory_result") -> {
                                if (menuMusicRes != 0) musicManager.startMusic(menuMusicRes, volume = 0.3f)
                                else musicManager.stopMusic()
                            }
                        }
                    }
                }

                // Clean up music on dispose
                DisposableEffect(Unit) {
                    onDispose { musicManager.release() }
                }

                val fadeIn300 = fadeIn(animationSpec = tween(300))
                val fadeOut300 = fadeOut(animationSpec = tween(300))
                val slideInFromRight = slideInHorizontally(tween(300)) { it / 3 } + fadeIn300
                val slideOutToLeft = slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut300
                val slideInFromLeft = slideInHorizontally(tween(300)) { -it / 3 } + fadeIn300
                val slideOutToRight = slideOutHorizontally(tween(300)) { it / 3 } + fadeOut300

                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    enterTransition = { slideInFromRight },
                    exitTransition = { slideOutToLeft },
                    popEnterTransition = { slideInFromLeft },
                    popExitTransition = { slideOutToRight },
                ) {
                    composable(
                        "splash",
                        enterTransition = { EnterTransition.None },
                        exitTransition = { fadeOut300 },
                    ) {
                        SplashScreen(onTap = {
                            navController.navigate("menu") {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }

                    composable("menu") {
                        MainMenuScreen(
                            preferencesManager = preferencesManager,
                            onPlay = {
                                navController.navigate("game") {
                                    popUpTo("menu")
                                }
                            },
                            onMemory = { navController.navigate("memory_pick") },
                            onHighScores = { navController.navigate("scores") },
                            onSettings = { navController.navigate("settings") },
                        )
                    }

                    composable(
                        "game",
                        enterTransition = { fadeIn(tween(400)) },
                        exitTransition = { fadeOut(tween(400)) },
                    ) {
                        val gameViewModel: GameViewModel = viewModel()
                        val hasSeen by preferencesManager.hasSeenTutorial
                            .collectAsState(initial = true) // default true to avoid flash
                        var showTutorial by remember { mutableStateOf(false) }

                        // Show tutorial if first time (once hasSeen loads as false)
                        LaunchedEffect(hasSeen) {
                            if (!hasSeen) {
                                showTutorial = true
                                gameViewModel.pause()
                            }
                        }

                        GameScreen(
                            viewModel = gameViewModel,
                            showPauseOverlay = !showTutorial,
                            onGameOver = { score, tilesCleared, maxCombo, totalSlashes, validSlashes ->
                                // Save score
                                CoroutineScope(Dispatchers.IO).launch {
                                    preferencesManager.saveScore(score)
                                }
                                val accuracy = if (totalSlashes > 0) (validSlashes * 100 / totalSlashes) else 0
                                navController.navigate(
                                    "gameover/$score/$tilesCleared/$maxCombo/$accuracy"
                                ) {
                                    popUpTo("menu")
                                }
                            }
                        )

                        if (showTutorial) {
                            TutorialOverlay(
                                onDismiss = {
                                    showTutorial = false
                                    gameViewModel.resume()
                                    CoroutineScope(Dispatchers.IO).launch {
                                        preferencesManager.setTutorialSeen()
                                    }
                                }
                            )
                        }
                    }

                    composable(
                        "gameover/{score}/{tiles}/{combo}/{accuracy}",
                        enterTransition = { fadeIn(tween(500)) },
                        exitTransition = { fadeOut(tween(300)) },
                        arguments = listOf(
                            navArgument("score") { type = NavType.IntType },
                            navArgument("tiles") { type = NavType.IntType },
                            navArgument("combo") { type = NavType.IntType },
                            navArgument("accuracy") { type = NavType.IntType },
                        )
                    ) { backStackEntry ->
                        val score = backStackEntry.arguments?.getInt("score") ?: 0
                        val tiles = backStackEntry.arguments?.getInt("tiles") ?: 0
                        val combo = backStackEntry.arguments?.getInt("combo") ?: 0
                        val accuracy = backStackEntry.arguments?.getInt("accuracy") ?: 0

                        GameOverScreen(
                            score = score,
                            tilesCleared = tiles,
                            maxCombo = combo,
                            accuracy = accuracy,
                            onPlayAgain = {
                                navController.navigate("game") {
                                    popUpTo("menu")
                                }
                            },
                            onMenu = {
                                navController.navigate("menu") {
                                    popUpTo("menu") { inclusive = true }
                                }
                            },
                        )
                    }

                    composable("memory_pick") {
                        MemoryDifficultyScreen(
                            onSelectDifficulty = { diff ->
                                navController.navigate("memory_game/${diff.name}") {
                                    popUpTo("memory_pick")
                                }
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        "memory_game/{difficulty}",
                        enterTransition = { fadeIn(tween(400)) },
                        exitTransition = { fadeOut(tween(400)) },
                        arguments = listOf(
                            navArgument("difficulty") { type = NavType.StringType },
                        )
                    ) { backStackEntry ->
                        val diffName = backStackEntry.arguments?.getString("difficulty") ?: "EASY"
                        val diff = MemoryDifficulty.valueOf(diffName)
                        MemoryGameScreen(
                            difficulty = diff,
                            onWin = { score, moves, timeMs, difficulty ->
                                navController.navigate(
                                    "memory_result/$score/$moves/$timeMs/${difficulty.name}"
                                ) {
                                    popUpTo("menu")
                                }
                            },
                            onBack = {
                                navController.navigate("menu") {
                                    popUpTo("menu") { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(
                        "memory_result/{score}/{moves}/{time}/{difficulty}",
                        enterTransition = { fadeIn(tween(500)) },
                        exitTransition = { fadeOut(tween(300)) },
                        arguments = listOf(
                            navArgument("score") { type = NavType.IntType },
                            navArgument("moves") { type = NavType.IntType },
                            navArgument("time") { type = NavType.LongType },
                            navArgument("difficulty") { type = NavType.StringType },
                        )
                    ) { backStackEntry ->
                        val score = backStackEntry.arguments?.getInt("score") ?: 0
                        val moves = backStackEntry.arguments?.getInt("moves") ?: 0
                        val timeMs = backStackEntry.arguments?.getLong("time") ?: 0L
                        val diffName = backStackEntry.arguments?.getString("difficulty") ?: "EASY"
                        val diff = MemoryDifficulty.valueOf(diffName)

                        MemoryResultScreen(
                            score = score,
                            moves = moves,
                            timeMs = timeMs,
                            difficulty = diff,
                            onPlayAgain = {
                                navController.navigate("memory_game/${diff.name}") {
                                    popUpTo("menu")
                                }
                            },
                            onMenu = {
                                navController.navigate("menu") {
                                    popUpTo("menu") { inclusive = true }
                                }
                            },
                        )
                    }

                    composable("scores") {
                        HighScoresScreen(
                            preferencesManager = preferencesManager,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            preferencesManager = preferencesManager,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
