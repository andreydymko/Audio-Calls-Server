package com.andreydymko.Connection;

import com.andreydymko.ByteUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.charset.Charset;
import java.util.UUID;

public class NetUtils {
    public static byte readByte(InputStream inputStream) throws IOException {
        return (byte) inputStream.read();
    }

    public static boolean readBoolean(InputStream inputStream) throws IOException {
        return readByte(inputStream) != 0;
    }

    public static int readInt(InputStream inputStream) throws IOException, BufferUnderflowException {
        byte[] intBuffer = new byte[Integer.BYTES];
        if (inputStream.read(intBuffer) != intBuffer.length) {
            throw new BufferUnderflowException();
        }
        return ByteUtils.getInt(intBuffer);
    }

    public static String readString(InputStream inputStream, int strByteLen, Charset charset) throws IOException, BufferUnderflowException {
        if (strByteLen == 0) {
            return "";
        }
        byte[] strByteBuff = new byte[strByteLen];
        if (inputStream.read(strByteBuff) != strByteBuff.length) {
            throw new BufferUnderflowException();
        }

        return new String(strByteBuff, charset);
    }

    public static UUID readUUID(InputStream inputStream) throws IOException, BufferUnderflowException {
        byte[] uuidBuffer = new byte[16];
        if (inputStream.read(uuidBuffer) != uuidBuffer.length) {
            throw new BufferUnderflowException();
        }
        return ByteUtils.getUUID(uuidBuffer);
    }
}
