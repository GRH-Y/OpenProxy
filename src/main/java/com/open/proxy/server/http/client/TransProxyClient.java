package com.open.proxy.server.http.client;


import com.jav.common.log.LogDog;
import com.jav.common.state.joggle.IControlStateMachine;
import com.jav.common.track.SpiderEnvoy;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.net.base.MultiBuffer;
import com.jav.net.base.SocketChannelCloseException;
import com.jav.net.base.joggle.INetReceiver;
import com.jav.net.base.joggle.NetErrorType;
import com.jav.net.nio.NioReceiver;
import com.jav.net.nio.NioSender;
import com.open.proxy.IConfigKey;
import com.open.proxy.OpContext;
import com.open.proxy.server.BindClientTask;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 代理转发客户http请求
 *
 * @author yyz
 */
public class TransProxyClient extends BindClientTask implements INetReceiver<MultiBuffer> {

    private String mRequestId;

    /**
     * 本地代理
     *
     * @param realHost
     * @param port
     */
    public TransProxyClient(String realHost, int port) {
        init(realHost, port);
    }

    /**
     * 远程代理
     *
     * @param requestId
     * @param realHost
     * @param port
     */
    public TransProxyClient(String requestId, String realHost, int port) {
        this.mRequestId = requestId;
        init(realHost, port);
    }

    private void init(String realHost, int port) {
        setAddress(realHost, port);
        ConfigFileEnvoy cFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        boolean isDebug = cFileEnvoy.getBooleanValue(IConfigKey.KEY_DEBUG_MODE);
        SpiderEnvoy.getInstance().setEnablePrint(isDebug);
    }

    @Override
    protected IControlStateMachine<Integer> getStatusMachine() {
        return super.getStatusMachine();
    }

    @Override
    protected void onBeReadyChannel(SelectionKey selectionKey, SocketChannel channel) {
        NioSender sender = new NioSender();
        sender.setChannel(selectionKey, channel);
        setSender(sender);

        NioReceiver receiver = new NioReceiver();
        receiver.setDataReceiver(this);
        setReceiver(receiver);

        if (mBindListener != null) {
            mBindListener.onBindClientByReady(mRequestId);
        }

        SpiderEnvoy.getInstance().pinKeyProbe(TransProxyClient.this.toString(), "onBeReadyChannel");

    }

    @Override
    public void onReceiveFullData(MultiBuffer buf) {
        if (mBindListener != null) {
            mBindListener.onBindClientData(mRequestId, buf);
        }
    }


    @Override
    public void onReceiveError(Throwable throwable) {
    }

    @Override
    protected void onErrorChannel(NetErrorType errorType, Throwable throwable) {
        if (errorType == NetErrorType.CONNECT) {
            if (mBindListener != null) {
                mBindListener.onBindClientByError(mRequestId);
            }
        }
        if (!(throwable instanceof SocketChannelCloseException)) {
            throwable.printStackTrace();
        }
        SpiderEnvoy.getInstance().pinKeyProbe(TransProxyClient.this.toString(), "onConnectError host = " + getHost() + ":" + getPort());
    }


    @Override
    protected void onCloseChannel() {
        if (mBindListener != null) {
            mBindListener.onBindClientClose(mRequestId);
        }
        LogDog.e("==> TransProxyClient close,  host = " + getHost() + ":" + getPort() + " this = " + this);
        String report = SpiderEnvoy.getInstance().endWatchKey(TransProxyClient.this.toString());
        LogDog.saveLog(report);
    }

}
