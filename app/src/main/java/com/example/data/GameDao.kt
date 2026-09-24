package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getPlayerProfile(): Flow<PlayerProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPlayerProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM character_progress")
    fun getAllCharacterProgress(): Flow<List<CharacterProgressEntity>>

    @Query("SELECT * FROM character_progress WHERE characterId = :id LIMIT 1")
    suspend fun getCharacterProgressDirect(id: String): CharacterProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCharacterProgress(progress: CharacterProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialCharacters(list: List<CharacterProgressEntity>)

    @Query("SELECT * FROM run_history ORDER BY score DESC LIMIT :limit")
    fun getTopRuns(limit: Int = 10): Flow<List<RunHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunHistoryEntity)

    @Query("UPDATE player_profile SET totalCoins = totalCoins + :amount WHERE id = 1")
    suspend fun addCoins(amount: Int)

    @Query("UPDATE player_profile SET totalCoins = totalCoins - :amount WHERE id = 1 AND totalCoins >= :amount")
    suspend fun spendCoins(amount: Int): Int

    @Query("UPDATE player_profile SET selectedCharacterId = :charId WHERE id = 1")
    suspend fun setSelectedCharacter(charId: String)
}
