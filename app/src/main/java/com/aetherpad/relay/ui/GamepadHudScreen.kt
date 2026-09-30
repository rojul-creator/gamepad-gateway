package com.aetherpad.relay.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherpad.relay.GamepadRelayService
import com.aetherpad.relay.MainActivity
import com.aetherpad.relay.receiver.ScreenAndBootReceiver

@Composable
fun GamepadHudScreen(
    onStartRelay: (ip: String, port: Int) -> Unit,
    onStopRelay: () -> Unit
) {
    val state by MainActivity.gamepadStateFlow.collectAsState()
    val isScreenOn by ScreenAndBootReceiver.isScreenOn.collectAsState()
    val isPhysicalGamepad by MainActivity.physicalGamepadConnected.collectAsState()

    var isRelayActive by remember { mutableStateOf(false) }
    var isBatterySaverMode by remember { mutableStateOf(false) }
    var targetIp by remember { mutableStateOf("192.168.1.3") }
    var targetPort by remember { mutableStateOf(47800) }

    val shouldRenderVisuals = isScreenOn && !isBatterySaverMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070D))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ───────────────── 1. HEADER ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090E1A))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AETHERPAD HUD // WIRELESS RELAY",
                        color = Color(0xFF00FFFF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isPhysicalGamepad) "Physical USB/BT Controller Active (Slot 1/1)" else "Virtual Testing Mode Active (Slot 1/1)",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                // Target PC IP / Port Status
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF131C2E))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$targetIp:$targetPort (120Hz)",
                        color = Color(0xFFFF00FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // ───────────────── 2. CENTER VISUALIZER ─────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = shouldRenderVisuals,
                    enter = fadeIn(tween(400)),
                    exit = fadeOut(tween(400))
                ) {
                    GamepadVisualizer(
                        state = state,
                        isBatterySaver = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!shouldRenderVisuals) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ADAPTIVE POWER SAVER ACTIVE",
                            color = Color(0xFF00FFFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Canvas paused (<18mA) · 120Hz UDP background thread running",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ───────────────── 3. FOOTER ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090E1A))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Latency Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF063B2B))
                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "LATENCY: 4 ms",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Big Start/Stop Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (isRelayActive) listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                else listOf(Color(0xFF00FFFF), Color(0xFFFF00FF))
                            )
                        )
                        .clickable {
                            isRelayActive = !isRelayActive
                            if (isRelayActive) onStartRelay(targetIp, targetPort)
                            else onStopRelay()
                        }
                        .padding(horizontal = 36.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (isRelayActive) "STOP RELAY" else "START RELAY",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                // Battery Saver Mode Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBatterySaverMode) Color(0xFF0A3C2F) else Color(0xFF131C2E))
                        .clickable { isBatterySaverMode = !isBatterySaverMode }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isBatterySaverMode) "HEMAT BATERAI: ON" else "HEMAT BATERAI: OFF",
                        color = if (isBatterySaverMode) Color(0xFF34D399) else Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
