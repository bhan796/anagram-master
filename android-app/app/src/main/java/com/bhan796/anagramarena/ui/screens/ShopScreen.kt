package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bhan796.anagramarena.online.CosmeticCatalog
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.ChestOpenModal
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.viewmodel.ShopViewModel

@Composable
fun ShopScreen(navController: NavController, viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

        ArcadeScaffold(contentPadding = PaddingValues(0.dp)) {
        ArcadeBackButton(onClick = { navController.popBackStack() })
        NeonTitle("SHOP")
        Text("RUNES: ${state.runes}", style = MaterialTheme.typography.headlineSmall, color = com.bhan796.anagramarena.ui.theme.ColorGold)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant)
                .border(1.dp, com.bhan796.anagramarena.ui.theme.ColorCyan.copy(alpha = 0.35f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("TREASURE CHEST", style = MaterialTheme.typography.labelLarge)
            Text("Contains a random cosmetic for your display name.", style = MaterialTheme.typography.bodySmall)
            Text("Cost: 200 RUNES", style = MaterialTheme.typography.labelMedium, color = com.bhan796.anagramarena.ui.theme.ColorGold)
            ArcadeButton("PURCHASE", onClick = { viewModel.purchaseChest() }, enabled = state.runes >= 200 && !state.isPurchasing)
            ArcadeButton("OPEN CHEST", onClick = { viewModel.openChest() }, enabled = state.pendingChests > 0, accentColor = com.bhan796.anagramarena.ui.theme.ColorGold)
        }

        Text("MY COSMETICS", style = MaterialTheme.typography.headlineSmall)
        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.inventory) { item ->
                Column(
                    modifier = Modifier
                        .background(com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant)
                        .border(1.dp, CosmeticCatalog.getRarityColor(item.rarity))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(item.name, style = MaterialTheme.typography.labelMedium)
                    Text(item.rarity.uppercase(), style = MaterialTheme.typography.bodySmall, color = CosmeticCatalog.getRarityColor(item.rarity))
                    ArcadeButton(
                        text = if (state.equippedItemId == item.id) "EQUIPPED" else "EQUIP",
                        onClick = { viewModel.equipItem(if (state.equippedItemId == item.id) null else item.id) },
                        enabled = true
                    )
                }
            }
        }

        if (state.chestResult != null) {
            ChestOpenModal(
                viewModel = viewModel,
                onDismiss = { viewModel.clearChestResult() },
                onEquip = {
                    viewModel.equipItem(it)
                    viewModel.clearChestResult()
                }
            )
        }
    }
}
