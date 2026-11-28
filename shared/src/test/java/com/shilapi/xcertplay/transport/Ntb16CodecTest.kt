package com.shilapi.xcertplay.transport

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ntb16CodecTest {
    @Test
    fun parseCanReadBlockFromLargerBuffer() {
        val frame = byteArrayOf(0x33, 0x33, 0, 0, 0, 1, 0x86.toByte(), 0xdd.toByte())
        val block = Ntb16Codec.build(frame, 7)
        val buffer = ByteArray(11) + block + ByteArray(3)

        val parsed = Ntb16Codec.parse(buffer, 11, block.size)

        assertArrayEquals(frame, parsed.single())
    }

    @Test
    fun largestDatagramKeepsBlockLengthWithinU16() {
        val block = Ntb16Codec.build(ByteArray(65_507), 0x1234)

        assertEquals(65_535, block.size)
        assertEquals(0xff, block[8].toInt() and 0xff)
        assertEquals(0xff, block[9].toInt() and 0xff)
    }

    @Test
    fun malformedNtbLengthIsRejectedWithoutThrowing() {
        val block = Ntb16Codec.build(byteArrayOf(1, 2, 3, 4), 1)
        block[8] = 0
        block[9] = 0
        assertTrue(Ntb16Codec.parse(block).isEmpty())
    }

    @Test
    fun malformedNdpLengthIsRejectedWithoutThrowing() {
        val block = Ntb16Codec.build(byteArrayOf(1, 2, 3, 4), 1)
        block[16] = 0
        block[17] = 0
        assertTrue(Ntb16Codec.parse(block).isEmpty())
    }

    @Test
    fun malformedNdpSignatureIsRejectedWithoutThrowing() {
        val block = Ntb16Codec.build(byteArrayOf(1, 2, 3, 4), 1)
        block[12] = 0
        assertTrue(Ntb16Codec.parse(block).isEmpty())
    }

    @Test
    fun datagramPointerOutsideBlockIsIgnored() {
        val block = Ntb16Codec.build(byteArrayOf(1, 2, 3, 4), 1)
        block[20] = 0xff.toByte()
        block[21] = 0xff.toByte()
        assertTrue(Ntb16Codec.parse(block).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun datagramThatWouldOverflowBlockLengthIsRejected() {
        Ntb16Codec.build(ByteArray(65_508), 0x1234)
    }
}
