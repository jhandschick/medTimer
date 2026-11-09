package com.futsch1.medtimer.core.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/** Finds a pharmacy package code (Data Matrix or Code 39) in an 8-bit luminance image, e.g. the Y plane of a camera frame. */
object BarcodeDecoder {
    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.DATA_MATRIX, BarcodeFormat.CODE_39),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "ISO-8859-1"
    )

    /** Returns the raw barcode text as decoded (group separators and Macro 06 envelope kept), or null if none was found. */
    fun decode(luminance: ByteArray, width: Int, height: Int, rowStride: Int = width): String? =
        read(luminance, rowStride, width, height)
            // Linear codes are only found along rows; a code held upright relative to the sensor needs the rotated frame.
            ?: read(rotate(luminance, width, height, rowStride), height, height, width)

    private fun read(data: ByteArray, dataWidth: Int, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(data, dataWidth, height, 0, 0, width, height, false)
        return try {
            MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (_: ReaderException) {
            null
        }
    }

    private fun rotate(data: ByteArray, width: Int, height: Int, rowStride: Int): ByteArray {
        val rotated = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                rotated[x * height + (height - 1 - y)] = data[y * rowStride + x]
            }
        }
        return rotated
    }
}
