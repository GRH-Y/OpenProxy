package connect.socks5.client;

import connect.AbsClient;
import connect.DecryptionReceiver;
import connect.EncryptionSender;
import connect.joggle.ISocks5ProcessListener;
import connect.network.base.joggle.INetReceiver;
import connect.network.nio.NioReceiver;
import connect.network.nio.NioSender;
import connect.network.xhttp.utils.MultiLevelBuf;
import connect.socks5.Socks5DecryptionReceiver;
import protocol.DataPacketTag;
import protocol.HtmlGenerator;

import java.nio.channels.SocketChannel;

/**
 * 连接代理服务端,作用于中转数据
 */
public class Socks5TransmissionClient extends AbsClient implements INetReceiver<MultiLevelBuf> {

    private ISocks5ProcessListener mListener;
    private String mRealHost;
    private int mRealPort;

    /**
     * 不走代理（客户端模式或者服务端模式）,连接真实目标服务,数据不经过加密（非socks5格式数据）
     *
     * @param listener
     */
    public Socks5TransmissionClient(ISocks5ProcessListener listener) {
        this.mListener = listener;
        init();
    }

    /**
     * 当前开启代理，而且是客户端模式,连接远程代理服务,数据经过加密（非socks5格式数据）
     *
     * @param listener
     * @param realHost
     * @param realPort
     */
    public Socks5TransmissionClient(ISocks5ProcessListener listener, String realHost, int realPort) {
        this.mListener = listener;
        this.mRealHost = realHost;
        this.mRealPort = realPort;
        initCryption();
        sendRealTargetInfo(realHost, realPort);
//        enableProxy();
    }

    private void enableProxy() {
        byte[] data = HtmlGenerator.httpsTunnelEstablished();
        mListener.onDownStreamData(data);
    }

    public String getRealHost() {
        return mRealHost == null ? getHost() : mRealHost;
    }

    public int getRealPort() {
        return mRealPort <= 0  ? getPort() : mRealPort;
    }

    private void init() {
        NioReceiver receiver = new NioReceiver();
        receiver.setDataReceiver(this);
        setReceive(receiver);
        NioSender sender = new NioSender();
        sender.setSenderFeedback(this);
        sender.enablePrestore();
        setSender(sender);
    }

    private void sendRealTargetInfo(String realHost, int realPort) {
        byte[] targetInfo = createTargetInfoProtocol(realHost, realPort);
        EncryptionSender sender = getSender();
        sender.sendData(targetInfo);
        //发送完hello数据,切换tag(PACK_SOCKS5_DATA_TAG)用于中转数据
        sender.setEncodeTag(DataPacketTag.PACK_SOCKS5_DATA_TAG);
    }

    private void initCryption() {
        Socks5DecryptionReceiver receiver = new Socks5DecryptionReceiver(mListener);
        receiver.setForwardModel();
        DecryptionReceiver decryptionReceiver = receiver.getReceiver();
        decryptionReceiver.setDecodeTag(DataPacketTag.PACK_SOCKS5_DATA_TAG);
        setReceive(decryptionReceiver);
        EncryptionSender sender = new EncryptionSender(true);
        sender.setSenderFeedback(this);
        sender.enablePrestore();
        //当前是远程服务端使用,第一个hello数据是 PACK_SOCKS5_HELLO_TAG 类型
        sender.setEncodeTag(DataPacketTag.PACK_SOCKS5_HELLO_TAG);
        setSender(sender);
    }

    private byte[] createTargetInfoProtocol(String host, int port) {
        int length = host.length();
        byte[] head = new byte[3 + length];
        int index = 0;
        head[index] = (byte) length;
        index++;
        byte[] hostByte = host.getBytes();
        System.arraycopy(hostByte, 0, head, 1, length);
        index += length;

        head[index] = (byte) (((port & 0XFF00) >> 8));
        head[index + 1] = (byte) (port & 0XFF);
        return head;
    }


    @Override
    protected void onConnectCompleteChannel(SocketChannel channel) {
        getSender().setChannel(getSelectionKey(), channel);
        getSender().disablePrestore();
    }


    @Override
    public void onReceiveFullData(MultiLevelBuf multiLevelBuf, Throwable throwable) {
        mListener.onDownStreamData(multiLevelBuf);
    }
}
