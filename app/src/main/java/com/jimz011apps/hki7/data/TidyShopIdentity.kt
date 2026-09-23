package com.jimz011apps.hki7.data

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.UUID

/**
 * This device's own credential for a TidyShop family connector.
 *
 * The private key is generated inside the Android Keystore and is not extractable: it cannot be
 * read by this app, cannot be put in a backup, and never travels. Only the public half is sent to
 * the connector, and possession of the private half is proved by signing a short-lived assertion
 * for each request — the same idea as OAuth's `private_key_jwt`.
 *
 * That is what lets HKI 7 talk to a family connector without an identity provider, and without a
 * password that could be forgotten or reused. It mirrors TidyShop's own `DeviceIdentity`, under a
 * separate key alias because this is a different device enrolment than the TidyShop app's own.
 */
object TidyShopIdentity {
    private const val KEY_ALIAS = "hki7-tidyshop-device-key"
    private const val KEYSTORE = "AndroidKeyStore"

    /** Routes the assertion to the connector's device path rather than its OIDC one. */
    private const val ISSUER = "tidyshop-device"
    private const val AUDIENCE = "tidyshop-connector"

    /** Matches the connector's window; short because an assertion is made fresh per request. */
    private const val LIFETIME_SECONDS = 300L

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    fun hasKey(): Boolean = runCatching { keyStore().containsAlias(KEY_ALIAS) }.getOrDefault(false)

    /**
     * Creates the keypair if it does not exist yet. StrongBox is used where the hardware has it,
     * falling back to the ordinary keystore, because a StrongBox request throws on devices without
     * one rather than degrading.
     */
    fun ensureKey(): Result<Unit> = runCatching {
        if (hasKey()) return@runCatching
        generate(strongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
    }.recoverCatching {
        generate(strongBox = false)
    }

    private fun generate(strongBox: Boolean) {
        val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            // The dashboard signs while the screen is on but the device may still be locked,
            // so this deliberately is not tied to an unlock prompt.
            .setUserAuthenticationRequired(false)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE).apply {
            initialize(builder.build())
            generateKeyPair()
        }
    }

    /** The public half as a JWK, which is what enrolment sends. */
    fun publicJwk(): JSONObject? = runCatching {
        val public = keyStore().getCertificate(KEY_ALIAS)?.publicKey as? ECPublicKey ?: return null
        JSONObject()
            .put("kty", "EC")
            .put("crv", "P-256")
            .put("x", encode(public.w.affineX))
            .put("y", encode(public.w.affineY))
    }.getOrNull()

    /** Signs one assertion. A fresh id each time is what makes replay detectable. */
    fun signAssertion(deviceId: String, userId: String): String? = runCatching {
        val entry = keyStore().getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry ?: return null
        val now = System.currentTimeMillis() / 1000
        val header = JSONObject()
            .put("alg", "ES256")
            .put("typ", "JWT")
            .put("kid", deviceId)
        val payload = JSONObject()
            .put("iss", ISSUER)
            .put("sub", userId)
            .put("aud", AUDIENCE)
            .put("iat", now)
            .put("exp", now + LIFETIME_SECONDS)
            .put("jti", UUID.randomUUID().toString())
        val signingInput =
            base64(header.toString().toByteArray()) + "." + base64(payload.toString().toByteArray())
        val der = Signature.getInstance("SHA256withECDSA").run {
            initSign(entry.privateKey)
            update(signingInput.toByteArray())
            sign()
        }
        signingInput + "." + base64(derToJose(der))
    }.getOrNull()

    /** Dropped when the device leaves the family, so the stored key cannot outlive its enrolment. */
    fun deleteKey() {
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    private fun base64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun encode(value: BigInteger): String = base64(pad32(value))
}

/** Fixed 32-byte big-endian, since JOSE carries no length and no sign byte. */
internal fun pad32(value: BigInteger): ByteArray {
    val raw = value.toByteArray()
    val out = ByteArray(32)
    when {
        raw.size == 32 -> return raw
        // BigInteger prepends a zero byte when the top bit is set; JOSE does not want it.
        raw.size > 32 -> System.arraycopy(raw, raw.size - 32, out, 0, 32)
        else -> System.arraycopy(raw, 0, out, 32 - raw.size, raw.size)
    }
    return out
}

/**
 * Converts a JCA ECDSA signature to the form JOSE expects.
 *
 * Java emits a DER `SEQUENCE { INTEGER r, INTEGER s }` whose halves are variable length, while
 * ES256 wants r and s raw and concatenated at exactly 32 bytes each. Skipping this conversion
 * produces a signature that is well formed but verifies nowhere, so it is worth its own test.
 */
internal fun derToJose(der: ByteArray): ByteArray {
    var offset = 0
    require(der.size >= 8) { "signature is too short to be DER" }
    require(der[offset++] == 0x30.toByte()) { "signature is not a DER sequence" }
    // Long-form length: the low bits say how many following bytes hold the real length.
    if (der[offset].toInt() and 0x80 != 0) offset += (der[offset].toInt() and 0x7F)
    offset++

    require(der[offset++] == 0x02.toByte()) { "signature has no r value" }
    val rLength = der[offset++].toInt() and 0xFF
    val r = BigInteger(1, der.copyOfRange(offset, offset + rLength))
    offset += rLength

    require(der[offset++] == 0x02.toByte()) { "signature has no s value" }
    val sLength = der[offset++].toInt() and 0xFF
    val s = BigInteger(1, der.copyOfRange(offset, offset + sLength))

    return pad32(r) + pad32(s)
}

/** The inverse, used only by tests to check a converted signature against a public key. */
internal fun joseToDer(jose: ByteArray): ByteArray {
    require(jose.size == 64) { "JOSE signature must be 64 bytes" }
    fun encodeInteger(bytes: ByteArray): ByteArray {
        var value = bytes.dropWhile { it == 0.toByte() }.toByteArray()
        if (value.isEmpty()) value = byteArrayOf(0)
        // DER integers are signed, so a leading high bit needs a zero byte in front.
        if (value[0].toInt() and 0x80 != 0) value = byteArrayOf(0) + value
        return byteArrayOf(0x02, value.size.toByte()) + value
    }
    val body = encodeInteger(jose.copyOfRange(0, 32)) + encodeInteger(jose.copyOfRange(32, 64))
    return byteArrayOf(0x30, body.size.toByte()) + body
}
