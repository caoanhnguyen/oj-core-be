package com.kma.ojcore.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidHelper {

    /**
     * Converts a 16-byte array into a standard UUID object.
     * Prevents the issue with UUID.nameUUIDFromBytes which generates a VERSION 3 UUID instead of restoring the original bits.
     *
     * @param bytes 16-byte array from database BINARY(16)
     * @return Valid UUID
     */
    public static UUID getUuidFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}
