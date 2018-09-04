package com.example.wxpay.module.util;


import com.github.wxpay.sdk.WXPayConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


public class WXConfigUtil implements WXPayConfig {
    private byte[] certData;
    public static final String APP_ID = "应用id";
    public static final String KEY = "秘钥";
    public static final String MCH_ID = "商户id";

    public WXConfigUtil() throws Exception {
//        String certPath = "证书地址";//从微信商户平台下载的安全证书存放的路径
        String certPath = "C:\\tomcat\\weixin\\apiclient_cert.p12";//从微信商户平台下载的安全证书存放的路径

        File file = new File(certPath);
        InputStream certStream = new FileInputStream(file);
        this.certData = new byte[(int) file.length()];
        certStream.read(this.certData);
        certStream.close();
    }

    @Override
    public String getAppID() {
        return APP_ID;
    }

    //parnerid，商户号
    @Override
    public String getMchID() {
        return MCH_ID;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public InputStream getCertStream() {
        ByteArrayInputStream certBis = new ByteArrayInputStream(this.certData);
        return certBis;
    }

    @Override
    public int getHttpConnectTimeoutMs() {
        return 8000;
    }

    @Override
    public int getHttpReadTimeoutMs() {
        return 10000;
    }
}

