package connect.socks5;

import connect.DecryptionReceiver;
import connect.joggle.IDecryptionDataListener;
import connect.joggle.ISocks5ProcessListener;
import connect.joggle.Socks5ProcessStatus;
import protocol.DataPacketTag;

import java.security.InvalidParameterException;

/**
 * socks5加密接收者
 */
public class Socks5DecryptionReceiver implements IDecryptionDataListener {

    private ISocks5ProcessListener mListener;
    private DecryptionReceiver mDecryptionReceiver;

    private Socks5ProcessStatus status = Socks5ProcessStatus.HELLO;
    private boolean serverMode = false;

    public Socks5DecryptionReceiver(ISocks5ProcessListener listener) {
        if (listener == null) {
            throw new InvalidParameterException("listener is null !!!");
        }
        this.mListener = listener;
        mDecryptionReceiver = new DecryptionReceiver(true);
        mDecryptionReceiver.setOnDecryptionDataListener(this);
    }

    public DecryptionReceiver getReceiver() {
        return mDecryptionReceiver;
    }

    public void setForwardModel() {
        status = Socks5ProcessStatus.FORWARD;
    }

    public void setServerMode() {
        this.serverMode = true;
    }

    @Override
    public void onDecryption(byte[] decrypt) {
        if (status == Socks5ProcessStatus.HELLO) {
            int index = 0;
            int hostLength = decrypt[index];
            index++;
            byte[] hostByte = new byte[hostLength];
            System.arraycopy(decrypt, index, hostByte, 0, hostLength);
            index = hostLength + 1;
            int targetPort = ((decrypt[index] & 0XFF) << 8) | (decrypt[index + 1] & 0XFF);
//            int targetPort = ((decrypt[index + 1] & 0XFF) << 8) | (decrypt[index] & 0XFF);
            //回调开启连接真实目标服务
            mListener.onBeginProxy(new String(hostByte), targetPort);
            //切换数据tag
            mDecryptionReceiver.setDecodeTag(DataPacketTag.PACK_SOCKS5_DATA_TAG);
            //改变状态为中转状态
            status = Socks5ProcessStatus.FORWARD;
        } else if (status == Socks5ProcessStatus.FORWARD) {
            //中转状态直接回调数据
            if (serverMode) {
                mListener.onUpstreamData(decrypt);
            } else {
                mListener.onDownStreamData(decrypt);
            }
        }
    }
}
