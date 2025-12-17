package com.danielgregorini.lab3.model

data class ClassificationResult(
    val activityName: String,
    val activityClass: Int,
    val confidence: Double
)

class ActivityClassifier(private val model: DecisionTreeModel) {

    fun classify(features: FloatArray): ClassificationResult {
        val leaf = traverse(model.tree, features)
        val activityName = model.metadata.class_names[leaf.classIndex]
        return ClassificationResult(
            activityName = activityName,
            activityClass = leaf.classIndex,
            confidence = leaf.confidence
        )
    }

    private fun traverse(node: TreeNode, features: FloatArray): TreeNode.Leaf {
        return when (node) {
            is TreeNode.Leaf -> node
            is TreeNode.Node -> {
                val featureValue = features[node.feature_index].toDouble()
                val nextNode = if (featureValue < node.threshold) {
                    node.left
                } else {
                    node.right
                }
                traverse(nextNode, features)
            }
        }
    }

    fun getFeatureNames(): List<String> = model.metadata.feature_names
    fun getClassNames(): List<String> = model.metadata.class_names
}