package com.example.signlanguagetranslator

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class SignClassifier(context: Context) {

    private var interpreter: Interpreter
    private var labelMap: Map<Int, String> = emptyMap()

    init {
        val model = loadModelFile(context, "sign_model.tflite")
        interpreter = Interpreter(model)
        labelMap = loadLabelMap(context, "label_map.json")
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabelMap(context: Context, filename: String): Map<Int, String> {
        val json = context.assets.open(filename).bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val map = mutableMapOf<Int, String>()
        obj.keys().forEach { key ->
            map[key.toInt()] = obj.getString(key)
        }
        return map
    }

    fun classify(landmarks: FloatArray): Pair<String, Float> {
        val normalized = normalizeLandmarks(landmarks)

        val input = arrayOf(normalized)
        val output = Array(1) { FloatArray(labelMap.size) }

        interpreter.run(input, output)

        val scores = output[0]
        var maxIdx = 0
        var maxVal = scores[0]
        for (i in scores.indices) {
            if (scores[i] > maxVal) {
                maxVal = scores[i]
                maxIdx = i
            }
        }

        val label = labelMap[maxIdx] ?: "Unknown"
        return Pair(label, maxVal)
    }

    private fun normalizeLandmarks(landmarks: FloatArray): FloatArray {
        val points = Array(21) { FloatArray(3) }
        for (i in 0 until 21) {
            points[i][0] = landmarks[i * 3]
            points[i][1] = landmarks[i * 3 + 1]
            points[i][2] = landmarks[i * 3 + 2]
        }

        val wristX = points[0][0]
        val wristY = points[0][1]
        val wristZ = points[0][2]

        for (i in 0 until 21) {
            points[i][0] -= wristX
            points[i][1] -= wristY
            points[i][2] -= wristZ
        }

        var maxDist = 0f
        for (i in 0 until 21) {
            val dist = sqrt(
                points[i][0] * points[i][0] +
                        points[i][1] * points[i][1] +
                        points[i][2] * points[i][2]
            )
            if (dist > maxDist) maxDist = dist
        }

        val result = FloatArray(63)
        if (maxDist > 0f) {
            for (i in 0 until 21) {
                result[i * 3] = points[i][0] / maxDist
                result[i * 3 + 1] = points[i][1] / maxDist
                result[i * 3 + 2] = points[i][2] / maxDist
            }
        } else {
            for (i in 0 until 21) {
                result[i * 3] = points[i][0]
                result[i * 3 + 1] = points[i][1]
                result[i * 3 + 2] = points[i][2]
            }
        }

        return result
    }

    fun close() {
        interpreter.close()
    }
}