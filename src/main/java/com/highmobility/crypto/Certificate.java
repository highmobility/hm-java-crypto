package com.highmobility.crypto;

import com.highmobility.utils.Base64;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by ttiganik on 13/04/16.
 */
public class Certificate {
    byte[] bytes;

    Certificate(byte[] bytes) {
        this.bytes = bytes;
    }

    Certificate(){}

    /**
     * @return The certificate data in binary form, excluding the signature.
     */
    public byte [] getCertificateData() {
        return null;
    }

    /**
     * @return The Certificate Authority's signature for the certificate, 64 bytes.
     */
    public byte[] getSignature() {
        return null;
    }

    /**
     * @return The full certificate bytes, with the signature if it exists.
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     *
     * @return The raw bytes encoded in base64.
     */
    public String getBase64() { return Base64.encode(bytes); }

    static Calendar dateFromBytes(byte[] bytes) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0);
        cal.set(2000 + bytes[0], bytes[1] - 1, bytes[2], bytes[3], bytes[4]);
        return cal; // get back a Date object
    }

    static byte[] bytesFromDate(Calendar calendar) {
        byte [] bytes = new byte[5];

        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        bytes[0] = (byte)(calendar.get(Calendar.YEAR) - 2000);
        bytes[1] = (byte)(calendar.get(Calendar.MONTH) + 1);
        bytes[2] = (byte)(calendar.get(Calendar.DAY_OF_MONTH));
        bytes[3] = (byte)(calendar.get(Calendar.HOUR));
        bytes[4] = (byte)(calendar.get(Calendar.MINUTE));

        return bytes;
    }
}
