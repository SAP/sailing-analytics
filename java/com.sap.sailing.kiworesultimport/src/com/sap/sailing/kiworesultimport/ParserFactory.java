package com.sap.sailing.kiworesultimport;

import com.sap.sailing.kiworesultimport.impl.ParserFactoryImpl;

public interface ParserFactory {
    ParserFactory INSTANCE = new ParserFactoryImpl();
    
    ResultListParser createResultListParser();

    StartberichtParser createStartberichtParser();
    
    ZipFileParser createZipFileParser();
}
