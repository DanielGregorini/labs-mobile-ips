package com.danielgregorini.lab3.model

import android.drm.DrmRights
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

@Serializable
sealed class TreeNode {
    @Serializable
    @SerialName("node")
    data class Node(
        val feature_index: Int,
        val threshold: Double,
        val left: TreeNode,
        val right: TreeNode
    ) : TreeNode()

    @Serializable
    @SerialName("leaf")
    data class Leaf(
        @SerialName("class")
        val classIndex: Int,
        val confidence: Double
    ) : TreeNode()
}

@Serializable
data class ModelMetadata(
    val feature_names: List<String>,
    val class_names: List<String>,
    val n_features: Int,
    val n_classes: Int
)

@Serializable
data class DecisionTreeModel(
    val tree: TreeNode,
    val metadata: ModelMetadata
)

class ModelLoader {
    companion object {
        fun loadModel(jsonString: String): DecisionTreeModel {
            val json = Json {
                ignoreUnknownKeys = true
                classDiscriminator = "type"
            }
            return json.decodeFromString<DecisionTreeModel>(jsonString)
        }
    }
}