package com.highmobility.cryptok

import com.highmobility.cryptok.value.DeviceSerial
import com.highmobility.cryptok.value.PrivateKey
import com.highmobility.value.Bytes
import org.junit.Test

internal class TelematicsContainerTest {
    val crypto = Crypto()

    val senderPrivateKey = PrivateKey(
        "468A685967EF57ADC7FB6C51B12045722C74277C45EDD8EC005D1FF4197D6006"
    )
    val certificate = AccessCertificate(
        "01746D63730908070605040302010102030405060708096A94B494B0BD287E9C98014CECC1E3E19F30C002B74116BB727B93FB422DBDC12172A3FD24C36EB2ABEC57AA94D74A53A393D7B0AE30E6131B1EEDBCA3A17530110101010111020201010301020310937CB737CD8EA3E5E510830A54D2945F5BD8C54A1486489D7E8B911B06ABC1CE12B1D1B4E9994D99987106C63730919E5630FFBD755CF00AD62ABA1AC53983"
    )
    val nonce = Bytes("AABBCCDDEEFF000000")
    val targetSerial = DeviceSerial("010203040506070809")
    val command = Bytes("AABB")

    val expectedBytes =
        Bytes("0002010203040506070809010203040506070809AABBCCDDEEFEFFFE00FE00FE00FE00FE000101FE00FE00FE0002DCF5831310BE87BDDCE08909F4FCA0D1FEFE0E05BA52F831ADECFEFFDE8DE86C571CFCC3FF")

    @Test
    fun varCtor() {
        // create the container with variables
        val container = TelematicsContainer(
            crypto,
            command,
            senderPrivateKey,
            certificate.gainerPublicKey,
            targetSerial,
            certificate.gainerSerial,
            nonce
        )

        // assert variables set to ivars
        assert(container.senderPrivateKey == senderPrivateKey)

        //  assert OEM container the same
        assert(container.getEscapedAndWithStartEndBytes() == expectedBytes)
    }

    @Test
    fun bytesCtor() {
        val container = TelematicsContainer(
            crypto,
            expectedBytes,
            senderPrivateKey,
            certificate.gainerPublicKey
        )

        // variables set to ivars
        assert(container.senderPrivateKey == senderPrivateKey)

        // decrypted payload correct
        assert(container.getUnencryptedPayload() == command)

        // OEM container the same
        assert(container.getEscapedAndWithStartEndBytes() == expectedBytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidHmacThrows() {
        val invalidHmacBytes = expectedBytes.set(expectedBytes.size - 1, 0xAA.toByte())
        TelematicsContainer(
            crypto,
            invalidHmacBytes,
            senderPrivateKey,
            certificate.gainerPublicKey
        )
    }
}