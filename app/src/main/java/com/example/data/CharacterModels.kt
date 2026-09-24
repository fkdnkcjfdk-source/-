package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChallengeType {
    NONE,
    SINGLE_RUN_SCORE,
    SINGLE_RUN_JUMPS,
    SINGLE_RUN_COINS_NO_BOARD,
    SINGLE_RUN_SECONDS,
    LIFETIME_HIGH_SCORE
}

data class CharacterDef(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val titleAr: String,
    val titleEn: String,
    val costCoins: Int,
    val challengeType: ChallengeType,
    val challengeTarget: Int,
    val challengeDescAr: String,
    val challengeDescEn: String,
    val perkDescAr: String,
    val perkDescEn: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val avatarEmoji: String,
    val boardDurationBonus: Float = 1.0f,
    val magnetRadiusBonus: Float = 1.0f,
    val hasDoubleJump: Boolean = false,
    val startsWithShield: Boolean = false,
    val scoreMultiplierBonus: Float = 1.0f
)

object CharacterCatalog {
    val JAKE = CharacterDef(
        id = "JAKE",
        nameAr = "جاك",
        nameEn = "Jake",
        titleAr = "عداء المترو الشجاع",
        titleEn = "The Subway Legend",
        costCoins = 0,
        challengeType = ChallengeType.NONE,
        challengeTarget = 0,
        challengeDescAr = "مفتوح تلقائياً لجميع اللاعبين",
        challengeDescEn = "Unlocked by default",
        perkDescAr = "متوازن وسريع الحركة على السكك",
        perkDescEn = "Balanced and agile runner",
        primaryColorHex = 0xFFFF3D00, // Vibrant orange/red
        secondaryColorHex = 0xFF2979FF, // Denim blue
        avatarEmoji = "🧢"
    )

    val TRICKY = CharacterDef(
        id = "TRICKY",
        nameAr = "تريكي",
        nameEn = "Tricky",
        titleAr = "أميرة التزلج البهلواني",
        titleEn = "Skate Prodigy",
        costCoins = 250,
        challengeType = ChallengeType.SINGLE_RUN_SCORE,
        challengeTarget = 1500,
        challengeDescAr = "حقق 1,500 نقطة في جولة واحدة",
        challengeDescEn = "Score 1,500 points in a single run",
        perkDescAr = "زيادة مدة الزلاجة بنسبة +25%",
        perkDescEn = "+25% Hoverboard duration",
        primaryColorHex = 0xFFE91E63, // Hot Pink
        secondaryColorHex = 0xFF00E5FF, // Cyan
        avatarEmoji = "🛹",
        boardDurationBonus = 1.25f
    )

    val FRESH = CharacterDef(
        id = "FRESH",
        nameAr = "فريش",
        nameEn = "Fresh",
        titleAr = "عازف إيقاعات الهيب هوب",
        titleEn = "Urban Beatmaster",
        costCoins = 500,
        challengeType = ChallengeType.SINGLE_RUN_JUMPS,
        challengeTarget = 20,
        challengeDescAr = "اقفز 20 قفزة في جولة واحدة",
        challengeDescEn = "Perform 20 jumps in a single run",
        perkDescAr = "مغناطيس العملات يجذب من مسافة أوسع (+30%)",
        perkDescEn = "+30% Magnet coin pull radius",
        primaryColorHex = 0xFF00E676, // Bright Neon Green
        secondaryColorHex = 0xFF7C4DFF, // Purple
        avatarEmoji = "🎧",
        magnetRadiusBonus = 1.30f
    )

    val NINJA = CharacterDef(
        id = "NINJA",
        nameAr = "نينجا",
        nameEn = "Ninja",
        titleAr = "شبح قطارات الليل",
        titleEn = "Shadow Sprinter",
        costCoins = 1000,
        challengeType = ChallengeType.SINGLE_RUN_COINS_NO_BOARD,
        challengeTarget = 50,
        challengeDescAr = "اجمع 50 عملة في جولة دون استخدام الزلاجة",
        challengeDescEn = "Collect 50 coins in a run without hoverboard",
        perkDescAr = "قفزة مزدوجة فريدة في الهواء (Double Jump)!",
        perkDescEn = "Double Jump mid-air capability!",
        primaryColorHex = 0xFF1A237E, // Midnight Navy
        secondaryColorHex = 0xFFFFD600, // Gold Yellow
        avatarEmoji = "🥷",
        hasDoubleJump = true
    )

    val CYBORG = CharacterDef(
        id = "CYBORG",
        nameAr = "سايبورغ",
        nameEn = "Cyborg 2099",
        titleAr = "متزلج المستقبل السيبراني",
        titleEn = "Cybernetic Runner",
        costCoins = 2000,
        challengeType = ChallengeType.SINGLE_RUN_SECONDS,
        challengeTarget = 60,
        challengeDescAr = "اصمد لمدة 60 ثانية متواصلة في جولة واحدة",
        challengeDescEn = "Survive 60 seconds in a single run",
        perkDescAr = "درع حماية فوري لمدة 5 ثوانٍ عند بدء كل جولة",
        perkDescEn = "Free 5s shield aura at the start of each run",
        primaryColorHex = 0xFF00B0FF, // Electric Blue
        secondaryColorHex = 0xFFFF0055, // Laser Magenta
        avatarEmoji = "🤖",
        startsWithShield = true
    )

    val GOLDEN_KING = CharacterDef(
        id = "GOLDEN_KING",
        nameAr = "الملك الذهبي",
        nameEn = "Golden King",
        titleAr = "سيد أنفاق الذهب الأسطوري",
        titleEn = "Subway Royalty",
        costCoins = 5000,
        challengeType = ChallengeType.LIFETIME_HIGH_SCORE,
        challengeTarget = 5000,
        challengeDescAr = "حقق رقماً قياسياً إجمالياً قدره 5,000 نقطة",
        challengeDescEn = "Reach 5,000 points high score",
        perkDescAr = "مضاعف نقاط دائم 1.5x على كافة النقاط!",
        perkDescEn = "Permanent 1.5x score multiplier on all runs!",
        primaryColorHex = 0xFFFFD700, // Pure Gold
        secondaryColorHex = 0xFF6200EA, // Imperial Violet
        avatarEmoji = "👑",
        scoreMultiplierBonus = 1.5f
    )

    val all: List<CharacterDef> = listOf(JAKE, TRICKY, FRESH, NINJA, CYBORG, GOLDEN_KING)

    fun getById(id: String): CharacterDef = all.find { it.id == id } ?: JAKE
}

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val totalCoins: Int = 100, // 100 welcome coins!
    val highScore: Int = 0,
    val selectedCharacterId: String = "JAKE",
    val keysCount: Int = 3,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val touchControlsEnabled: Boolean = true,
    val totalRuns: Int = 0,
    val totalDistance: Long = 0L
)

@Entity(tableName = "character_progress")
data class CharacterProgressEntity(
    @PrimaryKey val characterId: String,
    val isUnlocked: Boolean,
    val currentChallengeProgress: Int = 0,
    val unlockedAt: Long = 0L
)

@Entity(tableName = "run_history")
data class RunHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val coins: Int,
    val distance: Int,
    val characterId: String,
    val timestamp: Long = System.currentTimeMillis()
)
