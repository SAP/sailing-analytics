package com.sap.sailing.sailwave.resultimport;

import java.io.InputStream;

import com.sap.sailing.sailwave.resultimport.impl.CsvParserFactoryImpl;
import com.sap.sse.common.TimePoint;

public interface CsvParserFactory {
    CsvParserFactory INSTANCE = new CsvParserFactoryImpl();
    
    /**
     * @param is expected to be encoded with the "cp1252" character set
     */
    CsvParser createParser(InputStream is, String name, TimePoint lastModified);
}
