package com.payaq.opengatevpn.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payaq.opengatevpn.ui.theme.BorderHairline
import com.payaq.opengatevpn.ui.theme.GeistMono
import com.payaq.opengatevpn.ui.theme.LabelCaps
import com.payaq.opengatevpn.ui.theme.Primary
import com.payaq.opengatevpn.ui.theme.StatusError
import com.payaq.opengatevpn.ui.theme.StatusPaused
import com.payaq.opengatevpn.ui.theme.StatusSuccess
import com.payaq.opengatevpn.ui.theme.TechnicalData
import com.payaq.opengatevpn.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ── Logs & Security Tab ─────────────────────────────────────────────────────────

@Composable
fun LogsTabContent(viewModel: VpnViewModel) {
    val clipboardManager = LocalClipboardManager.current
    var copiedIp by remember { mutableStateOf(false) }
    var copiedLogs by remember { mutableStateOf(false) }
    val state = viewModel.connectionState
    val server = viewModel.selectedServer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ──
        Column {
            Text(
                text = "Diagnostics & Console",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Security Specifications · Live Terminal Feed",
                style = TechnicalData,
                color = TextMuted
            )
        }

        // ── Security & Endpoint Grid ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactInfoCard(
                title = "CIPHER",
                value = "AES-256-CBC",
                icon = Icons.Rounded.Security,
                modifier = Modifier.weight(1f)
            )
            CompactInfoCard(
                title = "PROTOCOL",
                value = "OpenVPN UDP",
                icon = Icons.Rounded.Lock,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Endpoint & Traffic Row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                .background(Color(0xFF101010))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "ENDPOINT IP", style = LabelCaps.copy(fontSize = 9.sp), color = TextMuted)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = server?.ip ?: "None Selected",
                    style = TechnicalData.copy(fontSize = 13.sp),
                    color = Primary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .border(
                        1.dp,
                        if (copiedIp) StatusSuccess else BorderHairline,
                        RoundedCornerShape(5.dp)
                    )
                    .background(if (copiedIp) Color(0xFF142414) else Color(0xFF181818))
                    .clickable {
                        server?.ip?.let { ip ->
                            clipboardManager.setText(AnnotatedString(ip))
                            copiedIp = true
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (copiedIp) "COPIED" else "COPY IP",
                    style = LabelCaps.copy(fontSize = 9.sp),
                    color = if (copiedIp) StatusSuccess else Primary
                )
            }
        }

        // ── Terminal Console Box ──
        TerminalConsoleBox(
            logs = viewModel.connectionLogs,
            vpnStatus = viewModel.vpnStatus,
            state = state,
            onClear = { viewModel.clearLogs() },
            onCopyAll = {
                val allLogsText = viewModel.connectionLogs.joinToString("\n") { log ->
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    "[$time] [${log.event}] ${log.detail}"
                }
                clipboardManager.setText(AnnotatedString(allLogsText))
                copiedLogs = true
            },
            copiedLogs = copiedLogs,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(6.dp))
    }
}


// ── Terminal Console Window Box ─────────────────────────────────────────────────

@Composable
private fun TerminalConsoleBox(
    logs: List<LogEntry>,
    vpnStatus: String,
    state: ConnectionState,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
    copiedLogs: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Blinking terminal cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "terminalCursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // Auto-scroll to latest log on update. Target the last log entry itself (index
    // logs.size - 1) with headroom to spare — the old size + 1 target sat exactly on
    // the final item and could go out of range when logs are truncated or cleared.
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem((logs.size - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(10.dp))
            .background(Color(0xFF070707))
    ) {
        // ── Terminal Window Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // macOS / Linux style terminal dot buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5F56))
                )
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFBD2E))
                )
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF27C93F))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "opengate@terminal:~$",
                    style = TechnicalData.copy(
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFFAAAAAA)
                )
            }

            // Action buttons (Clear & Copy)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Clear Logs",
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "CLEAR",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = TextMuted
                    )
                }

                // Copy all button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (copiedLogs) Color(0xFF1E3A1E) else Color(0xFF222222))
                        .border(
                            1.dp,
                            if (copiedLogs) StatusSuccess else BorderHairline,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable(onClick = onCopyAll)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy All Logs",
                        tint = if (copiedLogs) StatusSuccess else Primary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copiedLogs) "COPIED" else "COPY",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = if (copiedLogs) StatusSuccess else Primary
                    )
                }
            }
        }

        // ── Terminal Console Feed ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Status banner line
                item {
                    Text(
                        text = "# Active Gateway Status: $vpnStatus",
                        style = TechnicalData.copy(
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            color = when (state) {
                                ConnectionState.CONNECTED -> StatusSuccess
                                ConnectionState.ERROR -> StatusError
                                ConnectionState.PAUSED -> StatusPaused
                                else -> Color(0xFF888888)
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "[system] Session ready. Awaiting connection command...",
                            style = TechnicalData.copy(
                                fontFamily = GeistMono,
                                fontSize = 11.sp,
                                color = Color(0xFF555555)
                            )
                        )
                    }
                } else {
                    items(
                        items = logs,
                        key = { "${it.timestamp}_${it.event}" }
                    ) { log ->
                        TerminalLogLine(log)
                    }
                }

                // Prompt with blinking cursor
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "opengate:~# ",
                            style = TechnicalData.copy(
                                fontFamily = GeistMono,
                                fontSize = 11.sp,
                                color = Color(0xFF00FF66)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 6.dp, height = 12.dp)
                                .background(Color(0xFF00FF66).copy(alpha = cursorAlpha))
                        )
                    }
                }
            }
        }
    }
}


// ── Terminal Log Line ───────────────────────────────────────────────────────────

@Composable
private fun TerminalLogLine(log: LogEntry) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    val (tag, tagColor) = when (log.event) {
        "CONNECTED", "READY" -> Pair("[OK]  ", StatusSuccess)
        "ERROR" -> Pair("[ERR] ", StatusError)
        "PAUSED", "RETRY" -> Pair("[WARN]", StatusPaused)
        "STATE", "SWITCH" -> Pair("[STAT]", Color(0xFFB388FF))
        "CONNECTING", "CONNECT", "RESUME", "FETCH", "SELECT" -> Pair("[SYS] ", Color(0xFF00E5FF))
        "DISCONNECTED", "STOP" -> Pair("[STOP]", StatusError.copy(alpha = 0.8f))
        "IGNORED" -> Pair("[SKIP]", Color(0xFF666666))
        else -> Pair("[INFO]", Color(0xFFAAAAAA))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = "$timeStr ",
            style = TechnicalData.copy(
                fontFamily = GeistMono,
                fontSize = 11.sp,
                color = Color(0xFF4A4A4A)
            )
        )
        // Level Tag
        Text(
            text = "$tag ",
            style = TechnicalData.copy(
                fontFamily = GeistMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tagColor
            )
        )
        // Detail message
        Text(
            text = log.detail,
            style = TechnicalData.copy(
                fontFamily = GeistMono,
                fontSize = 11.sp,
                color = Color(0xFFCCCCCC)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}


// ── Compact Info Card ───────────────────────────────────────────────────────────

@Composable
private fun CompactInfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
            .background(Color(0xFF101010))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, style = LabelCaps.copy(fontSize = 8.sp), color = TextMuted)
            Text(
                text = value,
                style = TechnicalData.copy(fontSize = 12.sp),
                color = Primary,
                maxLines = 1
            )
        }
    }
}
