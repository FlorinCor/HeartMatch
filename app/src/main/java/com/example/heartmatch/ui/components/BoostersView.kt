package com.example.heartmatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.ui.theme.GardenPalette

@Composable
fun BoostersView(profile: PlayerProfile, activeBooster: String?, onBoosterClick: (String) -> Unit, onCancelBooster: () -> Unit, onPauseClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp)) {
        if(activeBooster != null) {
            Row(Modifier.fillMaxWidth().background(GardenPalette.PanelLight,RoundedCornerShape(12.dp)).padding(start=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(when(activeBooster){"HAMMER"->"Tap a tile to smash it";"BOMB"->"Tap a cell to place a bomb";"RAINBOW"->"Tap a cell to place a rainbow";else->"Select a target cell"},color=GardenPalette.Ivory,fontSize=13.sp,modifier=Modifier.weight(1f))
                TextButton(onClick=onCancelBooster){Text("Cancel",color=GardenPalette.RoseLight)}
            }
            Spacer(Modifier.height(6.dp))
        }
        val items = listOf(
            Triple("PAUSE","Pause",0), Triple("HAMMER","Hammer",profile.hammerCount),
            Triple("BOMB","Bomb",profile.bombBoosterCount), Triple("RAINBOW","Rainbow",profile.rainbowBoosterCount),
            Triple("SHUFFLE","Shuffle",profile.shuffleCount), Triple("EXTRA_MOVES","Moves",profile.extraMovesCount)
        )
        BoxWithConstraints(Modifier.fillMaxWidth().background(GardenPalette.Panel,RoundedCornerShape(18.dp)).padding(4.dp)) {
            val columns = if(maxWidth < 320.dp) 3 else 6
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                items.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { (key,name,count) ->
                            Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                                BoosterButton(if(key=="EXTRA_MOVES") "+5" else key.lowercase(),name,count,activeBooster==key,{if(key=="PAUSE") onPauseClick() else onBoosterClick(key)},showCount=key!="PAUSE")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoosterButton(icon: String, name: String, count: Int, isActive: Boolean, onClick: () -> Unit, showCount: Boolean = true) {
    Column(Modifier.fillMaxWidth().heightIn(min=64.dp)
        .background(if(isActive) GardenPalette.RoseDark else GardenPalette.Panel,RoundedCornerShape(12.dp))
        .border(1.dp,if(isActive) GardenPalette.Gold else GardenPalette.Panel,RoundedCornerShape(12.dp))
        .semantics(mergeDescendants=true) { contentDescription=if(showCount) "$name, $count available" else name; selected=isActive }
        .clickable(onClick=onClick).padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(30.dp),contentAlignment=Alignment.Center) {
            if(icon=="+5") Text("+5",color=GardenPalette.Ivory,fontSize=19.sp,fontWeight=FontWeight.Bold)
            else GardenIcon(icon)
            if(showCount) Text(count.toString(),color=GardenPalette.Gold,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.TopEnd).background(GardenPalette.Background,RoundedCornerShape(6.dp)).padding(horizontal=3.dp))
        }
        Text(name,color=GardenPalette.Ivory,fontSize=10.sp,maxLines=1)
    }
}
