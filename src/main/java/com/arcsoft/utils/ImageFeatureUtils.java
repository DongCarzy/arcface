package com.arcsoft.utils;

import com.arcsoft.*;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;

import static com.arcsoft.demo.FRTest.MAX_FACE_NUM;

/**
 * @author carzy.
 * @date 13:56 2019/4/23
 */
@SuppressWarnings("Duplicates")
public class ImageFeatureUtils {

    private String APPID = "e2CaBGmvWgLupK7FMGJL1GP7WdXL5C9KCgaT3pz2s2o";
    private String FD_SDKKEY = "9PP4oEazo6FcJwkhhJbfAbGWCyWx3RPyio29vcPXfF25";
    private String FR_SDKKEY = "9PP4oEazo6FcJwkhhJbfAbGzraZeXqwjEz7JmGxDQRLp";

    public ImageFeatureUtils(String appID, String fdSdkkey, String frSdkkey) {
        this.APPID = appID;
        this.FD_SDKKEY = fdSdkkey;
        this.FR_SDKKEY = frSdkkey;
    }

    private final int FD_WORKBUF_SIZE = 20 * 1024 * 1024;
    private final int FR_WORKBUF_SIZE = 40 * 1024 * 1024;

    public byte[] getFeature(String path) {
        // init Engine
        Pointer pFDWorkMem = CLibrary.INSTANCE.malloc(FD_WORKBUF_SIZE);
        Pointer pFRWorkMem = CLibrary.INSTANCE.malloc(FR_WORKBUF_SIZE);
        Pointer hFDEngine = null;
        Pointer hFREngine= null;

        try {
            // FD
            PointerByReference phFDEngine = new PointerByReference();
            AFD_FSDKLibrary.INSTANCE.AFD_FSDK_InitialFaceEngine(APPID, FD_SDKKEY, pFDWorkMem, FD_WORKBUF_SIZE, phFDEngine, _AFD_FSDK_OrientPriority.AFD_FSDK_OPF_0_HIGHER_EXT, 32, MAX_FACE_NUM);
            hFDEngine = phFDEngine.getValue();
            // FR
            PointerByReference phFREngine = new PointerByReference();
            AFR_FSDKLibrary.INSTANCE.AFR_FSDK_InitialEngine(APPID, FR_SDKKEY, pFRWorkMem, FR_WORKBUF_SIZE, phFREngine);
            hFREngine = phFREngine.getValue();

            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return printFacePbFeature(file, hFDEngine, hFREngine);
            } else {
                throw new RuntimeException("文件不存在");
            }
        } finally {
            if (hFDEngine != null){
                AFD_FSDKLibrary.INSTANCE.AFD_FSDK_UninitialFaceEngine(hFDEngine);
            }
            if (hFREngine != null){
                AFR_FSDKLibrary.INSTANCE.AFR_FSDK_UninitialEngine(hFREngine);
            }
            CLibrary.INSTANCE.free(pFDWorkMem);
            CLibrary.INSTANCE.free(pFRWorkMem);
        }
    }

    private byte[] printFacePbFeature(File file, Pointer hFDEngine, Pointer hFREngine) {
        try {
            ASVLOFFSCREEN img = loadImage(file.getPath());
            FaceInfo[] faceInfos = doFaceDetection(hFDEngine, img);
            if (faceInfos.length < 1) {
                throw new RuntimeException("no face in Image");
            }
            AFR_FSDK_FACEMODEL faceFeature = extractFRFeature(hFREngine, img, faceInfos[0]);
            if (faceFeature != null) {
                byte[] f = faceFeature.toByteArray();
                faceFeature.freeUnmanaged();
                return f;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * 加载图片
     *
     * @param filePath 图片地址
     * @return 图片信息
     */
    private ASVLOFFSCREEN loadImage(String filePath) {
        ASVLOFFSCREEN inputImg = new ASVLOFFSCREEN();
        BufferInfo bufferInfo = ImageLoader.getI420FromFile(filePath);
        inputImg.u32PixelArrayFormat = ASVL_COLOR_FORMAT.ASVL_PAF_I420;
        inputImg.i32Width = bufferInfo.width;
        inputImg.i32Height = bufferInfo.height;
        inputImg.pi32Pitch[0] = inputImg.i32Width;
        inputImg.pi32Pitch[1] = inputImg.i32Width / 2;
        inputImg.pi32Pitch[2] = inputImg.i32Width / 2;
        inputImg.ppu8Plane[0] = new Memory(inputImg.pi32Pitch[0] * inputImg.i32Height);
        inputImg.ppu8Plane[0].write(0, bufferInfo.buffer, 0, inputImg.pi32Pitch[0] * inputImg.i32Height);
        inputImg.ppu8Plane[1] = new Memory(inputImg.pi32Pitch[1] * inputImg.i32Height / 2);
        inputImg.ppu8Plane[1].write(0, bufferInfo.buffer, inputImg.pi32Pitch[0] * inputImg.i32Height, inputImg.pi32Pitch[1] * inputImg.i32Height / 2);
        inputImg.ppu8Plane[2] = new Memory(inputImg.pi32Pitch[2] * inputImg.i32Height / 2);
        inputImg.ppu8Plane[2].write(0, bufferInfo.buffer, inputImg.pi32Pitch[0] * inputImg.i32Height + inputImg.pi32Pitch[1] * inputImg.i32Height / 2, inputImg.pi32Pitch[2] * inputImg.i32Height / 2);
        inputImg.ppu8Plane[3] = Pointer.NULL;
        inputImg.setAutoRead(false);
        return inputImg;
    }

    /**
     * 获取人脸信息集合
     *
     * @param hFDEngine 驱动引擎
     * @param inputImg  图片文件
     * @return FaceInfo[]
     */
    private FaceInfo[] doFaceDetection(Pointer hFDEngine, ASVLOFFSCREEN inputImg) {
        FaceInfo[] faceInfo = new FaceInfo[0];

        PointerByReference ppFaceRes = new PointerByReference();
        NativeLong ret = AFD_FSDKLibrary.INSTANCE.AFD_FSDK_StillImageFaceDetection(hFDEngine, inputImg, ppFaceRes);
        if (ret.longValue() != 0) {
            throw new RuntimeException(String.format("AFD_FSDK_StillImageFaceDetection ret 0x%x", ret.longValue()));
        }

        AFD_FSDK_FACERES faceRes = new AFD_FSDK_FACERES(ppFaceRes.getValue());
        if (faceRes.nFace > 0) {
            faceInfo = new FaceInfo[faceRes.nFace];
            for (int i = 0; i < faceRes.nFace; i++) {
                MRECT rect = new MRECT(new Pointer(Pointer.nativeValue(faceRes.rcFace.getPointer()) + faceRes.rcFace.size() * i));
                int orient = faceRes.lfaceOrient.getPointer().getInt(i * 4);
                faceInfo[i] = new FaceInfo();

                faceInfo[i].left = rect.left;
                faceInfo[i].top = rect.top;
                faceInfo[i].right = rect.right;
                faceInfo[i].bottom = rect.bottom;
                faceInfo[i].orient = orient;
            }
        }
        return faceInfo;
    }

    /**
     * 获取人脸特征信息
     *
     * @param hFREngine 驱动引擎
     * @param inputImg  图片文件
     * @param faceInfo  FaceInfo
     * @return 人脸特征信息
     */
    private AFR_FSDK_FACEMODEL extractFRFeature(Pointer hFREngine, ASVLOFFSCREEN inputImg, FaceInfo faceInfo) {
        AFR_FSDK_FACEINPUT faceInput = new AFR_FSDK_FACEINPUT();
        faceInput.lOrient = faceInfo.orient;
        faceInput.rcFace.left = faceInfo.left;
        faceInput.rcFace.top = faceInfo.top;
        faceInput.rcFace.right = faceInfo.right;
        faceInput.rcFace.bottom = faceInfo.bottom;

        AFR_FSDK_FACEMODEL faceFeature = new AFR_FSDK_FACEMODEL();
        NativeLong ret = AFR_FSDKLibrary.INSTANCE.AFR_FSDK_ExtractFRFeature(hFREngine, inputImg, faceInput, faceFeature);
        if (ret.longValue() != 0) {
            throw new RuntimeException(String.format("AFR_FSDK_ExtractFRFeature ret 0x%x", ret.longValue()));
        }

        try {
            return faceFeature.deepCopy();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
