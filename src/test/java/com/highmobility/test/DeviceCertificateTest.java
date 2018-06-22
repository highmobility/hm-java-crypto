package com.highmobility.test;

import com.highmobility.crypto.DeviceCertificate;
import com.highmobility.crypto.value.AppIdentifier;
import com.highmobility.value.Bytes;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.crypto.value.Issuer;
import com.highmobility.crypto.value.PublicKey;
import com.highmobility.crypto.value.Signature;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class DeviceCertificateTest {
    Issuer issuer = new Issuer("00000000");
    AppIdentifier appIdentifier = new AppIdentifier("111111111111111111111111");
    DeviceSerial serial = new DeviceSerial("222222222222222222");
    PublicKey publicKey = new PublicKey
            ("33333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333");
    Signature signature = new Signature
            ("44444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444444");

    Bytes bytes = new Bytes(issuer.getHex() +
            appIdentifier.getHex() +
            serial.getHex() +
            publicKey.getHex() +
            signature.getHex());

    Bytes bytesWithOutSignature = new Bytes(issuer.getHex() +
            appIdentifier.getHex() +
            serial.getHex() +
            publicKey.getHex());

    @Test public void ctorWithBytes() {
        DeviceCertificate cert = new DeviceCertificate(bytes);
        assertTrue(cert.getIssuer().equals(issuer));
        assertTrue(cert.getAppIdentifier().equals(appIdentifier));
        assertTrue(cert.getSerial().equals(serial));
        assertTrue(cert.getPublicKey().equals(publicKey));
        assertTrue(cert.getSignature().equals(signature));
        assertTrue(cert.getCertificateData().equals(bytesWithOutSignature));
    }

    @Test public void ctorWithVars() {
        DeviceCertificate cert = new DeviceCertificate(issuer, appIdentifier, serial, publicKey);
        assertTrue(cert.getBytes().equals(bytesWithOutSignature));
        cert.setSignature(signature);
        assertTrue(cert.getBytes().equals(bytes));
        assertTrue(cert.getCertificateData().equals(bytesWithOutSignature));
    }
}