package com.sap.sailing.racecommittee.app.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.sap.sse.common.Util;

import android.net.Uri;
import androidx.annotation.Nullable;

public class UrlHelper {
    public static URL generateUrl(String baseURL, String path, List<Util.Pair<String, Object>> params)
            throws MalformedURLException {
        Uri.Builder builder = Uri.parse(baseURL).buildUpon();
        builder.path(path);
        if (params != null) {
            for (Util.Pair<String, Object> param : params) {
                builder.appendQueryParameter("" + param.getA(), "" + param.getB());
            }
        }
        return new URL(builder.build().toString());
    }

    public static String getServerUrl(URL url) {
        String protocol = url.getProtocol();
        String host = url.getHost();
        String port = url.getPort() == -1 ? "" : ":" + url.getPort();
        return protocol + "://" + host + port;
    }

    public static @Nullable URL tryConvertToURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
