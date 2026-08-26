package com.jimz011apps.hki7.data

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Encodes the analysis frame as JPEG. Rotation is handled by setting
 * [androidx.camera.core.ImageAnalysis.Builder.setTargetRotation] so this path does not
 * decode/re-compress every frame.
 */
internal fun ImageProxy.toJpegBytes(quality: Int = 70): ByteArray? {
    if (format != ImageFormat.YUV_420_888) return null
    val nv21 = toNv21() ?: return null
    val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    if (!yuv.compressToJpeg(Rect(0, 0, width, height), quality, out)) return null
    return out.toByteArray()
}

private fun ImageProxy.toNv21(): ByteArray? {
    val yPlane = planes.getOrNull(0) ?: return null
    val uPlane = planes.getOrNull(1) ?: return null
    val vPlane = planes.getOrNull(2) ?: return null
    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    val nv21 = ByteArray(width * height * 3 / 2)
    var pos = 0
    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    if (yPixelStride == 1 && yRowStride == width) {
        yBuffer.get(nv21, 0, width * height)
        pos = width * height
    } else {
        for (row in 0 until height) {
            val yOffset = row * yRowStride
            for (col in 0 until width) {
                nv21[pos++] = yBuffer.get(yOffset + col * yPixelStride)
            }
        }
    }
    val chromaHeight = height / 2
    val chromaWidth = width / 2
    val vRowStride = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride
    val uRowStride = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride
    for (row in 0 until chromaHeight) {
        val vOffset = row * vRowStride
        val uOffset = row * uRowStride
        for (col in 0 until chromaWidth) {
            nv21[pos++] = vBuffer.get(vOffset + col * vPixelStride)
            nv21[pos++] = uBuffer.get(uOffset + col * uPixelStride)
        }
    }
    return nv21
}
