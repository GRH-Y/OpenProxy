package com.open.proxy.server.sync.protocol;


import com.jav.common.cryption.joggle.ICipherComponent;
import com.jav.common.util.StringEnvoy;
import com.jav.net.security.protocol.AbsProxyProtocol;
import com.open.proxy.server.sync.bean.SyncActivityCode;
import com.open.proxy.server.sync.protocol.base.SyncOperateCode;

import java.nio.ByteBuffer;

/**
 * 服务器之间同步数据，10分钟请求一次，实现分布式
 *
 * @author yyz
 */
public class SyncProtocol extends AbsProxyProtocol {


    /**
     * 不包含length的4个字节的长度
     */
    private static final int HEAD_LENGTH = 47;

    /**
     * 机器码（32Byte）
     */
    private byte[] mMachineId;

    /**
     * loadAvg 系统负载值，根据cpu占用率 < 内存占用率 < 宽带占用率 < 网络延迟耗时计算出
     * 数值范围：0-127 数值越小表示系统压力小
     */
    private byte mLoadAvg;

    /**
     * 中转服务的端口号
     */
    private int mPort;


    public SyncProtocol(String machineId, int port, byte loadAvg) {
        if (StringEnvoy.isEmpty(machineId)) {
            throw new IllegalArgumentException("machineId id can not be null !!!");
        }
        this.mMachineId = machineId.getBytes();
        mLoadAvg = loadAvg;
        mPort = port;
    }

    public SyncProtocol(String machineId) {
        if (StringEnvoy.isEmpty(machineId)) {
            throw new IllegalArgumentException("machineId id can not be null !!!");
        }
        this.mMachineId = machineId.getBytes();
    }

    @Override
    public byte activityCode() {
        return SyncActivityCode.SYNC.getCode();
    }

    @Override
    public ByteBuffer toData(ICipherComponent encryptComponent) {
        int length = HEAD_LENGTH;

        Byte oCode = operateCode();
        byte[] sendData = sendData();

        if (oCode == SyncOperateCode.SYNC_MID.getCode() || oCode == SyncOperateCode.RESPOND_SYNC_MID.getCode()) {
            length += sendData.length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putLong(time());
        buffer.put(activityCode());
        buffer.put(mMachineId);
        buffer.put(operateCode());

        if (length == HEAD_LENGTH) {
            buffer.putInt(mPort);
            buffer.put(mLoadAvg);
        } else {
            buffer.put(sendData);
        }

        byte[] rawData = buffer.array();
        byte[] encodeData = encryptComponent.onEncrypt(rawData);
        return ByteBuffer.wrap(encodeData);
    }
}
