package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SubwaySurfersApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val playState by viewModel.gamePlayState.collectAsStateWithLifecycle()
    val playerProfile by viewModel.playerProfile.collectAsStateWithLifecycle()
    val selectedCharacter by viewModel.selectedCharacter.collectAsStateWithLifecycle()
    val characterCards by viewModel.characterCards.collectAsStateWithLifecycle()
    val topRuns by viewModel.topRuns.collectAsStateWithLifecycle()

    val isShopOpen by viewModel.isShopOpen.collectAsStateWithLifecycle()
    val isLeaderboardOpen by viewModel.isLeaderboardOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val runSummary by viewModel.runSummary.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 3D Game Canvas
            GameCanvas(
                state = playState,
                modifier = Modifier.fillMaxSize()
            )

            // 2. In-Game HUD (active when game is running)
            if (playState.isRunning) {
                GameHud(
                    state = playState,
                    onMoveLeft = viewModel::moveLeft,
                    onMoveRight = viewModel::moveRight,
                    onJump = viewModel::jump,
                    onRoll = viewModel::roll,
                    onHoverboard = { viewModel.activateHoverboard() },
                    onPauseToggle = {
                        if (playState.isPaused) viewModel.resume() else viewModel.pause()
                    },
                    showOnScreenButtons = playerProfile.touchControlsEnabled
                )
            }

            // 3. Main Menu Overlay (when not running or at startup)
            if (!playState.isRunning && runSummary == null) {
                MainMenuOverlay(
                    profile = playerProfile,
                    selectedCharacter = selectedCharacter,
                    onStartGame = viewModel::startGame,
                    onOpenShop = viewModel::openShop,
                    onOpenLeaderboard = viewModel::openLeaderboard,
                    onOpenSettings = viewModel::openSettings
                )
            }

            // 4. Game Over Dialog (upon crash)
            runSummary?.let { summary ->
                GameOverDialog(
                    summary = summary,
                    highScore = playerProfile.highScore,
                    onPlayAgain = {
                        viewModel.dismissRunSummary()
                        viewModel.startGame()
                    },
                    onOpenShop = {
                        viewModel.dismissRunSummary()
                        viewModel.openShop()
                    },
                    onBackToMenu = {
                        viewModel.dismissRunSummary()
                    }
                )
            }

            // 5. Character Shop BottomSheet (The Unlock System!)
            if (isShopOpen) {
                CharacterShopSheet(
                    characterCards = characterCards,
                    totalCoins = playerProfile.totalCoins,
                    onSelect = viewModel::selectCharacter,
                    onUnlockWithCoins = viewModel::unlockCharacterWithCoins,
                    onClaimChallenge = viewModel::claimChallengeCharacter,
                    onAddTestCoins = { viewModel.addTestCoins(300) },
                    onDismiss = viewModel::closeShop
                )
            }

            // 6. Leaderboard BottomSheet
            if (isLeaderboardOpen) {
                LeaderboardDialog(
                    runs = topRuns,
                    onDismiss = viewModel::closeLeaderboard
                )
            }

            // 7. Settings & Guide BottomSheet
            if (isSettingsOpen) {
                SettingsDialog(
                    profile = playerProfile,
                    onToggleSound = viewModel::toggleSound,
                    onToggleHaptic = viewModel::toggleHaptic,
                    onToggleTouchControls = viewModel::toggleTouchControls,
                    onDismiss = viewModel::closeSettings
                )
            }
        }
    }
}
