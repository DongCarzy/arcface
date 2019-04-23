package com.arcsoft;

import com.sun.jna.Native;

public class LoadUtils {

    public static <T> T loadLibrary(String filePath, Class<T> interfaceClass) {
        return Native.loadLibrary(filePath,interfaceClass);
    }
}
