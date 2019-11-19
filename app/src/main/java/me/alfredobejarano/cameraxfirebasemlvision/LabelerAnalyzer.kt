package me.alfredobejarano.cameraxfirebasemlvision

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions
import me.alfredobejarano.cameraxfirebasemlvision.extensions.asBitmap

/**
 * Created by alfredo on 2019-11-15.
 * Copyright © 2019 GROW. All rights reserved.
 */
class LabelerAnalyzer : ImageAnalysis.Analyzer {
    private companion object {
        const val CONFIDENCE_THRESHOLD = 0.7f
    }

    /**
     * Flags that defines if there is a current frame being scanned.
     */
    private var analysisRunning = false

    private val imageLabelingLiveData = MediatorLiveData<String>()
    val imageLabelingResults = imageLabelingLiveData as LiveData<String>

    private val labelerOptions = FirebaseVisionOnDeviceImageLabelerOptions.Builder()
        .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
        .build()

    /**
     * Labeler instance form Firebase Vision.
     */
    private val labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler(labelerOptions)

    /**
     * Called when a frame gets received from the CameraX stream.
     *
     * @param image The [ImageProxy] object received from the CameraX stream.
     * @param rotationDegrees Degrees rotation from the camera sensor.
     */
    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        if (analysisRunning) return

        analysisRunning = true
        val fireBaseImage = createFirebaseImage(image)

        labeler.processImage(fireBaseImage).addOnSuccessListener { labeledObjects ->
            val closestMatch = labeledObjects?.maxBy { it.confidence }
            imageLabelingLiveData.postValue(closestMatch?.text ?: "")
            analysisRunning = false
        }.addOnFailureListener {
            analysisRunning = false
        }
    }

    /**
     * Creates a [FirebaseVisionImage][image] object usable by the Firebase object labeler instance
     * from an [ImageProxy] result sent from CameraX.
     */
    private fun createFirebaseImage(image: ImageProxy?) =
        FirebaseVisionImage.fromBitmap(image.asBitmap())
}