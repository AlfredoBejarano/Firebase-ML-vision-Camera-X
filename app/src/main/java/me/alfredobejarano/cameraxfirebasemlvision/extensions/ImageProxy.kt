package me.alfredobejarano.cameraxfirebasemlvision.extensions

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Created by alfredo on 2019-11-19.
 * Copyright © 2019 GROW. All rights reserved.
 */

const val SCAN_WIDTH = 640
const val SCAN_HEIGHT = 480
const val JPEG_QUALITY = 50
val SCAN_RECT = Rect(0, 0, SCAN_WIDTH, SCAN_HEIGHT)

/**
 * Iterates through all the image planes from an [ImageProxy] object and
 * returns those bytes in a single [ByteArray].
 */
fun ImageProxy?.planesAsByteArray() = this?.planes
    ?.map { it.asByteArray().toTypedArray() }
    ?.toTypedArray()
    ?.flatten()
    ?.toByteArray() ?: byteArrayOf()

/**
 * Parses the data from an [ImageProxy] using the [YuvImage] class to return a Bitmap
 * usable by the Firebase detector.
 */
fun ImageProxy?.asBitmap() = ByteArrayOutputStream().let { outputStream ->
    YuvImage(planesAsByteArray(), ImageFormat.NV21, SCAN_WIDTH, SCAN_HEIGHT, null)
        .compressToJpeg(SCAN_RECT, JPEG_QUALITY, outputStream)

    val data = outputStream.toByteArray()
    BitmapFactory.decodeByteArray(data, 0, data.size)
}