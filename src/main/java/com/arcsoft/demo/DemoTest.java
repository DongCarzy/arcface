package com.arcsoft.demo;

import com.arcsoft.utils.FileHelper;
import com.arcsoft.utils.ImageFeatureUtils;

import java.io.IOException;

/**
 * @author carzy.
 * @date 14:17 2019/4/23
 */
public class DemoTest {

    public static void main(String[] args) throws IOException {
        String APPID = "e2CaBGmvWgLupK7FMGJL1GP7WdXL5C9KCgaT3pz2s2o";
        String FD_SDKKEY = "9PP4oEazo6FcJwkhhJbfAbGWCyWx3RPyio29vcPXfF25";
        String FR_SDKKEY = "9PP4oEazo6FcJwkhhJbfAbGzraZeXqwjEz7JmGxDQRLp";
        String path = "1.jpg";

        ImageFeatureUtils imageFeatureUtils = new ImageFeatureUtils(APPID, FD_SDKKEY, FR_SDKKEY);
        byte[] bytes = imageFeatureUtils.getFeature(path);
        FileHelper fileHelper = new FileHelper();
        fileHelper.createFile("img1.txt", bytes);
    }
}
