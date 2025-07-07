package com.pratham.backuputility.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for cryptographic hash operations
 */
public final class HashUtil {

    private HashUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Convert bytes to hexadecimal string representation
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Create a new SHA-256 message digest instance
     */
    public static MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Calculate SHA-256 hash of data and return as hex string
     */
    public static String calculateSha256Hash(byte[] data) {
        MessageDigest digest = createSha256Digest();
        digest.update(data);
        return bytesToHex(digest.digest());
    }

    /**
     * Calculate SHA-256 hash of partial data and return as hex string
     */
    public static String calculateSha256Hash(byte[] data, int length) {
        MessageDigest digest = createSha256Digest();
        digest.update(data, 0, length);
        return bytesToHex(digest.digest());
    }
}
