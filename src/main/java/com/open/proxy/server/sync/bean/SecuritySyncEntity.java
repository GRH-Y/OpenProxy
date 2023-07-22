package com.open.proxy.server.sync.bean;

/**
 * 同步服务负载数据
 *
 * @author yyz
 */
public class SecuritySyncEntity {

    /**
     * 目标代理服务的地址
     */
    private String mHost;

    /**
     * 目标代理服务的端口
     */
    private int mPort;

    /**
     * 目标机器的机器id
     */
    private String mMachineId;

    /**
     * 链接数量
     */
    private long mConnectCount;

    /**
     * 负载压力值
     */
    private byte avgLoad;

    public SecuritySyncEntity(String machineId, String proxyHost) {
        mMachineId = machineId;
        mHost = proxyHost;
    }

    public void updateConnectCount(long count) {
        mConnectCount = count;
    }

    public void updateAvgLoad(byte avg) {
        avgLoad = avg;
    }

    public String getProxyHost() {
        return mHost;
    }

    public void setProxyPort(int port) {
        mPort = port;
    }

    public int getProxyPort() {
        return mPort;
    }

    public String getMachineId() {
        return mMachineId;
    }

    public long getLoadCount() {
        return mConnectCount;
    }

    public byte getAvgLoad() {
        return avgLoad;
    }
}
