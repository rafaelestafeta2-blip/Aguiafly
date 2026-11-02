package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun AguiaFlyTopBarLogo(
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onReplayIntro() }
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .testTag("aguiafly_topbar_logo"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            modifier = Modifier
                .size(40.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(colors = listOf(gold, aeroCyan)),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.aguiafly_official_logo),
                contentDescription = "Logo Oficial AguiaFly",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "AGUIAFLY",
                style = MaterialTheme.typography.titleLarge,
                color = gold,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "ROYAL SAPPHIRE • NDC OTA",
                style = MaterialTheme.typography.bodySmall,
                color = aeroCyan
            )
        }
    }
}

@Composable
fun AguiaFlyEntrySplashOverlay(
    onFinished: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnim = true
        delay(2300)
        onFinished()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "aguiaFlyLogoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "aguiaFlyLogoAlpha"
    )
    val flightProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
        label = "aguiaFlyFlightProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "aguiaAura")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aguiaGlowPulse"
    )

    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1B38),
                        Color(0xFF132D59),
                        Color(0xFF1A3C73)
                    )
                )
            )
            .clickable { onFinished() }
            .testTag("aguiafly_splash_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.43f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(aeroCyan.copy(alpha = 0.28f * glowPulse), Color.Transparent),
                    center = center,
                    radius = size.width * 0.58f
                ),
                center = center,
                radius = size.width * 0.58f
            )
            drawArc(
                brush = Brush.linearGradient(colors = listOf(champagneGold, aeroCyan, champagneGold)),
                startAngle = 140f,
                sweepAngle = 240f * flightProgress,
                useCenter = false,
                topLeft = Offset(center.x - 165.dp.toPx(), center.y - 115.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(330.dp.toPx(), 230.dp.toPx()),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .scale(logoScale)
                .alpha(logoAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                tonalElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(colors = listOf(champagneGold, aeroCyan, champagneGold)),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.aguiafly_official_logo),
                    contentDescription = "Logo Oficial AguiaFly",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "AGUIAFLY",
                style = MaterialTheme.typography.displayMedium,
                color = champagneGold,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Royal Sapphire Executive • Emissão Direta NDC & Carteira Digital",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            LinearProgressIndicator(
                progress = { flightProgress },
                modifier = Modifier
                    .width(210.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = champagneGold,
                trackColor = aeroCyan.copy(alpha = 0.28f)
            )

            Text(
                text = "Toque para entrar imediatamente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
