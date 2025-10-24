package com.sap.sailing.domain.queclinkadapter;

import java.nio.CharBuffer;
import java.text.ParseException;

import com.sap.sailing.domain.queclinkadapter.impl.ByteStreamToMessageStreamConverterImpl;

/**
 * A stateful converter that converts characters passed as a {@link CharBuffer} into {@link Message} objects. All
 * characters are consumed from the buffer. The buffer does not need to end with a complete message. The excess
 * characters are stored in this instance and are prepended in front of the next buffer's contents.<p>
 * 
 * Internally, a {@link StringBuilder} is used to keep track of excess characters not yet consumed because of
 * incomplete messages.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface ByteStreamToMessageStreamConverter {
    static ByteStreamToMessageStreamConverter create() {
        return new ByteStreamToMessageStreamConverterImpl();
    }
    
    /**
     * The buffer {@code buf} is read from, using {@link CharBuffer#get(char[])} with a {@code char[]}
     * the size of the buffer's {@link CharBuffer#limit() limit}, thus consuming all characters from
     * the buffer.
     */
    Iterable<Message> convert(CharBuffer buf) throws ParseException;
}
