package com.arcsoft;

import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * arcface RF
 *
 * @author carzy
 */
public interface AFR_FSDKLibrary extends Library {
    AFR_FSDKLibrary INSTANCE = LoadUtils.loadLibrary(Platform.isWindows() ? "libarcsoft_fsdk_face_recognition.dll" : "/home/ubuntu/work/lib/libarcsoft_fsdk_face_recognition.so", AFR_FSDKLibrary.class);

    /**
     * 初始化
     */
    NativeLong AFR_FSDK_InitialEngine(
            String appid,
            String sdkid,
            Pointer pMem,
            int lMemSize,
            PointerByReference phEngine
    );

    /**
     * 获取脸部特征
     */
    NativeLong AFR_FSDK_ExtractFRFeature(
            Pointer hEngine,
            ASVLOFFSCREEN pImgData,
            AFR_FSDK_FACEINPUT pFaceRes,
            AFR_FSDK_FACEMODEL pFaceModels
    );

    /**
     * 脸部特征比较.
     */
    NativeLong AFR_FSDK_FacePairMatching(
            Pointer hEngine,
            AFR_FSDK_FACEMODEL reffeature,
            AFR_FSDK_FACEMODEL probefeature,
            FloatByReference pfSimilScore
    );

    /**
     * 销毁引擎，释放相应资源
     */
    NativeLong AFR_FSDK_UninitialEngine(Pointer hEngine);

    /**
     * 获取版本
     */
    AFR_FSDK_Version AFR_FSDK_GetVersion(Pointer hEngine);
}