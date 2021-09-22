package com.andreydymko;

import java.util.Collection;

public class ArrayLocalUtils {
    public static void arrayCopy(final byte[] src, int srcStart, byte[] dst, int dstStart, int length) {
        for (int i1 = srcStart, i2 = dstStart, end1 = srcStart + length, end2 = dstStart + length; i1 < end1 && i2 < end2; i1++, i2++) {
            dst[i2] = src[i1];
        }
    }

    public static <T> boolean addUnique(Collection<T> collection, T value) {
        if (collection.contains(value)) {
            return false;
        }
        return collection.add(value);
    }
}
