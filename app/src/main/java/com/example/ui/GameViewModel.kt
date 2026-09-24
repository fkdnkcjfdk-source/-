package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.AppDatabase
import com.example.data.CharacterCardState
import com.example.data.CharacterCatalog
import com.example.data.CharacterDef
import com.example.data.CharacterProgressEntity
import com.example.data.GameRepository
import com.example.data.PlayerProfileEntity
import com.example.data.RunHistoryEntity
import com.example.game.GameEngine
import com.example.game.GamePlayState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RunSummary(
    val score: Int,
    val coins: Int,
    val distance: Int,
    val isNewHighScore: Boolean,
    val unlockedCharacters: List<String>
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = GameRepository(database.gameDao())
    val soundManager = SoundManager(application)
    val gameEngine = GameEngine(soundManager)

    val gamePlayState: StateFlow<GamePlayState> = gameEngine.state

    val playerProfile: StateFlow<PlayerProfileEntity> = repository.playerProfile
        .combine(MutableStateFlow(Unit)) { p, _ -> p ?: PlayerProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfileEntity())

    val topRuns: StateFlow<List<RunHistoryEntity>> = repository.topRuns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _characterProgressList = repository.allCharacterProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val characterCards: StateFlow<List<CharacterCardState>> = combine(
        playerProfile,
        _characterProgressList
    ) { profile, progressList ->
        val progressMap = progressList.associateBy { it.characterId }
        CharacterCatalog.all.map { def ->
            val progress = progressMap[def.id]
            val isUnlocked = progress?.isUnlocked == true || def.id == CharacterCatalog.JAKE.id
            val currentProg = progress?.currentChallengeProgress ?: 0
            val isTargetReached = def.challengeTarget > 0 && currentProg >= def.challengeTarget
            val isSelected = (profile.selectedCharacterId == def.id)
            val canAfford = profile.totalCoins >= def.costCoins

            CharacterCardState(
                def = def,
                isUnlocked = isUnlocked,
                isSelected = isSelected,
                challengeProgress = currentProg,
                isChallengeCompleted = isTargetReached,
                canAffordWithCoins = canAfford
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCharacter: StateFlow<CharacterDef> = playerProfile
        .combine(MutableStateFlow(Unit)) { profile, _ ->
            CharacterCatalog.getById(profile.selectedCharacterId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CharacterCatalog.JAKE)

    private val _isShopOpen = MutableStateFlow(false)
    val isShopOpen: StateFlow<Boolean> = _isShopOpen.asStateFlow()

    private val _isLeaderboardOpen = MutableStateFlow(false)
    val isLeaderboardOpen: StateFlow<Boolean> = _isLeaderboardOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _runSummary = MutableStateFlow<RunSummary?>(null)
    val runSummary: StateFlow<RunSummary?> = _runSummary.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
        viewModelScope.launch {
            playerProfile.collect { profile ->
                soundManager.soundEnabled = profile.soundEnabled
                soundManager.hapticEnabled = profile.hapticEnabled
            }
        }
    }

    fun startGame() {
        _runSummary.value = null
        _isShopOpen.value = false
        val char = selectedCharacter.value
        gameEngine.startRun(char)
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val delta = (now - lastTime) / 1_000_000_000f
                lastTime = now

                val current = gameEngine.state.value
                if (current.isRunning && !current.isPaused && !current.isGameOver) {
                    gameEngine.update(delta)
                } else if (current.isGameOver && _runSummary.value == null) {
                    // Run finished! Process results
                    handleGameOver(current)
                    break
                }
                delay(16) // ~60 FPS
            }
        }
    }

    private suspend fun handleGameOver(state: GamePlayState) {
        val (isNewHigh, unlockedNames) = repository.recordRunResults(
            score = state.score,
            coinsEarned = state.coins,
            distance = state.distance,
            characterId = state.character.id,
            jumpsCount = state.jumpsCount,
            usedHoverboard = state.usedHoverboardThisRun,
            runDurationSeconds = state.runSeconds.toInt()
        )

        if (unlockedNames.isNotEmpty()) {
            soundManager.playUnlockSuccess()
        }

        _runSummary.value = RunSummary(
            score = state.score,
            coins = state.coins,
            distance = state.distance,
            isNewHighScore = isNewHigh,
            unlockedCharacters = unlockedNames
        )
    }

    fun moveLeft() = gameEngine.moveLeft()
    fun moveRight() = gameEngine.moveRight()
    fun jump() = gameEngine.jump()
    fun roll() = gameEngine.roll()
    fun activateHoverboard() = gameEngine.activateHoverboard()
    fun pause() = gameEngine.pause()
    fun resume() = gameEngine.resume()

    fun unlockCharacterWithCoins(charId: String) {
        viewModelScope.launch {
            val success = repository.unlockWithCoins(charId)
            if (success) {
                soundManager.playUnlockSuccess()
            }
        }
    }

    fun claimChallengeCharacter(charId: String) {
        viewModelScope.launch {
            val success = repository.claimChallengeReward(charId)
            if (success) {
                soundManager.playUnlockSuccess()
            }
        }
    }

    fun selectCharacter(charId: String) {
        viewModelScope.launch {
            repository.selectCharacter(charId)
            soundManager.playCoin()
        }
    }

    fun addTestCoins(amount: Int = 300) {
        viewModelScope.launch {
            repository.addTestCoins(amount)
            soundManager.playCoin()
        }
    }

    fun openShop() { _isShopOpen.value = true }
    fun closeShop() { _isShopOpen.value = false }

    fun openLeaderboard() { _isLeaderboardOpen.value = true }
    fun closeLeaderboard() { _isLeaderboardOpen.value = false }

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSetting("sound", enabled)
        }
    }

    fun toggleHaptic(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSetting("haptic", enabled)
        }
    }

    fun toggleTouchControls(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSetting("touchControls", enabled)
        }
    }

    fun dismissRunSummary() {
        _runSummary.value = null
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}
