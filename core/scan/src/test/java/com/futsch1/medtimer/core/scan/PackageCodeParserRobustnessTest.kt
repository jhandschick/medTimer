package com.futsch1.medtimer.core.scan

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.random.Random

@RunWith(Parameterized::class)
class PackageCodeParserRobustnessTest(private val name: String, private val raw: String) {
    companion object {
        private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ\u001D\u001E\u0004]d)>[- "

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun inputs(): List<Array<String>> {
            val fixed = listOf(
                "", " ", "\u001D", "]d2", "[)>\u001E06\u001D", "[)>\u001E06\u001D9N", "01", "0104150", "9N11", "D", "-", "PZN-",
                "\u001D".repeat(200), "]d2017", "]d23101", "]d2410", "[)>\u001E06\u001D9N111234567842\u001DD2612"
            )
            val random = Random(42)
            val generated = List(1000) { String(CharArray(random.nextInt(1, 60)) { ALPHABET[random.nextInt(ALPHABET.length)] }) }
            return (fixed + generated).mapIndexed { index, raw -> arrayOf("input $index", raw) }
        }
    }

    @Test
    fun `should not throw when input is malformed`() {
        PackageCodeParser.parse(raw)
    }
}
