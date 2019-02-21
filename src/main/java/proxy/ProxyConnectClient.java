package proxy;

import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import util.LogDog;

import java.net.InetAddress;

public class ProxyConnectClient extends NioClientTask {

    private NioSender target;

    public ProxyConnectClient(byte[] data, String host, int port, NioSender target) {
        if (data == null || target == null || host == null || port <= 0) {
            throw new NullPointerException("data host port or target is null !!!");
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            host = address.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.target = target;
        setAddress(host, port);
        NioSender sender = new HttpSender(this);
        sender.sendData(data);
        setSender(sender);
        setReceive(new NioReceive(this, "onHttpSubmitCallBack"));
    }

    private void onHttpSubmitCallBack(byte[] data) {
        if (data.length == 0) {
            NioClientFactory.getFactory().removeTask(this);
            return;
        }
        String html = new String(data);
        if (html.length() > 20) {
            LogDog.d("==> onHttpSubmitCallBack = " + html.substring(0, 20));
        } else {
            LogDog.d("==> onHttpSubmitCallBack = " + html);
        }
        target.sendData(data);
    }
}