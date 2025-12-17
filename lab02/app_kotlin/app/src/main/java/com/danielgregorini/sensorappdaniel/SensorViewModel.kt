package com.danielgregorini.sensorappdaniel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorViewModel : ViewModel() {

    // TODO 1: create MutableStateFlow<List<Float>> for sensor values (initial empty)
    private val _sensorValues = MutableStateFlow<List<Float>>(emptyList())
    // TODO 3: expose as StateFlow via asStateFlow()
    val sensorValues: StateFlow<List<Float>> = _sensorValues.asStateFlow()

    // TODO 2: create MutableStateFlow<Boolean> for collecting state (initial false)
    private val _isCollecting = MutableStateFlow(false)
    // TODO 3: expose as StateFlow via asStateFlow()
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()

    fun updateSensorValues(x: Float, y: Float, z: Float) {
        // TODO 4: update the values flow
        _sensorValues.value = listOf(x, y, z)
    }

    fun setCollecting(collecting: Boolean) {
        // TODO 5: update the collecting flow
        _isCollecting.value = collecting
    }
}
