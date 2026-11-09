package com.futsch1.medtimer.core.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.datamatrix.DataMatrixWriter
import com.google.zxing.oned.Code39Writer
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BarcodeDecoderTest {
    @Test
    fun `should decode ifa data matrix when written by zxing`() {
        val raw = "[)>${RS}06${GS}9N111234567842${GS}1TABC123${GS}D261231${GS}S1234567890$RS$EOT"

        val decoded = decode(image(DataMatrixWriter().encode(raw, BarcodeFormat.DATA_MATRIX, 0, 0)))

        assertEquals(PackageCode("12345678", "ABC123", LocalDate.of(2026, 12, 31), "1234567890"), PackageCodeParser.parse(decoded!!))
    }

    @Test
    fun `should decode gs1 data matrix when written by zxing`() {
        val content = "0104150123456782" + "17261231" + "10ABC123$GS" + "21SN0001"
        val matrix = DataMatrixWriter().encode(content, BarcodeFormat.DATA_MATRIX, 0, 0, mapOf(EncodeHintType.GS1_FORMAT to true))

        val decoded = decode(image(matrix))

        assertEquals(PackageCode("12345678", "ABC123", LocalDate.of(2026, 12, 31), "SN0001"), PackageCodeParser.parse(decoded!!))
    }

    @Test
    fun `should decode code 39 pzn when written by zxing`() {
        val decoded = decode(code39())

        assertEquals("12345678", PackageCodeParser.parse(decoded!!)?.pzn)
    }

    @Test
    fun `should decode code 39 when image is rotated by 90 degrees`() {
        val decoded = decode(code39().rotated())

        assertEquals("12345678", PackageCodeParser.parse(decoded!!)?.pzn)
    }

    @Test
    fun `should decode when row stride exceeds width`() {
        val img = image(DataMatrixWriter().encode("[)>${RS}06${GS}9N111234567842$RS$EOT", BarcodeFormat.DATA_MATRIX, 0, 0))
        val stride = img.width + 16
        val padded = ByteArray(stride * img.height) { 0x7F }
        for (y in 0 until img.height) img.pixels.copyInto(padded, y * stride, y * img.width, (y + 1) * img.width)

        val decoded = BarcodeDecoder.decode(padded, img.width, img.height, stride)

        assertEquals("12345678", PackageCodeParser.parse(decoded!!)?.pzn)
    }

    @Test
    fun `should return null when image is blank`() {
        assertNull(BarcodeDecoder.decode(ByteArray(200 * 200) { WHITE }, 200, 200))
    }

    @Test
    fun `should return null when barcode is a qr code`() {
        assertNull(decode(image(QRCodeWriter().encode("PZN-12345678", BarcodeFormat.QR_CODE, 0, 0))))
    }

    private class Gray(val pixels: ByteArray, val width: Int, val height: Int) {
        fun rotated(): Gray {
            val out = ByteArray(pixels.size)
            for (y in 0 until height) for (x in 0 until width) out[x * height + (height - 1 - y)] = pixels[y * width + x]
            return Gray(out, height, width)
        }
    }

    private fun decode(img: Gray) = BarcodeDecoder.decode(img.pixels, img.width, img.height)

    private fun code39() = image(Code39Writer().encode("-12345678", BarcodeFormat.CODE_39, 0, 80), moduleSize = 2, rowHeight = 1)

    /** Renders [matrix] scaled by [moduleSize] horizontally and [rowHeight] vertically, with a white quiet zone, as an 8-bit luminance buffer. */
    private fun image(matrix: BitMatrix, moduleSize: Int = 6, rowHeight: Int = moduleSize, quiet: Int = 10): Gray {
        val width = matrix.width * moduleSize + 2 * quiet
        val height = matrix.height * rowHeight + 2 * quiet
        val yScale = rowHeight
        val pixels = ByteArray(width * height) { WHITE }
        for (y in 0 until height - 2 * quiet) for (x in 0 until width - 2 * quiet) {
            if (matrix[x / moduleSize, y / yScale]) pixels[(y + quiet) * width + x + quiet] = 0
        }
        return Gray(pixels, width, height)
    }

    private companion object {
        const val GS = "\u001D"
        const val RS = "\u001E"
        const val EOT = "\u0004"
        const val WHITE = 0xFF.toByte()
    }
}
