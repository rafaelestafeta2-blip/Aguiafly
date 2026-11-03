package com.example.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.example.ui.theme.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "AGUIAFLY",
                    style = MaterialTheme.typography.titleLarge,
                    color = gold,
                    fontWeight = FontWeight.ExtraBold
                )
                Icon(
                    imageVector = Icons.Default.FlightTakeoff,
                    contentDescription = "Reproduzir decolagem",
                    tint = gold,
                    modifier = Modifier.size(16.dp)
                )
            }
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
    var isMuted by remember { mutableStateOf(false) }
    val stopAudioSignal = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            stopAudioSignal.set(true)
        }
    }

    LaunchedEffect(isMuted) {
        if (!isMuted) {
            stopAudioSignal.set(false)
            withContext(Dispatchers.IO) {
                playSynthesizedJetTakeoffAudio(stopAudioSignal)
            }
        } else {
            stopAudioSignal.set(true)
        }
    }

    LaunchedEffect(Unit) {
        startAnim = true
        delay(3750)
        stopAudioSignal.set(true)
        onFinished()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.82f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "aguiaFlyLogoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "aguiaFlyLogoAlpha"
    )
    val flightProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 3450, easing = FastOutSlowInEasing),
        label = "aguiaFlyFlightProgress"
    )
    val runwaySpeedProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 3450, easing = FastOutLinearInEasing),
        label = "runwaySpeedProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "aguiaTakeoffInfinite")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aguiaGlowPulse"
    )
    val strobeBlink by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aircraftStrobeBlink"
    )
    val runwayStripePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "runwayStripePhase"
    )

    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary

    // Calculate live aviation telemetry based on flightProgress
    val airspeedKnots = (flightProgress * 192f).roundToInt()
    val altitudeFeet = if (flightProgress < 0.42f) 0 else ((flightProgress - 0.42f) / 0.58f * 4250f).roundToInt()
    val engineN1Percent = (24f + (flightProgress.coerceAtMost(0.65f) / 0.65f) * 79.4f).coerceAtMost(103.4f)
    val flightPhaseLabel = when {
        flightProgress < 0.22f -> "PISTA 03R • AUTORIZADO DECOLAGEM (TO/GA)"
        flightProgress < 0.42f -> "80 KTS • POTÊNCIA MÁXIMA DE TURBINA"
        flightProgress < 0.66f -> "V1 • VR • ROTAÇÃO E DECOLAGEM!"
        else -> "POSITIVE CLIMB • GEAR UP • BOA VIAGEM AGUIAFLY"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040B18),
                        Color(0xFF0B1D3A),
                        Color(0xFF132F5C),
                        Color(0xFF09152B)
                    )
                )
            )
            .clickable {
                stopAudioSignal.set(true)
                onFinished()
            }
            .testTag("aguiafly_splash_overlay")
    ) {
        // Full-Screen Runway + Sky + Commercial Jet Takeoff Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * (0.66f + flightProgress * 0.14f)

            // 1. Atmospheric Horizon Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        champagneGold.copy(alpha = 0.25f * glowPulse),
                        aeroCyan.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.55f, horizonY),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.55f, horizonY),
                radius = w * 0.85f
            )

            // 2. High-altitude speed lines & clouds during climb
            val climbFactor = ((flightProgress - 0.30f) / 0.70f).coerceIn(0f, 1f)
            if (climbFactor > 0f) {
                for (i in 0..9) {
                    val seed = (i * 37) % 100 / 100f
                    val streakProgress = (runwayStripePhase + seed) % 1f
                    val startX = w * (1.1f - streakProgress * 1.3f)
                    val startY = h * (0.12f + (i * 0.07f)) + streakProgress * h * 0.22f
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.24f * climbFactor),
                                aeroCyan.copy(alpha = 0.08f * climbFactor),
                                Color.Transparent
                            )
                        ),
                        start = Offset(startX, startY),
                        end = Offset(startX + 120.dp.toPx(), startY - 38.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Perspective Airport Runway (fades downward as aircraft climbs into the sky)
            val runwayVisibility = (1f - ((flightProgress - 0.58f) / 0.42f).coerceIn(0f, 1f))
            if (runwayVisibility > 0.02f) {
                val vanishX = w * 0.72f
                val vanishY = horizonY
                val bottomLeftX = -w * 0.25f
                val bottomRightX = w * 0.88f
                val bottomY = h * 1.02f

                val runwayPath = Path().apply {
                    moveTo(vanishX - 28.dp.toPx(), vanishY)
                    lineTo(vanishX + 28.dp.toPx(), vanishY)
                    lineTo(bottomRightX, bottomY)
                    lineTo(bottomLeftX, bottomY)
                    close()
                }
                drawPath(
                    path = runwayPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0E1B31).copy(alpha = 0.75f * runwayVisibility),
                            Color(0xFF060C17).copy(alpha = 0.95f * runwayVisibility)
                        ),
                        startY = vanishY,
                        endY = bottomY
                    )
                )

                // Runway edge lights & animated centerline dashes rushing backward
                val totalSteps = 12
                for (step in 0 until totalSteps) {
                    val rawT = ((step.toFloat() / totalSteps) + (1f - runwayStripePhase) * (1f / totalSteps)).coerceIn(0f, 1f)
                    // Perspective quadratic distribution
                    val t = rawT * rawT
                    val nextT = ((rawT + 0.045f).coerceAtMost(1f)).let { it * it }

                    val centerStartX = vanishX + (w * 0.24f - vanishX) * t
                    val centerStartY = vanishY + (bottomY - vanishY) * t
                    val centerEndX = vanishX + (w * 0.24f - vanishX) * nextT
                    val centerEndY = vanishY + (bottomY - vanishY) * nextT

                    drawLine(
                        color = Color.White.copy(alpha = (0.25f + 0.7f * t) * runwayVisibility),
                        start = Offset(centerStartX, centerStartY),
                        end = Offset(centerEndX, centerEndY),
                        strokeWidth = (1.5f + 5f * t).dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Left & Right Runway Edge Lights (Gold & AeroCyan ALS strobes)
                    val leftEdgeX = (vanishX - 24.dp.toPx()) + (bottomLeftX - (vanishX - 24.dp.toPx())) * t
                    val rightEdgeX = (vanishX + 24.dp.toPx()) + (bottomRightX - (vanishX + 24.dp.toPx())) * t
                    val lightRadius = (1.8f + 4.2f * t).dp.toPx()

                    drawCircle(
                        color = champagneGold.copy(alpha = (0.4f + 0.6f * t) * runwayVisibility),
                        radius = lightRadius,
                        center = Offset(leftEdgeX, centerStartY)
                    )
                    drawCircle(
                        color = aeroCyan.copy(alpha = (0.4f + 0.6f * t) * runwayVisibility),
                        radius = lightRadius,
                        center = Offset(rightEdgeX, centerStartY)
                    )
                }
            }

            // 4. Calculate Airplane Trajectory (Runway Roll -> VR Rotation -> Steep Climb Out)
            val rollPhase = (flightProgress / 0.42f).coerceIn(0f, 1f)
            val liftoffPhase = ((flightProgress - 0.38f) / 0.62f).coerceIn(0f, 1f)

            val planeX = w * (0.14f + 0.72f * flightProgress)
            val groundY = h * 0.77f - (rollPhase * h * 0.08f)
            val climbYOffset = liftoffPhase * liftoffPhase * (h * 0.56f)
            val planeY = groundY - climbYOffset

            // Pitch up angle (nose up during rotation and climb)
            val pitchDegrees = when {
                flightProgress < 0.34f -> -3f
                flightProgress < 0.55f -> -3f - ((flightProgress - 0.34f) / 0.21f) * 21f
                else -> -24f + ((flightProgress - 0.55f) / 0.45f) * 4f
            }

            val planeScale = 0.78f + 0.42f * sin(flightProgress * PI.toFloat() * 0.75f)

            // Contrail & Golden Flight Ribbon behind the climbing jet
            val trailStart = Offset(w * 0.06f, h * 0.79f)
            val trailControl = Offset(w * 0.42f, h * 0.74f)
            val contrailPath = Path().apply {
                moveTo(trailStart.x, trailStart.y)
                quadraticTo(
                    trailControl.x.coerceAtMost(planeX),
                    trailControl.y.coerceAtLeast(planeY),
                    planeX - 24.dp.toPx(),
                    planeY + 10.dp.toPx()
                )
            }
            drawPath(
                path = contrailPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        aeroCyan.copy(alpha = 0.45f),
                        champagneGold.copy(alpha = 0.92f)
                    )
                ),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = contrailPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        champagneGold.copy(alpha = 0.18f),
                        aeroCyan.copy(alpha = 0.35f)
                    )
                ),
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )

            // Ground shadow on runway before and right after liftoff
            if (liftoffPhase < 0.65f) {
                val shadowAlpha = (1f - liftoffPhase / 0.65f) * 0.45f
                val shadowY = groundY + 22.dp.toPx() + (liftoffPhase * 45.dp.toPx())
                drawOval(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    topLeft = Offset(planeX - 55.dp.toPx() * (1f - liftoffPhase * 0.4f), shadowY),
                    size = Size(110.dp.toPx() * (1f - liftoffPhase * 0.4f), 14.dp.toPx())
                )
            }

            // 5. Render the Executive Twin-Engine Jetliner with Afterburner Thrust
            withTransform({
                translate(left = planeX, top = planeY)
                rotate(degrees = pitchDegrees, pivot = Offset.Zero)
                scale(scaleX = planeScale, scaleY = planeScale, pivot = Offset.Zero)
            }) {
                drawExecutiveJetTakeoff(
                    champagneGold = champagneGold,
                    aeroCyan = aeroCyan,
                    glowPulse = glowPulse,
                    strobeOn = strobeBlink > 0.72f,
                    gearRetracted = liftoffPhase > 0.32f
                )
            }
        }

        // Top Bar Controls: Live Audio Visualizer Badge + Mute Toggle + Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 42.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF0A1931).copy(alpha = 0.86f),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = champagneGold.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { isMuted = !isMuted }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("splash_sound_toggle_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (isMuted) "Ativar som da decolagem" else "Silenciar som da decolagem",
                        tint = if (isMuted) Color.White.copy(alpha = 0.6f) else champagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isMuted) "SOM MUDO" else "TURBINA ATIVA",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMuted) Color.White.copy(alpha = 0.7f) else champagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isMuted) {
                        // Animated acoustic bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0.5f, 1.0f, 0.7f, 0.9f).forEachIndexed { idx, factor ->
                                val barHeight = (6f + 10f * ((glowPulse * factor + idx * 0.2f) % 1f)).dp
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(barHeight)
                                        .clip(CircleShape)
                                        .background(aeroCyan)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF0A1931).copy(alpha = 0.78f),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = aeroCyan.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable {
                        stopAudioSignal.set(true)
                        onFinished()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("splash_skip_button")
            ) {
                Text(
                    text = "PULAR DECOLAGEM →",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center-Top Brand Identity Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 96.dp)
                .scale(logoScale)
                .alpha(logoAlpha)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 12.dp,
                modifier = Modifier
                    .width(220.dp)
                    .height(135.dp)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(colors = listOf(champagneGold, aeroCyan, champagneGold)),
                        shape = RoundedCornerShape(24.dp)
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
                style = MaterialTheme.typography.displaySmall,
                color = champagneGold,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Text(
                text = "Royal Sapphire Executive • Emissão Direta NDC & Carteira Digital",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )
        }

        // Bottom Avionics Cockpit Telemetry HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF071326).copy(alpha = 0.88f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                champagneGold.copy(alpha = 0.65f),
                                aeroCyan.copy(alpha = 0.65f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VELOCIDADE (IAS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$airspeedKnots KTS",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                color = champagneGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TURBINA N1",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${"%.1f".format(engineN1Percent)}% TO/GA",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                color = aeroCyan,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ALTITUDE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$altitudeFeet FT",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF4ADE80),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { flightProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = champagneGold,
                        trackColor = aeroCyan.copy(alpha = 0.24f)
                    )

                    Text(
                        text = flightPhaseLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Draws an executive widebody jetliner in side-perspective takeoff profile
 * with glowing jet engine exhaust, illuminated cabin windows, gold tail fin, and navigation strobes.
 */
private fun DrawScope.drawExecutiveJetTakeoff(
    champagneGold: Color,
    aeroCyan: Color,
    glowPulse: Float,
    strobeOn: Boolean,
    gearRetracted: Boolean
) {
    val u = 1.dp.toPx()

    // 1. Twin Turbofan Jet Exhaust Plume (Afterburner / Takeoff Thrust)
    val plumeLength = (75f + 38f * glowPulse) * u
    val plumePath = Path().apply {
        moveTo(-18f * u, 8f * u)
        lineTo(-18f * u - plumeLength, 20f * u)
        lineTo(-18f * u - plumeLength * 0.75f, 10f * u)
        lineTo(-18f * u - plumeLength, 2f * u)
        close()
    }
    drawPath(
        path = plumePath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                aeroCyan.copy(alpha = 0.72f),
                champagneGold,
                Color.White
            ),
            startX = -18f * u - plumeLength,
            endX = -18f * u
        )
    )

    // 2. Landing Gear (visible during runway roll, retracts after VR liftoff)
    if (!gearRetracted) {
        // Rear main bogie
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(-6f * u, 9f * u),
            end = Offset(-8f * u, 23f * u),
            strokeWidth = 2.5f * u,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 4.2f * u,
            center = Offset(-10f * u, 24f * u)
        )
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 4.2f * u,
            center = Offset(-4f * u, 24f * u)
        )
        // Nose gear + taxi light
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(38f * u, 7f * u),
            end = Offset(37f * u, 20f * u),
            strokeWidth = 2f * u,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 3.2f * u,
            center = Offset(37f * u, 21f * u)
        )
    }

    // 3. Far-side Wing (Swept back)
    val farWingPath = Path().apply {
        moveTo(8f * u, -2f * u)
        lineTo(-22f * u, -24f * u)
        lineTo(-30f * u, -23f * u)
        lineTo(-10f * u, -1f * u)
        close()
    }
    drawPath(path = farWingPath, color = Color(0xFF94A3B8))

    // 4. Vertical Tail Stabilizer (AguiaFly Royal Sapphire & Gold Livery)
    val tailFinPath = Path().apply {
        moveTo(-36f * u, 0f * u)
        lineTo(-64f * u, -34f * u)
        lineTo(-50f * u, -34f * u)
        lineTo(-18f * u, -2f * u)
        close()
    }
    drawPath(
        path = tailFinPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF0F2952), champagneGold, aeroCyan),
            start = Offset(-64f * u, -34f * u),
            end = Offset(-18f * u, 0f * u)
        )
    )

    // 5. Fuselage Body (Aerodynamic Widebody Profile)
    val fuselagePath = Path().apply {
        moveTo(-54f * u, 1f * u)
        lineTo(36f * u, -6f * u)
        quadraticTo(58f * u, -5f * u, 66f * u, 3f * u)
        quadraticTo(56f * u, 10f * u, 32f * u, 10f * u)
        lineTo(-38f * u, 9f * u)
        quadraticTo(-50f * u, 7f * u, -54f * u, 1f * u)
        close()
    }
    drawPath(
        path = fuselagePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFE2E8F0),
                Color(0xFF94A3B8)
            ),
            startY = -8f * u,
            endY = 12f * u
        )
    )

    // Golden-Cyan Cheatline along the fuselage
    drawLine(
        brush = Brush.horizontalGradient(colors = listOf(champagneGold, aeroCyan, champagneGold)),
        start = Offset(-46f * u, 3f * u),
        end = Offset(52f * u, 1.5f * u),
        strokeWidth = 2.2f * u,
        cap = StrokeCap.Round
    )

    // Cockpit Windshield
    val cockpitPath = Path().apply {
        moveTo(43f * u, -4f * u)
        lineTo(54f * u, -1f * u)
        lineTo(56f * u, 2f * u)
        lineTo(42f * u, 1f * u)
        close()
    }
    drawPath(path = cockpitPath, color = Color(0xFF0A192F))

    // Illuminated Executive Cabin Windows
    for (wIdx in -6..6) {
        val wx = (wIdx * 5.2f) * u
        drawRoundRect(
            color = champagneGold.copy(alpha = 0.9f),
            topLeft = Offset(wx, -1.8f * u),
            size = Size(2.6f * u, 3.2f * u),
            cornerRadius = CornerRadius(1.2f * u, 1.2f * u)
        )
    }

    // 6. Near-side Swept Wing & Winglet
    val nearWingPath = Path().apply {
        moveTo(12f * u, 5f * u)
        lineTo(-26f * u, 26f * u)
        lineTo(-36f * u, 24f * u)
        lineTo(-8f * u, 4f * u)
        close()
    }
    drawPath(
        path = nearWingPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1), champagneGold)
        )
    )

    // 7. Under-wing High-Bypass Turbofan Engine Nacelle
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E3A8A), champagneGold, Color(0xFF0F172A))
        ),
        topLeft = Offset(-16f * u, 7f * u),
        size = Size(24f * u, 10f * u),
        cornerRadius = CornerRadius(5f * u, 5f * u)
    )
    // Glowing intake ring
    drawOval(
        color = aeroCyan,
        topLeft = Offset(5f * u, 8f * u),
        size = Size(3.5f * u, 8f * u)
    )

    // 8. Nose Landing Light Beam + Anti-Collision Strobes
    val headlightBeam = Path().apply {
        moveTo(64f * u, 3f * u)
        lineTo(145f * u, -14f * u)
        lineTo(145f * u, 22f * u)
        close()
    }
    drawPath(
        path = headlightBeam,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                champagneGold.copy(alpha = 0.18f),
                Color.Transparent
            ),
            startX = 64f * u,
            endX = 145f * u
        )
    )

    if (strobeOn) {
        // Wingtip & Tail Beacon Strobes
        drawCircle(
            color = Color.White,
            radius = 5.5f * u,
            center = Offset(-31f * u, 25f * u)
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 4.5f * u,
            center = Offset(-58f * u, -34f * u)
        )
    }
}

/**
 * Synthesizes a realistic 3.4-second commercial twin-turbofan takeoff sound using Android's AudioTrack:
 * 1. Airbus/Boeing dual-tone cockpit "Cleared for Takeoff" chime (D5 -> B4)
 * 2. Twin turbofan N1 compressor whine spooling up (190 Hz -> 1,380 Hz with blade harmonics)
 * 3. Full TO/GA jet thrust combustion roar & aerodynamic runway/liftoff flyby whoosh
 */
private suspend fun playSynthesizedJetTakeoffAudio(stopSignal: AtomicBoolean) {
    var audioTrack: AudioTrack? = null
    try {
        val sampleRate = 24000
        val durationSeconds = 3.45
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val chunkSamples = 1200 // 50ms chunks for responsive cancellation

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(chunkSamples * 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack.play()

        var turbinePhase1 = 0.0
        var turbinePhase2 = 0.0
        var rumblePhase = 0.0
        var lowPassNoise = 0.0
        var bandPassNoise = 0.0
        val random = Random(42)

        val buffer = ShortArray(chunkSamples)
        var sampleIndex = 0

        while (sampleIndex < totalSamples && !stopSignal.get()) {
            val count = minOf(chunkSamples, totalSamples - sampleIndex)
            for (i in 0 until count) {
                val globalIdx = sampleIndex + i
                val t = globalIdx.toDouble() / sampleRate
                val progress = (t / durationSeconds).coerceIn(0.0, 1.0)

                // 1. Cockpit "Cleared for Takeoff" Hi-Lo Chime (0.02s - 0.55s)
                val chimeSample = when {
                    t in 0.02..0.24 -> {
                        val env = exp(-(t - 0.02) * 11.0)
                        sin(2.0 * PI * 587.33 * t) * env * 0.28
                    }
                    t in 0.24..0.58 -> {
                        val env = exp(-(t - 0.24) * 8.5)
                        sin(2.0 * PI * 493.88 * t) * env * 0.28
                    }
                    else -> 0.0
                }

                // 2. Twin Turbofan Compressor Spool-Up Whine (sweeps from 185 Hz to 1320 Hz, then Doppler shift)
                val spoolCurve = when {
                    progress < 0.62 -> Math.pow(progress / 0.62, 1.35)
                    else -> 1.0 - ((progress - 0.62) / 0.38) * 0.24 // slight Doppler drop after flyby
                }
                val f1 = 190.0 + spoolCurve * 1120.0
                val f2 = f1 * 1.505 // Second stage fan harmonic

                turbinePhase1 += 2.0 * PI * f1 / sampleRate
                turbinePhase2 += 2.0 * PI * f2 / sampleRate
                rumblePhase += 2.0 * PI * (58.0 + spoolCurve * 42.0) / sampleRate

                val turbineWhine = (sin(turbinePhase1) * 0.16 + sin(turbinePhase2) * 0.09) *
                    (0.25 + 0.75 * spoolCurve)

                // 3. Jet Exhaust Combustion Roar & Aerodynamic Lift Whoosh (Filtered White -> Brown/Pink Noise)
                val white = random.nextDouble(-1.0, 1.0)
                lowPassNoise = 0.94 * lowPassNoise + 0.06 * white
                bandPassNoise = 0.82 * bandPassNoise + 0.18 * (white - lowPassNoise)

                val thrustEnvelope = when {
                    progress < 0.12 -> progress / 0.12 * 0.35
                    progress < 0.68 -> 0.35 + ((progress - 0.12) / 0.56) * 0.65
                    else -> (1.0 - (progress - 0.68) / 0.32).coerceIn(0.0, 1.0)
                }

                val lowEndRumble = (lowPassNoise * 0.68 + sin(rumblePhase) * 0.18) * thrustEnvelope
                val airframeWhoosh = bandPassNoise * 0.36 * thrustEnvelope

                // Master Fade-in / Fade-out Envelope
                val masterEnvelope = when {
                    progress < 0.04 -> progress / 0.04
                    progress > 0.82 -> ((1.0 - progress) / 0.18).coerceIn(0.0, 1.0)
                    else -> 1.0
                }

                val mixed = ((chimeSample + turbineWhine + lowEndRumble + airframeWhoosh) * masterEnvelope)
                    .coerceIn(-0.95, 0.95)

                buffer[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
            }

            audioTrack.write(buffer, 0, count)
            sampleIndex += count
        }
    } catch (_: Throwable) {
        // Safely ignore if audio hardware is unavailable in headless environment
    } finally {
        try {
            audioTrack?.stop()
        } catch (_: Throwable) {}
        try {
            audioTrack?.release()
        } catch (_: Throwable) {}
    }
}
