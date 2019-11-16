package me.alfredobejarano.cameraxfirebasemlvision

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions

/**
 * Created by alfredo on 2019-11-15.
 * Copyright © 2019 GROW. All rights reserved.
 */
class LabelerAnalyzer : ImageAnalysis.Analyzer {
    private companion object {
        const val SCAN_WIDTH = 640
        const val SCAN_HEIGHT = 480
        const val LABELING_CONFIDENCE_THRESHOLD = 0.8f
    }

    private var analysisRunning = false

    private val imageLabelingLiveData = MediatorLiveData<List<String>>()
    val imageLabelingResults = imageLabelingLiveData as LiveData<List<String>>

    private val firebaseImageMetadata = FirebaseVisionImageMetadata.Builder()
        .setWidth(SCAN_WIDTH)
        .setHeight(SCAN_HEIGHT)
        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
        .build()

    private val firebaseLabelerOptions = FirebaseVisionOnDeviceImageLabelerOptions.Builder()
        .setConfidenceThreshold(LABELING_CONFIDENCE_THRESHOLD)
        .build()

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        if (analysisRunning) return

        analysisRunning = true

        val firebaseImage = createFirebaseImage(image.toByteArray())

        val labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler(firebaseLabelerOptions)
        labeler.processImage(firebaseImage).addOnSuccessListener { detectedObjects ->
            val results = detectedObjects?.map { detectedObject -> detectedObject.entityId ?: "" }
            imageLabelingLiveData.postValue(results)
            analysisRunning = false
        }.addOnFailureListener { analysisRunning = false }
    }

    /**
     * Creates a [FirebaseVisionImage][image] object usable by the Firebase object labeler instance.
     */
    private fun createFirebaseImage(image: ByteArray) =
        FirebaseVisionImage.fromByteArray(image, firebaseImageMetadata)

    /**
     * Extension function for the [ImageProxy] class that will pastse its UV plane bytes into a ByteArray.
     */
    private fun ImageProxy?.toByteArray() = this?.planes?.first()?.buffer?.let { buffer ->
        buffer.rewind()
        val array = ByteArray(buffer.remaining())
        buffer.get(array)
        array
    } ?: run {
        byteArrayOf()
    }
}