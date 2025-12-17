package com.danielgregorini.sensorappdaniel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SensorScreen(
    sensorValues: List<Float>,   // [x, y, z]
    isCollecting: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val x = sensorValues.getOrElse(0) { 0f }
    val y = sensorValues.getOrElse(1) { 0f }
    val z = sensorValues.getOrElse(2) { 0f }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp), // TODO 1: add 24.dp padding
        horizontalAlignment = Alignment.CenterHorizontally // TODO 2: center items horizontally
    ) {

        // TODO 3: Card with:
        //  - fillMaxWidth
        //  - primaryContainer when collecting, else surfaceVariant
        //  - 16.dp inner padding
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isCollecting)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Live Accelerometer",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // TODO 4: display X,Y,Z with 2 decimal places
                Text("X: ${x.format(2)} g", style = MaterialTheme.typography.bodyMedium)
                Text("Y: ${y.format(2)} g", style = MaterialTheme.typography.bodyMedium)
                Text("Z: ${z.format(2)} g", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TODO 5: Row with Start & Stop buttons (enabled/disabled correctly)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = onStartClick, enabled = !isCollecting) {
                Text("Start")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = onStopClick, enabled = isCollecting) {
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isCollecting) "Collecting at 50 Hz" else "Idle",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Helper para formatar com n casas decimais
private fun Float.format(decimals: Int): String =
    "%.${decimals}f".format(this)
