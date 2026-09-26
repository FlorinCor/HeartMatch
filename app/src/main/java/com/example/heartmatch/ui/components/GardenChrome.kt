package com.example.heartmatch.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.example.heartmatch.R
import com.example.heartmatch.ui.theme.GardenPalette
import com.example.heartmatch.ui.theme.LocalReducedMotion
import kotlin.math.sin

/** A shared, resolution-independent line icon family for interface controls. */
@Composable
fun GardenIcon(name: String, modifier: Modifier = Modifier.size(24.dp), tint: Color = GardenPalette.Ivory) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(tint, point(x,y), point(x2,y2), 1.7f*unit, StrokeCap.Round)
        fun circle(x: Float, y: Float, r: Float) = drawCircle(tint,r*unit,point(x,y),style=Stroke(1.7f*unit))
        fun rect(x: Float,y: Float,w: Float,h: Float) = drawRect(tint,point(x,y),Size(w*unit,h*unit),style=Stroke(1.7f*unit))
        when(name) {
            "settings" -> { circle(12f,12f,6f); circle(12f,12f,2f); for(i in 0..7) { val a=i*Math.PI/4; line(12f+6f*kotlin.math.cos(a).toFloat(),12f+6f*sin(a).toFloat(),12f+9f*kotlin.math.cos(a).toFloat(),12f+9f*sin(a).toFloat()) } }
            "pause" -> { line(8f,5f,8f,19f); line(16f,5f,16f,19f) }
            "gift" -> { rect(4f,10f,16f,11f); rect(3f,7f,18f,4f); line(12f,7f,12f,21f); circle(9f,4f,3f); circle(15f,4f,3f) }
            "help" -> { circle(12f,12f,9f); drawArc(tint,200f,250f,false,point(9f,6f),Size(6f*unit,6f*unit),style=Stroke(1.7f*unit)); line(12f,12f,12f,14f); circle(12f,18f,0.4f) }
            "back" -> { line(16f,5f,9f,12f); line(9f,12f,16f,19f) }
            "locate" -> { circle(12f,12f,6f); circle(12f,12f,1.5f); line(12f,2f,12f,5f); line(12f,19f,12f,22f); line(2f,12f,5f,12f); line(19f,12f,22f,12f) }
            "hammer", "🔨" -> { line(6f,20f,15f,9f); line(11f,5f,19f,12f); line(11f,5f,14f,2f); line(19f,12f,22f,9f); line(14f,2f,22f,9f) }
            "bomb", "💣" -> { circle(11f,14f,7f); line(14f,7f,17f,3f); line(17f,3f,21f,4f); line(21f,1f,21f,2f) }
            "rainbow", "🌈" -> { for(r in listOf(9f,6f,3f)) drawArc(tint,180f,180f,false,point(12f-r,17f-r),Size(r*2*unit,r*2*unit),style=Stroke(1.7f*unit)) }
            "shuffle", "🔀" -> { line(3f,6f,7f,6f); line(7f,6f,17f,18f); line(17f,18f,21f,18f); line(3f,18f,7f,18f); line(7f,18f,17f,6f); line(17f,6f,21f,6f); line(18f,3f,21f,6f); line(21f,6f,18f,9f) }
            "lock" -> { rect(5f,10f,14f,11f); drawArc(tint,180f,180f,false,point(8f,3f),Size(8f*unit,14f*unit),style=Stroke(1.7f*unit)); line(12f,14f,12f,17f) }
            "coin" -> { circle(12f,12f,9f); circle(12f,12f,6f); line(12f,8f,12f,16f) }
            "motion" -> { line(3f,7f,14f,7f); line(6f,12f,21f,12f); line(3f,17f,14f,17f) }
            "sound", "🔊" -> { rect(3f,9f,4f,6f); line(7f,9f,12f,5f); line(12f,5f,12f,19f); line(12f,19f,7f,15f); drawArc(tint,-60f,120f,false,point(9f,4f),Size(13f*unit,16f*unit),style=Stroke(1.7f*unit)) }
            "haptic", "📳" -> { rect(8f,3f,8f,18f); line(4f,8f,4f,16f); line(20f,8f,20f,16f) }
            else -> { line(12f,3f,12f,21f); line(3f,12f,21f,12f) }
        }
    }
}

@Composable
fun GardenIconButton(name: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        // The button owns the accessible label; the drawing is decorative.
        Box(Modifier.semanticsLabel(label)) { GardenIcon(name) }
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier = this.then(
    Modifier.semantics { contentDescription = label }
)

@Composable
fun GardenBackdrop(modifier: Modifier = Modifier, dim: Float = 0.72f) {
    Box(modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.rose_garden_backdrop), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(GardenPalette.Background.copy(alpha = dim)))
    }
}

/** Very sparse edge petals, never drawn over the puzzle board. */
@Composable
fun GardenAtmosphere(modifier: Modifier = Modifier) {
    if (LocalReducedMotion.current) return
    val transition = rememberInfiniteTransition(label = "Garden breeze")
    val phase by transition.animateFloat(0f,1f,infiniteRepeatable(tween(24000,easing=LinearEasing)),label="Petal drift")
    Canvas(modifier.fillMaxSize()) {
        repeat(5) { i ->
            val progress = (phase + i * 0.21f) % 1f
            val side = if (i % 2 == 0) 0.055f else 0.945f
            val x = size.width * (side + sin(progress * 6.28f + i) * 0.025f)
            val alpha = sin(progress * Math.PI).toFloat() * 0.45f
            drawOval(GardenPalette.RoseLight.copy(alpha=alpha), Offset(x, progress*size.height), Size(4.dp.toPx(),7.dp.toPx()))
        }
    }
}
