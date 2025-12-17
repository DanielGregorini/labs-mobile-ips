package com.danielgregorini.lab3.model

import com.danielgregorini.lab3.AccelerometerData
import kotlin.math.sqrt

class FeatureExtractor(private val windowSize: Int = 50) {
    private val dataBuffer = mutableListOf<AccelerometerData>()

    fun addSample(data: AccelerometerData) {
        dataBuffer.add(data)
        if (dataBuffer.size > windowSize) {
            dataBuffer.removeAt(0)
        }
    }

    fun isReady(): Boolean = dataBuffer.size >= windowSize

    fun getBufferSize(): Int = dataBuffer.size

    fun extractFeatures(): FloatArray {
        if (!isReady()) return FloatArray(16)

        val xValues = dataBuffer.map { it.x }
        val yValues = dataBuffer.map { it.y }
        val zValues = dataBuffer.map { it.z }
        val magValues = dataBuffer.map {
            sqrt(it.x * it.x + it.y * it.y + it.z * it.z)
        }

        val features = FloatArray(16)

        // X features (0-3)
        features[0] = xValues.average().toFloat()
        features[1] = calculateStd(xValues)
        features[2] = xValues.minOrNull() ?: 0f
        features[3] = xValues.maxOrNull() ?: 0f

        // Y features (4-7)
        features[4] = yValues.average().toFloat()
        features[5] = calculateStd(yValues)
        features[6] = yValues.minOrNull() ?: 0f
        features[7] = yValues.maxOrNull() ?: 0f

        // Z features (8-11)
        features[8] = zValues.average().toFloat()
        features[9] = calculateStd(zValues)
        features[10] = zValues.minOrNull() ?: 0f
        features[11] = zValues.maxOrNull() ?: 0f

        // Magnitude features (12-15)
        features[12] = magValues.average().toFloat()
        features[13] = calculateStd(magValues)
        features[14] = magValues.minOrNull() ?: 0f
        features[15] = magValues.maxOrNull() ?: 0f

        return features
    }

    private fun calculateStd(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    fun reset() {
        dataBuffer.clear()
    }
}