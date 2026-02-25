package com.bhan796.anagramarena.online

// 0x00000000 = transparent. ARGB packed int = solid color.
typealias PixelFrame = Array<IntArray> // [48 rows][32 cols]

data class AvatarAnimation(val frames: List<PixelFrame>, val fps: Int)

enum class AvatarState { IDLE, BATTLE, ATTACK, VICTORY, DEFEAT }

data class AvatarDefinition(
    val id: String,
    val name: String,
    val rarity: String,
    val animations: Map<AvatarState, AvatarAnimation>
)
