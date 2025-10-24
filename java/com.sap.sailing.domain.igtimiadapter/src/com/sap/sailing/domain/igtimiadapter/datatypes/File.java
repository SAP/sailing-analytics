package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.TimePoint;

public class File extends Fix {
    private static final long serialVersionUID = 7580174784400926003L;
    private final TimePoint start;
    private final TimePoint end;
    private final String fileName;
    private final String md5;
    private final String contentType;
    private final long size;
    
    public File(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        super(sensor, timePoint);
        start = timePoint;
        end = timePoint; // a bit strange with start/end for a file... in the protobuf File message, endTimePoint is field #2...
        fileName = ((String) valuesPerSubindex.get(1));
        md5 = ((String) valuesPerSubindex.get(2));
        contentType = ((String) valuesPerSubindex.get(3));
        size = ((Number) valuesPerSubindex.get(4)).longValue();
    }

    public TimePoint getStart() {
        return start;
    }

    public TimePoint getEnd() {
        return end;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMd5() {
        return md5;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    @Override
    protected String localToString() {
        return "File: "+getFileName()+", MD5: "+getMd5()+" content type "+getContentType()+", "+getSize()+" bytes";
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
