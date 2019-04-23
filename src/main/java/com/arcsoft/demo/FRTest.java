package com.arcsoft.demo;

import com.arcsoft.*;
import com.arcsoft.utils.BufferInfo;
import com.arcsoft.utils.FileHelper;
import com.arcsoft.utils.ImageLoader;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * @author carzy
 */
@SuppressWarnings("Duplicates")
public class FRTest {

    public static final String APPID = "e2CaBGmvWgLupK7FMGJL1GP7WdXL5C9KCgaT3pz2s2o";
    public static final String FD_SDKKEY = "9PP4oEazo6FcJwkhhJbfAbGWCyWx3RPyio29vcPXfF25";
    public static final String FR_SDKKEY = "9PP4oEazo6FcJwkhhJbfAbGzraZeXqwjEz7JmGxDQRLp";

    public static final int FD_WORKBUF_SIZE = 20 * 1024 * 1024;
    public static final int FR_WORKBUF_SIZE = 40 * 1024 * 1024;
    public static final int MAX_FACE_NUM = 50;

    public static final boolean bUseRAWFile = false;
    public static final boolean bUseBGRToEngine = true;

    public static void main(String[] args) throws Exception {

        System.out.println("开始");

        // init Engine
        Pointer pFDWorkMem = CLibrary.INSTANCE.malloc(FD_WORKBUF_SIZE);
        Pointer pFRWorkMem = CLibrary.INSTANCE.malloc(FR_WORKBUF_SIZE);

        // FD
        PointerByReference phFDEngine = new PointerByReference();
        AFD_FSDKLibrary.INSTANCE.AFD_FSDK_InitialFaceEngine(APPID, FD_SDKKEY, pFDWorkMem, FD_WORKBUF_SIZE, phFDEngine, _AFD_FSDK_OrientPriority.AFD_FSDK_OPF_0_HIGHER_EXT, 32, MAX_FACE_NUM);
        // print FDEngine version
        Pointer hFDEngine = phFDEngine.getValue();
        AFD_FSDK_Version versionFD = AFD_FSDKLibrary.INSTANCE.AFD_FSDK_GetVersion(hFDEngine);
        System.out.println(String.format("%d %d %d %d", versionFD.lCodebase, versionFD.lMajor, versionFD.lMinor, versionFD.lBuild));
        System.out.println("versionFD => Version:" + versionFD.Version + ",BuildDate:" + versionFD.BuildDate + ",CopyRight:" + versionFD.CopyRight);

        // FR
        PointerByReference phFREngine = new PointerByReference();
        AFR_FSDKLibrary.INSTANCE.AFR_FSDK_InitialEngine(APPID, FR_SDKKEY, pFRWorkMem, FR_WORKBUF_SIZE, phFREngine);
        // print FREngine version
        Pointer hFREngine = phFREngine.getValue();
        AFR_FSDK_Version versionFR = AFR_FSDKLibrary.INSTANCE.AFR_FSDK_GetVersion(hFREngine);
        System.out.println(String.format("%d %d %d %d", versionFR.lCodebase, versionFR.lMajor, versionFR.lMinor, versionFR.lBuild));
        System.out.println("versionFR => Version:" + versionFR.Version + ",BuildDate:" + versionFR.BuildDate + ",CopyRight:" + versionFR.CopyRight);


        File file = new File("imgs");
        File[] files = file.listFiles();
        for (File file1 : files) {
            if (file1.isFile() && file1.getName().endsWith("jpg")) {
                printfacePbFeature(file1, hFDEngine, hFREngine);
            }
        }

        // 卸载
        AFD_FSDKLibrary.INSTANCE.AFD_FSDK_UninitialFaceEngine(hFDEngine);
        AFR_FSDKLibrary.INSTANCE.AFR_FSDK_UninitialEngine(hFREngine);
        CLibrary.INSTANCE.free(pFDWorkMem);
        CLibrary.INSTANCE.free(pFRWorkMem);
        System.out.println("结束");
    }

    private static void printfacePbFeature(File file, Pointer hFDEngine, Pointer hFREngine) {
        try {
            ASVLOFFSCREEN img = loadImage(file.getPath());
            FaceInfo[] faceInfos = doFaceDetection(hFDEngine, img);
            if (faceInfos.length < 1) {
                System.err.println("no face in Image");
            }
            AFR_FSDK_FACEMODEL faceFeature = extractFRFeature(hFREngine, img, faceInfos[0]);
            FileHelper fileHelper = new FileHelper();
            if (faceFeature != null) {
                fileHelper.createFile(file.getName() + ".data", faceFeature.toByteArray());
                faceFeature.freeUnmanaged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static FaceInfo[] doFaceDetection(Pointer hFDEngine, ASVLOFFSCREEN inputImg) {
        FaceInfo[] faceInfo = new FaceInfo[0];

        PointerByReference ppFaceRes = new PointerByReference();
        NativeLong ret = AFD_FSDKLibrary.INSTANCE.AFD_FSDK_StillImageFaceDetection(hFDEngine, inputImg, ppFaceRes);
        if (ret.longValue() != 0) {
            System.out.println(String.format("AFD_FSDK_StillImageFaceDetection ret 0x%x", ret.longValue()));
            return faceInfo;
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

                System.out.println(String.format("%d (%d %d %d %d) orient %d", i, rect.left, rect.top, rect.right, rect.bottom, orient));
            }
        }
        return faceInfo;
    }

    public static AFR_FSDK_FACEMODEL extractFRFeature(Pointer hFREngine, ASVLOFFSCREEN inputImg, FaceInfo faceInfo) {

        AFR_FSDK_FACEINPUT faceinput = new AFR_FSDK_FACEINPUT();
        faceinput.lOrient = faceInfo.orient;
        faceinput.rcFace.left = faceInfo.left;
        faceinput.rcFace.top = faceInfo.top;
        faceinput.rcFace.right = faceInfo.right;
        faceinput.rcFace.bottom = faceInfo.bottom;

        AFR_FSDK_FACEMODEL faceFeature = new AFR_FSDK_FACEMODEL();
        NativeLong ret = AFR_FSDKLibrary.INSTANCE.AFR_FSDK_ExtractFRFeature(hFREngine, inputImg, faceinput, faceFeature);
        if (ret.longValue() != 0) {
            System.out.println(String.format("AFR_FSDK_ExtractFRFeature ret 0x%x", ret.longValue()));
            return null;
        }

        try {
            return faceFeature.deepCopy();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static float compareFaceSimilarity(Pointer hFDEngine, Pointer hFREngine, ASVLOFFSCREEN inputImgA, ASVLOFFSCREEN inputImgB) {
        // Do Face Detect
        FaceInfo[] faceInfosA = doFaceDetection(hFDEngine, inputImgA);
        if (faceInfosA.length < 1) {
            System.out.println("no face in Image A ");
            return 0.0f;
        }

        FaceInfo[] faceInfosB = doFaceDetection(hFDEngine, inputImgB);
        if (faceInfosB.length < 1) {
            System.out.println("no face in Image B ");
            return 0.0f;
        }

        // Extract Face Feature
        AFR_FSDK_FACEMODEL faceFeatureA = extractFRFeature(hFREngine, inputImgA, faceInfosA[0]);
        if (faceFeatureA == null) {
            System.out.println("extract face feature in Image A failed");
            return 0.0f;
        }
        System.out.println("提取到图片1的脸部特征:" + Arrays.toString(faceFeatureA.pbFeature.getPointer().getByteArray(0, faceFeatureA.lFeatureSize)));
        try {
            FileHelper fileHelper = new FileHelper();
            fileHelper.createFile("img1.data", faceFeatureA.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("提取到图片1的特征信息长度:" + faceFeatureA.lFeatureSize);

        AFR_FSDK_FACEMODEL faceFeatureB = extractFRFeature(hFREngine, inputImgB, faceInfosB[0]);
        if (faceFeatureB == null) {
            System.out.println("extract face feature in Image B failed");
            faceFeatureA.freeUnmanaged();
            return 0.0f;
        }
        System.out.println("提取到图片2的脸部特征:" + faceFeatureB.pbFeature);
        System.out.println("提取到图片2的特征信息长度:" + faceFeatureB.lFeatureSize);

        // calc similarity between faceA and faceB
        FloatByReference fSimilScore = new FloatByReference(0.0f);
        NativeLong ret = AFR_FSDKLibrary.INSTANCE.AFR_FSDK_FacePairMatching(hFREngine, faceFeatureA, faceFeatureB, fSimilScore);
        faceFeatureA.freeUnmanaged();
        faceFeatureB.freeUnmanaged();
        if (ret.longValue() != 0) {
            System.out.println(String.format("AFR_FSDK_FacePairMatching failed:ret 0x%x", ret.longValue()));
            return 0.0f;
        }

        try {
            faceFeatureA = extractFRFeature(hFREngine, inputImgA, faceInfosA[0]);
            File fileC = new File("z.txt");
            byte[] c = Files.readAllBytes(fileC.toPath());
            AFR_FSDK_FACEMODEL faceFeatureC = AFR_FSDK_FACEMODEL.fromByteArray(c);
            FloatByReference fSimilScorec = new FloatByReference(0.0f);
            NativeLong ret1 = AFR_FSDKLibrary.INSTANCE.AFR_FSDK_FacePairMatching(hFREngine, faceFeatureA, faceFeatureC, fSimilScorec);
            faceFeatureA.freeUnmanaged();
            faceFeatureC.freeUnmanaged();
            System.out.println("A与C的相似度:" + fSimilScorec.getValue());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fSimilScore.getValue();
    }

    public static ASVLOFFSCREEN loadImage(String filePath) {
        ASVLOFFSCREEN inputImg = new ASVLOFFSCREEN();

        if (bUseBGRToEngine) {
            BufferInfo bufferInfo = ImageLoader.getBGRFromFile(filePath);
            inputImg.u32PixelArrayFormat = ASVL_COLOR_FORMAT.ASVL_PAF_RGB24_B8G8R8;
            inputImg.i32Width = bufferInfo.width;
            inputImg.i32Height = bufferInfo.height;
            inputImg.pi32Pitch[0] = inputImg.i32Width * 3;
            inputImg.ppu8Plane[0] = new Memory(inputImg.pi32Pitch[0] * inputImg.i32Height);
            inputImg.ppu8Plane[0].write(0, bufferInfo.buffer, 0, inputImg.pi32Pitch[0] * inputImg.i32Height);
            inputImg.ppu8Plane[1] = Pointer.NULL;
            inputImg.ppu8Plane[2] = Pointer.NULL;
            inputImg.ppu8Plane[3] = Pointer.NULL;
        } else {
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
        }

        inputImg.setAutoRead(false);
        return inputImg;
    }

}
