package com.payaq.opengatevpn.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payaq.opengatevpn.data.model.VpnServer
import com.payaq.opengatevpn.ui.theme.BorderHairline
import com.payaq.opengatevpn.ui.theme.CardBackground
import com.payaq.opengatevpn.ui.theme.LabelCaps
import com.payaq.opengatevpn.ui.theme.OnSurface
import com.payaq.opengatevpn.ui.theme.Primary
import com.payaq.opengatevpn.ui.theme.StatusError
import com.payaq.opengatevpn.ui.theme.StatusPaused
import com.payaq.opengatevpn.ui.theme.StatusSuccess
import com.payaq.opengatevpn.ui.theme.TechnicalData
import com.payaq.opengatevpn.ui.theme.TextMuted
import com.payaq.opengatevpn.ui.util.countryCodeToFlag
import com.payaq.opengatevpn.ui.util.formatElapsedTime
import com.payaq.opengatevpn.ui.util.formatScore
import com.payaq.opengatevpn.ui.util.formatSpeed
import kotlinx.coroutines.delay


// ── Connection Tab Content ──────────────────────────────────────────────────────

@Composable
fun ConnectionTabContent(
    viewModel: VpnViewModel,
    state: ConnectionState,
    server: VpnServer?,
    onConnectClicked: (String) -> Unit,
    onShowServerExplorer: () -> Unit,
    onNavigateToServers: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.1f))

        // Status Badge — label comes straight from the ViewModel so it can say
        // exactly what's happening (changing server, retrying x/3, ...) beyond the
        // coarse ConnectionState enum; dot color/pulse stay keyed on the state.
        StatusBadge(state = state, label = viewModel.statusLabel)

        Spacer(modifier = Modifier.height(28.dp))

        // Hero Squircle Power Button
        SquirclePowerButton(
            state = state,
            onClick = {
                when (state) {
                    ConnectionState.IDLE,
                    ConnectionState.DISCONNECTED,
                    ConnectionState.ERROR -> {
                        val config = viewModel.bestServerConfig
                        if (config != null) {
                            viewModel.onConnectRequested()
                            onConnectClicked(config)
                        }
                    }
                    ConnectionState.CONNECTED,
                    ConnectionState.CONNECTING,
                    ConnectionState.RECONNECTING -> {
                        viewModel.onStopClicked()
                    }
                    ConnectionState.PAUSED -> {
                        viewModel.onResumeClicked()
                    }
                    ConnectionState.SEARCHING_NODES -> { /* Searching in progress */ }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Connection Timer
        AnimatedVisibility(
            visible = state == ConnectionState.CONNECTED || state == ConnectionState.PAUSED,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 }
        ) {
            ConnectionTimer(
                startTimeMillis = viewModel.connectionStartTime,
                pausedSeconds = viewModel.pausedElapsedSeconds,
                isPaused = state == ConnectionState.PAUSED
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Telemetry Bar
        AnimatedVisibility(
            visible = state == ConnectionState.CONNECTED,
            enter = fadeIn(tween(400)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically { it / 2 }
        ) {
            TelemetryBar(
                uploadSpeed = viewModel.uploadSpeed,
                downloadSpeed = viewModel.downloadSpeed
            )
        }

        // Server Failure Assistance Banner (Actionable guidance when server is unreachable)
        AnimatedVisibility(
            visible = state == ConnectionState.ERROR,
            enter = fadeIn(tween(300)) + slideInVertically { -it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically { -it / 2 }
        ) {
            ServerFailureBanner(
                headline = viewModel.lastErrorHeadline,
                detail = viewModel.lastErrorDetail,
                onRetry = {
                    val config = viewModel.bestServerConfig
                    if (config != null) {
                        // Single-shot: one attempt, then straight back to this
                        // banner on failure — no automatic retry chain.
                        viewModel.prepareManualRetry()
                        viewModel.onConnectRequested()
                        onConnectClicked(config)
                    }
                },
                onChangeServer = onNavigateToServers
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Active Server Card
        ServerCard(
            state = state,
            server = server,
            nodeCount = viewModel.displayServers.size,
            isBestServer = viewModel.isAutoSelectedBest,
            onClick = {
                if (state != ConnectionState.SEARCHING_NODES) {
                    onShowServerExplorer()
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Actions
        QuickActionRow(
            state = state,
            onPause = { viewModel.onPauseClicked() },
            onResume = { viewModel.onResumeClicked() },
            onDisconnect = { viewModel.onStopClicked() }
        )

        Spacer(modifier = Modifier.weight(0.1f))
    }
}


// ── Server Failure Assistance Banner ────────────────────────────────────────────

@Composable
private fun ServerFailureBanner(
    headline: String?,
    detail: String?,
    onRetry: () -> Unit,
    onChangeServer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, StatusError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(Color(0xFF1E0C0C), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Text(text = "⚠️", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline ?: "Server Unreachable / Dropped",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = StatusError
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = detail ?: "This volunteer gateway failed to respond. Retry or choose another node.",
                    style = TechnicalData.copy(fontSize = 11.sp, lineHeight = 16.sp),
                    color = Primary.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Retry Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E1212))
                    .border(1.dp, StatusError.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onRetry)
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RETRY",
                    style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Primary
                )
            }

            // Choose Other Server Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181818))
                    .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                    .clickable(onClick = onChangeServer)
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHANGE SERVER",
                    style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}


// ── Status Badge ────────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(state: ConnectionState, label: String) {
    val dotColor by animateColorAsState(
        targetValue = when (state) {
            ConnectionState.CONNECTED -> StatusSuccess
            ConnectionState.CONNECTING, ConnectionState.SEARCHING_NODES -> Primary
            ConnectionState.RECONNECTING, ConnectionState.PAUSED -> StatusPaused
            ConnectionState.ERROR -> StatusError
            else -> TextMuted
        },
        animationSpec = tween(400),
        label = "statusDotColor"
    )

    val isPulsing = state == ConnectionState.CONNECTING ||
            state == ConnectionState.RECONNECTING ||
            state == ConnectionState.SEARCHING_NODES

    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == ConnectionState.RECONNECTING) 600 else 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val effectiveDotAlpha = if (isPulsing) pulseAlpha else 1f

    Row(
        modifier = Modifier
            .border(1.dp, BorderHairline, RoundedCornerShape(50))
            .background(CardBackground, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = effectiveDotAlpha))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = LabelCaps,
            color = dotColor
        )
    }
}


// ── Hero Squircle Power Button ──────────────────────────────────────────────────

@Composable
private fun SquirclePowerButton(
    state: ConnectionState,
    onClick: () -> Unit,
    buttonSize: Dp = 150.dp,
    cornerRadiusDp: Dp = 32.dp
) {
    val isConnected = state == ConnectionState.CONNECTED
    val isConnecting = state == ConnectionState.CONNECTING
    val isReconnecting = state == ConnectionState.RECONNECTING
    val isSearching = state == ConnectionState.SEARCHING_NODES
    val isPaused = state == ConnectionState.PAUSED
    val isIdle = state == ConnectionState.IDLE
    val isDisconnected = state == ConnectionState.DISCONNECTED
    val isError = state == ConnectionState.ERROR

    val infiniteTransition = rememberInfiniteTransition(label = "borderBeam")
    val beamProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isConnecting || isReconnecting -> 1600
                    isSearching -> 2000
                    else -> 4200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "beamProgress"
    )

    // Pulsing alpha for PAUSED state
    val pausePulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pausePulse"
    )

    val beamColor = when {
        isConnected -> StatusSuccess
        isPaused || isReconnecting -> StatusPaused
        isError -> StatusError
        else -> Primary
    }

    val iconColor by animateColorAsState(
        targetValue = when {
            isConnected -> StatusSuccess
            isConnecting -> Primary
            isReconnecting -> StatusPaused
            isPaused -> StatusPaused
            isSearching -> Primary.copy(alpha = 0.9f)
            isError -> StatusError.copy(alpha = 0.8f)
            else -> Primary.copy(alpha = 0.7f)
        },
        animationSpec = tween(300),
        label = "iconColor"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(100),
        label = "pressScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(buttonSize * pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.requiredSize(buttonSize * pressScale + 40.dp)) {
            val actualBtnSize = buttonSize.toPx() * pressScale
            val offsetX = (size.width - actualBtnSize) / 2f
            val offsetY = (size.height - actualBtnSize) / 2f

            translate(left = offsetX, top = offsetY) {
                val cr = cornerRadiusDp.toPx()
                val borderInset = 0.75.dp.toPx()

                // A. Pitch-black background
                drawRoundRect(
                    color = Color(0xFF000000),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(actualBtnSize, actualBtnSize),
                    cornerRadius = CornerRadius(cr, cr)
                )

                // B. Dim base micro-border
                drawRoundRect(
                    color = if (isConnected) Color(0xFF333333) else BorderHairline,
                    topLeft = Offset(borderInset, borderInset),
                    size = androidx.compose.ui.geometry.Size(
                        actualBtnSize - borderInset * 2, actualBtnSize - borderInset * 2
                    ),
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(width = 1.dp.toPx())
                )

                // C. Inner Inset Double-Border Track
                val inset = 6.dp.toPx()
                val innerCr = (cr - 4.dp.toPx()).coerceAtLeast(12f)
                val innerColor = when {
                    isConnected -> StatusSuccess.copy(alpha = 0.32f)
                    isPaused -> StatusPaused.copy(alpha = 0.32f)
                    else -> Color(0xFF1C1C1C)
                }
                drawRoundRect(
                    color = innerColor,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(
                        actualBtnSize - inset * 2, actualBtnSize - inset * 2
                    ),
                    cornerRadius = CornerRadius(innerCr, innerCr),
                    style = Stroke(width = 1.dp.toPx())
                )

                // D. The Animated Glowing Border
                val isFullGlow = isConnected || isPaused
                val isStaticDim = isIdle || isDisconnected
                val centerX = actualBtnSize / 2f
                val centerY = actualBtnSize / 2f

                val rect = android.graphics.RectF(
                    borderInset, borderInset,
                    actualBtnSize - borderInset, actualBtnSize - borderInset
                )

                if (isStaticDim) {
                    // IDLE / DISCONNECTED: dim static border only
                } else if (isFullGlow) {
                    val glowAlpha = if (isPaused) pausePulse else 1f
                    val solidColor = beamColor.copy(alpha = glowAlpha).toArgb()

                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val blurPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = solidColor
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 6.dp.toPx()
                            maskFilter = android.graphics.BlurMaskFilter(
                                14.dp.toPx(),
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        nativeCanvas.drawRoundRect(rect, cr, cr, blurPaint)
                        val corePaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = solidColor
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 1.5.dp.toPx()
                            strokeCap = android.graphics.Paint.Cap.ROUND
                        }
                        nativeCanvas.drawRoundRect(rect, cr, cr, corePaint)
                    }
                } else if (isError) {
                    drawRoundRect(
                        color = StatusError.copy(alpha = 0.6f),
                        topLeft = Offset(borderInset, borderInset),
                        size = androidx.compose.ui.geometry.Size(
                            actualBtnSize - borderInset * 2, actualBtnSize - borderInset * 2
                        ),
                        cornerRadius = CornerRadius(cr, cr),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                } else {
                    // Spinning comet
                    val sweepColors = intArrayOf(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        beamColor.copy(alpha = 0.2f).toArgb(),
                        beamColor.toArgb(),
                        android.graphics.Color.TRANSPARENT
                    )
                    val sweepPositions = floatArrayOf(0.0f, 0.45f, 0.85f, 0.97f, 1.0f)
                    val sweepShader = android.graphics.SweepGradient(
                        centerX, centerY, sweepColors, sweepPositions
                    )
                    val matrix = android.graphics.Matrix()
                    matrix.postRotate(beamProgress * 360f, centerX, centerY)
                    sweepShader.setLocalMatrix(matrix)

                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val blurPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            shader = sweepShader
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 6.dp.toPx()
                            maskFilter = android.graphics.BlurMaskFilter(
                                14.dp.toPx(),
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        nativeCanvas.drawRoundRect(rect, cr, cr, blurPaint)
                        val corePaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            shader = sweepShader
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 1.5.dp.toPx()
                            strokeCap = android.graphics.Paint.Cap.ROUND
                        }
                        nativeCanvas.drawRoundRect(rect, cr, cr, corePaint)
                    }
                }
            }
        }

        Icon(
            imageVector = Icons.Rounded.PowerSettingsNew,
            contentDescription = "Power Toggle",
            tint = iconColor,
            modifier = Modifier.size(36.dp)
        )
    }
}


// ── Bloom Background ────────────────────────────────────────────────────────────

@Composable
fun BloomBackground(state: ConnectionState) {
    val isConnected = state == ConnectionState.CONNECTED
    val isPaused = state == ConnectionState.PAUSED

    if (isConnected || isPaused) {
        Canvas(modifier = Modifier.fillMaxWidth().height(600.dp)) {
            val bloomColor = if (isPaused) StatusPaused else StatusSuccess
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bloomColor.copy(alpha = 0.05f),
                        bloomColor.copy(alpha = 0.01f),
                        Color.Transparent
                    ),
                    center = Offset(this.size.width / 2f, this.size.height * 0.38f),
                    radius = this.size.width * 0.8f
                ),
                center = Offset(this.size.width / 2f, this.size.height * 0.38f),
                radius = this.size.width * 0.8f
            )
        }
    }
}


// ── Connection Timer ────────────────────────────────────────────────────────────

@Composable
private fun ConnectionTimer(
    startTimeMillis: Long,
    pausedSeconds: Long,
    isPaused: Boolean
) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(startTimeMillis, isPaused, pausedSeconds) {
        if (isPaused) {
            elapsedSeconds = pausedSeconds
        } else if (startTimeMillis > 0L) {
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000
                delay(1000)
            }
        }
    }

    Text(
        text = formatElapsedTime(elapsedSeconds),
        style = TechnicalData.copy(
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 2.sp
        ),
        color = if (isPaused) StatusPaused else OnSurface
    )
}


// ── Telemetry Bar ───────────────────────────────────────────────────────────────

@Composable
private fun TelemetryBar(uploadSpeed: String, downloadSpeed: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryStat(label = "UP", value = uploadSpeed)
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(BorderHairline)
        )
        Spacer(modifier = Modifier.width(24.dp))
        TelemetryStat(label = "DOWN", value = downloadSpeed)
    }
}

@Composable
private fun TelemetryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = LabelCaps, color = TextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = TechnicalData, color = Primary)
    }
}


// ── Active Server Card ──────────────────────────────────────────────────────────

@Composable
private fun ServerCard(
    state: ConnectionState,
    server: VpnServer?,
    nodeCount: Int,
    isBestServer: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isSearching = state == ConnectionState.SEARCHING_NODES

    val infiniteTransition = rememberInfiniteTransition(label = "serverCardShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    // Pulsing ring scale for scanning state
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Primary else BorderHairline,
        animationSpec = tween(180),
        label = "cardBorder"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(CardBackground, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Top Row: Flag Circle / Radar scanning + Country + [BEST SERVER Badge] + CHANGE button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag / Radar Circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E), CircleShape)
                    .border(1.dp, BorderHairline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSearching || server == null) {
                    Box(
                        modifier = Modifier
                            .size((18 * ringScale).dp)
                            .clip(CircleShape)
                            .border(
                                1.5.dp,
                                Primary.copy(alpha = shimmerAlpha * 0.6f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = shimmerAlpha))
                        )
                    }
                } else {
                    Text(
                        text = countryCodeToFlag(server.countryShort),
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Country Name & Badges
            Column(modifier = Modifier.weight(1f)) {
                if (isSearching || server == null) {
                    Text(
                        text = "Scanning Nodes...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Primary.copy(alpha = shimmerAlpha),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (nodeCount > 0) "Analyzing $nodeCount nodes" else "Connecting to VPN Gate...",
                        style = TechnicalData.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = server.countryLong,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // BEST SERVER badge (shown when it's the auto-selected best gateway)
                        if (isBestServer) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0D2818))
                                    .border(1.dp, StatusSuccess.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "★ BEST SERVER",
                                    style = LabelCaps.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = StatusSuccess
                                )
                            }
                        }
                    }
                }
            }

            // Right Action: CHANGE Button
            if (!isSearching && server != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, BorderHairline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CHANGE",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Change Server",
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Bottom Metrics Bar (Divided, balanced grid with IP, Ping, Speed, and Score)
        if (!isSearching && server != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF1C1C1C))
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // IP Address
                Column {
                    Text(
                        text = "IP ADDRESS",
                        style = LabelCaps.copy(fontSize = 8.sp),
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = server.ip,
                        style = TechnicalData.copy(fontSize = 12.sp),
                        color = Primary
                    )
                }

                // Ping
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LATENCY",
                        style = LabelCaps.copy(fontSize = 8.sp),
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${server.ping}ms",
                        style = TechnicalData.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = when {
                            server.ping < 60 -> StatusSuccess
                            server.ping < 120 -> Color(0xFF00E5FF)
                            else -> Color(0xFFFFB300)
                        }
                    )
                }

                // Speed
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SPEED",
                        style = LabelCaps.copy(fontSize = 8.sp),
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = formatSpeed(server.speed),
                        style = TechnicalData.copy(fontSize = 12.sp),
                        color = Primary
                    )
                }

                // Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SCORE",
                        style = LabelCaps.copy(fontSize = 8.sp),
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = formatScore(server.score),
                        style = TechnicalData.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = if (isBestServer) StatusSuccess else Primary
                    )
                }
            }
        }
    }
}


// ── Quick Action Row ────────────────────────────────────────────────────────────

@Composable
private fun QuickActionRow(
    state: ConnectionState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDisconnect: () -> Unit
) {
    val showActions = state == ConnectionState.CONNECTED ||
            state == ConnectionState.PAUSED ||
            state == ConnectionState.RECONNECTING

    AnimatedVisibility(
        visible = showActions,
        enter = fadeIn(tween(300)) + slideInVertically { it / 2 },
        exit = fadeOut(tween(200)) + slideOutVertically { it / 2 }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (state) {
                ConnectionState.CONNECTED -> {
                    QuickActionButton(
                        label = "Pause",
                        icon = Icons.Rounded.Pause,
                        tintColor = Primary,
                        borderColor = BorderHairline,
                        onClick = onPause,
                        modifier = Modifier.weight(1f)
                    )
                }
                ConnectionState.PAUSED -> {
                    QuickActionButton(
                        label = "Resume",
                        icon = Icons.Rounded.PlayArrow,
                        tintColor = StatusPaused,
                        borderColor = StatusPaused.copy(alpha = 0.8f),
                        onClick = onResume,
                        modifier = Modifier.weight(1f)
                    )
                }
                ConnectionState.RECONNECTING -> {
                    QuickActionButton(
                        label = "Reconnecting...",
                        icon = Icons.Rounded.Refresh,
                        tintColor = StatusPaused,
                        borderColor = StatusPaused.copy(alpha = 0.5f),
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {}
            }

            QuickActionButton(
                label = "Disconnect",
                icon = Icons.Rounded.Close,
                tintColor = StatusError,
                borderColor = StatusError.copy(alpha = 0.35f),
                onClick = onDisconnect,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    tintColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val activeBorder by animateColorAsState(
        targetValue = if (isPressed) tintColor else borderColor,
        animationSpec = tween(150),
        label = "actionBorder"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, activeBorder, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tintColor
        )
    }
}
