package com.bhan796.anagramarena.online

data class CosmeticItem(
    val id: String,
    val name: String,
    val rarity: String,
    val cssClass: String,
    val description: String
)

data class AchievementEntry(
    val id: String,
    val name: String,
    val description: String,
    val tier: String,
    val runesReward: Int,
    val unlocked: Boolean,
    val unlockedAt: String?
)

data class InventoryResponse(
    val items: List<CosmeticItem>,
    val equippedCosmetic: String?,
    val pendingChests: Int,
    val runes: Int
)

data class PlayerRewardsPayload(
    val runesEarned: Int,
    val newAchievements: List<AchievementEntry>
)

data class PurchaseResult(
    val remainingRunes: Int,
    val pendingChests: Int
)