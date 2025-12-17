package com.danielgregorini.sensorappdaniel

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SensorService : Service(), SensorEventListener {

    companion object {
        private const val CHANNEL_ID = "sensor_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // TODO 1: create notification channel (only on O+)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO 2: start foreground with notification
        accelerometer?.let {
            // TODO 3: register listener at 50 Hz (20000 μs)
        } ?: stopSelf()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            // TODO 4: broadcast Intent("ACTION_SENSOR_DATA") with extras x,y,z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // TODO 5: create low-importance channel + manager.createNotificationChannel
        }
    }

    private fun createNotification(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Activity Monitor")
            .setContentText("Streaming accelerometer data")
    // TODO 6: setSmallIcon, setPriority(LOW), build()
}