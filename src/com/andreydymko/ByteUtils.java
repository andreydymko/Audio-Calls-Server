package com.andreydymko;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

public class ByteUtils {
    public final static int UUID_BYTES = Long.BYTES * 2;

    public static byte getBoolBytes(boolean bool) {
        return (byte) (bool ? 1 : 0);
    }

    public static int getInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] getIntBytes(int integer) {
        return ByteBuffer.wrap(new byte[Integer.BYTES]).putInt(integer).array();
    }

    public static UUID getUUID(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static byte[] getUUIDBytes(UUID uuid) {
        return ByteBuffer.wrap(new byte[UUID_BYTES])
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    public static byte[] concatAll(byte b, byte[]... bytes) {
        int totalLength = 1;
        for (byte[] array : bytes) {
            totalLength += array.length;
        }
        byte[] result = new byte[totalLength];
        result[0] = b;

        int offset = 1;
        for (byte[] array : bytes) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
