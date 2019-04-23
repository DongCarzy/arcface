package com.arcsoft;

import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public interface AFD_FSDKLibrary extends Library {

    AFD_FSDKLibrary INSTANCE = LoadUtils.loadLibrary(Platform.isWindows() ? "libarcsoft_fsdk_face_detection.dll" : "libarcsoft_fsdk_face_detection.so", AFD_FSDKLibrary.class);

    /**
     * 初始化
     */
    NativeLong AFD_FSDK_InitialFaceEngine(String appid, String sdkid, Pointer pMem, int lMemSize, PointerByReference phEngine, int iOrientPriority, int nScale, int nMaxFaceNum);

    /**
     * 根据输入的图像检测出人脸位置，一般用于静态图像检测
     *
     * @return
     */
    NativeLong AFD_FSDK_StillImageFaceDetection(Pointer hEngine, ASVLOFFSCREEN pImgData, PointerByReference pFaceRes);

    /**
     * 卸载
     */
    NativeLong AFD_FSDK_UninitialFaceEngine(Pointer hEngine);

    /**
     * 获取版本
     */
    AFD_FSDK_Version AFD_FSDK_GetVersion(Pointer hEngine);
}