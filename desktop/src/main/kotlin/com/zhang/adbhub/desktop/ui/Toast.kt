package com.zhang.adbhub.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class ToastType {
    SUCCESS,
    ERROR,
    INFO
}

data class ToastData(
    val message: String,
    val type: ToastType,
    val duration: Long = 2000L
)

class ToastState {
    private val _currentToast = mutableStateOf<ToastData?>(null)
    val currentToast: State<ToastData?> = _currentToast

    suspend fun showToast(message: String, type: ToastType, duration: Long = 2000L) {
        _currentToast.value = ToastData(message, type, duration)
        delay(duration)
        _currentToast.value = null
    }

    fun dismiss() {
        _currentToast.value = null
    }
}

@Composable
fun rememberToastState(): ToastState {
    return remember { ToastState() }
}

@Composable
fun ToastHost(
    toastState: ToastState,
    modifier: Modifier = Modifier
) {
    val currentToast by toastState.currentToast

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = currentToast != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeOut()
        ) {
            currentToast?.let { toast ->
                ToastContent(toast = toast)
            }
        }
    }
}

@Composable
private fun ToastContent(toast: ToastData) {
    val (backgroundColor, contentColor, icon) = when (toast.type) {
        ToastType.SUCCESS -> Triple(
            Color(0xFF10B981), // 绿色
            Color.White,
            Icons.Default.CheckCircle
        )
        ToastType.ERROR -> Triple(
            Color(0xFFEF4444), // 红色
            Color.White,
            Icons.Default.Error
        )
        ToastType.INFO -> Triple(
            Color(0xFF3B82F6), // 蓝色
            Color.White,
            Icons.Default.Info
        )
    }

    Card(
        modifier = Modifier
            .padding(top = 16.dp)
            .widthIn(min = 300.dp, max = 500.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = backgroundColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = toast.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
