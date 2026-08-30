package com.payaq.opengatevpn.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import com.payaq.opengatevpn.data.model.VpnServerDisplay
import com.payaq.opengatevpn.ui.theme.BorderHairline
import com.payaq.opengatevpn.ui.theme.CardBackground
import com.payaq.opengatevpn.ui.theme.LabelCaps
import com.payaq.opengatevpn.ui.theme.Primary
import com.payaq.opengatevpn.ui.theme.StatusSuccess
import com.payaq.opengatevpn.ui.theme.TechnicalData
import com.payaq.opengatevpn.ui.theme.TextMuted
import com.payaq.opengatevpn.ui.theme.Void
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.SolidColor
import com.payaq.opengatevpn.R


// ── Main Screen (Shell with Bottom Navigation & Animated Splash) ────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VpnViewModel,
    onConnectClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.connectionState
    val server = viewModel.selectedServer

    var selectedTab by remember { mutableIntStateOf(0) }
    var showServerExplorer by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Stable server selection callback
    val currentOnServerSelected by rememberUpdatedState { display: VpnServerDisplay ->
        val fullServer = viewModel.getFullServer(display.ip)
        if (fullServer != null) {
            // Switch to connection tab after selection
            selectedTab = 0
            viewModel.onServerSwitchRequested(fullServer, onConnectClicked)
        }
    }
    val stableOnServerSelected: (VpnServerDisplay) -> Unit = remember { { currentOnServerSelected(it) } }

    // The splash is an OVERLAY on the real UI: the app composes once beneath it and
    // the logo simply fades away into the initializing content — one continuous
    // experience instead of a hard swap into a second branded page.
    val showSplash = viewModel.displayServers.isEmpty() && state == ConnectionState.SEARCHING_NODES

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Void)
    ) {
        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(tween(500)),
            exit = ExitTransition.None,
            label = "mainContentReveal"
        ) {
        // Ambient background bloom (visible across all tabs)
        BloomBackground(state = state)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // ── Top Bar ─────────────────────────────────────────────────────────
            TopBar(
                isRefreshing = viewModel.isRefreshing,
                onRefreshClick = { viewModel.loadServers() },
                onSettingsClick = { /* Future settings */ }
            )

            // ── Tab Content ─────────────────────────────────────────────────────
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> ConnectionTabContent(
                        viewModel = viewModel,
                        state = state,
                        server = server,
                        onConnectClicked = onConnectClicked,
                        onShowServerExplorer = { showServerExplorer = true },
                        onNavigateToServers = { selectedTab = 1 }
                    )
                    1 -> ServersTabContent(
                        viewModel = viewModel,
                        onServerSelected = stableOnServerSelected
                    )
                    2 -> LogsTabContent(
                        viewModel = viewModel
                    )
                }
            }

            // ── Bottom Navigation Bar ───────────────────────────────────────────
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // System nav bar padding
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }

        // ── Server Explorer Bottom Sheet (from Connection tab) ──────────────
        if (showServerExplorer) {
            ServerExplorerSheet(
                viewModel = viewModel,
                sheetState = sheetState,
                onDismiss = { showServerExplorer = false },
                onServerSelected = { display ->
                    val fullServer = viewModel.getFullServer(display.ip)
                    if (fullServer != null) {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showServerExplorer = false
                        }
                        viewModel.onServerSwitchRequested(fullServer, onConnectClicked)
                    }
                }
            )
        }
        }

        // ── Splash Overlay ──────────────────────────────────────────────────
        // Covers the UI until the first directory load finishes, then fades out.
        AnimatedVisibility(
            visible = showSplash,
            enter = EnterTransition.None,
            exit = fadeOut(tween(450)),
            label = "splashOverlay"
        ) {
            SplashScreen()
        }
    }
}


// ── Animated App Launch Splash Screen (Minimalist Black & White) ────────────────

@Composable
private fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Crisp Minimalist B&W Vector Shield Logo
            Image(
                painter = painterResource(id = R.drawable.ic_opengate_logo),
                contentDescription = "OpenGate VPN Logo",
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        alpha = alphaAnim
                    }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // App Name (Minimalist, Crisp Monochrome Typography)
            Text(
                text = "OpenGate VPN",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle Status
            Text(
                text = "INITIALIZING",
                style = LabelCaps.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 3.sp
                ),
                color = Color(0xFF666666)
            )
        }
    }
}


// ── Bottom Navigation Bar (AMOLED Black & Perfectly Centered) ───────────────────

@Composable
private fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTab(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Shield,
            label = "Connect",
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        NavTab(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Dns,
            label = "Servers",
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavTab(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Terminal,
            label = "Logs",
            isSelected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
    }
}

@Composable
private fun NavTab(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) Primary else Color(0xFF555555),
        animationSpec = tween(200),
        label = "navColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = tween(200),
        label = "navScale"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            style = LabelCaps.copy(
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.sp
            ),
            color = tintColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Dynamic active indicator pill
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 2.5.dp)
                .clip(CircleShape)
                .background(if (isSelected) StatusSuccess else Color.Transparent)
        )
    }
}


// ── Top Bar ─────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
        ),
        label = "refreshRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_opengate_logo),
                contentDescription = "OpenGate Logo",
                modifier = Modifier
                    .size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "OpenGate",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Primary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Disabled while a refresh is in flight — overlapping fetches would race each other.
            IconButton(onClick = onRefreshClick, enabled = !isRefreshing) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh Servers",
                    tint = when {
                        isRefreshing -> Primary
                        else -> TextMuted
                    },
                    modifier = Modifier
                        .alpha(if (isRefreshing) 1f else 0.7f)
                        .rotate(if (isRefreshing) rotation else 0f)
                )
            }
        }
    }
}


// ── Server Explorer Bottom Sheet (Quick picker from Connection tab) ─────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerExplorerSheet(
    viewModel: VpnViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onServerSelected: (VpnServerDisplay) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0C0C0C),
        contentColor = Primary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E2E2E))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                text = "Quick Server Switch",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${viewModel.quickChangeServers.size} of ${viewModel.displayServers.size} nodes · Best ranked first",
                style = TechnicalData,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, BorderHairline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = viewModel.quickSearchQuery,
                        onValueChange = { viewModel.quickSearchQuery = it },
                        textStyle = TechnicalData.copy(color = Primary, fontSize = 14.sp),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (viewModel.quickSearchQuery.isEmpty()) {
                                Text(
                                    text = "Search servers...",
                                    style = TechnicalData.copy(color = TextMuted, fontSize = 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (viewModel.quickSearchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.quickSearchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Server List — deliberately uses the tab-independent quick-change list,
            // NOT filteredServers (which carries Servers-tab filters).
            if (viewModel.quickChangeServers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No servers match your search",
                        style = TechnicalData,
                        color = TextMuted.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val bestIp = viewModel.bestServerIp
                    items(
                        items = viewModel.quickChangeServers,
                        key = { it.ip },
                        contentType = { "server_node" }
                    ) { srv ->
                        val isSelected = srv.ip == viewModel.selectedServer?.ip
                        QuickServerItem(
                            server = srv,
                            isSelected = isSelected,
                            isBestServer = srv.ip == bestIp,
                            onClick = { onServerSelected(srv) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuickServerItem(
    server: VpnServerDisplay,
    isSelected: Boolean,
    isBestServer: Boolean,
    onClick: () -> Unit
) {
    // Sub-15ms reports are implausible for an internet relay — suspect data.
    val pingColor = when {
        server.ping < 15 -> Color(0xFFFFB300)
        server.ping < 60 -> Color(0xFF00E676)
        server.ping < 120 -> Color(0xFF00E5FF)
        else -> Color(0xFFFFB300)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF181818) else Color(0xFF101010))
            .border(
                1.dp,
                if (isSelected) Primary.copy(alpha = 0.5f) else Color(0xFF1A1A1A),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = server.flagEmoji,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = server.countryLong,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isBestServer) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BEST SERVER",
                        style = LabelCaps.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                        color = StatusSuccess,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(StatusSuccess.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = server.ip,
                style = TechnicalData.copy(fontSize = 10.sp),
                color = TextMuted
            )
        }
        Text(
            text = server.pingText,
            style = TechnicalData,
            color = pingColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = server.formattedSpeed,
            style = TechnicalData.copy(fontSize = 10.sp),
            color = TextMuted
        )
    }
}