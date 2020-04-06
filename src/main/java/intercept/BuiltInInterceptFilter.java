package intercept;

import intercept.joggle.IInterceptFilter;
import log.LogDog;
import storage.FileHelper;
import util.StringEnvoy;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BuiltInInterceptFilter implements IInterceptFilter {
    private List<String> blackList;
    private List<String> whiteList;

    private List<String> a;
    private List<String> b;
    private List<String> c;
    private List<String> d;
    private List<String> e;
    private List<String> f;
    private List<String> g;
    private List<String> h;
    private List<String> i;
    private List<String> j;
    private List<String> k;
    private List<String> l;
    private List<String> m;
    private List<String> n;
    private List<String> o;
    private List<String> p;
    private List<String> q;
    private List<String> r;
    private List<String> s;
    private List<String> t;
    private List<String> u;
    private List<String> v;
    private List<String> w;
    private List<String> x;
    private List<String> y;
    private List<String> z;

    private List<String> zero;
    private List<String> one;
    private List<String> two;
    private List<String> three;
    private List<String> four;
    private List<String> five;
    private List<String> six;
    private List<String> seven;
    private List<String> eight;
    private List<String> nine;

    public BuiltInInterceptFilter() {
        blackList = new ArrayList<>();
        whiteList = new ArrayList<>();
    }

    public void init(byte[] data) {
        if (data == null) {
            LogDog.e("read ip table file error or file is empty!!! ");
            return;
        }
        String content = new String(data);
        initImpl(content);
    }

    public void init(String ipTablePath) {
        byte[] data = FileHelper.readFileMemMap(ipTablePath);
        if (data == null) {
            LogDog.e("read ip table file error or file is empty !!! path = " + ipTablePath);
            return;
        }
        String content = new String(data);
        initImpl(content);
    }

    private void initImpl(String content) {
        Properties props = System.getProperties();
        String os = props.getProperty("os.name").toLowerCase();
        String[] array;
        if (os.contains("windows")) {
            array = content.split("\r\n");
        } else {
            array = content.split("\n");
        }

        for (String item : array) {
            if (!item.startsWith("//") && !item.startsWith("##") && !item.startsWith("#")) {
                String[] itemArray = item.split("!");
                //添加黑名单
                blackList.add(itemArray[0]);
                for (int index = 1; index < itemArray.length; index++) {
                    //添加白名单
                    whiteList.add(itemArray[index]);
                }
            }
        }
        LogDog.d("Load filter ingress configuration , Number of blacklists = " + blackList.size() + " Number of whitelists = " + whiteList.size());
    }

    public boolean isIntercept(String host) {
        if (StringEnvoy.isNotEmpty(host)) {
            for (String tmp : blackList) {
                if (host.contains(tmp)) {
                    for (String white : whiteList) {
                        if (host.contains(white)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}