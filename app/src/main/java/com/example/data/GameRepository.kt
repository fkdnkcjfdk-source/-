package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.max

data class CharacterCardState(
    val def: CharacterDef,
    val isUnlocked: Boolean,
    val isSelected: Boolean,
    val challengeProgress: Int,
    val isChallengeCompleted: Boolean,
    val canAffordWithCoins: Boolean
)

class GameRepository(private val gameDao: GameDao) {

    val playerProfile: Flow<PlayerProfileEntity?> = gameDao.getPlayerProfile()
    val allCharacterProgress: Flow<List<CharacterProgressEntity>> = gameDao.getAllCharacterProgress()
    val topRuns: Flow<List<RunHistoryEntity>> = gameDao.getTopRuns()

    suspend fun ensureInitialized() {
        val currentProfile = gameDao.getPlayerProfile().firstOrNull()
        if (currentProfile == null) {
            gameDao.setPlayerProfile(
                PlayerProfileEntity(
                    id = 1,
                    totalCoins = 150, // Starter coins
                    highScore = 0,
                    selectedCharacterId = CharacterCatalog.JAKE.id,
                    keysCount = 3
                )
            )
        }

        // Initialize character unlock records if not present
        val initialList = CharacterCatalog.all.map { def ->
            CharacterProgressEntity(
                characterId = def.id,
                isUnlocked = (def.id == CharacterCatalog.JAKE.id), // Only Jake is unlocked at start!
                currentChallengeProgress = 0,
                unlockedAt = if (def.id == CharacterCatalog.JAKE.id) System.currentTimeMillis() else 0L
            )
        }
        gameDao.insertInitialCharacters(initialList)
    }

    suspend fun selectCharacter(charId: String): Boolean {
        val progress = gameDao.getCharacterProgressDirect(charId)
        if (progress?.isUnlocked == true) {
            gameDao.setSelectedCharacter(charId)
            return true
        }
        return false
    }

    suspend fun unlockWithCoins(charId: String): Boolean {
        val def = CharacterCatalog.getById(charId)
        val profile = gameDao.getPlayerProfile().firstOrNull() ?: return false
        if (profile.totalCoins < def.costCoins) return false

        val rowsUpdated = gameDao.spendCoins(def.costCoins)
        if (rowsUpdated > 0) {
            gameDao.upsertCharacterProgress(
                CharacterProgressEntity(
                    characterId = charId,
                    isUnlocked = true,
                    currentChallengeProgress = def.challengeTarget,
                    unlockedAt = System.currentTimeMillis()
                )
            )
            // Automatically select freshly unlocked character
            gameDao.setSelectedCharacter(charId)
            return true
        }
        return false
    }

    suspend fun claimChallengeReward(charId: String): Boolean {
        val def = CharacterCatalog.getById(charId)
        val progress = gameDao.getCharacterProgressDirect(charId) ?: return false
        if (progress.currentChallengeProgress >= def.challengeTarget && !progress.isUnlocked) {
            gameDao.upsertCharacterProgress(
                progress.copy(
                    isUnlocked = true,
                    unlockedAt = System.currentTimeMillis()
                )
            )
            gameDao.setSelectedCharacter(charId)
            return true
        }
        return false
    }

    suspend fun recordRunResults(
        score: Int,
        coinsEarned: Int,
        distance: Int,
        characterId: String,
        jumpsCount: Int,
        usedHoverboard: Boolean,
        runDurationSeconds: Int
    ): Pair<Boolean, List<String>> {
        // 1. Add run to history
        gameDao.insertRun(
            RunHistoryEntity(
                score = score,
                coins = coinsEarned,
                distance = distance,
                characterId = characterId
            )
        )

        // 2. Update player profile stats
        val profile = gameDao.getPlayerProfile().firstOrNull() ?: PlayerProfileEntity()
        val isNewHighScore = score > profile.highScore
        val newHighScore = max(profile.highScore, score)
        val updatedCoins = profile.totalCoins + coinsEarned
        val updatedTotalRuns = profile.totalRuns + 1
        val updatedDistance = profile.totalDistance + distance

        gameDao.setPlayerProfile(
            profile.copy(
                totalCoins = updatedCoins,
                highScore = newHighScore,
                totalRuns = updatedTotalRuns,
                totalDistance = updatedDistance
            )
        )

        // 3. Update character challenge progress & detect newly completed challenges
        val newlyCompletedOrUnlocked = mutableListOf<String>()

        for (def in CharacterCatalog.all) {
            val progress = gameDao.getCharacterProgressDirect(def.id) ?: continue
            if (progress.isUnlocked) continue

            var newProgress = progress.currentChallengeProgress
            var achieved = false

            when (def.challengeType) {
                ChallengeType.SINGLE_RUN_SCORE -> {
                    if (score > newProgress) {
                        newProgress = score.coerceAtMost(def.challengeTarget)
                    }
                    if (score >= def.challengeTarget) achieved = true
                }
                ChallengeType.SINGLE_RUN_JUMPS -> {
                    if (jumpsCount > newProgress) {
                        newProgress = jumpsCount.coerceAtMost(def.challengeTarget)
                    }
                    if (jumpsCount >= def.challengeTarget) achieved = true
                }
                ChallengeType.SINGLE_RUN_COINS_NO_BOARD -> {
                    if (!usedHoverboard && coinsEarned > newProgress) {
                        newProgress = coinsEarned.coerceAtMost(def.challengeTarget)
                    }
                    if (!usedHoverboard && coinsEarned >= def.challengeTarget) achieved = true
                }
                ChallengeType.SINGLE_RUN_SECONDS -> {
                    if (runDurationSeconds > newProgress) {
                        newProgress = runDurationSeconds.coerceAtMost(def.challengeTarget)
                    }
                    if (runDurationSeconds >= def.challengeTarget) achieved = true
                }
                ChallengeType.LIFETIME_HIGH_SCORE -> {
                    if (newHighScore > newProgress) {
                        newProgress = newHighScore.coerceAtMost(def.challengeTarget)
                    }
                    if (newHighScore >= def.challengeTarget) achieved = true
                }
                ChallengeType.NONE -> {}
            }

            if (achieved) {
                // Automatically unlock or mark ready to claim! Let's auto-unlock to make the reward immediate and exciting!
                gameDao.upsertCharacterProgress(
                    progress.copy(
                        isUnlocked = true,
                        currentChallengeProgress = def.challengeTarget,
                        unlockedAt = System.currentTimeMillis()
                    )
                )
                newlyCompletedOrUnlocked.add(def.nameAr)
            } else if (newProgress != progress.currentChallengeProgress) {
                gameDao.upsertCharacterProgress(
                    progress.copy(currentChallengeProgress = newProgress)
                )
            }
        }

        return Pair(isNewHighScore, newlyCompletedOrUnlocked)
    }

    suspend fun addTestCoins(amount: Int) {
        gameDao.addCoins(amount)
    }

    suspend fun toggleSetting(setting: String, enabled: Boolean) {
        val profile = gameDao.getPlayerProfile().firstOrNull() ?: return
        val updated = when (setting) {
            "sound" -> profile.copy(soundEnabled = enabled)
            "haptic" -> profile.copy(hapticEnabled = enabled)
            "touchControls" -> profile.copy(touchControlsEnabled = enabled)
            else -> profile
        }
        gameDao.setPlayerProfile(updated)
    }
}
