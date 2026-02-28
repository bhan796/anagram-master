package com.bhan796.anagramarena.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bhan796.anagramarena.online.CosmeticCatalog
import com.bhan796.anagramarena.online.CosmeticItem
import com.bhan796.anagramarena.ui.theme.ColorBackground
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.ColorWhite
import com.bhan796.anagramarena.viewmodel.ShopViewModel

private data class CarouselEntry(
    val item: CosmeticItem,
    val isWinner: Boolean
)

@Composable
fun ChestOpenModal(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit,
    onEquip: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var carouselItems by remember { mutableStateOf<List<CarouselEntry>>(emptyList()) }
    var viewportWidthDp by remember { mutableStateOf(360f) }
    val density = LocalDensity.current
    val offset = remember { Animatable(0f) }

    val won = state.chestResult
    LaunchedEffect(won?.id, viewportWidthDp) {
        if (won == null) return@LaunchedEffect
        val fillers = CosmeticCatalog.items
        val entries = buildList {
            repeat(35) { add(CarouselEntry(fillers[it % fillers.size], isWinner = false)) }
            add(CarouselEntry(won, isWinner = true))
            repeat(6) { add(CarouselEntry(fillers[(it + 8) % fillers.size], isWinner = false)) }
        }
        carouselItems = entries
        val winIndex = entries.indexOfFirst { it.isWinner }.coerceAtLeast(0)
        val targetOffset = winIndex * 88f - (viewportWidthDp / 2f - 44f)
        offset.snapTo(0f)
        offset.animateTo(targetOffset, animationSpec = tween(3500, easing = FastOutSlowInEasing))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBackground)
                .padding(16.dp)
        ) {
            if (won == null) {
                Text("Opening chest...", color = ColorWhite, style = MaterialTheme.typography.headlineSmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CHEST OPEN", color = com.bhan796.anagramarena.ui.theme.ColorGold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .onSizeChanged { size ->
                                viewportWidthDp = with(density) { size.width.toDp().value }
                            }
                            .clipToBounds()
                            .background(ColorSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(start = (-offset.value).dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            carouselItems.forEach { entry ->
                                val item = entry.item
                                val rarityColor = CosmeticCatalog.getRarityColor(item.rarity)
                                Box(
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(120.dp)
                                        .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                                        .border(if (entry.isWinner) 4.dp else 3.dp, rarityColor, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.labelSmall, color = ColorWhite)
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(2.dp, CosmeticCatalog.getRarityColor(won.rarity), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(won.name, color = CosmeticCatalog.getRarityColor(won.rarity), style = MaterialTheme.typography.headlineSmall)
                            Text(won.rarity.uppercase(), style = MaterialTheme.typography.labelMedium, color = ColorWhite)
                        }
                    }
                    ArcadeButton("EQUIP NOW", onClick = { onEquip(won.id) }, accentColor = com.bhan796.anagramarena.ui.theme.ColorGold)
                    ArcadeButton("CLOSE", onClick = onDismiss)
                }
            }
        }
    }
}
