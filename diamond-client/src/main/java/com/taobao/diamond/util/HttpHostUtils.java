package com.taobao.diamond.util;

import org.apache.commons.httpclient.HttpHost;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * HttpHost工具类。
 *
 * @author Daniel Li
 * @since 18 July 2015
 */
public class HttpHostUtils {

    public static HttpHost valueOf(String socket) {
        if (StringUtils.isBlank(socket)) {
            throw new IllegalArgumentException("parameter 'socket' must be ip:port");
        }
        String[] values = socket.split(":");
        return new HttpHost(values[0], Integer.parseInt(values[2]));
    }

    public static Collection<HttpHost> valuesOf(Collection<String> sockets) {
        List<HttpHost> httpHosts = new ArrayList<HttpHost>(sockets.size());
        for (String socket : sockets) {
            httpHosts.add(valueOf(socket));
        }
        return httpHosts;
    }
}
