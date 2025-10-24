package com.sap.sse.common.http;

import com.sap.sse.common.Util;

public interface HttpHeaderUtil {
    static boolean isValidOriginHeaderValue(String value) {
        return Util.hasLength(value) && value.matches("[^ *+<>;\"]*$");
    }
}
