package me.alfredobejarano.cameraxfirebasemlvision

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageProxy.PlaneProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import java.io.ByteArrayOutputStream

/**
 * Created by alfredo on 2019-11-15.
 * Copyright © 2019 GROW. All rights reserved.
 */
class LabelerAnalyzer : ImageAnalysis.Analyzer {
    private companion object {
        const val SCAN_WIDTH = 640
        const val SCAN_HEIGHT = 480
        const val JPEG_QUALITY = 50
        val SCAN_RECT = Rect(0, 0, SCAN_WIDTH, SCAN_HEIGHT)
    }

    /**
     * Flags that defines if there is a current frame being scanned.
     */
    private var analysisRunning = false

    private val imageLabelingLiveData = MediatorLiveData<List<String>>()
    val imageLabelingResults = imageLabelingLiveData as LiveData<List<String>>

    /**
     * Labeler instance form Firebase Vision.
     */
    private val labeler = FirebaseVision.getInstance().onDeviceImageLabeler

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
            val results = labeledObjects?.map { labeledObject -> labeledObject.text }
            imageLabelingLiveData.postValue(results ?: listOf())
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
        FirebaseVisionImage.fromBitmap(image.asByteArray())

    /**(
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
    private fun ImageProxy?.asByteArray() = ByteArrayOutputStream().let { outputStream ->
        YuvImage(planesAsByteArray(), ImageFormat.NV21, SCAN_WIDTH, SCAN_HEIGHT, null)
            .compressToJpeg(SCAN_RECT, JPEG_QUALITY, outputStream)

        val data = outputStream.toByteArray()
        BitmapFactory.decodeByteArray(data, 0, data.size)
    }
}