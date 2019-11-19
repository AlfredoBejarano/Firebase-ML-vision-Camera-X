package me.alfredobejarano.cameraxfirebasemlvision.extensions

import androidx.camera.core.ImageProxy.PlaneProxy

/**
 * Created by alfredo on 2019-11-19.
 * Copyright © 2019 GROW. All rights reserved.
 */

/**
 * Parse a [PlaneProxy] into a [ByteArray].
 */
fun PlaneProxy?.asByteArray() = this?.buffer?.let { buffer ->
    buffer.rewind()
    val array = ByteArray(buffer.remaining())
    buffer.get(array)
    array
} ?: run {
    byteArrayOf()
}