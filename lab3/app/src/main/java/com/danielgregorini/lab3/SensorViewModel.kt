package com.danielgregorini.lab3

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgregorini.lab3.model.ActivityClassifier
import com.danielgregorini.lab3.model.FeatureExtractor
import com.danielgregorini.lab3.model.ModelLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SensorStatus {
    INITIALIZING,
    ACTIVE,
    ERROR
}

data class ActivityState(
    val currentActivity: String = "Initializing...",
    val confidence: Double = 0.0,
    val sensorData: AccelerometerData? = null,
    val sensorStatus: SensorStatus = SensorStatus.INITIALIZING,
    val errorMessage: String = "",
    val bufferProgress: Float = 0f,
    val isModelLoaded: Boolean = false
)

class SensorViewModel : ViewModel() {
    private val _activityState = MutableStateFlow(ActivityState())
    val activityState: StateFlow<ActivityState> = _activityState.asStateFlow()

    private var sensorService: SensorService? = null
    private var classifier: ActivityClassifier? = null
    private val featureExtractor = FeatureExtractor(windowSize = 50)

    // UI update throttling
    private var lastUiUpdate = 0L
    private val uiUpdateInterval = 100L // Update UI every 100ms

    // Classification control
    private var sampleCount = 0
    private val classificationInterval = 10 // Classify every 10 samples after buffer is full

    fun initSensorService(context: Context) {
        viewModelScope.launch {
            try {
                _activityState.value = _activityState.value.copy(
                    sensorStatus = SensorStatus.INITIALIZING,
                    errorMessage = "",
                    currentActivity = "Loading model..."
                )

                // Load the decision tree model
                val jsonString = try {
                    context.assets.open("decision_tree.json")
                        .bufferedReader()
                        .use { it.readText() }
                } catch (e: Exception) {
                    throw Exception("Failed to load decision_tree.json from assets: ${e.message}")
                }

                val model = ModelLoader.loadModel(jsonString)
                classifier = ActivityClassifier(model)

                _activityState.value = _activityState.value.copy(
                    isModelLoaded = true,
                    currentActivity = "Collecting sensor data..."
                )

                // Initialize sensor service
                sensorService = SensorService(context)

                val success = sensorService?.registerListener(
                    callback = { data -> onSensorDataReceived(data) },
                    onError = { error -> onSensorError(error) }
                ) ?: false

                if (success) {
                    _activityState.value = _activityState.value.copy(
                        sensorStatus = SensorStatus.ACTIVE
                    )
                } else {
                    _activityState.value = _activityState.value.copy(
                        sensorStatus = SensorStatus.ERROR,
                        errorMessage = "Failed to initialize sensor",
                        isModelLoaded = false
                    )
                }
            } catch (e: Exception) {
                _activityState.value = _activityState.value.copy(
                    sensorStatus = SensorStatus.ERROR,
                    errorMessage = "Initialization error: ${e.message}",
                    currentActivity = "Error",
                    isModelLoaded = false
                )
            }
        }
    }

    private fun onSensorDataReceived(data: AccelerometerData) {
        // Always add to feature extractor (no throttling for data collection)
        featureExtractor.addSample(data)

        // Calculate buffer progress
        val progress = featureExtractor.getBufferSize() / 50f

        // Throttle UI updates to prevent performance issues
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUiUpdate > uiUpdateInterval) {
            _activityState.value = _activityState.value.copy(
                sensorData = data,
                bufferProgress = progress
            )
            lastUiUpdate = currentTime
        }

        // Classify activity periodically once buffer is ready
        if (featureExtractor.isReady()) {
            sampleCount++
            if (sampleCount >= classificationInterval) {
                sampleCount = 0
                classifyActivity()
            }
        }
    }

    private fun classifyActivity() {
        viewModelScope.launch {
            try {
                val features = featureExtractor.extractFeatures()
                val result = classifier?.classify(features)

                result?.let {
                    _activityState.value = _activityState.value.copy(
                        currentActivity = it.activityName,
                        confidence = it.confidence
                    )
                }
            } catch (e: Exception) {
                _activityState.value = _activityState.value.copy(
                    errorMessage = "Classification error: ${e.message}"
                )
            }
        }
    }

    private fun onSensorError(error: String) {
        _activityState.value = _activityState.value.copy(
            sensorStatus = SensorStatus.ERROR,
            errorMessage = error,
            currentActivity = "Sensor Error"
        )
    }

    override fun onCleared() {
        // Safe cleanup
        sensorService?.unregisterListener()
        sensorService = null
        super.onCleared()
    }
}