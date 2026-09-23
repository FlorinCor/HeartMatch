package com.example.heartmatch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.HeartColor
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val isHeart: Boolean = false
)

data class ParticleBurst(
    val id: Long = System.nanoTime(),
    val origin: Offset,
    val color: Color,
    val particles: List<Particle>
)

data class ScorePopupData(
    val id: Long = System.nanoTime(),
    val text: String,
    val position: Offset,
    val color: Color
)

data class ComboBannerData(
    val id: Long = System.nanoTime(),
    val title: String,
    val subtitle: String,
    val color: Color
)

data class LaserBeamData(
    val id: Long = System.nanoTime(),
    val isHorizontal: Boolean,
    val coordIndex: Int,
    val color: Color
)

data class BlastWaveData(
    val id: Long = System.nanoTime(),
    val center: Offset,
    val maxRadius: Float,
    val color: Color
)

fun createBurstParticles(origin: Offset, baseColor: Color, count: Int = 16): List<Particle> {
    val rng = Random(System.nanoTime())
    return (0 until count).map {
        val angle = rng.nextFloat() * 2f * PI.toFloat()
        val speed = rng.nextFloat() * 180f + 60f
        val vx = cos(angle) * speed
        val vy = sin(angle) * speed
        val size = rng.nextFloat() * 8f + 4f
        val colorVariation = when (rng.nextInt(3)) {
            0 -> baseColor
            1 -> Color.White
            else -> baseColor.copy(alpha = 0.8f)
        }
        Particle(
            x = origin.x,
            y = origin.y,
            vx = vx,
            vy = vy,
            size = size,
            color = colorVariation,
            isHeart = rng.nextBoolean()
        )
    }
}

@Composable
fun ParticleBurstEffect(
    burst: ParticleBurst,
    onFinished: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(burst.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550, easing = LinearOutSlowInEasing)
        )
        onFinished()
    }

    val progress = animProgress.value
    val alpha = (1f - progress).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        burst.particles.forEach { p ->
            val curX = p.x + p.vx * progress
            val curY = p.y + p.vy * progress + (80f * progress * progress) // gravity
            val curSize = p.size * (1f - progress * 0.5f)

            if (p.isHeart) {
                val heartPath = createHeartPath(curSize * 2f, curSize * 2f, 0f)
                translate(curX - curSize, curY - curSize) {
                    drawPath(
                        path = heartPath,
                        color = p.color.copy(alpha = alpha)
                    )
                }
            } else {
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = curSize,
                    center = Offset(curX, curY)
                )
            }
        }
    }
}

@Composable
fun ScorePopupItem(
    popup: ScorePopupData,
    onFinished: () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(popup.id) {
        scale.animateTo(1.2f, tween(150, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(100))
        offsetY.animateTo(-60f, tween(600, easing = FastOutLinearInEasing))
        alpha.animateTo(0f, tween(200))
        onFinished()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(popup.position.x.roundToInt(), (popup.position.y + offsetY.value).roundToInt()) }
            .scale(scale.value)
            .alpha(alpha.value)
    ) {
        Text(
            text = popup.text,
            color = popup.color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ComboBannerOverlay(
    banner: ComboBannerData?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = scaleIn(tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
        exit = scaleOut(tween(250)) + fadeOut(tween(200)),
        modifier = modifier
    ) {
        if (banner != null) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                banner.color.copy(alpha = 0.95f),
                                Color(0xFFFF4081).copy(alpha = 0.95f),
                                banner.color.copy(alpha = 0.95f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${banner.title} ${banner.subtitle}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun BlastWaveEffect(
    blast: BlastWaveData,
    onFinished: () -> Unit
) {
    val radiusAnim = remember { Animatable(0f) }

    LaunchedEffect(blast.id) {
        radiusAnim.animateTo(blast.maxRadius, tween(350, easing = FastOutSlowInEasing))
        onFinished()
    }

    val maxRadius = blast.maxRadius.coerceAtLeast(1f)
    val progress = (radiusAnim.value / maxRadius).coerceIn(0f, 1f)
    val alpha = (1f - progress).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // A radial gradient with a zero radius throws IllegalArgumentException on Android,
        // and the animation starts from 0 – skip the very first (invisible) frame.
        val radius = radiusAnim.value
        if (radius <= 0f) return@Canvas
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blast.color.copy(alpha = alpha * 0.7f), Color.White.copy(alpha = alpha * 0.9f), Color.Transparent),
                center = blast.center,
                radius = radius
            ),
            radius = radius,
            center = blast.center,
            style = Stroke(width = (8f * (1f - progress)).coerceAtLeast(1f), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun LaserBeamEffect(
    laser: LaserBeamData,
    cellSizePx: Float,
    onFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(laser.id) {
        delay(120)
        alphaAnim.animateTo(0f, tween(250))
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        if (laser.isHorizontal) {
            val y = (laser.coordIndex + 0.5f) * cellSizePx
            drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White, laser.color, Color.Transparent),
                    startY = y - cellSizePx * 0.4f,
                    endY = y + cellSizePx * 0.4f
                ),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = cellSizePx * 0.7f * alphaAnim.value
            )
        } else {
            val x = (laser.coordIndex + 0.5f) * cellSizePx
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White, laser.color, Color.Transparent),
                    startX = x - cellSizePx * 0.4f,
                    endX = x + cellSizePx * 0.4f
                ),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = cellSizePx * 0.7f * alphaAnim.value
            )
        }
    }
}
