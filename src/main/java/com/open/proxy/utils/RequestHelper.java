package com.open.proxy.utils;


import com.jav.net.base.RequestMode;

public class RequestHelper {

    private static final int REQUEST_LENGTH = 7;

    private RequestHelper() {
    }

    /**
     * 判断数据是否是http协议head数据
     *
     * @param data
     * @return
     */
    public static boolean isRequest(byte[] data) {
        if (data == null || data.length < REQUEST_LENGTH) {
            return false;
        }
        String method = new String(data, 0, REQUEST_LENGTH);
        for (RequestMode mode : RequestMode.values()) {
            if (method.contains(mode.getMode())) {
                return true;
            }
        }
        return false;
    }
}
