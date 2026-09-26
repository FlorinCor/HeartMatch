package com.example.heartmatch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.ui.components.*
import com.example.heartmatch.ui.theme.GardenPalette

@Composable
fun SettingsScreen(
    profile: PlayerProfile,
    onUpdateSettings: (Boolean, Boolean, Boolean) -> Unit,
    onResetData: () -> Unit,
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        GardenBackdrop()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                GardenIconButton("back", "Back to garden", onBackClick)
                Text("Settings",fontSize=24.sp,fontWeight=FontWeight.Bold,color=GardenPalette.Ivory)
            }
            Surface(color=GardenPalette.Panel,shape=androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    SettingsToggleRow("Sound effects","sound",profile.sfxEnabled) { onUpdateSettings(it,profile.hapticsEnabled,profile.reducedMotion) }
                    SettingsToggleRow("Haptic vibration","haptic",profile.hapticsEnabled) { onUpdateSettings(profile.sfxEnabled,it,profile.reducedMotion) }
                    SettingsToggleRow("Reduce decoration motion","motion",profile.reducedMotion) { onUpdateSettings(profile.sfxEnabled,profile.hapticsEnabled,it) }
                    Text("Stops drifting petals and the current-level pulse. Puzzle feedback stays unchanged.",color=GardenPalette.Ivory.copy(alpha=0.7f),fontSize=13.sp)
                }
            }
            OutlinedButton(onClick=onHelpClick,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
                GardenIcon("help"); Spacer(Modifier.width(12.dp)); Text("How to play",color=GardenPalette.Ivory)
            }
            Text("Heart Match\nA little colour. A little joy. One level at a time.",color=GardenPalette.Ivory.copy(alpha=0.75f),fontSize=14.sp,modifier=Modifier.padding(horizontal=12.dp))
            TextButton(onClick={showResetConfirm=true}) { Text("Reset all progress",color=GardenPalette.RoseLight) }
        }
    }
    if(showResetConfirm) AlertDialog(
        onDismissRequest={showResetConfirm=false},
        title={Text("Reset all progress?")},
        text={Text("This will erase your stars, unlocked levels, and booster inventory.")},
        confirmButton={TextButton(onClick={showResetConfirm=false;onResetData()}){Text("Reset")}},
        dismissButton={TextButton(onClick={showResetConfirm=false}){Text("Cancel")}}
    )
}

@Composable
fun SettingsToggleRow(title: String, icon: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        GardenIcon(icon,tint=GardenPalette.Gold)
        Text(title,color=GardenPalette.Ivory,fontSize=15.sp,modifier=Modifier.weight(1f))
        Switch(checked=isChecked,onCheckedChange=onCheckedChange)
    }
}
