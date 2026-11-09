package com.futsch1.medtimer.feature.ui.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.futsch1.medtimer.core.scan.BarcodeDecoder

/** Decodes the luminance (Y) plane of each camera frame while [shouldDecode] is true and reports any barcode text. */
class PackageAnalyzer(
    private val shouldDecode: () -> Boolean,
    private val onRaw: (String) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        image.use {
            if (!shouldDecode()) {
                return
            }
            // YUV_420_888: the first plane is luminance with a pixel stride of 1
            val plane = it.planes[0]
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BarcodeDecoder.decode(bytes, it.width, it.height, plane.rowStride)?.let(onRaw)
        }
    }
}
