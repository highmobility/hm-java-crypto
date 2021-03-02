package com.highmobility.cryptok

import com.highmobility.cryptok.value.*
import com.highmobility.cryptok.value.PrivateKey
import com.highmobility.cryptok.value.PublicKey
import com.highmobility.cryptok.value.Signature
import com.highmobility.value.Bytes
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils.xor
import java.security.*
import java.util.*
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

typealias JavaSignature = java.security.Signature
typealias JavaPrivateKey = java.security.PrivateKey
typealias JavaPublicKey = java.security.PublicKey

/*
Key-pair used in Public-Key Infrastructure: ECDH secp256r1
Signature for downloading access certificates: ECDSA, SHA256
JWT signature For signing Service Account API requests: ES256
 */

val KEY_GEN_ALGORITHM = "ECDH" // EC and ECDSA can be used with same algorithm
var SIGN_ALGORITHM = "SHA256withPLAIN-ECDSA"

val CURVE_NAME = "secp256r1" // this is 1.3.132.0.prime256v1
val params = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
val CURVE = ECDomainParameters(params.curve, params.g, params.n, params.h)
val CURVE_SPEC = ECParameterSpec(params.curve, params.g, params.n, params.h)

class Crypto {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    /**
     * Create a random serial number.
     *
     * @return the serial number.
     */
    fun createSerialNumber(): DeviceSerial {
        val serialBytes = ByteArray(9)
        Random().nextBytes(serialBytes)
        return DeviceSerial(serialBytes)
    }

    /**
     * Create a keypair.
     *
     * @return The KeyPair.
     */
    fun createKeypair(): HMKeyPair {
        val javaKeyPair = createJavaKeypair()
        val publicKeyBytes = javaKeyPair.public.getBytes()

        val publicKey = PublicKey(publicKeyBytes)
        val privateKey = PrivateKey(javaKeyPair.private.getBytes())

        return HMKeyPair(privateKey, publicKey)
    }

    fun createJavaKeypair(): KeyPair {
        val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
        val g = KeyPairGenerator.getInstance(KEY_GEN_ALGORITHM, "BC")
        g.initialize(ecSpec, SecureRandom())
        val javaKeyPair = g.generateKeyPair()
        return javaKeyPair
    }

    fun verify(message: Bytes, signature: Signature, publicKey: JavaPublicKey): Boolean {
        val formattedMessage = message.fillWith0sUntil(64)

        val ecdsaVerify = JavaSignature.getInstance("SHA256withPLAIN-ECDSA", "BC")
        ecdsaVerify.initVerify(publicKey)
        ecdsaVerify.update(formattedMessage.byteArray)
        val result = ecdsaVerify.verify(signature.byteArray)
        return result
    }

    /**
     * Sign data.
     *
     * @param message      The message that will be signed.
     * @param privateKey The private key that will be used for signing.
     * @return The signature.
     */
    fun sign(message: Bytes, privateKey: PrivateKey): Signature {
        val formattedMessage = message.fillWith0sUntil(64)
        // https://stackoverflow.com/questions/34063694/fixed-length-64-bytes-ec-p-256-signature-with-jce
        // there are also withCVC-ECDSA, withECDSA
        val signature = JavaSignature.getInstance(SIGN_ALGORITHM, "BC")
        signature.initSign(privateKey.toJavaKey())
        signature.update(formattedMessage.byteArray)
        val sigBytes = signature.sign()
        return Signature(sigBytes)
    }

    /**
     * Sign data.
     *
     * @param message      The message that will be signed.
     * @param privateKey The private key that will be used for signing.
     * @return The signature.
     */
    fun sign(message: ByteArray, privateKey: PrivateKey): Signature {
        return sign(Bytes(message), privateKey)
    }

    /**
     * Verify a signature.
     *
     * @param message      The message that was signed.
     * @param signature The signature.
     * @param publicKey The public key that is used for verifying.
     * @return The verification result.
     */
    fun verify(message: Bytes, signature: Signature, publicKey: PublicKey): Boolean {
        return verify(message, signature, publicKey.toJavaKey())
    }

    /**
     * Verify a signature.
     *
     * @param data      The data that was signed.
     * @param signature The signature.
     * @param publicKey The public key that is used for verifying.
     * @return The verification result.
     */
    fun verify(data: Bytes, signature: Signature, publicKey: ByteArray): Boolean {
        return verify(data, signature, PublicKey(publicKey))
    }

    fun sha256(bytes: ByteArray): Sha256 {
        val digest = MessageDigest.getInstance("SHA-256", "BC")
        return Sha256(digest.digest(bytes))
    }

    fun sha256(bytes: Bytes): Sha256 {
        return sha256(bytes.byteArray)
    }

    /**
     * Create JWT signature. It is the same as normal signing, but without padding to 64
     *
     * @param message The message
     * @param privateKey The private key
     * @return The signature
     */
    fun signJWT(message: ByteArray, privateKey: PrivateKey): Signature {
        val signature = JavaSignature.getInstance(SIGN_ALGORITHM, "BC")
        signature.initSign(privateKey.toJavaKey())
        signature.update(message)
        val sigBytes = signature.sign()
        return Signature(sigBytes)
    }

    /**
     * Create JWT signature. It is the same as normal signing, but without padding to 64
     *
     * @param message The message
     * @param privateKey The private key
     * @return The signature
     */
    fun signJWT(message: Bytes, privateKey: PrivateKey): Signature {
        return signJWT(message.byteArray, privateKey)
    }

    // MARK: Telematics

    fun createTelematicsContainer(
        command: Bytes,
        privateKey: PrivateKey,
        serial: DeviceSerial,
        accessCertificate: AccessCertificate,
        nonce: Bytes,
    ): Bytes {
        val container = TelematicsContainer(
            this,
            command,
            privateKey,
            // if fleet is sending, the sender serial is the gainer(fleet)
            accessCertificate.gainerPublicKey,
            serial,
            accessCertificate.gainerSerial,
            nonce
        )
        return container.getEscapedAndWithStartEndBytes()
    }

    // This can throw IllegalArgumentException
    fun getCommandFromTelematicsContainer(
        container: Bytes,
        privateKey: PrivateKey,
        accessCertificate: AccessCertificate,
    ): Bytes {
        val container = TelematicsContainer(
            this,
            container,
            privateKey,
            accessCertificate.gainerPublicKey
        )

        return container.getUnecryptedPayload()
    }

    internal fun encryptDecrypt(
        message: Bytes,
        privateKey: PrivateKey,
        publicKey: PublicKey,
        nonce: Bytes
    ): Bytes {
        // Uses private_key and access_certificate to compute key
        // Creates session_key(hmac) using compute_key as key and nonce as message
        val sessionKey = createSessionKey(privateKey, publicKey, nonce)
        // Creates signature using session_key
        val encryptionKey = sessionKey.getRange(0, 16)
        // expand nonce because it should be 16 bytes long
        val iv = nonce.getRange(0, 7).concat(nonce)
        // Creates signature using session_key
        val cipher = aes(iv, encryptionKey)

        // Expand cipher binary(repeat it) until the container length

        var cipherExpanded = cipher
        while (cipherExpanded.size < message.size) {
            cipherExpanded = cipherExpanded.concat(cipherExpanded)
        }
        cipherExpanded = cipherExpanded.getRange(0, message.size)
        // XOR the expanded cipher and container
        val result = xor(cipherExpanded.byteArray, message.byteArray)
        return Bytes(result)
    }

    internal fun createSessionKey(
        privateKey: PrivateKey,
        otherPublicKey: PublicKey,
        nonce: Bytes
    ): Bytes {
        return hmac(createSharedSecret(privateKey, otherPublicKey), nonce)
    }

    internal fun hmac(sharedSecret: Bytes, message: Bytes): Bytes {
        val key: SecretKey = SecretKeySpec(sharedSecret.byteArray, "HmacSHA256")
        val mac: Mac = Mac.getInstance("HmacSHA256", "BC")
        mac.init(key)
        val hmac = mac.doFinal(message.fillWith0sUntil(256).byteArray)
        return Bytes(hmac)
    }

    // Shared Diffie-Helman key from my private and other public
    internal fun createSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): Bytes {
        val ka = KeyAgreement.getInstance(KEY_GEN_ALGORITHM, "BC")
        ka.init(privateKey.toJavaKey())
        ka.doPhase(publicKey.toJavaKey(), true);
        val secret = ka.generateSecret() // 32 bytes
        return Bytes(secret)
    }

    internal fun aes(key: Bytes, message: Bytes): Bytes {
        val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC")
        val key = SecretKeySpec(key.byteArray, "AES")

        // Encrypt
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val cipherText = ByteArray(cipher.getOutputSize(message.length))
        var ctLength = cipher.update(message.byteArray, 0, message.length, cipherText, 0)
        ctLength += cipher.doFinal(cipherText, ctLength)

        return Bytes(cipherText)
    }
}