package com.project.salbabida.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.project.salbabida.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    
    // Animate the background circles
    val circleAnim = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Parallel animations
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        
        launch {
            delay(300)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }

        // Slow pulsing for background
        launch {
             circleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2000)
            )
        }

        delay(2500) // Keep the splash screen visible for a moment
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        when {
            currentUser == null -> onNavigateToLogin()
            else -> onNavigateToMain()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // layered background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base gradient
            drawRect(
                brush = Brush.verticalGradient(
                     colors = listOf(
                        Color(0xFF4338CA), // Indigo 700
                        Color(0xFF312E81)  // Indigo 900
                    )
                )
            )

            // Abstract decoration circles (modern "bokeh" effect)
            // Top Left - Large light circle
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                center = Offset(x = 0f, y = 0f),
                radius = w * 0.7f * circleAnim.value
            )
            
            // Top Right - Smaller Accent
            drawCircle(
                color = Color(0xFF6366F1).copy(alpha = 0.1f), // Indigo 500
                center = Offset(x = w, y = h * 0.2f),
                radius = w * 0.3f
            )

            // Bottom Right - Large Anchoring Circle
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                center = Offset(x = w, y = h),
                radius = w * 0.8f
            )
            
             // Bottom Left - Floating Bubble
            drawCircle(
                color = Color(0xFF818CF8).copy(alpha = 0.05f), // Indigo 400
                center = Offset(x = w * 0.2f, y = h * 0.85f),
                radius = w * 0.25f
            )
        }

        // Content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Container with elevation and shape
            Surface(
                modifier = Modifier
                    .scale(scale.value)
                    .size(160.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "SalbaBida Logo",
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Title
            Text(
                text = "SALBA-bida",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White.copy(alpha = textAlpha.value),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Community Flood Preparedness",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = textAlpha.value * 0.8f),
                fontWeight = FontWeight.Light
            )
        }
        
        // Copyright/Version at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
             Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}
