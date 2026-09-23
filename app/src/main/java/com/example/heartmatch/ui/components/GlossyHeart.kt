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
import androidx.compose.ui.unit.dp
import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.FireDirection
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
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
    isHinted: Boolean = false
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
                    drawGlossyBlockerHeart(tile, w, h, shimmerRotation)
                }
            }

            if (isSelected) {
                drawPath(createHeartPath(w, h), color = Color.White.copy(alpha = 0.18f))
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
    }
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
    shimmerRotation: Float
) {
    val heartPath = createHeartPath(w, h, 0.08f)
    val damaged = tile.durability < tile.maxDurability

    when (tile.blockerType) {
        BlockerType.STONE_HEART -> drawStoneHeart(heartPath, w, h, damaged)
        BlockerType.ICE_HEART -> drawIceHeart(tile, heartPath, w, h, damaged)
        BlockerType.WOODEN_HEART -> drawWoodenHeart(heartPath, w, h, damaged)
        BlockerType.BARBED_HEART -> drawBarbedHeart(tile, heartPath, w, h)
        BlockerType.BROKEN_HEART -> drawBrokenHeart(tile, heartPath, w, h)
        BlockerType.STITCHED_HEART -> drawStitchedHeart(tile, heartPath, w, h)
        BlockerType.CHAINED_HEART -> drawBubbleHeart(tile, w, h, shimmerRotation)
        BlockerType.DARK_HEART -> drawDarkHeart(heartPath, w, h)
    }
}

private fun DrawScope.drawStoneHeart(heartPath: Path, w: Float, h: Float, damaged: Boolean) {
    drawHeartShadow(heartPath, h)
    drawHeartBody(
        heartPath, w, h,
        light = Color(0xFFC9C9CF),
        main = Color(0xFF80808A),
        dark = Color(0xFF3F3F48),
        outline = Color(0xFF23232B)
    )

    clipPath(heartPath) {
        // Rocky facets
        val facetLight = Color.White.copy(alpha = 0.14f)
        val facetDark = Color.Black.copy(alpha = 0.18f)
        drawPath(Path().apply {
            moveTo(w * 0.18f, h * 0.30f); lineTo(w * 0.40f, h * 0.24f); lineTo(w * 0.36f, h * 0.48f); lineTo(w * 0.14f, h * 0.46f); close()
        }, facetLight)
        drawPath(Path().apply {
            moveTo(w * 0.56f, h * 0.20f); lineTo(w * 0.82f, h * 0.30f); lineTo(w * 0.70f, h * 0.50f); lineTo(w * 0.52f, h * 0.42f); close()
        }, facetDark)
        drawPath(Path().apply {
            moveTo(w * 0.36f, h * 0.55f); lineTo(w * 0.66f, h * 0.58f); lineTo(w * 0.50f, h * 0.86f); close()
        }, facetLight)

        // Crack lines
        val crack = Path().apply {
            moveTo(w * 0.30f, h * 0.22f)
            lineTo(w * 0.44f, h * 0.40f)
            lineTo(w * 0.40f, h * 0.55f)
            lineTo(w * 0.52f, h * 0.72f)
            moveTo(w * 0.44f, h * 0.40f)
            lineTo(w * 0.66f, h * 0.34f)
            lineTo(w * 0.76f, h * 0.44f)
        }
        drawPath(crack, color = Color(0xFF23232B), style = Stroke(width = w * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (damaged) {
            val extra = Path().apply {
                moveTo(w * 0.52f, h * 0.72f); lineTo(w * 0.60f, h * 0.62f); lineTo(w * 0.72f, h * 0.66f)
                moveTo(w * 0.40f, h * 0.55f); lineTo(w * 0.24f, h * 0.60f)
            }
            drawPath(extra, color = Color(0xFF15151B), style = Stroke(width = w * 0.03f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
    drawHeartGloss(heartPath, w, h, intensity = 0.45f)
}

private fun DrawScope.drawIceHeart(tile: Tile.Blocker, heartPath: Path, w: Float, h: Float, damaged: Boolean) {
    val payloadColor = (tile.payloadTile as? Tile.Normal)?.color ?: tile.color
    drawHeartShadow(heartPath, h)
    if (payloadColor != null) {
        val (light, main, dark) = payloadColor.getGradients()
        drawHeartBody(heartPath, w, h, light, main, dark)
    }

    // Frosted crystal shell
    val shellAlpha = if (payloadColor != null) 0.86f else 1f
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
        // Faceted crystal geometry
        val facets = Path().apply {
            moveTo(w * 0.5f, h * 0.30f); lineTo(w * 0.18f, h * 0.14f)
            moveTo(w * 0.5f, h * 0.30f); lineTo(w * 0.82f, h * 0.14f)
            moveTo(w * 0.5f, h * 0.30f); lineTo(w * 0.5f, h * 0.96f)
            moveTo(w * 0.5f, h * 0.30f); lineTo(w * 0.08f, h * 0.44f)
            moveTo(w * 0.5f, h * 0.30f); lineTo(w * 0.92f, h * 0.44f)
            moveTo(w * 0.22f, h * 0.50f); lineTo(w * 0.5f, h * 0.60f); lineTo(w * 0.78f, h * 0.50f)
        }
        drawPath(facets, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = w * 0.02f, cap = StrokeCap.Round))
        drawPath(Path().apply {
            moveTo(w * 0.5f, h * 0.30f); lineTo(w * 0.22f, h * 0.50f); lineTo(w * 0.5f, h * 0.60f); close()
        }, color = Color.White.copy(alpha = 0.30f))
        drawPath(Path().apply {
            moveTo(w * 0.5f, h * 0.60f); lineTo(w * 0.78f, h * 0.50f); lineTo(w * 0.5f, h * 0.96f); close()
        }, color = Color(0xFF1565C0).copy(alpha = 0.22f))

        if (damaged) {
            val fracture = Path().apply {
                moveTo(w * 0.30f, h * 0.36f); lineTo(w * 0.44f, h * 0.52f); lineTo(w * 0.38f, h * 0.70f); lineTo(w * 0.50f, h * 0.84f)
                moveTo(w * 0.44f, h * 0.52f); lineTo(w * 0.70f, h * 0.40f)
            }
            drawPath(fracture, color = Color(0xFF0D47A1).copy(alpha = 0.7f), style = Stroke(width = w * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    drawPath(heartPath, color = Color(0xFF1B62A8), style = Stroke(width = w * 0.032f, join = StrokeJoin.Round))
    drawHeartGloss(heartPath, w, h)
}

private fun DrawScope.drawWoodenHeart(heartPath: Path, w: Float, h: Float, damaged: Boolean) {
    drawHeartShadow(heartPath, h)
    drawHeartBody(
        heartPath, w, h,
        light = Color(0xFFDDA35F),
        main = Color(0xFFA0642F),
        dark = Color(0xFF55300F),
        outline = Color(0xFF3A1E06)
    )

    clipPath(heartPath) {
        // Vertical wood grain
        val grain = Color(0xFF4A2A0C).copy(alpha = 0.45f)
        for (i in 0..6) {
            val x = w * (0.14f + i * 0.12f)
            val wave = Path().apply {
                moveTo(x, 0f)
                cubicTo(x + w * 0.03f, h * 0.3f, x - w * 0.03f, h * 0.6f, x + w * 0.02f, h)
            }
            drawPath(wave, color = grain, style = Stroke(width = w * (if (i % 2 == 0) 0.016f else 0.010f)))
        }
        // Central plank seam
        drawLine(Color(0xFF3A1E06), Offset(w * 0.5f, h * 0.27f), Offset(w * 0.5f, h * 0.98f), strokeWidth = w * 0.02f)

        // Jagged crack
        val crack = Path().apply {
            moveTo(w * 0.5f, h * 0.27f)
            lineTo(w * 0.44f, h * 0.42f)
            lineTo(w * 0.54f, h * 0.55f)
            lineTo(w * 0.46f, h * 0.72f)
            lineTo(w * 0.5f, h * 0.98f)
        }
        drawPath(crack, color = Color(0xFF2A1404), style = Stroke(width = w * (if (damaged) 0.05f else 0.03f), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (damaged) {
            drawPath(Path().apply {
                moveTo(w * 0.54f, h * 0.55f); lineTo(w * 0.74f, h * 0.48f)
                moveTo(w * 0.44f, h * 0.42f); lineTo(w * 0.24f, h * 0.36f)
            }, color = Color(0xFF2A1404), style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
        }
    }
    drawHeartGloss(heartPath, w, h, intensity = 0.4f)
}

private fun DrawScope.drawBarbedHeart(tile: Tile.Blocker, heartPath: Path, w: Float, h: Float) {
    val color = tile.color ?: HeartColor.RED
    val (light, main, dark) = color.getGradients()
    drawHeartShadow(heartPath, h)
    drawHeartBody(heartPath, w, h, light, main, dark)
    drawHeartGloss(heartPath, w, h)

    drawBarbedWire(Offset(w * 0.08f, h * 0.30f), Offset(w * 0.92f, h * 0.62f), w)
    drawBarbedWire(Offset(w * 0.10f, h * 0.60f), Offset(w * 0.90f, h * 0.28f), w)
    drawBarbedWire(Offset(w * 0.30f, h * 0.10f), Offset(w * 0.56f, h * 0.94f), w)
}

private fun DrawScope.drawBarbedWire(start: Offset, end: Offset, w: Float) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy)
    val ux = dx / len
    val uy = dy / len
    val nx = -uy
    val ny = ux

    drawLine(Color(0xFF3F3F47), start, end, strokeWidth = w * 0.055f, cap = StrokeCap.Round)
    drawLine(Color(0xFFC9C9D1), start, end, strokeWidth = w * 0.026f, cap = StrokeCap.Round)

    val barbs = 4
    val s = w * 0.05f
    for (i in 1..barbs) {
        val t = i / (barbs + 1f)
        val p = Offset(start.x + dx * t, start.y + dy * t)
        val a1 = Offset(p.x - ux * s * 0.6f + nx * s, p.y - uy * s * 0.6f + ny * s)
        val a2 = Offset(p.x + ux * s * 0.6f - nx * s, p.y + uy * s * 0.6f - ny * s)
        val b1 = Offset(p.x + ux * s * 0.6f + nx * s, p.y + uy * s * 0.6f + ny * s)
        val b2 = Offset(p.x - ux * s * 0.6f - nx * s, p.y - uy * s * 0.6f - ny * s)
        drawLine(Color(0xFF3F3F47), a1, a2, strokeWidth = w * 0.03f, cap = StrokeCap.Round)
        drawLine(Color(0xFF3F3F47), b1, b2, strokeWidth = w * 0.03f, cap = StrokeCap.Round)
        drawLine(Color(0xFFE0E0E6), a1, a2, strokeWidth = w * 0.014f, cap = StrokeCap.Round)
        drawLine(Color(0xFFE0E0E6), b1, b2, strokeWidth = w * 0.014f, cap = StrokeCap.Round)
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

    // Trapped heart
    scale(0.74f, pivot = center) {
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
