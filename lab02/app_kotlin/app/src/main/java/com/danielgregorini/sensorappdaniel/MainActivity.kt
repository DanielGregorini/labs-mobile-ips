package com.danielgregorini.sensorappdaniel

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielgregorini.sensorappdaniel.ui.SensorScreen

class MainActivity : ComponentActivity() {
    private lateinit var vm: SensorViewModel

    companion object {
        const val ACTION_SENSOR_DATA = "ACTION_SENSOR_DATA"
    }

    private val sensorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SENSOR_DATA) {
                val x = intent.getFloatExtra("x", 0f)
                val y = intent.getFloatExtra("y", 0f)
                val z = intent.getFloatExtra("z", 0f)
                vm.updateSensorValues(x, y, z)
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startService() else showDenied()
        }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm = ViewModelProvider(this)[SensorViewModel::class.java]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                sensorReceiver,
                IntentFilter(ACTION_SENSOR_DATA),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                sensorReceiver,
                IntentFilter(ACTION_SENSOR_DATA)
            )
        }

        setContent {
            val sensorValuesState = vm.sensorValues.collectAsStateWithLifecycle()
            val isCollectingState = vm.isCollecting.collectAsStateWithLifecycle()

            SensorScreen(
                sensorValues = sensorValuesState.value,
                isCollecting = isCollectingState.value,
                onStartClick = {
                    if (hasPermission()) startService()
                    else requestPermission()
                },
                onStopClick = { stopService() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(sensorReceiver)
    }

    private fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun startService() {
        val intent = Intent(this, SensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        vm.setCollecting(true)
    }

    private fun stopService() {
        val intent = Intent(this, SensorService::class.java)
        stopService(intent)
        vm.setCollecting(false)
    }

    private fun showDenied() {
        android.widget.Toast.makeText(
            this,
            "Permission required",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

