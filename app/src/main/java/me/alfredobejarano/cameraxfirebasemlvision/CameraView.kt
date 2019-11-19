package me.alfredobejarano.cameraxfirebasemlvision

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors.newSingleThreadExecutor

/**
 * Created by alfredo on 2019-11-15.
 * Copyright © 2019 GROW. All rights reserved.
 */
class CameraView(ctx: Context, attrs: AttributeSet? = null) : TextureView(ctx, attrs) {
    private var onLabelResults: (results: String) -> Unit = {}

    /**
     * Reference to the CameraX [Preview] use case used by this class.
     * This reference is used to turn on or off the torch.
     */
    private lateinit var previewUseCase: Preview

    /**
     * Listener that will report updates of a CameraX [Preview] use case and assigns them to this [TextureView].
     */
    private val onPreviewUpdateListener = Preview.OnPreviewOutputUpdateListener { output ->
        GlobalScope.launch(Dispatchers.Main) {
            (parent as? ViewGroup)?.apply {
                removeView(this@CameraView)
                addView(this@CameraView, 0)
                surfaceTexture = output.surfaceTexture
                updateRotation()
            }
        }
    }

    /**
     * [LifecycleObserver] that will detect when the bind LifeCycleOwner has paused
     * in order to stop the cameraX engine.
     */
    private val ownerObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() = CameraX.unbindAll()
    }

    /**
     * Assigns a [LifecycleOwner] to the CameraX engine.
     */
    fun bindToLifeCycle(owner: LifecycleOwner) {
        if (owner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            owner.lifecycle.addObserver(ownerObserver)
            startCamera(owner)
        } else {
            throw RuntimeException("What do you expect me to do with a destroyed LifeCycleOwner!?")
        }
    }

    /**
     * Generates a Matrix to apply the current rotation of the device to this TextureView.
     */
    private fun updateRotation() = setTransform(Matrix().apply {
        val rotationDegrees = when (val rotation = display?.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> rotation ?: 0
        }.toFloat()
        postRotate(-rotationDegrees, width / 2f, height / 2f)
    })

    /**
     * Creates a [Preview] use case to be assigned to CameraX.
     */
    private fun createPreviewUseCase() = Preview(PreviewConfig.Builder().build()).also {
        it.setOnPreviewOutputUpdateListener(newSingleThreadExecutor(), onPreviewUpdateListener)
    }.also { previewUseCase = it }

    /**
     * Creates the CameraX [ImageAnalysis] UseCase that will label objects in a given
     * frame using Firebase ML Kit Vision.
     */
    private fun createImageAnalysisUseCase(owner: LifecycleOwner): ImageAnalysis {
        val analysisConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        return ImageAnalysis(analysisConfig).apply {
            setAnalyzer(newSingleThreadExecutor(), observeLabelingResults(owner))
        }
    }

    /**
     * Creates a [LabelerAnalyzer] to be used by CameraX.
     * Returns its labeling results to the defined [onLabelResults] Listener using the
     * function.
     */
    private fun observeLabelingResults(owner: LifecycleOwner) = LabelerAnalyzer().also { analyzer ->
        analyzer.imageLabelingResults.observe(owner, Observer(onLabelResults))
    }

    /**
     * Binds the necessary UseCase classes to the CameraX engine.
     */
    private fun startCamera(owner: LifecycleOwner) {
        CameraX.bindToLifecycle(owner, createPreviewUseCase(), createImageAnalysisUseCase(owner))
        setOnClickListener { previewUseCase.enableTorch(!previewUseCase.isTorchOn) }
    }

    /**
     * Sets a listener for this view to report its results from image labeling.
     */
    fun addOnLabelResultsListener(listener: (results: String) -> Unit) {
        onLabelResults = listener
    }
}