package com.sap.sailing.domain.queclinkadapter.impl;

import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.queclinkadapter.ByteStreamToMessageStreamConverter;
import com.sap.sailing.domain.queclinkadapter.Message;
import com.sap.sailing.domain.queclinkadapter.MessageParser;

public class ByteStreamToMessageStreamConverterImpl implements ByteStreamToMessageStreamConverter {
    private final static String TERMINATOR = ""+MessageParserImpl.TERMINATION_CHARACTER;
    private final static MessageParser messageParser = MessageParser.create();
    private final StringBuilder buffer;
    
    public ByteStreamToMessageStreamConverterImpl() {
        buffer = new StringBuilder();
    }
    
    @Override
    public Iterable<Message> convert(CharBuffer buf) throws ParseException {
        final int size = buf.limit();
        final char[] readFromBuffer = new char[size];
        buf.get(readFromBuffer);
        buffer.append(readFromBuffer);
        final List<Message> messages = new ArrayList<>();
        int previousTerminator = -1;
        int nextTerminator;
        while ((nextTerminator=buffer.indexOf(TERMINATOR, previousTerminator+1)) != -1) {
            messages.add(messageParser.parse(buffer.substring(previousTerminator+1, nextTerminator+1)));
            previousTerminator = nextTerminator;
        }
        if (previousTerminator != -1) {
            buffer.delete(0, previousTerminator+1);
        }
        return messages;
    }

}
