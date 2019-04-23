package com.arcsoft.utils;

import java.math.BigInteger;

/**
 * @author carzy.
 * @date 9:55 2019/4/20
 */
public class ByteUtil {

    /**
     * 将byte[]转为各种进制的字符串
     * <p>
     * 这里的1代表正数
     *
     * @param bytes byte[]
     * @param radix 基数可以转换进制的范围，从Character.MIN_RADIX到Character.MAX_RADIX，超出范围后变为10进制
     * @return 转换后的字符串
     */
    public static String binary(byte[] bytes, int radix) {
        return new BigInteger(1, bytes).toString(radix);
    }
}
