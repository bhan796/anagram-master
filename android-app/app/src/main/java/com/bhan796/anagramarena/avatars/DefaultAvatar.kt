package com.bhan796.anagramarena.avatars

import com.bhan796.anagramarena.online.AvatarAnimation
import com.bhan796.anagramarena.online.AvatarDefinition
import com.bhan796.anagramarena.online.AvatarState
import com.bhan796.anagramarena.online.PixelFrame

private object Palette {
    const val TRANSPARENT = 0x00000000
    const val ARMOR = 0xFF00F5FF.toInt()
    const val SHADOW = 0xFF0088AA.toInt()
    const val VISOR = 0xFF0A0A18.toInt()
    const val SKIN = 0xFFFFB38A.toInt()
    const val SUIT = 0xFF1C1C3A.toInt()
    const val TRIM = 0xFFFFD700.toInt()
}

private fun emptyFrame(): PixelFrame = Array(48) { IntArray(32) { Palette.TRANSPARENT } }

private fun paintRect(frame: PixelFrame, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
    for (y in y1.coerceAtLeast(0)..y2.coerceAtMost(47)) {
        for (x in x1.coerceAtLeast(0)..x2.coerceAtMost(31)) {
            frame[y][x] = color
        }
    }
}

private fun shift(frame: PixelFrame, dx: Int, dy: Int): PixelFrame {
    val out = emptyFrame()
    for (y in 0 until 48) {
        for (x in 0 until 32) {
            val color = frame[y][x]
            if (color == Palette.TRANSPARENT) continue
            val nx = x + dx
            val ny = y + dy
            if (nx in 0..31 && ny in 0..47) {
                out[ny][nx] = color
            }
        }
    }
    return out
}

private fun clone(frame: PixelFrame): PixelFrame = Array(frame.size) { y -> frame[y].clone() }

private fun buildBody(
    crouch: Int = 0,
    lean: Int = 0,
    leftArmLift: Int = 0,
    rightArmLift: Int = 0,
    rightPunch: Int = 0,
    rightArmBack: Int = 0,
    headDown: Int = 0,
    slump: Int = 0,
    rightArmUp: Boolean = false,
    wideFeet: Boolean = false,
    handsLow: Int = 0
): PixelFrame {
    val f = emptyFrame()
    val ox = lean
    val oy = crouch

    paintRect(f, 10 + ox, 0 + oy + headDown, 21 + ox, 7 + oy + headDown, Palette.ARMOR)
    paintRect(f, 11 + ox, 1 + oy + headDown, 20 + ox, 6 + oy + headDown, Palette.SHADOW)
    paintRect(f, 13 + ox, 3 + oy + headDown, 18 + ox, 4 + oy + headDown, Palette.VISOR)
    paintRect(f, 14 + ox, 3 + oy + headDown, 14 + ox, 3 + oy + headDown, Palette.ARMOR)
    paintRect(f, 17 + ox, 3 + oy + headDown, 17 + ox, 3 + oy + headDown, Palette.ARMOR)

    paintRect(f, 13 + ox, 8 + oy, 18 + ox, 9 + oy, Palette.SKIN)

    paintRect(f, 10 + ox, 10 + oy, 21 + ox, 27 + oy, Palette.ARMOR)
    paintRect(f, 11 + ox, 12 + oy, 20 + ox, 25 + oy, Palette.SHADOW)
    paintRect(f, 10 + ox, 12 + oy, 10 + ox, 25 + oy, Palette.SUIT)
    paintRect(f, 21 + ox, 12 + oy, 21 + ox, 25 + oy, Palette.SUIT)

    paintRect(f, 2 + ox, 13 + oy - leftArmLift + slump, 9 + ox, 22 + oy - leftArmLift + slump + handsLow, Palette.ARMOR)
    paintRect(f, 3 + ox, 14 + oy - leftArmLift + slump, 8 + ox, 21 + oy - leftArmLift + slump + handsLow, Palette.SHADOW)
    paintRect(f, 2 + ox, 22 + oy - leftArmLift + slump + handsLow, 4 + ox, 23 + oy - leftArmLift + slump + handsLow, Palette.TRIM)

    if (rightArmUp) {
        paintRect(f, 19 + ox, 2 + oy, 22 + ox, 18 + oy, Palette.ARMOR)
        paintRect(f, 20 + ox, 3 + oy, 21 + ox, 16 + oy, Palette.SHADOW)
        paintRect(f, 19 + ox, 1 + oy, 22 + ox, 2 + oy, Palette.TRIM)
    } else {
        paintRect(
            f,
            22 + ox + rightArmBack,
            13 + oy - rightArmLift + slump,
            29 + ox + rightPunch,
            22 + oy - rightArmLift + slump + handsLow,
            Palette.ARMOR
        )
        paintRect(
            f,
            23 + ox + rightArmBack,
            14 + oy - rightArmLift + slump,
            28 + ox + rightPunch,
            21 + oy - rightArmLift + slump + handsLow,
            Palette.SHADOW
        )
        paintRect(
            f,
            27 + ox + rightPunch,
            22 + oy - rightArmLift + slump + handsLow,
            29 + ox + rightPunch,
            23 + oy - rightArmLift + slump + handsLow,
            Palette.TRIM
        )
    }

    paintRect(f, 11 + ox, 28 + oy, 20 + ox, 34 + oy, Palette.ARMOR)
    paintRect(f, 12 + ox, 29 + oy, 19 + ox, 33 + oy, Palette.SHADOW)

    paintRect(f, 11 + ox, 35 + oy, 15 + ox, 42 + oy, Palette.SUIT)
    paintRect(f, 16 + ox, 35 + oy, 20 + ox, 42 + oy, Palette.SUIT)
    paintRect(f, 12 + ox, 38 + oy, 14 + ox, 39 + oy, Palette.ARMOR)
    paintRect(f, 17 + ox, 38 + oy, 19 + ox, 39 + oy, Palette.ARMOR)

    val leftBootStart = if (wideFeet) 9 else 10
    val rightBootEnd = if (wideFeet) 22 else 21
    paintRect(f, leftBootStart + ox, 43 + oy, 15 + ox, 47 + oy, Palette.SHADOW)
    paintRect(f, 16 + ox, 43 + oy, rightBootEnd + ox, 47 + oy, Palette.SHADOW)
    paintRect(f, leftBootStart + ox, 46 + oy, 15 + ox, 47 + oy, Palette.TRIM)
    paintRect(f, 16 + ox, 46 + oy, rightBootEnd + ox, 47 + oy, Palette.TRIM)

    return f
}

private val idle0 = buildBody()
private val idle1 = shift(buildBody(handsLow = 1), 0, 1)
private val idle2 = shift(buildBody(handsLow = 1), 0, 0)

private val battle0 = buildBody(crouch = 2, leftArmLift = 5, rightArmLift = 3)
private val battle1 = shift(buildBody(crouch = 2, leftArmLift = 5, rightArmLift = 3, lean = 1), 1, 0)
private val battle2 = buildBody(crouch = 2, leftArmLift = 3, rightPunch = 1, rightArmLift = 2, lean = 1)
private val battle3 = clone(battle0)

private val attack0 = buildBody(crouch = 2, rightArmBack = -2, lean = -1, leftArmLift = 3)
private val attack1 = buildBody(crouch = 1, rightPunch = 4, lean = 2, leftArmLift = 3)
private val attack2 = clone(battle0)

private val victory0 = buildBody(crouch = 3, wideFeet = true, leftArmLift = 2)
private val victory1 = shift(buildBody(leftArmLift = 4, rightArmLift = 2), 0, -6)
private val victory2 = shift(buildBody(leftArmLift = 4, rightArmUp = true), 0, -4)
private val victory3 = buildBody(crouch = 1, leftArmLift = 2, rightArmLift = 1, wideFeet = true)

private val defeat0 = buildBody(headDown = 2, slump = 2, lean = 1, leftArmLift = -1, rightArmLift = -1)
private val defeat1 = shift(buildBody(headDown = 2, slump = 3, lean = 1, leftArmLift = -1, rightArmLift = -1, handsLow = 1), 0, 1)

val DefaultRookieAvatar = AvatarDefinition(
    id = "default_rookie",
    name = "The Rookie",
    rarity = "default",
    animations = mapOf(
        AvatarState.IDLE to AvatarAnimation(listOf(idle0, idle1, idle2), fps = 6),
        AvatarState.BATTLE to AvatarAnimation(listOf(battle0, battle1, battle2, battle3), fps = 8),
        AvatarState.ATTACK to AvatarAnimation(listOf(attack0, attack1, attack2), fps = 12),
        AvatarState.VICTORY to AvatarAnimation(listOf(victory0, victory1, victory2, victory3), fps = 8),
        AvatarState.DEFEAT to AvatarAnimation(listOf(defeat0, defeat1), fps = 4)
    )
)
