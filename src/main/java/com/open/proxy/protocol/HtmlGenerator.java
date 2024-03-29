package com.open.proxy.protocol;

/**
 * 生成拒绝访问的html响应
 *
 * @author yyz
 */
public class HtmlGenerator {

    private HtmlGenerator() {
    }

    /**
     * https请求代理成功响应
     *
     * @return
     */
    public static byte[] httpsTunnelEstablished() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 Connection established\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

    /**
     * https请求代理失败响应
     *
     * @return
     */
    public static byte[] headDenyService(String interceptHost) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 503 Denial Service\r\n");
        sb.append("Proxy-agent: YYD-HttpProxy\r\n");
        String context = createInterceptHtml(interceptHost);
        sb.append("Content-Length: ");
        sb.append(context.length());
        sb.append("\r\n\r\n");
        sb.append(context);
        sb.append("\r\n");
        return sb.toString().getBytes();
    }

    /**
     * 响应请求拦截数据
     *
     * @param interceptHost
     * @return
     */
    public static String createInterceptHtml(String interceptHost) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n");
        builder.append("<html>\n");
        builder.append("<head>\n");
        builder.append("<meta charset=\"utf-8\">\n");
        builder.append("<title>Warring</title>\n");
        builder.append("</head>\n");
        builder.append("<body>\n");
        builder.append("    <h1> host: ");
        builder.append(interceptHost);
        builder.append(" in the blacklist");
        builder.append("</h1>");
        builder.append("</body>\n");
        builder.append("</html>\n");
        return builder.toString();
    }
}
