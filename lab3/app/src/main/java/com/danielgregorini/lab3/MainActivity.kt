package com.danielgregorini.lab3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YourAppTheme {
                ActivityRecognitionScreen()
            }
        }
    }
}

@Composable
fun ActivityRecognitionScreen(viewModel: SensorViewModel = viewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initSensorService(context)
    }

    val activityState by viewModel.activityState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Status Bar
            StatusBar(activityState)

            // Middle Section: Activity Display
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActivityCard(activityState)
            }

            // Bottom Section: Sensor Data & Buffer Progress
            Column {
                if (activityState.sensorData != null && !activityState.isModelLoaded) {
                    BufferProgressCard(activityState)
                }

                if (activityState.isModelLoaded) {
                    SensorDataCard(activityState.sensorData)
                }
            }
        }
    }
}

@Composable
fun StatusBar(state: ActivityState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state.sensorStatus) {
                SensorStatus.ACTIVE -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                SensorStatus.ERROR -> Color(0xFFF44336).copy(alpha = 0.1f)
                SensorStatus.INITIALIZING -> Color(0xFFFF9800).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (state.sensorStatus) {
                                SensorStatus.ACTIVE -> Color(0xFF4CAF50)
                                SensorStatus.ERROR -> Color(0xFFF44336)
                                SensorStatus.INITIALIZING -> Color(0xFFFF9800)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when (state.sensorStatus) {
                        SensorStatus.ACTIVE -> "Active"
                        SensorStatus.ERROR -> "Error"
                        SensorStatus.INITIALIZING -> "Initializing"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (state.isModelLoaded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Model Loaded",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Error Message
    if (state.errorMessage.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = state.errorMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun ActivityCard(state: ActivityState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = getActivityColor(state.currentActivity)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Activity",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = state.currentActivity,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            AnimatedVisibility(visible = state.confidence > 0 && state.isModelLoaded) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", state.confidence * 100)}% confident",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = state.confidence.toFloat(),
                        animationSpec = tween(durationMillis = 300)
                    )

                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun BufferProgressCard(state: ActivityState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Collecting Data",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(state.bufferProgress * 100).toInt()}%",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = state.bufferProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun SensorDataCard(sensorData: AccelerometerData?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Accelerometer",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (sensorData != null) {
                SensorDataRow("X", sensorData.x)
                SensorDataRow("Y", sensorData.y)
                SensorDataRow("Z", sensorData.z)
            } else {
                Text(
                    text = "Waiting for sensor data...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SensorDataRow(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = String.format("%.2f m/s²", value),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.Normal
        )
    }
}

fun getActivityColor(activity: String): Color {
    return when (activity.lowercase()) {
        "walking" -> Color(0xFF2196F3) // Blue
        "standing" -> Color(0xFFFF9800) // Orange
        "sitting" -> Color(0xFF4CAF50) // Green
        "laying" -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF607D8B) // Grey
    }
}

@Composable
fun YourAppTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC6),
        tertiary = Color(0xFF3700B3)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}