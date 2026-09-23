package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * The device assertion is signed with JCA, which emits DER, and verified by the connector as
 * JOSE, which wants raw r||s. These tests exercise that conversion against real keys, because
 * getting it wrong produces a signature that looks fine and verifies nowhere — the failure would
 * only show up as a 401 from somebody else's server.
 *
 * The Keystore half of [TidyShopIdentity] is not covered here: it needs a device.
 */
class TidyShopIdentityTest {

    private fun keyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
    }.generateKeyPair()

    @Test
    fun `converted signature is 64 bytes and still verifies`() {
        val keys = keyPair()
        // Many rounds: r or s having a leading zero, or a high bit, is the case that breaks
        // naive conversions, and it only shows up in a fraction of signatures.
        repeat(200) {
            val message = ByteArray(48).also { bytes -> SecureRandom().nextBytes(bytes) }
            val der = Signature.getInstance("SHA256withECDSA").run {
                initSign(keys.private)
                update(message)
                sign()
            }

            val jose = derToJose(der)
            assertEquals("ES256 signatures are exactly 64 bytes", 64, jose.size)

            val verified = Signature.getInstance("SHA256withECDSA").run {
                initVerify(keys.public)
                update(message)
                verify(joseToDer(jose))
            }
            assertTrue("converted signature must still verify", verified)
        }
    }

    @Test
    fun `padding drops the sign byte and left-pads short values`() {
        // 33 bytes: BigInteger's leading zero for a high top bit must be dropped, not kept.
        val high = BigInteger(1, ByteArray(32) { 0xFF.toByte() })
        assertEquals(33, high.toByteArray().size)
        assertEquals(32, pad32(high).size)
        assertEquals(0xFF.toByte(), pad32(high)[0])

        // A small value must be right-aligned, not left-aligned.
        val small = BigInteger.valueOf(1)
        val padded = pad32(small)
        assertEquals(32, padded.size)
        assertEquals(1.toByte(), padded[31])
        assertEquals(0.toByte(), padded[0])
    }

    @Test
    fun `a truncated signature is rejected rather than misread`() {
        val keys = keyPair()
        val der = Signature.getInstance("SHA256withECDSA").run {
            initSign(keys.private)
            update("hello".toByteArray())
            sign()
        }
        val truncated = der.copyOfRange(0, 4)
        assertTrue("a short DER blob must fail loudly", runCatching { derToJose(truncated) }.isFailure)
    }
}
