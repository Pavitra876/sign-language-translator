package com.example.signlanguagetranslator

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    context: Context,
    private val onResult: (HandLandmarkerResult) -> Unit
) {
    private var handLandmarker: HandLandmarker? = null

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ -> onResult(result) }
            .setErrorListener { error -> error.printStackTrace() }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detectAsync(bitmap: Bitmap, timestampMs: Long) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        handLandmarker?.detectAsync(mpImage, timestampMs)
    }

    fun close() {
        handLandmarker?.close()
    }
}