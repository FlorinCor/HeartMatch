package com.example.heartmatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.data.PlayerProfile

data class DailyRewardItem(
    val day: Int,
    val coins: Int,
    val boosterType: String?,
    val icon: String
)

val dailyRewardsList = listOf(
    DailyRewardItem(1, 100, null, "💰"),
    DailyRewardItem(2, 150, "HAMMER", "🔨"),
    DailyRewardItem(3, 200, "SHUFFLE", "🔀"),
    DailyRewardItem(4, 250, "BOMB", "💣"),
    DailyRewardItem(5, 300, "EXTRA_MOVES", "+5"),
    DailyRewardItem(6, 400, "RAINBOW", "🌈"),
    DailyRewardItem(7, 1000, "RAINBOW", "🎁")
)

@Composable
fun DailyRewardScreen(
    profile: PlayerProfile,
    onClaimReward: (day: Int, coins: Int, booster: String?) -> Unit,
    onBackClick: () -> Unit
) {
    val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
    val hasClaimedToday = profile.lastDailyClaimDay == currentDay
    val nextDayToClaim = if (hasClaimedToday) profile.dailyRewardStreak else (profile.dailyRewardStreak % 7) + 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3F145B),
                        Color(0xFF6A1B9A),
                        Color(0xFF1A002C)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(6.dp, CircleShape)
                        .background(Color(0xFF2E0854), CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "◀", color = Color.White, fontSize = 18.sp)
                }

                Text(
                    text = "DAILY REWARD",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Box(modifier = Modifier.size(42.dp))
            }

            // Streak Info Banner
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎁 7-Day Login Bonus",
                    color = Color(0xFFFFD54F),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (hasClaimedToday) "You've claimed today's reward! Come back tomorrow." else "Claim your daily rewards to boost your game!",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }

            // 7 Days Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Days 1-3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in 0..2) {
                        DailyCard(
                            item = dailyRewardsList[i],
                            streak = profile.dailyRewardStreak,
                            hasClaimedToday = hasClaimedToday,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Days 4-6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in 3..5) {
                        DailyCard(
                            item = dailyRewardsList[i],
                            streak = profile.dailyRewardStreak,
                            hasClaimedToday = hasClaimedToday,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Day 7 Big Jackpot Card
                val day7 = dailyRewardsList[6]
                val isClaimed = profile.dailyRewardStreak >= 7
                val isCurrentClaimable = !hasClaimedToday && nextDayToClaim == 7

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(if (isCurrentClaimable) 14.dp else 6.dp, RoundedCornerShape(20.dp))
                        .background(
                            brush = if (isCurrentClaimable) Brush.horizontalGradient(listOf(Color(0xFFFF4081), Color(0xFFFFD700)))
                            else Brush.verticalGradient(listOf(Color(0xFF2E0854), Color(0xFF1E0A30))),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = if (isCurrentClaimable) 2.5.dp else 1.dp,
                            color = if (isCurrentClaimable) Color.White else Color(0xFFFFD700).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "DAY 7 GRAND PRIZE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text(text = "+${day7.coins} Coins & Bonus Booster Pack!", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = if (isClaimed) "✓" else "🎁", fontSize = 28.sp)
                    }
                }
            }

            // Claim Button
            val currentClaimItem = dailyRewardsList.getOrNull(nextDayToClaim - 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .shadow(16.dp, RoundedCornerShape(26.dp))
                    .background(
                        brush = if (!hasClaimedToday && currentClaimItem != null) Brush.horizontalGradient(listOf(Color(0xFFFF4081), Color(0xFFFF8F00)))
                        else Brush.horizontalGradient(listOf(Color(0xFF616161), Color(0xFF424242))),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .border(2.dp, Color.White, RoundedCornerShape(26.dp))
                    .clickable(enabled = !hasClaimedToday && currentClaimItem != null) {
                        if (currentClaimItem != null) {
                            onClaimReward(nextDayToClaim, currentClaimItem.coins, currentClaimItem.boosterType)
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasClaimedToday) "CLAIMED FOR TODAY ✓" else "CLAIM DAY $nextDayToClaim REWARD 🎁",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun DailyCard(
    item: DailyRewardItem,
    streak: Int,
    hasClaimedToday: Boolean,
    modifier: Modifier = Modifier
) {
    val isClaimed = streak >= item.day
    val isClaimable = !hasClaimedToday && (streak % 7) + 1 == item.day

    Box(
        modifier = modifier
            .shadow(if (isClaimable) 10.dp else 4.dp, RoundedCornerShape(18.dp))
            .background(
                brush = when {
                    isClaimable -> Brush.verticalGradient(listOf(Color(0xFFFF4081), Color(0xFFFF8F00)))
                    isClaimed -> Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)))
                    else -> Brush.verticalGradient(listOf(Color(0xFF2E0854), Color(0xFF1E0A30)))
                },
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = if (isClaimable) 2.dp else 1.dp,
                color = when {
                    isClaimable -> Color.White
                    isClaimed -> Color(0xFF4CAF50)
                    else -> Color.White.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Day ${item.day}",
                color = if (isClaimable) Color.White else Color(0xFFFFD54F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isClaimed) "Claimed ✓" else "+${item.coins}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
