package com.example.heartmatch.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.FireDirection
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object HeartColors {
    val RedLight = Color(0xFFFF5277)
    val RedMain = Color(0xFFE91E63)
    val RedDark = Color(0xFF880E4F)
    val RedGloss = Color(0xFFFFD4E0)

    val PinkLight = Color(0xFFFF80AB)
    val PinkMain = Color(0xFFFF4081)
    val PinkDark = Color(0xFFC2185B)
    val PinkGloss = Color(0xFFFFE0EB)

    val BlueLight = Color(0xFF40C4FF)
    val BlueMain = Color(0xFF0091EA)
    val BlueDark = Color(0xFF01579B)
    val BlueGloss = Color(0xFFE1F5FE)

    val GreenLight = Color(0xFF69F0AE)
    val GreenMain = Color(0xFF00E676)
    val GreenDark = Color(0xFF007E33)
    val GreenGloss = Color(0xFFE8F5E9)

    val YellowLight = Color(0xFFFFFF00)
    val YellowMain = Color(0xFFFFD600)
    val YellowDark = Color(0xFFFF6D00)
    val YellowGloss = Color(0xFFFFFDE7)

    val PurpleLight = Color(0xFFE040FB)
    val PurpleMain = Color(0xFFAA00FF)
    val PurpleDark = Color(0xFF4A148C)
    val PurpleGloss = Color(0xFFF3E5F5)

    val OrangeLight = Color(0xFFFFAB40)
    val OrangeMain = Color(0xFFFF6D00)
    val OrangeDark = Color(0xFFBF360C)
    val OrangeGloss = Color(0xFFFFF3E0)
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

fun createHeartPath(width: Float, height: Float, paddingRatio: Float = 0.08f): Path {
    val padX = width * paddingRatio
    val padY = height * paddingRatio
    val w = width - padX * 2
    val h = height - padY * 2
    val left = padX
    val top = padY

    val path = Path()
    path.moveTo(left + w * 0.5f, top + h * 0.28f)
    // Left lobe
    path.cubicTo(
        left + w * 0.5f, top + h * 0.05f,
        left, top + h * 0.05f,
        left, top + h * 0.38f
    )
    path.cubicTo(
        left, top + h * 0.65f,
        left + w * 0.32f, top + h * 0.85f,
        left + w * 0.5f, top + h * 1.0f
    )
    // Right lobe
    path.cubicTo(
        left + w * 0.68f, top + h * 0.85f,
        left + w * 1.0f, top + h * 0.65f,
        left + w * 1.0f, top + h * 0.38f
    )
    path.cubicTo(
        left + w * 1.0f, top + h * 0.05f,
        left + w * 0.5f, top + h * 0.05f,
        left + w * 0.5f, top + h * 0.28f
    )
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
                val haloPath = createHeartPath(w, h, paddingRatio = 0.02f)
                drawPath(
                    path = haloPath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.8f), Color(0xFFFFD700).copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(w / 2, h / 2),
                        radius = w * 0.6f
                    ),
                    style = Stroke(width = 6.dp.toPx())
                )
            } else if (isHinted) {
                val haloPath = createHeartPath(w, h, paddingRatio = 0.04f)
                drawPath(
                    path = haloPath,
                    color = Color(0xFFFFEB3B).copy(alpha = 0.7f),
                    style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
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
        }
    }
}

fun DrawScope.drawGlossyNormalHeart(
    color: HeartColor,
    w: Float,
    h: Float,
    paddingRatio: Float = 0.08f
) {
    val heartPath = createHeartPath(w, h, paddingRatio)
    val (light, main, dark) = color.getGradients()
    val glossColor = color.getGlossColor()

    // 1. Drop shadow / 3D Bottom Base
    drawPath(
        path = heartPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, dark.copy(alpha = 0.6f)),
            startY = h * 0.5f,
            endY = h
        )
    )

    // 2. Main 3D Radial Body
    drawPath(
        path = heartPath,
        brush = Brush.radialGradient(
            colors = listOf(light, main, dark),
            center = Offset(w * 0.35f, h * 0.32f),
            radius = w * 0.65f
        )
    )

    // 3. Inner Bevel Highlight Rim
    drawPath(
        path = heartPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent, dark.copy(alpha = 0.4f)),
            startY = 0f,
            endY = h
        ),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // 4. Specular Gloss Oval (Left Lobe)
    clipPath(heartPath) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f), glossColor.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(w * 0.30f, h * 0.26f),
                radius = w * 0.22f
            ),
            topLeft = Offset(w * 0.16f, h * 0.14f),
            size = Size(w * 0.28f, h * 0.24f)
        )

        // Subtle specular on right lobe
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(w * 0.70f, h * 0.28f),
                radius = w * 0.14f
            ),
            topLeft = Offset(w * 0.60f, h * 0.20f),
            size = Size(w * 0.20f, h * 0.16f)
        )

        // Bottom curved rim reflection
        drawArc(
            color = Color.White.copy(alpha = 0.25f),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.25f, h * 0.65f),
            size = Size(w * 0.5f, h * 0.25f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

fun DrawScope.drawGlossySpecialHeart(
    tile: Tile.Special,
    w: Float,
    h: Float,
    shimmerRotation: Float
) {
    val heartPath = createHeartPath(w, h, 0.08f)

    when (tile.specialType) {
        SpecialHeartType.RAINBOW_HEART -> {
            // Prismatic rainbow sweep
            val rainbowColors = listOf(
                Color(0xFFFF1744),
                Color(0xFFFF9100),
                Color(0xFFFFEA00),
                Color(0xFF00E676),
                Color(0xFF00E5FF),
                Color(0xFF651FFF),
                Color(0xFFFF1744)
            )

            drawPath(
                path = heartPath,
                brush = Brush.sweepGradient(
                    colors = rainbowColors,
                    center = Offset(w / 2, h / 2)
                )
            )

            // Outer Celestial Shimmer Halo
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(w / 2, h / 2),
                    radius = w * 0.55f
                ),
                style = Stroke(width = 4.dp.toPx())
            )

            // Star Sparkle overlay
            rotate(shimmerRotation, pivot = Offset(w / 2, h / 2)) {
                drawStar(Offset(w / 2, h / 2), outerRadius = w * 0.22f, innerRadius = w * 0.08f, color = Color.White)
            }
            drawStar(Offset(w * 0.3f, h * 0.3f), outerRadius = w * 0.1f, innerRadius = w * 0.04f, color = Color.White.copy(alpha = 0.9f))
            drawStar(Offset(w * 0.7f, h * 0.4f), outerRadius = w * 0.08f, innerRadius = w * 0.03f, color = Color.White.copy(alpha = 0.8f))
        }

        SpecialHeartType.FIRE_HEART -> {
            // Base fiery heart
            val baseColor = tile.baseColor ?: HeartColor.RED
            val (light, main, dark) = baseColor.getGradients()
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD54F), Color(0xFFFF3D00), dark),
                    center = Offset(w * 0.4f, h * 0.35f),
                    radius = w * 0.6f
                )
            )

            // Directional flame arrows / lasers
            clipPath(heartPath) {
                when (tile.fireDirection) {
                    FireDirection.ROW -> {
                        // Horizontal flame stripe & arrows
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFFFFEB3B).copy(alpha = 0.8f), Color.Transparent),
                                startY = h * 0.35f,
                                endY = h * 0.65f
                            ),
                            topLeft = Offset(0f, h * 0.35f),
                            size = Size(w, h * 0.30f)
                        )
                        drawDoubleArrow(Offset(w / 2, h / 2), length = w * 0.6f, isHorizontal = true, color = Color.White)
                    }
                    FireDirection.COLUMN -> {
                        // Vertical flame stripe & arrows
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color(0xFFFFEB3B).copy(alpha = 0.8f), Color.Transparent),
                                startX = w * 0.35f,
                                endX = w * 0.65f
                            ),
                            topLeft = Offset(w * 0.35f, 0f),
                            size = Size(w * 0.30f, h)
                        )
                        drawDoubleArrow(Offset(w / 2, h / 2), length = h * 0.6f, isHorizontal = false, color = Color.White)
                    }
                    FireDirection.BOTH, null -> {
                        // Cross arrows
                        drawDoubleArrow(Offset(w / 2, h / 2), length = w * 0.55f, isHorizontal = true, color = Color.White)
                        drawDoubleArrow(Offset(w / 2, h / 2), length = h * 0.55f, isHorizontal = false, color = Color.White)
                    }
                }
            }
            drawGlossOverlay(heartPath, w, h)
        }

        SpecialHeartType.BOMB_HEART -> {
            // Explosive dark crimson/charcoal core with gold fuse
            val baseColor = tile.baseColor ?: HeartColor.PURPLE
            val (_, main, _) = baseColor.getGradients()

            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF5252), Color(0xFF424242), Color(0xFF212121)),
                    center = Offset(w * 0.4f, h * 0.4f),
                    radius = w * 0.6f
                )
            )

            // Glowing bomb fuse & spark in center
            drawCircle(
                color = Color(0xFFFFD700),
                radius = w * 0.16f,
                center = Offset(w / 2, h * 0.48f)
            )
            drawCircle(
                color = Color(0xFFFF3D00),
                radius = w * 0.10f,
                center = Offset(w / 2, h * 0.48f)
            )
            drawStar(Offset(w / 2, h * 0.48f), outerRadius = w * 0.15f, innerRadius = w * 0.06f, color = Color.White)

            // Bomb blast ring
            drawPath(
                path = heartPath,
                color = Color(0xFFFFC107).copy(alpha = 0.8f),
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
            )
        }

        SpecialHeartType.GIFT_HEART -> {
            // Festive golden wrapped gift heart
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF80AB), Color(0xFFE91E63), Color(0xFF880E4F)),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Golden ribbon cross
            clipPath(heartPath) {
                // Vertical gold band
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFF9C4), Color(0xFFFFB300)),
                        startX = w * 0.40f,
                        endX = w * 0.60f
                    ),
                    topLeft = Offset(w * 0.40f, 0f),
                    size = Size(w * 0.20f, h)
                )
                // Horizontal gold band
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFF9C4), Color(0xFFFFB300)),
                        startY = h * 0.40f,
                        endY = h * 0.60f
                    ),
                    topLeft = Offset(0f, h * 0.40f),
                    size = Size(w, h * 0.20f)
                )
            }

            // 3D Tied Bow at center
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFB300)), center = Offset(w / 2, h * 0.48f), radius = w * 0.12f),
                radius = w * 0.12f,
                center = Offset(w / 2, h * 0.48f)
            )
            drawStar(Offset(w / 2, h * 0.48f), outerRadius = w * 0.12f, innerRadius = w * 0.05f, color = Color.White)
        }

        SpecialHeartType.ROYAL_HEART -> {
            // Radiant Golden Royal Jewel Heart
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF176), Color(0xFFFFD54F), Color(0xFFFF8F00), Color(0xFFE65100)),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Outer Golden Bevel Rim
            drawPath(
                path = heartPath,
                brush = Brush.sweepGradient(
                    listOf(Color(0xFFFFD700), Color(0xFFFFF9C4), Color(0xFFFFB300), Color(0xFFFFD700)),
                    center = Offset(w / 2, h / 2)
                ),
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Golden Crown atop Heart
            val crownPath = Path()
            val crownTop = h * 0.08f
            val crownBottom = h * 0.26f
            crownPath.moveTo(w * 0.30f, crownBottom)
            crownPath.lineTo(w * 0.24f, crownTop + h * 0.04f)
            crownPath.lineTo(w * 0.38f, crownTop + h * 0.09f)
            crownPath.lineTo(w * 0.50f, crownTop)
            crownPath.lineTo(w * 0.62f, crownTop + h * 0.09f)
            crownPath.lineTo(w * 0.76f, crownTop + h * 0.04f)
            crownPath.lineTo(w * 0.70f, crownBottom)
            crownPath.close()

            drawPath(
                path = crownPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF59D), Color(0xFFFFD700), Color(0xFFFF8F00)),
                    startY = crownTop,
                    endY = crownBottom
                )
            )
            drawPath(crownPath, color = Color(0xFF795548), style = Stroke(width = 1.5.dp.toPx()))

            // Crown Jewels
            drawCircle(Color(0xFFFF1744), radius = w * 0.025f, center = Offset(w * 0.50f, crownTop + h * 0.02f))
            drawCircle(Color(0xFF2979FF), radius = w * 0.020f, center = Offset(w * 0.26f, crownTop + h * 0.05f))
            drawCircle(Color(0xFF00E676), radius = w * 0.020f, center = Offset(w * 0.74f, crownTop + h * 0.05f))

            drawGlossOverlay(heartPath, w, h)
        }

        SpecialHeartType.ANGEL_HEART -> {
            // Celestial Glowing Golden-White Angel Heart
            // 1. Feathered Wings Left & Right
            val leftWing = Path()
            leftWing.moveTo(w * 0.28f, h * 0.45f)
            leftWing.cubicTo(w * 0.10f, h * 0.25f, 0f, h * 0.30f, 0f, h * 0.48f)
            leftWing.cubicTo(0f, h * 0.65f, w * 0.12f, h * 0.75f, w * 0.30f, h * 0.60f)
            leftWing.close()

            val rightWing = Path()
            rightWing.moveTo(w * 0.72f, h * 0.45f)
            rightWing.cubicTo(w * 0.90f, h * 0.25f, w, h * 0.30f, w, h * 0.48f)
            rightWing.cubicTo(w, h * 0.65f, w * 0.88f, h * 0.75f, w * 0.70f, h * 0.60f)
            rightWing.close()

            val wingBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFCDD2), Color(0xFFF8BBD0)),
                startY = h * 0.25f,
                endY = h * 0.75f
            )
            drawPath(leftWing, brush = wingBrush)
            drawPath(rightWing, brush = wingBrush)
            drawPath(leftWing, color = Color(0xFFF06292).copy(alpha = 0.5f), style = Stroke(width = 1.5.dp.toPx()))
            drawPath(rightWing, color = Color(0xFFF06292).copy(alpha = 0.5f), style = Stroke(width = 1.5.dp.toPx()))

            // 2. Central Angelic Golden-Cream Heart
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFE082), Color(0xFFFFB74D)),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Golden Divine Halo Outline
            drawPath(
                path = heartPath,
                color = Color(0xFFFFD54F).copy(alpha = 0.8f),
                style = Stroke(width = 2.5.dp.toPx())
            )

            drawGlossOverlay(heartPath, w, h)
        }
    }
}

fun DrawScope.drawGlossyBlockerHeart(
    tile: Tile.Blocker,
    w: Float,
    h: Float,
    shimmerRotation: Float
) {
    val heartPath = createHeartPath(w, h, 0.08f)

    when (tile.blockerType) {
        BlockerType.STONE_HEART -> {
            // Chiseled Granite Slate Stone Heart
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFB0BEC5), Color(0xFF607D8B), Color(0xFF263238)),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Crack lines
            drawPath(
                path = heartPath,
                color = Color(0xFF1B2429),
                style = Stroke(width = 3.dp.toPx())
            )

            val crackPath = Path()
            crackPath.moveTo(w * 0.3f, h * 0.3f)
            crackPath.lineTo(w * 0.5f, h * 0.5f)
            crackPath.lineTo(w * 0.45f, h * 0.7f)
            crackPath.moveTo(w * 0.5f, h * 0.5f)
            crackPath.lineTo(w * 0.75f, h * 0.4f)
            drawPath(crackPath, color = Color(0xFF1B2429), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        BlockerType.ICE_HEART -> {
            // Underlying payload heart or default blue heart
            val payload = tile.payloadTile
            if (payload is Tile.Normal) {
                drawGlossyNormalHeart(payload.color, w, h)
            } else {
                drawGlossyNormalHeart(HeartColor.BLUE, w, h)
            }

            // Translucent Frosted Cyan Ice Overlay
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xBBFFFFFF), Color(0xAA80DEEA), Color(0x8800ACC1)),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Crystalline Ice Frost Border & Fractures
            drawPath(
                path = heartPath,
                color = Color.White.copy(alpha = 0.9f),
                style = Stroke(width = 3.dp.toPx())
            )
            val iceCrack = Path()
            iceCrack.moveTo(w * 0.25f, h * 0.35f)
            iceCrack.lineTo(w * 0.45f, h * 0.55f)
            iceCrack.lineTo(w * 0.70f, h * 0.30f)
            iceCrack.moveTo(w * 0.45f, h * 0.55f)
            iceCrack.lineTo(w * 0.50f, h * 0.85f)
            drawPath(iceCrack, color = Color.White.copy(alpha = 0.95f), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }

        BlockerType.WOODEN_HEART -> {
            // Cedar wood plank texture
            drawPath(
                path = heartPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFD7CCC8), Color(0xFFA1887F), Color(0xFF4E342E)),
                    startY = 0f,
                    endY = h
                )
            )

            // Horizontal plank dividers & wood grain
            clipPath(heartPath) {
                for (i in 1..3) {
                    val y = h * (i * 0.25f)
                    drawLine(
                        color = Color(0xFF3E2723),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 3.dp.toPx()
                    )
                    // Rivets / Nails
                    drawCircle(color = Color(0xFF2E1C18), radius = 2.5.dp.toPx(), center = Offset(w * 0.25f, y - 6.dp.toPx()))
                    drawCircle(color = Color(0xFF2E1C18), radius = 2.5.dp.toPx(), center = Offset(w * 0.75f, y - 6.dp.toPx()))
                }
            }

            drawPath(
                path = heartPath,
                color = Color(0xFF3E2723),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        BlockerType.BARBED_HEART -> {
            // Base dark metallic heart
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF78909C), Color(0xFF37474F), Color(0xFF212121)),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Barbed Wire wrapping
            val wirePath = Path()
            wirePath.moveTo(w * 0.15f, h * 0.35f)
            wirePath.lineTo(w * 0.85f, h * 0.65f)
            wirePath.moveTo(w * 0.15f, h * 0.65f)
            wirePath.lineTo(w * 0.85f, h * 0.35f)

            drawPath(wirePath, color = Color(0xFFCFD8DC), style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))

            // Barbed thorns
            drawStar(Offset(w * 0.35f, h * 0.45f), outerRadius = 6.dp.toPx(), innerRadius = 2.dp.toPx(), color = Color(0xFFFF5252))
            drawStar(Offset(w * 0.65f, h * 0.55f), outerRadius = 6.dp.toPx(), innerRadius = 2.dp.toPx(), color = Color(0xFFFF5252))
        }

        BlockerType.BROKEN_HEART -> {
            // Split jagged broken heart
            val (light, main, dark) = HeartColor.RED.getGradients()
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(light, main, dark),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Jagged fracture line down center
            val splitPath = Path()
            splitPath.moveTo(w * 0.5f, h * 0.28f)
            splitPath.lineTo(w * 0.40f, h * 0.45f)
            splitPath.lineTo(w * 0.60f, h * 0.60f)
            splitPath.lineTo(w * 0.45f, h * 0.78f)
            splitPath.lineTo(w * 0.50f, h * 1.0f)

            drawPath(splitPath, color = Color(0xFF212121), style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))

            // Bandage / Stitch across break
            drawLine(
                color = Color(0xFFFFF9C4),
                start = Offset(w * 0.30f, h * 0.50f),
                end = Offset(w * 0.70f, h * 0.54f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFFF9C4),
                start = Offset(w * 0.35f, h * 0.68f),
                end = Offset(w * 0.65f, h * 0.72f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        BlockerType.STITCHED_HEART -> {
            // Crimson Leather / Fabric Stitched Heart
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F), Color(0xFF7F0000)),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Vertical Center Suture Seam
            val seamPath = Path()
            seamPath.moveTo(w * 0.50f, h * 0.15f)
            seamPath.cubicTo(w * 0.48f, h * 0.40f, w * 0.52f, h * 0.65f, w * 0.50f, h * 0.90f)
            drawPath(seamPath, color = Color(0xFF4A0000), style = Stroke(width = 3.dp.toPx()))

            // Cross Stitches across seam
            for (i in 1..6) {
                val progress = i / 7f
                val y = h * (0.20f + progress * 0.65f)
                val x = w * (0.50f + sin(progress * PI.toFloat()) * 0.02f)
                drawLine(
                    color = Color(0xFFFFFFFF),
                    start = Offset(x - w * 0.08f, y - h * 0.02f),
                    end = Offset(x + w * 0.08f, y + h * 0.02f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Knot points
                drawCircle(Color(0xFF3E2723), radius = 2.dp.toPx(), center = Offset(x - w * 0.08f, y - h * 0.02f))
                drawCircle(Color(0xFF3E2723), radius = 2.dp.toPx(), center = Offset(x + w * 0.08f, y + h * 0.02f))
            }

            // Purple corner fabric patch
            val patchPath = Path()
            patchPath.moveTo(w * 0.12f, h * 0.25f)
            patchPath.lineTo(w * 0.28f, h * 0.18f)
            patchPath.lineTo(w * 0.22f, h * 0.38f)
            patchPath.lineTo(w * 0.10f, h * 0.35f)
            patchPath.close()
            drawPath(patchPath, color = Color(0xFF7B1FA2))
            drawPath(patchPath, color = Color(0xFFE1BEE7), style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))))

            drawGlossOverlay(heartPath, w, h)
        }

        BlockerType.CHAINED_HEART -> {
            // Base payload heart
            val payload = tile.payloadTile
            if (payload is Tile.Normal) {
                drawGlossyNormalHeart(payload.color, w, h)
            } else {
                drawGlossyNormalHeart(HeartColor.PINK, w, h)
            }

            // Crossed heavy silver chains
            val chain1 = Path()
            chain1.moveTo(w * 0.1f, h * 0.2f)
            chain1.lineTo(w * 0.9f, h * 0.8f)
            val chain2 = Path()
            chain2.moveTo(w * 0.9f, h * 0.2f)
            chain2.lineTo(w * 0.1f, h * 0.8f)

            drawPath(chain1, color = Color(0xFFB0BEC5), style = Stroke(width = 5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))))
            drawPath(chain2, color = Color(0xFFB0BEC5), style = Stroke(width = 5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))))

            // Center Padlock
            drawRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(w * 0.38f, h * 0.42f),
                size = Size(w * 0.24f, h * 0.22f)
            )
            drawCircle(
                color = Color(0xFF37474F),
                radius = w * 0.04f,
                center = Offset(w / 2, h * 0.50f)
            )
        }

        BlockerType.DARK_HEART -> {
            // Abyss Purple-Black Corrupted Void Heart
            drawPath(
                path = heartPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE040FB), Color(0xFF4A148C), Color(0xFF000000)),
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = w * 0.65f
                )
            )

            // Sinister pulsing crimson eye / nucleus
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF1744), Color(0xFFD50000), Color.Transparent),
                    center = Offset(w / 2, h * 0.48f),
                    radius = w * 0.20f
                ),
                radius = w * 0.18f,
                center = Offset(w / 2, h * 0.48f)
            )
            drawStar(Offset(w / 2, h * 0.48f), outerRadius = w * 0.15f, innerRadius = w * 0.05f, color = Color.White)

            // Corrupted dark tendril border
            drawPath(
                path = heartPath,
                brush = Brush.sweepGradient(
                    listOf(Color(0xFFD50000), Color(0xFF651FFF), Color(0xFFD50000)),
                    center = Offset(w / 2, h / 2)
                ),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

fun DrawScope.drawGlossOverlay(heartPath: Path, w: Float, h: Float) {
    clipPath(heartPath) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
                center = Offset(w * 0.30f, h * 0.26f),
                radius = w * 0.22f
            ),
            topLeft = Offset(w * 0.16f, h * 0.14f),
            size = Size(w * 0.28f, h * 0.24f)
        )
    }
}

fun DrawScope.drawDoubleArrow(center: Offset, length: Float, isHorizontal: Boolean, color: Color) {
    val half = length / 2
    val arrowSize = 6.dp.toPx()
    if (isHorizontal) {
        drawLine(color = color, start = Offset(center.x - half, center.y), end = Offset(center.x + half, center.y), strokeWidth = 3.dp.toPx())
        // Left head
        drawLine(color = color, start = Offset(center.x - half, center.y), end = Offset(center.x - half + arrowSize, center.y - arrowSize), strokeWidth = 3.dp.toPx())
        drawLine(color = color, start = Offset(center.x - half, center.y), end = Offset(center.x - half + arrowSize, center.y + arrowSize), strokeWidth = 3.dp.toPx())
        // Right head
        drawLine(color = color, start = Offset(center.x + half, center.y), end = Offset(center.x + half - arrowSize, center.y - arrowSize), strokeWidth = 3.dp.toPx())
        drawLine(color = color, start = Offset(center.x + half, center.y), end = Offset(center.x + half - arrowSize, center.y + arrowSize), strokeWidth = 3.dp.toPx())
    } else {
        drawLine(color = color, start = Offset(center.x, center.y - half), end = Offset(center.x, center.y + half), strokeWidth = 3.dp.toPx())
        // Top head
        drawLine(color = color, start = Offset(center.x, center.y - half), end = Offset(center.x - arrowSize, center.y - half + arrowSize), strokeWidth = 3.dp.toPx())
        drawLine(color = color, start = Offset(center.x, center.y - half), end = Offset(center.x + arrowSize, center.y - half + arrowSize), strokeWidth = 3.dp.toPx())
        // Bottom head
        drawLine(color = color, start = Offset(center.x, center.y + half), end = Offset(center.x - arrowSize, center.y + half - arrowSize), strokeWidth = 3.dp.toPx())
        drawLine(color = color, start = Offset(center.x, center.y + half), end = Offset(center.x + arrowSize, center.y + half - arrowSize), strokeWidth = 3.dp.toPx())
    }
}

fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    numPoints: Int = 4
) {
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
    drawPath(path, color = color, style = Fill)
}
