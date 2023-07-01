package com.open.proxy.connect.joggle;

/**
 * 解密数据回调接口
 *
 * @author yyz
 */
public interface IDecryptionDataListener {

    /**
     * 解密数据
     *
     * @param decrypt
     */
    void onDecryption(byte[] decrypt);
}
