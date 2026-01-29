package com.project.salbabida.ui.screens.safety

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun SafetyScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Safety Tools State
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isSirenOn by remember { mutableStateOf(false) }
    
    // Siren Logic
    DisposableEffect(isSirenOn) {
        var toneGenerator: ToneGenerator? = null
        var job: kotlinx.coroutines.Job? = null
        
        if (isSirenOn) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            job = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                while (isActive) {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                    delay(1000)
                }
            }
        }
        
        onDispose {
            job?.cancel()
            toneGenerator?.release()
        }
    }
    
    // Flashlight Logic
    fun toggleFlashlight(turnOn: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, turnOn)
            isFlashlightOn = turnOn
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error (no flash, permission denied, etc)
        }
    }

    // Hotlines Data
    val hotlines = remember {
        listOf(
            Hotline("National Emergency", "911", Icons.Default.LocalPolice, Color(0xFF4F46E5)), // Primary
            Hotline("Red Cross", "143", Icons.Default.MedicalServices, Color(0xFFE11D48)), // Error
            Hotline("Fire Department", "911", Icons.Default.LocalFireDepartment, Color(0xFFF59E0B)), // Secondary
            Hotline("National Disaster", "8-911-5061", Icons.Default.Warning, Color(0xFF7C3AED)) // Violet
        )
    }

    // Main Content - No Scaffold here to avoid duplicate headers with MainScreen
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp), // Comfortable horizontal padding
        verticalArrangement = Arrangement.spacedBy(16.dp), // Reduced spacing from 24dp for tighter feel
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp) // Top padding for visual breathing room
    ) {
        item {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Flashlight Card
                SafetyToolCard(
                    title = "Flashlight",
                    icon = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    isActive = isFlashlightOn,
                    onClick = { toggleFlashlight(!isFlashlightOn) },
                    modifier = Modifier.weight(1f),
                    activeColor = MaterialTheme.colorScheme.primary
                )
                
                // Siren Card
                SafetyToolCard(
                    title = "Siren",
                    icon = Icons.Default.Campaign,
                    isActive = isSirenOn,
                    onClick = { isSirenOn = !isSirenOn },
                    modifier = Modifier.weight(1f),
                    activeColor = MaterialTheme.colorScheme.error,
                    isPulsing = isSirenOn
                )
            }
        }

        item {
            Text(
                text = "Emergency Hotlines",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }

        items(hotlines.size) { index ->
            val hotline = hotlines[index]
            HotlineCard(hotline = hotline) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${hotline.number}")
                }
                context.startActivity(intent)
            }
            Spacer(modifier = Modifier.height(4.dp)) // Minimal space between cards if needed, or rely on verticalArrangement
        }
    }
}

data class Hotline(
    val name: String,
    val number: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun SafetyToolCard(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color,
    isPulsing: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )

    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val contentColor by animateColorAsState(
        targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )

    Card(
        modifier = modifier
            .height(150.dp) // Slightly shorter than before
            .scale(scale)
            .alpha(if (isPulsing) pulseAlpha else 1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) activeColor else MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 8.dp else 0.dp
        ),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Gradient Overlay for active state
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isActive) Color.White.copy(alpha = 0.2f) 
                            else MaterialTheme.colorScheme.surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = if (isActive) "Active" else "Tap to activate",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun HotlineCard(
    hotline: Hotline,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp) // Optimized size
                    .clip(RoundedCornerShape(14.dp))
                    .background(hotline.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = hotline.icon,
                    contentDescription = null,
                    tint = hotline.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hotline.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hotline.number,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
