package com.sap.sailing.domain.queclinkadapter;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.logging.Level;

import com.sap.sailing.domain.queclinkadapter.impl.MessageParserImpl;

/**
 * Parser utility for Queclink GL 300 data streams. The GL 300 is assumed to send a message stream over some connection,
 * usually a UDP or TCP connection, but theoretically also through one or more SMS text messages. The messages consist
 * of printable ASCII characters, introduced by a message type, and terminated by the special termination character "$"
 * (dollar sign).<p>
 * 
 * This parser utility can read and parse those messages into objects of type {@link Message}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface MessageParser {
    static MessageParser create() {
        return new MessageParserImpl();
    }

    /**
     * Tries to parse a single message that includes the termination character "$". If the message cannot be
     * parsed, e.g., because no {@link MessageFactory} is found for the message type found at the beginning
     * of the message, {@code null} is returned. If the message is not syntactically correct, a
     * {@link ParseException} will be thrown.
     */
    Message parse(String messageIncludingTailCharacter) throws ParseException;

    /**
     * Reads from the {@code reader} until EOF is reached and produces {@link Message}s based on {@link #parse(String)}
     * and the splitting of the input based on the termination character "$" (dollar sign). If parsing any message in the stream
     * throws a {@link ParseException}, this exception is forwarded by this method, and parsing the stream stops.
     * If a message is syntactically correct, but no {@link MessageParser} can be found for the type of message,
     * that message is skipped, and the skipping is logged as a {@link Level#WARNING warning}.
     */
    Iterable<Message> parse(Reader reader) throws ParseException, IOException;
}
