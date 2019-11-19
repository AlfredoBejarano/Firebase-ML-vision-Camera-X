package me.alfredobejarano.cameraxfirebasemlvision

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageProxy.PlaneProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions
import me.alfredobejarano.cameraxfirebasemlvision.extensions.JPEG_QUALITY
import me.alfredobejarano.cameraxfirebasemlvision.extensions.SCAN_HEIGHT
import me.alfredobejarano.cameraxfirebasemlvision.extensions.SCAN_RECT
import me.alfredobejarano.cameraxfirebasemlvision.extensions.SCAN_WIDTH
import java.io.ByteArrayOutputStream

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

    /**
     * Parse a [PlaneProxy] into a [ByteArray].
     */
    private fun PlaneProxy?.asByteArray() = this?.buffer?.let { buffer ->
        buffer.rewind()
        val array = ByteArray(buffer.remaining())
        buffer.get(array)
        array
    } ?: run {
        byteArrayOf()
    }

    /**
     * Iterates through all the image planes from an [ImageProxy] object and
     * returns those bytes in a single [ByteArray].
     */
    private fun ImageProxy?.planesAsByteArray() = this?.planes
        ?.map { it.asByteArray().toTypedArray() }
        ?.toTypedArray()
        ?.flatten()
        ?.toByteArray() ?: byteArrayOf()

    /**
     * Parses the data from an [ImageProxy] using the [YuvImage] class to return a Bitmap
     * usable by the Firebase detector.
     */
    private fun ImageProxy?.asBitmap() = ByteArrayOutputStream().let { outputStream ->
        YuvImage(planesAsByteArray(), ImageFormat.NV21, SCAN_WIDTH, SCAN_HEIGHT, null)
            .compressToJpeg(SCAN_RECT, JPEG_QUALITY, outputStream)

        val data = outputStream.toByteArray()
        BitmapFactory.decodeByteArray(data, 0, data.size)
    }
}