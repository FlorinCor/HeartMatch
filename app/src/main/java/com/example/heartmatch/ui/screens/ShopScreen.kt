package com.example.heartmatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.data.RewardRules
import com.example.heartmatch.ui.theme.GardenPalette

@Composable
fun ShopScreen(profile: PlayerProfile, onPurchase: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(GardenPalette.Background).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Back to garden") }
        Text("Garden shop", style = MaterialTheme.typography.headlineMedium, color = GardenPalette.Ivory)
        Text("${profile.coins} earned coins", color = GardenPalette.Gold)
        Text("Boosters cost coins earned by playing. Decorations are optional and change only your garden.", color = GardenPalette.Ivory)
        RewardRules.prices.forEach { (item, price) ->
            val owned = item in profile.ownedDecorations
            val selected = profile.gardenDecoration == item
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })
                    Text(if (item.endsWith("_GARDEN")) "Garden flower display" else "One booster")
                    Button(onClick = { onPurchase(item) }, enabled = !selected && (owned || profile.coins >= price)) {
                        Text(if (selected) "Selected" else if (owned) "Use decoration" else "$price coins · Buy")
                    }
                }
            }
        }
        TextButton(onClick = { onPurchase("CLASSIC") }) { Text("Use classic garden") }
    }
}
