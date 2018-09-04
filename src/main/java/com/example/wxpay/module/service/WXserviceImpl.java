package com.example.wxpay.module.service;

import com.example.wxpay.module.util.WXConfigUtil;
import com.example.wxpay.module.util.WxMD5Util;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WXserviceImpl implements WXservice {
    private static final Logger logger = LoggerFactory.getLogger("MainLogger");
    public static final String SPBILL_CREATE_IP = "120.79.246.227";
    public static final String NOTIFY_URL = "http://www.hingin.vip/v1/weixin/notify.json";
    public static final String TRADE_TYPE_APP = "APP";


    /**
     * 调用官方SDK 获取预支付订单等参数
     * @param attach 额外参数
     * @param total_fee 总价
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, String> dounifiedOrder(String attach, String total_fee) throws Exception {
        WxMD5Util md5Util = new WxMD5Util();
        Map<String, String> returnMap = new HashMap<>();
        WXConfigUtil config = new WXConfigUtil();
        WXPay wxpay = new WXPay(config);
        Map<String, String> data = new HashMap<>();
        //生成商户订单号，不可重复
        String out_trade_no = "wxpay" + System.currentTimeMillis();

        data.put("appid", config.getAppID());
        data.put("mch_id", config.getMchID());
        data.put("nonce_str", WXPayUtil.generateNonceStr());
        String body = "订单支付";
        data.put("body", body);
        data.put("out_trade_no", out_trade_no);
        data.put("total_fee", total_fee);
        //自己的服务器IP地址
        data.put("spbill_create_ip", SPBILL_CREATE_IP);
        //异步通知地址（请注意必须是外网）
        data.put("notify_url", NOTIFY_URL);
        //交易类型
        data.put("trade_type", TRADE_TYPE_APP);
        //附加数据，在查询API和支付通知中原样返回，该字段主要用于商户携带订单的自定义数据
        data.put("attach", attach);
        String sign1 = md5Util.getSign(data);
        data.put("sign", sign1);

        try {
            //使用官方API请求预付订单
            Map<String, String> response = wxpay.unifiedOrder(data);
            System.out.println(response);
            String returnCode = response.get("return_code");    //获取返回码
            //若返回码为SUCCESS，则会返回一个result_code,再对该result_code进行判断
            if (returnCode.equals("SUCCESS")) {//主要返回以下5个参数
                String resultCode = response.get("result_code");
                returnMap.put("appid", response.get("appid"));
                returnMap.put("mch_id", response.get("mch_id"));
                returnMap.put("nonce_str", response.get("nonce_str"));
                returnMap.put("sign", response.get("sign"));
                if ("SUCCESS".equals(resultCode)) {//resultCode 为SUCCESS，才会返回prepay_id和trade_type
                    //获取预支付交易回话标志
                    returnMap.put("trade_type", response.get("trade_type"));
                    returnMap.put("prepay_id", response.get("prepay_id"));
                    return returnMap;
                } else {
                    //此时返回没有预付订单的数据
                    return returnMap;
                }
            } else {
                return returnMap;
            }
        } catch (Exception e) {
            System.out.println(e);
            //系统等其他错误的时候
        }
        return returnMap;
    }

    /**
     *
     * @param notifyData 异步通知后的XML数据
     * @return
     */
    @Override
    public String payBack(String notifyData) {
        WXConfigUtil config = null;
        try {
            config = new WXConfigUtil();
        } catch (Exception e) {
            e.printStackTrace();
        }
        WXPay wxpay = new WXPay(config);
        String xmlBack = "";
        Map<String, String> notifyMap = null;
        try {
            notifyMap = WXPayUtil.xmlToMap(notifyData);         // 调用官方SDK转换成map类型数据
            if (wxpay.isPayResultNotifySignatureValid(notifyMap)) {//验证签名是否有效，有效则进一步处理

                String return_code = notifyMap.get("return_code");//状态
                String out_trade_no = notifyMap.get("out_trade_no");//商户订单号
                if (return_code.equals("SUCCESS")) {
                    if (out_trade_no != null) {
                        // 注意特殊情况：订单已经退款，但收到了支付结果成功的通知，不应把商户的订单状态从退款改成支付成功
                        // 注意特殊情况：微信服务端同样的通知可能会多次发送给商户系统，所以数据持久化之前需要检查是否已经处理过了，处理了直接返回成功标志
                        //业务数据持久化

                        System.err.println("支付成功");

                        logger.info("微信手机支付回调成功订单号:{}", out_trade_no);
                        xmlBack = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>" + "<return_msg><![CDATA[OK]]></return_msg>" + "</xml> ";
                    } else {
                        logger.info("微信手机支付回调失败订单号:{}", out_trade_no);
                        xmlBack = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>" + "<return_msg><![CDATA[报文为空]]></return_msg>" + "</xml> ";
                    }
                }
                return xmlBack;
            } else {
                // 签名错误，如果数据里没有sign字段，也认为是签名错误
                //失败的数据要不要存储？
                logger.error("手机支付回调通知签名错误");
                xmlBack = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>" + "<return_msg><![CDATA[报文为空]]></return_msg>" + "</xml> ";
                return xmlBack;
            }
        } catch (Exception e) {
            logger.error("手机支付回调通知失败", e);
            xmlBack = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>" + "<return_msg><![CDATA[报文为空]]></return_msg>" + "</xml> ";
        }
        return xmlBack;
    }

}
