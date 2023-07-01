package com.open.proxy.connect;


import com.jav.net.nio.NioClientTask;
import com.jav.net.ssl.TLSHandler;
import com.open.proxy.connect.joggle.IBindClientListener;

import java.nio.channels.SocketChannel;

/**
 * 可绑定的client
 *
 * @author yyz
 */
public class BindClientTask extends NioClientTask {

    protected IBindClientListener mBindListener;

    public BindClientTask() {
    }

    public BindClientTask(SocketChannel channel, TLSHandler tlsHandler) {
        super(channel, tlsHandler);
    }

    public void setBindClientListener(IBindClientListener listener) {
        this.mBindListener = listener;
    }


    @Override
    protected void onRecovery() {
        super.onRecovery();
        mBindListener = null;
    }
}
