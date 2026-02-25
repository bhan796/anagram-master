package com.bhan796.anagramarena.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.avatars.AvatarCatalog
import com.bhan796.anagramarena.online.AvatarState
import kotlinx.coroutines.delay

enum class Facing { LEFT, RIGHT }

@Composable
fun AvatarSprite(
    avatarId: String,
    state: AvatarState,
    scale: Float,
    facing: Facing,
    modifier: Modifier = Modifier
) {
    val avatar = remember(avatarId) { AvatarCatalog.getById(avatarId) }
    val animation = avatar.animations[state] ?: avatar.animations[AvatarState.IDLE] ?: return
    var frameIndex by remember(avatarId, state) { mutableIntStateOf(0) }

    LaunchedEffect(avatarId, state) {
        while (true) {
            delay((1000L / animation.fps.coerceAtLeast(1)))
            frameIndex = (frameIndex + 1) % animation.frames.size.coerceAtLeast(1)
        }
    }

    val density = LocalDensity.current
    Canvas(modifier = modifier.size((32 * scale).dp, (48 * scale).dp)) {
        val frame = animation.frames[frameIndex % animation.frames.size]
        val scalePx = with(density) { scale.dp.toPx() }

        if (facing == Facing.LEFT) {
            scale(scaleX = -1f, scaleY = 1f, pivot = center) {
                for (y in frame.indices) {
                    for (x in frame[y].indices) {
                        val pixel = frame[y][x]
                        if (pixel == 0x00000000) continue
                        drawRect(
                            color = Color(pixel),
                            topLeft = Offset(x * scalePx, y * scalePx),
                            size = Size(scalePx, scalePx)
                        )
                    }
                }
            }
        } else {
            for (y in frame.indices) {
                for (x in frame[y].indices) {
                    val pixel = frame[y][x]
                    if (pixel == 0x00000000) continue
                    drawRect(
                        color = Color(pixel),
                        topLeft = Offset(x * scalePx, y * scalePx),
                        size = Size(scalePx, scalePx)
                    )
                }
            }
        }
    }
}
