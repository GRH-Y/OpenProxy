package com.open.proxy.connect.socks5.client;

import com.jav.common.log.LogDog;
import com.jav.common.util.ConfigFileEnvoy;
import com.jav.net.entity.MultiByteBuffer;
import com.jav.net.nio.NioSender;
import com.open.proxy.IConfigKey;
import com.open.proxy.OpContext;
import com.open.proxy.connect.BindClientTask;
import com.open.proxy.connect.joggle.IBindClientListener;
import com.open.proxy.connect.joggle.ISocks5ProcessListener;
import com.open.proxy.connect.socks5.*;
import com.open.proxy.connect.socks5.server.Socks5Server;
import com.open.proxy.intercept.ProxyFilterManager;
import com.open.proxy.protocol.DataPacketTag;
import com.open.proxy.protocol.Socks5Generator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * sock5响应客户（支持用户名和密码校验）
 */
public class Socks5InteractiveClient extends BindClientTask implements ISocks5ProcessListener, IBindClientListener {

    private Socks5TransmissionClient transmissionClient;
    private boolean enableSocks5Proxy;
    private boolean isServerMode;
    private boolean allowProxy;

    public Socks5InteractiveClient(SocketChannel channel) {
        super(channel, null);
        ConfigFileEnvoy configFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
        enableSocks5Proxy = configFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ENABLE_SOCKS5_PROXY);
        isServerMode = configFileEnvoy.getBooleanValue(IConfigKey.CONFIG_IS_SERVER_MODE);
        allowProxy = configFileEnvoy.getBooleanValue(IConfigKey.CONFIG_ALLOW_PROXY);
    }

    @Override
    protected void onBeReadyChannel(SocketChannel channel) {
        if (isServerMode) {
            // 当前是服务模式，响应客户端代理转发请求,数据经过加密（非socks5格式数据）
            Socks5DecryptionReceiver receiver = new Socks5DecryptionReceiver(this);
            receiver.setServerMode();
            DecryptionReceiver decryptionReceiver = receiver.getReceiver();
            decryptionReceiver.setDecodeTag(DataPacketTag.PACK_SOCKS5_HELLO_TAG);
            setReceiver(decryptionReceiver);
            EncryptionSender sender = new EncryptionSender(true);
            sender.setEncodeTag(DataPacketTag.PACK_SOCKS5_DATA_TAG);
            sender.setChannel(getSelectionKey(), channel);
            setSender(sender);
        } else {
            // 当前是客户端模式，响应本地client代理,数据没经过加密(socks5格式数据交互)
            Socks5Receiver receiver = new Socks5Receiver(this);
            setReceiver(receiver.getReceiver());
            try {
                InetSocketAddress address = (InetSocketAddress) channel.getLocalAddress();
                setAddress(address.getAddress().getHostAddress(), address.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
            NioSender sender = new NioSender();
            sender.setChannel(getSelectionKey(), channel);
            setSender(sender);
        }
        LogDog.d("<-x_proxy_socks5-> Socks5LocalClient has com.open.proxy.connect " + " [ com.open.proxy.connect count = " + Socks5Server.localConnectCount.incrementAndGet() + " ] ");
    }

    @Override
    protected void onCloseChannel() {
        if (transmissionClient != null) {
            LogDog.d("<-x_proxy_socks5-> Socks5LocalClient close host =  " + transmissionClient.getRealHost() + ":" + transmissionClient.getRealPort());
        }
        LogDog.d("<-x_proxy_socks5-> Socks5LocalClient has close " + " [ com.open.proxy.connect count = " + Socks5Server.localConnectCount.decrementAndGet() + " ] ");
        Socks5NetFactory.getFactory().getNetTaskComponent().addUnExecTask(transmissionClient);
    }

    @Override
    public Socks5Generator.Socks5Verification onClientSupportMethod(List<Socks5Generator.Socks5Verification> methods) {
        // 响应不需要用户和密码验证
        Socks5Generator.Socks5Verification choiceMethod = methods.get(0);
        getSender().sendData(new MultiByteBuffer(Socks5Generator.buildVerVerificationMethodResponse(choiceMethod)));
        return choiceMethod;
    }

    @Override
    public boolean onVerification(String userName, String password) {
        // 响应通过校验
        LogDog.d("<-x_proxy_socks5-> Socks5LocalClient verification client userName = " + userName + " password = " + password);
        getSender().sendData(new MultiByteBuffer(Socks5Generator.buildVerVerificationResponse()));
        return true;
    }

    @Override
    public void onReportCommandStatus(Socks5Generator.Socks5CommandStatus status) {
        try {
            byte[] response = Socks5Generator.buildCommandResponse(status.rangeStart,
                    Socks5Generator.Socks5AddressType.DOMAIN, getHost(), getPort());
            getSender().sendData(new MultiByteBuffer(response));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBeginProxy(String targetAddress, int targetPort) {
        LogDog.d("<-x_proxy_socks5-> Socks5LocalClient proxy client address = " + targetAddress + " port = " + targetPort);
        boolean isNeedProxy = allowProxy;
        targetPort = targetPort <= 0 ? 80 : targetPort;
        if (enableSocks5Proxy && !isServerMode && !allowProxy) {
            isNeedProxy = ProxyFilterManager.getInstance().isNeedProxy(targetAddress);
        }
        if (enableSocks5Proxy && !isServerMode && isNeedProxy) {
            // 当前开启代理，而且是客户端模式,连接远程代理服务,数据经过加密（非socks5格式数据）
            transmissionClient = new Socks5TransmissionClient(this, targetAddress, targetPort);
            // 获取配置数据,连接代理服务端
            ConfigFileEnvoy configFileEnvoy = OpContext.getInstance().getConfigFileEnvoy();
            String remoteHost = configFileEnvoy.getValue(IConfigKey.CONFIG_REMOTE_SOCKS5_PROXY_HOST);
            int remotePort = configFileEnvoy.getIntValue(IConfigKey.CONFIG_REMOTE_SOCKS5_PROXY_PORT);
            transmissionClient.setAddress(remoteHost, remotePort);
        } else {
            // 不走代理（客户端模式或者服务端模式）,连接真实目标服务,数据不经过加密（非socks5格式数据）
            transmissionClient = new Socks5TransmissionClient(this);
            transmissionClient.setAddress(targetAddress, targetPort);
        }
        transmissionClient.setBindClientListener(this);
        Socks5NetFactory.getFactory().getNetTaskComponent().addUnExecTask(transmissionClient);
    }

    /**
     * 把代理客户端请求的数据中转发送给目标服务（远程代理服务或者真实目标服务）
     *
     * @param buffer
     */
    @Override
    public void onUpstreamData(MultiByteBuffer buffer) {
        // 如果是服务端模式,则把数据发给真实目录服务端
        transmissionClient.getSender().sendData(buffer);
    }

    /**
     * 把远程服务或者真实目标服务数据回传给代理客户端
     *
     * @param buffer
     */
    @Override
    public void onDownStreamData(MultiByteBuffer buffer) {
        getSender().sendData(buffer);
    }

    @Override
    public void onBindClientByReady(String requestId) {

    }

    @Override
    public void onBindClientByError(String requestId) {

    }

    @Override
    public void onBindClientData(String requestId, MultiByteBuffer buffer) {

    }

    @Override
    public void onBindClientClose(String requestId) {
        Socks5NetFactory.getFactory().getNetTaskComponent().addUnExecTask(this);
    }
}
