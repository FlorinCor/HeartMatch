package com.example.heartmatch.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.FireDirection
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Palette tuned to the "Heart Match" concept art: saturated candy bodies,
 * deep shadow tones for the outline / lower half, and near-white gloss tints.
 */
object HeartColors {
    val RedLight = Color(0xFFFF7B7B)
    val RedMain = Color(0xFFE5202B)
    val RedDark = Color(0xFF7E0812)
    val RedGloss = Color(0xFFFFE3E3)

    val PinkLight = Color(0xFFFFA6D8)
    val PinkMain = Color(0xFFFF3D9E)
    val PinkDark = Color(0xFF9E0C5B)
    val PinkGloss = Color(0xFFFFF0F7)

    val BlueLight = Color(0xFF7CCBFF)
    val BlueMain = Color(0xFF1E7BF2)
    val BlueDark = Color(0xFF0A3A8F)
    val BlueGloss = Color(0xFFE6F4FF)

    val GreenLight = Color(0xFF9BEF6E)
    val GreenMain = Color(0xFF3CBB25)
    val GreenDark = Color(0xFF125A16)
    val GreenGloss = Color(0xFFEFFFE8)

    val YellowLight = Color(0xFFFFE97A)
    val YellowMain = Color(0xFFFFB400)
    val YellowDark = Color(0xFFB85C00)
    val YellowGloss = Color(0xFFFFFBE0)

    val PurpleLight = Color(0xFFDC97FF)
    val PurpleMain = Color(0xFFA23FE6)
    val PurpleDark = Color(0xFF4B0E7E)
    val PurpleGloss = Color(0xFFF7E8FF)

    val OrangeLight = Color(0xFFFFB86B)
    val OrangeMain = Color(0xFFFF7A1C)
    val OrangeDark = Color(0xFF9E3300)
    val OrangeGloss = Color(0xFFFFF1E3)

    // Shared accent tones used by special / blocker hearts
    val GoldLight = Color(0xFFFFF3B0)
    val GoldMain = Color(0xFFFFC629)
    val GoldDark = Color(0xFFB8700A)
    val GoldOutline = Color(0xFF6E4200)

    val BoardShadow = Color(0xFF1A0F26)
}

fun HeartColor.getGradients(): Triple<Color, Color, Color> {
    return when (this) {
        HeartColor.RED -> Triple(HeartColors.RedLight, HeartColors.RedMain, HeartColors.RedDark)
        HeartColor.PINK -> Triple(HeartColors.PinkLight, HeartColors.PinkMain, HeartColors.PinkDark)
        HeartColor.BLUE -> Triple(HeartColors.BlueLight, HeartColors.BlueMain, HeartColors.BlueDark)
        HeartColor.GREEN -> Triple(HeartColors.GreenLight, HeartColors.GreenMain, HeartColors.GreenDark)
        HeartColor.YELLOW -> Triple(HeartColors.YellowLight, HeartColors.YellowMain, HeartColors.YellowDark)
        HeartColor.PURPLE -> Triple(HeartColors.PurpleLight, HeartColors.PurpleMain, HeartColors.PurpleDark)
        HeartColor.ORANGE -> Triple(HeartColors.OrangeLight, HeartColors.OrangeMain, HeartColors.OrangeDark)
    }
}

fun HeartColor.getGlossColor(): Color {
    return when (this) {
        HeartColor.RED -> HeartColors.RedGloss
        HeartColor.PINK -> HeartColors.PinkGloss
        HeartColor.BLUE -> HeartColors.BlueGloss
        HeartColor.GREEN -> HeartColors.GreenGloss
        HeartColor.YELLOW -> HeartColors.YellowGloss
        HeartColor.PURPLE -> HeartColors.PurpleGloss
        HeartColor.ORANGE -> HeartColors.OrangeGloss
    }
}

/**
 * Plump, rounded heart silhouette matching the concept art: wide lobes,
 * a fairly deep notch and a softly tapered point.
 */
fun createHeartPath(width: Float, height: Float, paddingRatio: Float = 0.08f): Path {
    val padX = width * paddingRatio
    val padY = height * paddingRatio
    val w = width - padX * 2
    val h = height - padY * 2
    val l = padX
    val t = padY

    val path = Path()
    path.moveTo(l + w * 0.5f, t + h * 0.26f)
    // Left lobe
    path.cubicTo(
        l + w * 0.46f, t + h * 0.12f,
        l + w * 0.36f, t + h * 0.03f,
        l + w * 0.25f, t + h * 0.03f
    )
    path.cubicTo(
        l + w * 0.10f, t + h * 0.03f,
        l, t + h * 0.16f,
        l, t + h * 0.34f
    )
    path.cubicTo(
        l, t + h * 0.58f,
        l + w * 0.24f, t + h * 0.76f,
        l + w * 0.5f, t + h * 0.98f
    )
    // Right lobe
    path.cubicTo(
        l + w * 0.76f, t + h * 0.76f,
        l + w, t + h * 0.58f,
        l + w, t + h * 0.34f
    )
    path.cubicTo(
        l + w, t + h * 0.16f,
        l + w * 0.90f, t + h * 0.03f,
        l + w * 0.75f, t + h * 0.03f
    )
    path.cubicTo(
        l + w * 0.64f, t + h * 0.03f,
        l + w * 0.54f, t + h * 0.12f,
        l + w * 0.5f, t + h * 0.26f
    )
    path.close()
    return path
}

fun createStarPath(center: Offset, outerRadius: Float, innerRadius: Float, numPoints: Int = 5): Path {
    val path = Path()
    val angleStep = PI.toFloat() / numPoints
    var angle = -PI.toFloat() / 2
    for (i in 0 until numPoints * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += angleStep
    }
    path.close()
    return path
}

@Composable
fun TileView(
    tile: Tile?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isHinted: Boolean = false,
    isHealed: Boolean = false
) {
    if (tile == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "TileAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected) 1.15f else if (isHinted) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "ShimmerRotation"
    )

    val healingPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HealingPulse"
    )

    val woodenHeartImage = if (tile is Tile.Blocker && tile.blockerType == BlockerType.WOODEN_HEART) {
        ImageBitmap.imageResource(com.example.heartmatch.R.drawable.wooden_heart_oak)
    } else {
        null
    }
    val stoneHeartImage = if (tile is Tile.Blocker && tile.blockerType == BlockerType.STONE_HEART) {
        ImageBitmap.imageResource(com.example.heartmatch.R.drawable.stone_heart_granite)
    } else {
        null
    }
    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        val w = size.width
        val h = size.height

        scale(if (isSelected || isHinted) pulseScale else 1f, pivot = Offset(w / 2, h / 2)) {
            // Draw selection halo
            if (isSelected) {
                val haloPath = createHeartPath(w, h, paddingRatio = 0.01f)
                drawPath(
                    path = haloPath,
                    color = HeartColors.GoldMain.copy(alpha = 0.35f),
                    style = Stroke(width = w * 0.14f, join = StrokeJoin.Round)
                )
                drawPath(
                    path = haloPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, HeartColors.GoldMain),
                        startY = 0f,
                        endY = h
                    ),
                    style = Stroke(width = w * 0.05f, join = StrokeJoin.Round)
                )
            } else if (isHinted) {
                val haloPath = createHeartPath(w, h, paddingRatio = 0.03f)
                drawPath(
                    path = haloPath,
                    color = Color(0xFFFFEB3B).copy(alpha = 0.75f),
                    style = Stroke(width = w * 0.04f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                )
            }

            when (tile) {
                is Tile.Normal -> {
                    drawGlossyNormalHeart(tile.color, w, h)
                }
                is Tile.Special -> {
                    drawGlossySpecialHeart(tile, w, h, shimmerRotation)
                }
                is Tile.Blocker -> {
                    drawGlossyBlockerHeart(tile, w, h, shimmerRotation, woodenHeartImage, stoneHeartImage)
                }
            }

            if (isSelected) {
                drawPath(createHeartPath(w, h), color = Color.White.copy(alpha = 0.18f))
            }

            if (tile is Tile.Blocker) {
                val count = tile.durability.coerceAtMost(6)
                repeat(count) { index ->
                    val center = Offset(w * 0.5f + (index - (count - 1) / 2f) * w * 0.12f, h * 0.86f)
                    drawCircle(Color(0xFF152F29), w * 0.052f, center)
                    drawCircle(Color.White, w * 0.029f, center)
                }
            }
            if (isHealed) {
                val glowAlpha = 0.52f + (1f - healingPulse) * 0.30f
                drawPath(
                    path = createHeartPath(w, h, paddingRatio = 0.045f),
                    color = Color(0xFFB9FFD1).copy(alpha = glowAlpha),
                    style = Stroke(width = w * (0.025f + healingPulse * 0.035f), join = StrokeJoin.Round)
                )
                val sparkleAlpha = 0.45f + (1f - healingPulse) * 0.5f
                listOf(
                    Offset(w * 0.20f, h * 0.37f),
                    Offset(w * 0.80f, h * 0.38f),
                    Offset(w * 0.51f, h * 0.81f)
                ).forEach { center ->
                    drawPath(
                        path = createStarPath(center, w * 0.045f, w * 0.016f, numPoints = 4),
                        color = Color(0xFFE5FFE9).copy(alpha = sparkleAlpha)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Core shading primitives (shared by every heart type)
// ---------------------------------------------------------------------------

fun DrawScope.drawHeartShadow(heartPath: Path, h: Float) {
    translate(0f, h * 0.055f) {
        drawPath(heartPath, color = Color.Black.copy(alpha = 0.20f))
    }
    translate(0f, h * 0.028f) {
        drawPath(heartPath, color = Color.Black.copy(alpha = 0.22f))
    }
}

/**
 * Fills the heart with a deep radial gradient (light top-left → dark bottom-right),
 * adds a soft reflected rim light along the lower edge and a crisp dark outline.
 */
fun DrawScope.drawHeartBody(
    heartPath: Path,
    w: Float,
    h: Float,
    light: Color,
    main: Color,
    dark: Color,
    outline: Color = dark,
    outlineWidth: Float = w * 0.032f
) {
    drawPath(
        path = heartPath,
        brush = Brush.radialGradient(
            0f to light,
            0.5f to main,
            1f to dark,
            center = Offset(w * 0.36f, h * 0.30f),
            radius = w * 0.80f
        )
    )

    // Reflected rim light on the lower edge
    clipPath(heartPath) {
        translate(-w * 0.04f, -h * 0.05f) {
            drawPath(
                path = heartPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, light.copy(alpha = 0.55f)),
                    startY = h * 0.35f,
                    endY = h
                ),
                style = Stroke(width = w * 0.10f)
            )
        }
    }

    drawPath(
        path = heartPath,
        color = outline,
        style = Stroke(width = outlineWidth, join = StrokeJoin.Round)
    )
}

/**
 * Big bean-shaped specular highlight on the left lobe plus a small spot on the right lobe.
 */
fun DrawScope.drawHeartGloss(heartPath: Path, w: Float, h: Float, intensity: Float = 1f) {
    clipPath(heartPath) {
        rotate(-32f, pivot = Offset(w * 0.30f, h * 0.29f)) {
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f * intensity),
                        Color.White.copy(alpha = 0.55f * intensity),
                        Color.White.copy(alpha = 0.05f * intensity)
                    ),
                    startY = h * 0.16f,
                    endY = h * 0.42f
                ),
                topLeft = Offset(w * 0.15f, h * 0.16f),
                size = Size(w * 0.30f, h * 0.26f)
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.75f * intensity),
            radius = w * 0.045f,
            center = Offset(w * 0.72f, h * 0.21f)
        )
    }
}

fun DrawScope.drawGlossOverlay(heartPath: Path, w: Float, h: Float) {
    drawHeartGloss(heartPath, w, h)
}

// ---------------------------------------------------------------------------
// Normal hearts
// ---------------------------------------------------------------------------

fun DrawScope.drawGlossyNormalHeart(
    color: HeartColor,
    w: Float,
    h: Float,
    paddingRatio: Float = 0.08f
) {
    val heartPath = createHeartPath(w, h, paddingRatio)
    val (light, main, dark) = color.getGradients()

    drawHeartShadow(heartPath, h)
    drawHeartBody(heartPath, w, h, light, main, dark)
    drawHeartGloss(heartPath, w, h)
}

// ---------------------------------------------------------------------------
// Special hearts
// ---------------------------------------------------------------------------

fun DrawScope.drawGlossySpecialHeart(
    tile: Tile.Special,
    w: Float,
    h: Float,
    shimmerRotation: Float
) {
    val heartPath = createHeartPath(w, h, 0.08f)

    when (tile.specialType) {
        SpecialHeartType.RAINBOW_HEART -> drawRainbowHeart(heartPath, w, h, shimmerRotation)
        SpecialHeartType.FIRE_HEART -> drawFireHeart(tile, w, h, shimmerRotation)
        SpecialHeartType.BOMB_HEART -> drawStarHeart(w, h, shimmerRotation)
        SpecialHeartType.GIFT_HEART -> drawGiftHeart(heartPath, w, h)
        SpecialHeartType.ROYAL_HEART -> drawRoyalHeart(heartPath, w, h)
        SpecialHeartType.ANGEL_HEART -> drawAngelHeart(w, h)
        SpecialHeartType.LIGHT_HEART -> drawLightHeart(heartPath, w, h, shimmerRotation)
    }
}

private fun DrawScope.drawLightHeart(heartPath: Path, w: Float, h: Float, shimmerRotation: Float) {
    // Radiating light rays behind the heart
    val center = Offset(w * 0.5f, h * 0.52f)
    rotate(shimmerRotation, pivot = center) {
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val len = if (i % 2 == 0) w * 0.55f else w * 0.42f
            val end = Offset(
                center.x + (Math.cos(angle) * len).toFloat(),
                center.y + (Math.sin(angle) * len).toFloat()
            )
            drawLine(
                color = Color(0xFFFFF3B0).copy(alpha = 0.55f),
                start = center,
                end = end,
                strokeWidth = w * 0.03f
            )
        }
    }

    drawHeartShadow(heartPath, h)
    drawHeartBody(
        heartPath, w, h,
        light = Color(0xFFFFFDF2),
        main = HeartColors.GoldLight,
        dark = HeartColors.GoldMain,
        outline = HeartColors.GoldDark
    )
    // Radial glow overlay
    drawPath(
        heartPath,
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0f)),
            center = center,
            radius = w * 0.45f
        )
    )
    drawHeartGloss(heartPath, w, h)

    // Central sparkle
    drawCircle(Color.White, radius = w * 0.06f, center = center)
    drawLine(Color.White.copy(alpha = 0.9f), Offset(center.x, center.y - h * 0.18f), Offset(center.x, center.y + h * 0.18f), strokeWidth = w * 0.02f)
    drawLine(Color.White.copy(alpha = 0.9f), Offset(center.x - w * 0.18f, center.y), Offset(center.x + w * 0.18f, center.y), strokeWidth = w * 0.02f)
}

private fun DrawScope.drawRainbowHeart(heartPath: Path, w: Float, h: Float, shimmerRotation: Float) {
    drawHeartShadow(heartPath, h)

    val bands = listOf(
        Color(0xFFFF3B3B),
        Color(0xFFFF8F1F),
        Color(0xFFFFE53B),
        Color(0xFF45D23A),
        Color(0xFF2E8DFF),
        Color(0xFF9B3BE0)
    )
    val stops = ArrayList<Pair<Float, Color>>()
    bands.forEachIndexed { i, c ->
        stops.add(i / bands.size.toFloat() to c)
        stops.add((i + 1) / bands.size.toFloat() to c)
    }

    // Diagonal hard-edged rainbow stripes
    drawPath(
        path = heartPath,
        brush = Brush.linearGradient(
            *stops.toTypedArray(),
            start = Offset(w * 0.12f, h * 0.10f),
            end = Offset(w * 0.88f, h * 0.90f)
        )
    )

    // 3D shading over the stripes
    drawPath(
        path = heartPath,
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.25f),
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.40f),
            center = Offset(w * 0.36f, h * 0.30f),
            radius = w * 0.80f
        )
    )

    clipPath(heartPath) {
        translate(-w * 0.04f, -h * 0.05f) {
            drawPath(
                path = heartPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.40f)),
                    startY = h * 0.35f,
                    endY = h
                ),
                style = Stroke(width = w * 0.10f)
            )
        }
    }

    drawPath(heartPath, color = Color(0xFF4A1466), style = Stroke(width = w * 0.032f, join = StrokeJoin.Round))
    drawHeartGloss(heartPath, w, h)

    // Slowly drifting sparkle
    val sparkleCenter = Offset(w * 0.68f, h * 0.36f)
    rotate(shimmerRotation, pivot = sparkleCenter) {
        drawStar(sparkleCenter, outerRadius = w * 0.09f, innerRadius = w * 0.025f, color = Color.White.copy(alpha = 0.9f))
    }
}

private fun DrawScope.drawFireHeart(tile: Tile.Special, w: Float, h: Float, shimmerRotation: Float) {
    val flicker = 1f + 0.035f * sin(shimmerRotation * PI.toFloat() / 45f)

    // Outer flames rising above the heart
    val flame = Path().apply {
        moveTo(w * 0.16f, h * 0.55f)
        cubicTo(w * 0.09f, h * 0.35f, w * 0.15f, h * 0.16f, w * 0.26f, h * 0.06f)
        cubicTo(w * 0.29f, h * 0.16f, w * 0.32f, h * 0.24f, w * 0.37f, h * 0.24f)
        cubicTo(w * 0.40f, h * 0.13f, w * 0.44f, h * 0.05f, w * 0.50f, 0f)
        cubicTo(w * 0.56f, h * 0.06f, w * 0.60f, h * 0.15f, w * 0.62f, h * 0.26f)
        cubicTo(w * 0.68f, h * 0.22f, w * 0.72f, h * 0.12f, w * 0.76f, h * 0.04f)
        cubicTo(w * 0.87f, h * 0.14f, w * 0.92f, h * 0.35f, w * 0.84f, h * 0.55f)
        close()
    }

    scale(flicker, pivot = Offset(w * 0.5f, h * 0.55f)) {
        drawPath(
            flame,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF176), Color(0xFFFFA000), Color(0xFFE53935)),
                startY = 0f,
                endY = h * 0.55f
            )
        )
        drawPath(flame, color = Color(0xFF9E1B00).copy(alpha = 0.6f), style = Stroke(width = w * 0.02f, join = StrokeJoin.Round))
        scale(0.62f, pivot = Offset(w * 0.5f, h * 0.55f)) {
            drawPath(flame, color = Color(0xFFFFF59D).copy(alpha = 0.85f))
        }
    }

    // Heart body sitting inside the fire
    val heartPath = createHeartPath(w, h, 0.15f)
    translate(0f, h * 0.06f) {
        drawHeartShadow(heartPath, h)
        drawHeartBody(
            heartPath, w, h,
            light = Color(0xFFFF8A65),
            main = Color(0xFFE8321F),
            dark = Color(0xFF7A0E0A),
            outline = Color(0xFF5A0A05)
        )
        // Hot inner glow near the top
        clipPath(heartPath) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFE082).copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.30f),
                    radius = w * 0.28f
                ),
                radius = w * 0.28f,
                center = Offset(w * 0.5f, h * 0.30f)
            )
        }
        drawHeartGloss(heartPath, w, h, intensity = 0.85f)

        // Directional indicator
        val arrowColor = Color.White.copy(alpha = 0.9f)
        when (tile.fireDirection) {
            FireDirection.ROW -> drawDoubleArrow(Offset(w / 2, h * 0.52f), length = w * 0.42f, isHorizontal = true, color = arrowColor)
            FireDirection.COLUMN -> drawDoubleArrow(Offset(w / 2, h * 0.52f), length = h * 0.42f, isHorizontal = false, color = arrowColor)
            FireDirection.BOTH, null -> {
                drawDoubleArrow(Offset(w / 2, h * 0.52f), length = w * 0.40f, isHorizontal = true, color = arrowColor)
                drawDoubleArrow(Offset(w / 2, h * 0.52f), length = h * 0.40f, isHorizontal = false, color = arrowColor)
            }
        }
    }
}

private fun DrawScope.drawStarHeart(w: Float, h: Float, shimmerRotation: Float) {
    val center = Offset(w * 0.5f, h * 0.53f)
    val outer = min(w, h) * 0.47f
    val inner = outer * 0.48f
    val star = createStarPath(center, outer, inner)

    translate(0f, h * 0.05f) {
        drawPath(star, color = Color.Black.copy(alpha = 0.32f))
    }

    drawPath(
        star,
        brush = Brush.radialGradient(
            0f to Color(0xFFFFF9C4),
            0.45f to Color(0xFFFFC107),
            1f to Color(0xFFE65100),
            center = Offset(w * 0.40f, h * 0.36f),
            radius = outer * 1.3f
        )
    )

    clipPath(star) {
        translate(-w * 0.03f, -h * 0.04f) {
            drawPath(
                star,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFFE082).copy(alpha = 0.6f)),
                    startY = h * 0.4f,
                    endY = h
                ),
                style = Stroke(width = w * 0.09f)
            )
        }
        rotate(-30f, pivot = Offset(w * 0.34f, h * 0.36f)) {
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.05f)),
                    startY = h * 0.26f,
                    endY = h * 0.48f
                ),
                topLeft = Offset(w * 0.22f, h * 0.26f),
                size = Size(w * 0.22f, h * 0.20f)
            )
        }
    }

    drawPath(star, color = Color(0xFF8A4500), style = Stroke(width = w * 0.03f, join = StrokeJoin.Round))

    // Twinkle at the tip
    val twinkle = Offset(w * 0.5f, h * 0.10f)
    rotate(shimmerRotation, pivot = twinkle) {
        drawStar(twinkle, outerRadius = w * 0.07f, innerRadius = w * 0.02f, color = Color.White.copy(alpha = 0.9f))
    }
}

private fun DrawScope.drawGiftHeart(heartPath: Path, w: Float, h: Float) {
    drawHeartShadow(heartPath, h)
    drawHeartBody(heartPath, w, h, HeartColors.RedLight, HeartColors.RedMain, HeartColors.RedDark)

    val ribbonBrush = Brush.linearGradient(
        colors = listOf(HeartColors.GoldLight, HeartColors.GoldMain, HeartColors.GoldDark),
        start = Offset(w * 0.3f, h * 0.2f),
        end = Offset(w * 0.7f, h * 0.9f)
    )
    val ribbonEdge = Color(0xFF8A5200)
    val bandW = w * 0.16f

    clipPath(heartPath) {
        // Vertical band
        drawRect(ribbonBrush, topLeft = Offset(w * 0.5f - bandW / 2, 0f), size = Size(bandW, h))
        drawLine(ribbonEdge, Offset(w * 0.5f - bandW / 2, 0f), Offset(w * 0.5f - bandW / 2, h), strokeWidth = w * 0.012f)
        drawLine(ribbonEdge, Offset(w * 0.5f + bandW / 2, 0f), Offset(w * 0.5f + bandW / 2, h), strokeWidth = w * 0.012f)
        // Horizontal band
        drawRect(ribbonBrush, topLeft = Offset(0f, h * 0.52f - bandW / 2), size = Size(w, bandW))
        drawLine(ribbonEdge, Offset(0f, h * 0.52f - bandW / 2), Offset(w, h * 0.52f - bandW / 2), strokeWidth = w * 0.012f)
        drawLine(ribbonEdge, Offset(0f, h * 0.52f + bandW / 2), Offset(w, h * 0.52f + bandW / 2), strokeWidth = w * 0.012f)
    }

    // Bow sitting on the notch
    val bowCenter = Offset(w * 0.5f, h * 0.30f)
    val loopSize = Size(w * 0.22f, h * 0.14f)
    rotate(-25f, pivot = bowCenter) {
        drawOval(ribbonBrush, topLeft = Offset(bowCenter.x - loopSize.width, bowCenter.y - loopSize.height / 2), size = loopSize)
        drawOval(ribbonEdge, topLeft = Offset(bowCenter.x - loopSize.width, bowCenter.y - loopSize.height / 2), size = loopSize, style = Stroke(w * 0.015f))
    }
    rotate(25f, pivot = bowCenter) {
        drawOval(ribbonBrush, topLeft = Offset(bowCenter.x, bowCenter.y - loopSize.height / 2), size = loopSize)
        drawOval(ribbonEdge, topLeft = Offset(bowCenter.x, bowCenter.y - loopSize.height / 2), size = loopSize, style = Stroke(w * 0.015f))
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(HeartColors.GoldLight, HeartColors.GoldDark), center = bowCenter, radius = w * 0.06f),
        radius = w * 0.055f,
        center = bowCenter
    )
    drawCircle(ribbonEdge, radius = w * 0.055f, center = bowCenter, style = Stroke(w * 0.015f))

    drawHeartGloss(heartPath, w, h, intensity = 0.8f)
}

private fun DrawScope.drawRoyalHeart(heartPath: Path, w: Float, h: Float) {
    drawHeartShadow(heartPath, h)
    drawHeartBody(
        heartPath, w, h,
        light = HeartColors.GoldLight,
        main = HeartColors.GoldMain,
        dark = HeartColors.GoldDark,
        outline = HeartColors.GoldOutline
    )
    drawHeartGloss(heartPath, w, h)

    // Golden crown perched on the notch
    val crownTop = h * 0.02f
    val crownBottom = h * 0.27f
    val crown = Path().apply {
        moveTo(w * 0.32f, crownBottom)
        lineTo(w * 0.27f, crownTop + h * 0.06f)
        lineTo(w * 0.39f, crownTop + h * 0.12f)
        lineTo(w * 0.50f, crownTop)
        lineTo(w * 0.61f, crownTop + h * 0.12f)
        lineTo(w * 0.73f, crownTop + h * 0.06f)
        lineTo(w * 0.68f, crownBottom)
        close()
    }
    translate(0f, h * 0.02f) { drawPath(crown, Color.Black.copy(alpha = 0.3f)) }
    drawPath(
        crown,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF8D6), HeartColors.GoldMain, Color(0xFFD48A00)),
            startY = crownTop,
            endY = crownBottom
        )
    )
    drawPath(crown, color = HeartColors.GoldOutline, style = Stroke(width = w * 0.02f, join = StrokeJoin.Round))
    drawLine(Color(0xFFD48A00), Offset(w * 0.32f, crownBottom - h * 0.05f), Offset(w * 0.68f, crownBottom - h * 0.05f), strokeWidth = w * 0.012f)

    // Jewels
    drawCircle(Color(0xFFFF1744), radius = w * 0.035f, center = Offset(w * 0.5f, crownTop + h * 0.10f))
    drawCircle(Color.White.copy(alpha = 0.8f), radius = w * 0.012f, center = Offset(w * 0.49f, crownTop + h * 0.085f))
    drawCircle(Color(0xFF2979FF), radius = w * 0.02f, center = Offset(w * 0.28f, crownTop + h * 0.06f))
    drawCircle(Color(0xFF00E676), radius = w * 0.02f, center = Offset(w * 0.72f, crownTop + h * 0.06f))
    drawCircle(Color(0xFFFFF8D6), radius = w * 0.02f, center = Offset(w * 0.5f, crownTop))
}

private fun DrawScope.drawAngelHeart(w: Float, h: Float) {
    val wingBrush = Brush.verticalGradient(
        colors = listOf(Color.White, Color(0xFFFFF3F7), Color(0xFFFFD6E4)),
        startY = h * 0.2f,
        endY = h * 0.75f
    )
    val wingEdge = Color(0xFFE8A3BE)

    fun DrawScope.drawWing(mirror: Boolean) {
        val s = if (mirror) -1f else 1f
        val cx = w * 0.5f
        fun x(v: Float) = cx + s * (v - 0.5f) * w
        val feathers = listOf(
            Triple(0.13f, 0.36f, -35f),
            Triple(0.15f, 0.48f, -15f),
            Triple(0.18f, 0.58f, 5f)
        )
        feathers.forEachIndexed { i, (fx, fy, angle) ->
            val length = w * (0.34f - i * 0.05f)
            val thick = h * (0.15f - i * 0.02f)
            rotate(s * angle, pivot = Offset(x(fx), h * fy)) {
                val left = if (mirror) x(fx) - length else x(fx)
                drawOval(
                    wingBrush,
                    topLeft = Offset(left, h * fy - thick / 2),
                    size = Size(length, thick)
                )
                drawOval(
                    wingEdge,
                    topLeft = Offset(left, h * fy - thick / 2),
                    size = Size(length, thick),
                    style = Stroke(width = w * 0.012f)
                )
            }
        }
    }

    translate(0f, h * 0.03f) {
        drawWing(mirror = false)
        drawWing(mirror = true)
    }

    val heartPath = createHeartPath(w, h, 0.17f)
    translate(0f, h * 0.04f) {
        drawHeartShadow(heartPath, h)
        drawHeartBody(
            heartPath, w, h,
            light = Color(0xFFFFF9C4),
            main = Color(0xFFFFD54F),
            dark = Color(0xFFE08A00),
            outline = Color(0xFF9C5A00)
        )
        drawHeartGloss(heartPath, w, h)
    }
}

// ---------------------------------------------------------------------------
// Blocker hearts
// ---------------------------------------------------------------------------

fun DrawScope.drawGlossyBlockerHeart(
    tile: Tile.Blocker,
    w: Float,
    h: Float,
    shimmerRotation: Float,
    woodenHeartImage: ImageBitmap? = null,
    stoneHeartImage: ImageBitmap? = null,
) {
    val heartPath = createHeartPath(w, h, 0.08f)
    val damaged = tile.durability < tile.maxDurability

    when (tile.blockerType) {
        BlockerType.STONE_HEART -> drawStoneHeart(heartPath, w, h, damaged, stoneHeartImage)
        BlockerType.ICE_HEART -> drawIceHeart(tile, heartPath, w, h, damaged)
        BlockerType.WOODEN_HEART -> drawWoodenHeart(heartPath, w, h, damaged, woodenHeartImage)
        BlockerType.BARBED_HEART -> drawBarbedHeart(tile, heartPath, w, h)
        BlockerType.BROKEN_HEART -> drawBrokenHeart(tile, heartPath, w, h)
        BlockerType.STITCHED_HEART -> drawStitchedHeart(tile, heartPath, w, h)
        BlockerType.CHAINED_HEART -> drawBubbleHeart(tile, w, h, shimmerRotation)
        BlockerType.DARK_HEART -> drawDarkHeart(heartPath, w, h)
    }
}

private fun DrawScope.drawStoneHeart(heartPath: Path, w: Float, h: Float, damaged: Boolean, stoneHeartImage: ImageBitmap?) {
    drawHeartShadow(heartPath, h)
    if (stoneHeartImage != null) {
        drawHeartSprite(stoneHeartImage, w, h)
    } else {
        drawHeartBody(
            heartPath, w, h,
            light = Color(0xFFE0E0E3),
            main = Color(0xFF999AA2),
            dark = Color(0xFF555761),
            outline = Color(0xFF30313A)
        )
    }

    // Lift the granite mids slightly while keeping its natural texture and dark edge.
    clipPath(heartPath) {
        drawRect(color = Color(0xFFDDE2EA).copy(alpha = 0.16f), size = Size(w, h))
    }

    if (damaged) clipPath(heartPath) {
        // A pale chipped edge around the dark fissure makes the first hit visible
        // against both the pale facets and the darker granite texture.
        val crack = Path().apply {
            moveTo(w * 0.30f, h * 0.22f)
            lineTo(w * 0.44f, h * 0.40f)
            lineTo(w * 0.40f, h * 0.55f)
            lineTo(w * 0.52f, h * 0.72f)
            moveTo(w * 0.44f, h * 0.40f)
            lineTo(w * 0.66f, h * 0.34f)
            lineTo(w * 0.76f, h * 0.44f)
        }
        drawPath(crack, color = Color(0xFFE2E5EA).copy(alpha = 0.9f), style = Stroke(width = w * 0.047f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(crack, color = Color(0xFF17191F), style = Stroke(width = w * 0.024f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val extra = Path().apply {
            moveTo(w * 0.52f, h * 0.72f); lineTo(w * 0.60f, h * 0.62f); lineTo(w * 0.72f, h * 0.66f)
            moveTo(w * 0.40f, h * 0.55f); lineTo(w * 0.24f, h * 0.60f)
        }
        drawPath(extra, color = Color(0xFFC6CBD2).copy(alpha = 0.86f), style = Stroke(width = w * 0.039f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(extra, color = Color(0xFF1C1E25), style = Stroke(width = w * 0.019f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // A broken notch on the right edge makes damage easy to distinguish.
        val chip = Path().apply {
            moveTo(w * 0.86f, h * 0.39f)
            lineTo(w * 0.98f, h * 0.36f)
            lineTo(w * 0.94f, h * 0.48f)
            lineTo(w * 0.99f, h * 0.55f)
            lineTo(w * 0.84f, h * 0.52f)
            close()
        }
        drawPath(chip, color = Color(0xFF303039))
        drawPath(chip, color = Color(0xFFD7D7DE).copy(alpha = 0.75f), style = Stroke(width = w * 0.018f, join = StrokeJoin.Round))
        drawLine(Color(0xFFB8B8C1), Offset(w * 0.86f, h * 0.40f), Offset(w * 0.93f, h * 0.47f), strokeWidth = w * 0.018f)
    }
}

private fun DrawScope.drawHeartSprite(image: ImageBitmap, w: Float, h: Float) {
    val aspectRatio = image.width.toFloat() / image.height
    val maxWidth = w * 0.92f
    val maxHeight = h * 0.92f
    val imageWidth = min(maxWidth, maxHeight * aspectRatio)
    val imageHeight = imageWidth / aspectRatio
    drawImage(
        image = image,
        dstOffset = IntOffset(((w - imageWidth) / 2f).roundToInt(), ((h - imageHeight) / 2f).roundToInt()),
        dstSize = IntSize(imageWidth.roundToInt(), imageHeight.roundToInt())
    )
}

private fun DrawScope.drawIceHeart(tile: Tile.Blocker, heartPath: Path, w: Float, h: Float, damaged: Boolean) {
    val payloadColor = (tile.payloadTile as? Tile.Normal)?.color ?: tile.color
    drawHeartShadow(heartPath, h)
    if (payloadColor != null) {
        val (light, main, dark) = payloadColor.getGradients()
        drawHeartBody(heartPath, w, h, light, main, dark)
    }

    // Frosted crystal shell
    val shellAlpha = if (payloadColor != null) 0.35f else 1f
    drawPath(
        heartPath,
        brush = Brush.radialGradient(
            0f to Color(0xFFF6FCFF).copy(alpha = shellAlpha),
            0.5f to Color(0xFF9EDCFF).copy(alpha = shellAlpha),
            1f to Color(0xFF2E8FDB).copy(alpha = shellAlpha),
            center = Offset(w * 0.36f, h * 0.30f),
            radius = w * 0.80f
        )
    )

    clipPath(heartPath) {
        // Uneven crystal facets keep the ice from looking like a perfectly mirrored shell.
        val facets = Path().apply {
            moveTo(w * 0.46f, h * 0.32f); lineTo(w * 0.20f, h * 0.13f)
            moveTo(w * 0.46f, h * 0.32f); lineTo(w * 0.78f, h * 0.16f)
            moveTo(w * 0.46f, h * 0.32f); lineTo(w * 0.54f, h * 0.94f)
            moveTo(w * 0.46f, h * 0.32f); lineTo(w * 0.09f, h * 0.47f)
            moveTo(w * 0.46f, h * 0.32f); lineTo(w * 0.91f, h * 0.42f)
            moveTo(w * 0.19f, h * 0.49f); lineTo(w * 0.47f, h * 0.57f); lineTo(w * 0.79f, h * 0.53f)
        }
        drawPath(facets, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = w * 0.02f, cap = StrokeCap.Round))
        drawPath(Path().apply {
            moveTo(w * 0.46f, h * 0.32f); lineTo(w * 0.19f, h * 0.49f); lineTo(w * 0.47f, h * 0.57f); close()
        }, color = Color.White.copy(alpha = 0.30f))
        drawPath(Path().apply {
            moveTo(w * 0.47f, h * 0.57f); lineTo(w * 0.79f, h * 0.53f); lineTo(w * 0.54f, h * 0.94f); close()
        }, color = Color(0xFF1565C0).copy(alpha = 0.22f))

        if (damaged) {
            val fracture = Path().apply {
                moveTo(w * 0.31f, h * 0.35f)
                lineTo(w * 0.39f, h * 0.45f)
                lineTo(w * 0.35f, h * 0.55f)
                lineTo(w * 0.48f, h * 0.63f)
                lineTo(w * 0.43f, h * 0.75f)
                lineTo(w * 0.57f, h * 0.89f)
                moveTo(w * 0.39f, h * 0.45f)
                lineTo(w * 0.55f, h * 0.43f)
                lineTo(w * 0.68f, h * 0.34f)
                moveTo(w * 0.48f, h * 0.63f)
                lineTo(w * 0.62f, h * 0.59f)
            }
            drawPath(fracture, color = Color(0xFF0D47A1).copy(alpha = 0.7f), style = Stroke(width = w * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    drawPath(heartPath, color = Color(0xFF1B62A8), style = Stroke(width = w * 0.032f, join = StrokeJoin.Round))
    drawHeartGloss(heartPath, w, h)
}

private fun DrawScope.drawWoodenHeart(
    heartPath: Path,
    w: Float,
    h: Float,
    damaged: Boolean,
    woodenHeartImage: ImageBitmap?
) {
    drawHeartShadow(heartPath, h)
    if (woodenHeartImage != null) {
        drawHeartSprite(woodenHeartImage, w, h)
    } else {
        drawHeartBody(
            heartPath, w, h,
            light = Color(0xFFDDA35F),
            main = Color(0xFFA0642F),
            dark = Color(0xFF55300F),
            outline = Color(0xFF3A1E06)
        )
    }

    if (damaged) {
        clipPath(heartPath) {
            // Split wood fibers only appear after the blocker takes damage.
            val crack = Path().apply {
                moveTo(w * 0.56f, h * 0.28f)
                lineTo(w * 0.47f, h * 0.40f)
                lineTo(w * 0.55f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.66f)
                lineTo(w * 0.50f, h * 0.79f)
                lineTo(w * 0.46f, h * 0.96f)
                moveTo(w * 0.47f, h * 0.40f)
                lineTo(w * 0.68f, h * 0.36f)
                moveTo(w * 0.42f, h * 0.66f)
                lineTo(w * 0.27f, h * 0.59f)
            }
            drawPath(crack, color = Color(0xFF2A1404), style = Stroke(width = w * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // A dark inset and pale cut face read as a missing splinter from the wood.
            val chip = Path().apply {
                moveTo(w * 0.86f, h * 0.39f)
                lineTo(w * 0.99f, h * 0.35f)
                lineTo(w * 0.95f, h * 0.47f)
                lineTo(w * 0.99f, h * 0.54f)
                lineTo(w * 0.84f, h * 0.51f)
                close()
            }
            drawPath(chip, color = Color(0xFF321609))
            drawPath(
                Path().apply {
                    moveTo(w * 0.87f, h * 0.40f)
                    lineTo(w * 0.96f, h * 0.37f)
                    lineTo(w * 0.91f, h * 0.45f)
                    close()
                },
                color = Color(0xFFE7A75E)
            )
        }
    }
}

private fun DrawScope.drawBarbedHeart(tile: Tile.Blocker, heartPath: Path, w: Float, h: Float) {
    val color = tile.color ?: HeartColor.RED
    val (light, main, dark) = color.getGradients()
    drawHeartShadow(heartPath, h)
    drawHeartBody(heartPath, w, h, light, main, dark)
    drawHeartGloss(heartPath, w, h)

    // Keep both curved strands and every thorn inside the heart silhouette.
    clipPath(heartPath) {
        drawBarbedWire(
            listOf(
                WireCurve(Offset(-w * 0.04f, h * 0.31f), Offset(w * 0.18f, h * 0.36f), Offset(w * 0.41f, h * 0.46f), Offset(w * 0.50f, h * 0.50f)),
                WireCurve(Offset(w * 0.50f, h * 0.50f), Offset(w * 0.59f, h * 0.54f), Offset(w * 0.82f, h * 0.68f), Offset(w * 1.04f, h * 0.77f))
            ),
            w,
            twistCount = 9,
            barbCount = 3
        )
        // Draw the second strand over the first at the center of the X.
        drawBarbedWire(
            listOf(
                WireCurve(Offset(-w * 0.04f, h * 0.78f), Offset(w * 0.18f, h * 0.70f), Offset(w * 0.41f, h * 0.54f), Offset(w * 0.50f, h * 0.50f)),
                WireCurve(Offset(w * 0.50f, h * 0.50f), Offset(w * 0.59f, h * 0.46f), Offset(w * 0.82f, h * 0.36f), Offset(w * 1.04f, h * 0.30f))
            ),
            w,
            twistCount = 9,
            barbCount = 3
        )
    }
}

private data class WireCurve(val start: Offset, val control1: Offset, val control2: Offset, val end: Offset)

private fun DrawScope.drawBarbedWire(curves: List<WireCurve>, w: Float, twistCount: Int = 20, barbCount: Int = 8) {
    val wire = Path().apply {
        moveTo(curves.first().start.x, curves.first().start.y)
        curves.forEach { curve -> cubicTo(curve.control1.x, curve.control1.y, curve.control2.x, curve.control2.y, curve.end.x, curve.end.y) }
    }

    // Dark twisted core, steel body, then a narrow cool highlight for rounded metal.
    drawPath(wire, color = Color(0xFF262B32), style = Stroke(width = w * 0.047f, cap = StrokeCap.Round))
    drawPath(wire, color = Color(0xFF68727E), style = Stroke(width = w * 0.033f, cap = StrokeCap.Round))
    drawPath(wire, color = Color(0xFFD6DCE3), style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
    drawPath(wire, color = Color.White.copy(alpha = 0.70f), style = Stroke(width = w * 0.006f, cap = StrokeCap.Round))

    fun pointAt(t: Float): Offset {
        val scaled = (t.coerceIn(0f, 1f) * curves.size).coerceAtMost(curves.size - 0.0001f)
        val curve = curves[scaled.toInt()]
        val localT = scaled - scaled.toInt()
        val u = 1f - localT
        return Offset(
            u * u * u * curve.start.x + 3f * u * u * localT * curve.control1.x + 3f * u * localT * localT * curve.control2.x + localT * localT * localT * curve.end.x,
            u * u * u * curve.start.y + 3f * u * u * localT * curve.control1.y + 3f * u * localT * localT * curve.control2.y + localT * localT * localT * curve.end.y
        )
    }

    fun tangentAt(t: Float): Offset {
        val scaled = (t.coerceIn(0f, 1f) * curves.size).coerceAtMost(curves.size - 0.0001f)
        val curve = curves[scaled.toInt()]
        val localT = scaled - scaled.toInt()
        val u = 1f - localT
        val dx = 3f * u * u * (curve.control1.x - curve.start.x) + 6f * u * localT * (curve.control2.x - curve.control1.x) + 3f * localT * localT * (curve.end.x - curve.control2.x)
        val dy = 3f * u * u * (curve.control1.y - curve.start.y) + 6f * u * localT * (curve.control2.y - curve.control1.y) + 3f * localT * localT * (curve.end.y - curve.control2.y)
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        return Offset(dx / length, dy / length)
    }

    // Short diagonal wraps suggest two strands twisted together.
    for (i in 0 until twistCount) {
        val t = 0.025f + i * (0.95f / twistCount)
        val p = pointAt(t)
        val tangent = tangentAt(t)
        val normal = Offset(-tangent.y, tangent.x)
        val direction = if (i % 2 == 0) 1f else -1f
        val a = Offset(p.x - tangent.x * w * 0.017f - normal.x * w * 0.021f * direction, p.y - tangent.y * w * 0.017f - normal.y * w * 0.021f * direction)
        val b = Offset(p.x + tangent.x * w * 0.017f + normal.x * w * 0.021f * direction, p.y + tangent.y * w * 0.017f + normal.y * w * 0.021f * direction)
        drawLine(Color(0xFF454D57), a, b, strokeWidth = w * 0.008f, cap = StrokeCap.Round)
        drawLine(Color(0xFFE7EBF0).copy(alpha = 0.85f), Offset(a.x - normal.x * w * 0.006f, a.y - normal.y * w * 0.006f), Offset(b.x - normal.x * w * 0.006f, b.y - normal.y * w * 0.006f), strokeWidth = w * 0.004f, cap = StrokeCap.Round)
    }

    // Each barb is a small metal thorn rooted into the cable. Alternating sides
    // keeps the silhouette close to the reference without overcrowding the heart.
    for (i in 1..barbCount) {
        // Keep thorns away from the X intersection so the crossing stays clean.
        val t = if (barbCount == 3) {
            when (i) {
                1 -> 0.22f
                2 -> 0.39f
                else -> 0.78f
            }
        } else {
            i / (barbCount + 1f)
        }
        val p = pointAt(t)
        val tangent = tangentAt(t)
        val normal = Offset(-tangent.y, tangent.x)
        val side = if (i % 2 == 0) 1f else -1f
        val root = Offset(p.x + tangent.x * w * 0.014f, p.y + tangent.y * w * 0.014f)
        val tip = Offset(p.x + normal.x * w * 0.072f * side, p.y + normal.y * w * 0.072f * side)
        val barb = Path().apply {
            moveTo(p.x - tangent.x * w * 0.020f, p.y - tangent.y * w * 0.020f)
            lineTo(tip.x, tip.y)
            lineTo(root.x, root.y)
        }
        drawPath(barb, color = Color(0xFF303740), style = Stroke(width = w * 0.024f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(barb, color = Color(0xFFD8DEE5), style = Stroke(width = w * 0.011f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(Color.White.copy(alpha = 0.72f), Offset(tip.x - tangent.x * w * 0.01f, tip.y - tangent.y * w * 0.01f), root, strokeWidth = w * 0.004f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawBrokenHeart(tile: Tile.Blocker, heartPath: Path, w: Float, h: Float) {
    val color = tile.color ?: HeartColor.RED
    val (light, main, dark) = color.getGradients()
    drawHeartShadow(heartPath, h)
    drawHeartBody(heartPath, w, h, light, main, dark)
    drawHeartGloss(heartPath, w, h)

    // Jagged split with the board showing through the gap
    val crack = Path().apply {
        moveTo(w * 0.50f, h * 0.26f)
        lineTo(w * 0.40f, h * 0.42f)
        lineTo(w * 0.58f, h * 0.56f)
        lineTo(w * 0.44f, h * 0.74f)
        lineTo(w * 0.52f, h * 0.99f)
    }
    val repairs = (tile.maxDurability - tile.durability).coerceAtLeast(0)
    val gapWidth = w * (0.075f - repairs * 0.02f).coerceAtLeast(0.03f)
    clipPath(heartPath) {
        drawPath(crack, color = HeartColors.BoardShadow, style = Stroke(width = gapWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(crack, color = dark, style = Stroke(width = gapWidth + w * 0.02f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(crack, color = HeartColors.BoardShadow, style = Stroke(width = gapWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawStitchedHeart(tile: Tile.Blocker, heartPath: Path, w: Float, h: Float) {
    val color = tile.color ?: HeartColor.RED
    val (light, main, dark) = color.getGradients()
    drawHeartShadow(heartPath, h)
    // Slightly matte fabric body
    drawHeartBody(
        heartPath, w, h,
        light = light.copy(alpha = 0.9f).compositeOver(main),
        main = main,
        dark = dark,
        outline = Color(0xFF3A0A10)
    )

    clipPath(heartPath) {
        // Fabric patches
        val patch = Path().apply {
            moveTo(w * 0.12f, h * 0.30f); lineTo(w * 0.34f, h * 0.22f); lineTo(w * 0.30f, h * 0.46f); lineTo(w * 0.10f, h * 0.44f); close()
        }
        drawPath(patch, color = dark.copy(alpha = 0.85f))
        drawPath(patch, color = Color(0xFFFFE6E6), style = Stroke(width = w * 0.012f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.035f, w * 0.03f))))

        val patch2 = Path().apply {
            moveTo(w * 0.62f, h * 0.58f); lineTo(w * 0.82f, h * 0.50f); lineTo(w * 0.74f, h * 0.74f); lineTo(w * 0.58f, h * 0.70f); close()
        }
        drawPath(patch2, color = light.copy(alpha = 0.5f))
        drawPath(patch2, color = Color(0xFFFFE6E6), style = Stroke(width = w * 0.012f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.035f, w * 0.03f))))

        // Central seam
        val seam = Path().apply {
            moveTo(w * 0.50f, h * 0.27f)
            cubicTo(w * 0.46f, h * 0.45f, w * 0.54f, h * 0.65f, w * 0.50f, h * 0.95f)
        }
        drawPath(seam, color = Color(0xFF3A0A10), style = Stroke(width = w * 0.028f, cap = StrokeCap.Round))

        // Cross stitches
        val stitches = 5
        for (i in 0 until stitches) {
            val t = (i + 0.5f) / stitches
            val y = h * (0.30f + t * 0.60f)
            val x = w * (0.50f - 0.03f * sin(t * PI.toFloat() * 2f))
            val r = w * 0.055f
            drawLine(Color(0xFFFFF3E0), Offset(x - r, y - r * 0.6f), Offset(x + r, y + r * 0.6f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFFF3E0), Offset(x - r, y + r * 0.6f), Offset(x + r, y - r * 0.6f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
        }
    }
    drawHeartGloss(heartPath, w, h, intensity = 0.55f)
}

private fun DrawScope.drawBubbleHeart(tile: Tile.Blocker, w: Float, h: Float, shimmerRotation: Float) {
    val payloadColor = (tile.payloadTile as? Tile.Normal)?.color ?: tile.color ?: HeartColor.PINK
    val center = Offset(w / 2, h / 2)
    val radius = min(w, h) * 0.48f

    // Keep a little breathing room inside the bubble; the released heart renders at full size.
    scale(0.88f, pivot = center) {
        drawGlossyNormalHeart(payloadColor, w, h)
    }

    // Translucent soap bubble
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.02f),
            0.72f to Color(0xFFBFE6FF).copy(alpha = 0.10f),
            0.95f to Color(0xFF8CCBF7).copy(alpha = 0.40f),
            1f to Color(0xFF5C9BD6).copy(alpha = 0.55f),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(Color(0xFF3F7FBF).copy(alpha = 0.85f), radius = radius, center = center, style = Stroke(width = w * 0.028f))
    drawCircle(Color.White.copy(alpha = 0.55f), radius = radius - w * 0.03f, center = center, style = Stroke(width = w * 0.012f))

    // Highlights
    val arcBox = Size(radius * 1.6f, radius * 1.6f)
    val arcTopLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f)
    drawArc(Color.White.copy(alpha = 0.92f), startAngle = 200f, sweepAngle = 50f, useCenter = false, topLeft = arcTopLeft, size = arcBox, style = Stroke(width = w * 0.04f, cap = StrokeCap.Round))
    drawArc(Color.White.copy(alpha = 0.5f), startAngle = 25f, sweepAngle = 30f, useCenter = false, topLeft = arcTopLeft, size = arcBox, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round))
    drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.04f, center = Offset(center.x - radius * 0.45f, center.y - radius * 0.58f))

    val sparkle = Offset(center.x + radius * 0.55f, center.y - radius * 0.45f)
    rotate(shimmerRotation, pivot = sparkle) {
        drawStar(sparkle, outerRadius = w * 0.05f, innerRadius = w * 0.015f, color = Color.White.copy(alpha = 0.85f))
    }
}

private fun DrawScope.drawDarkHeart(heartPath: Path, w: Float, h: Float) {
    drawHeartShadow(heartPath, h)
    drawHeartBody(
        heartPath, w, h,
        light = Color(0xFF7D6790),
        main = Color(0xFF2B1F36),
        dark = Color(0xFF07040B),
        outline = Color(0xFF000000)
    )
    // Faint violet sheen creeping from the bottom
    clipPath(heartPath) {
        drawPath(
            heartPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFF6A1B9A).copy(alpha = 0.35f)),
                startY = h * 0.45f,
                endY = h
            )
        )
    }
    drawHeartGloss(heartPath, w, h, intensity = 0.8f)
}

// ---------------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------------

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f
    )
}

fun DrawScope.drawDoubleArrow(center: Offset, length: Float, isHorizontal: Boolean, color: Color) {
    val half = length / 2
    val arrowSize = 6.dp.toPx()
    val stroke = 3.dp.toPx()
    if (isHorizontal) {
        drawLine(color = color, start = Offset(center.x - half, center.y), end = Offset(center.x + half, center.y), strokeWidth = stroke, cap = StrokeCap.Round)
        // Left head
        drawLine(color = color, start = Offset(center.x - half, center.y), end = Offset(center.x - half + arrowSize, center.y - arrowSize), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(center.x - half, center.y), end = Offset(center.x - half + arrowSize, center.y + arrowSize), strokeWidth = stroke, cap = StrokeCap.Round)
        // Right head
        drawLine(color = color, start = Offset(center.x + half, center.y), end = Offset(center.x + half - arrowSize, center.y - arrowSize), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(center.x + half, center.y), end = Offset(center.x + half - arrowSize, center.y + arrowSize), strokeWidth = stroke, cap = StrokeCap.Round)
    } else {
        drawLine(color = color, start = Offset(center.x, center.y - half), end = Offset(center.x, center.y + half), strokeWidth = stroke, cap = StrokeCap.Round)
        // Top head
        drawLine(color = color, start = Offset(center.x, center.y - half), end = Offset(center.x - arrowSize, center.y - half + arrowSize), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(center.x, center.y - half), end = Offset(center.x + arrowSize, center.y - half + arrowSize), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom head
        drawLine(color = color, start = Offset(center.x, center.y + half), end = Offset(center.x - arrowSize, center.y + half - arrowSize), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(center.x, center.y + half), end = Offset(center.x + arrowSize, center.y + half - arrowSize), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    numPoints: Int = 4
) {
    val path = createStarPath(center, outerRadius, innerRadius, numPoints)
    drawPath(path, color = color, style = Fill)
}
