package com.danielgregorini.lab3


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

data class AccelerometerData(val x: Float, val y: Float, val z: Float)

class SensorService(context: Context) {
    // Use applicationContext to prevent memory leaks
    private val sensorManager: SensorManager by lazy {
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val accelerometer: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private var sensorListener: SensorEventListener? = null

    /**
     * Registers a listener for accelerometer data
     * @param callback Function to receive accelerometer data
     * @param onError Function to handle errors (optional)
     * @return Boolean indicating success or failure
     */
    fun registerListener(
        callback: (AccelerometerData) -> Unit,
        onError: ((String) -> Unit)? = null
    ): Boolean {
        // Check if accelerometer is available
        if (accelerometer == null) {
            onError?.invoke("Accelerometer not available on this device")
            return false
        }

        try {
            sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        // Ensure we have valid data
                        if (event.values.size >= 3) {
                            callback(
                                AccelerometerData(
                                    x = event.values[0],
                                    y = event.values[1],
                                    z = event.values[2]
                                )
                            )
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // Optional: Handle accuracy changes
                    // You could notify the callback about low accuracy if needed
                }
            }

            // Use SENSOR_DELAY_GAME for better sampling rate (~50Hz)
            // This is suitable for motion detection and activity recognition
            val registered = sensorManager.registerListener(
                sensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )

            if (!registered) {
                onError?.invoke("Failed to register sensor listener")
                return false
            }

            return true
        } catch (e: Exception) {
            onError?.invoke("Error registering sensor: ${e.message}")
            return false
        }
    }

    /**
     * Unregisters the sensor listener
     * Safe to call multiple times
     */
    fun unregisterListener() {
        try {
            sensorListener?.let {
                sensorManager.unregisterListener(it)
                sensorListener = null
            }
        } catch (e: Exception) {
            // Silent failure on cleanup - already shutting down
        }
    }
}