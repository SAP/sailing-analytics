package com.sap.sailing.nmeaconnector.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.nmeaconnector.NmeaFactory;
import com.sap.sse.common.Util;

import net.sf.marineapi.nmea.event.AbstractSentenceListener;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.sentence.PositionSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.TimeSentence;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;

public class GeneralizedSentenceListenerTest {
    private SentenceReader sentenceReader;
    private SentenceReaderSupport readerSupport;

    @BeforeEach
    public void setUp() throws FileNotFoundException {
        sentenceReader = new SentenceReader(new FileInputStream("resources/LogSS.txt"));
        readerSupport = new SentenceReaderSupport(sentenceReader);
    }
    
    @Test
    public void testRegisterPositionListener() throws InterruptedException {
        final List<Position> positions = new ArrayList<>();
        SentenceListener positionListener = new AbstractSentenceListener<PositionSentence>() {
            @Override
            public void sentenceRead(PositionSentence sentence) {
                try {
                    final Position position = sentence.getPosition();
                    positions.add(position);
                } catch (DataNotAvailableException e) {
                    // this can happen and simply means we don't have a position; so we
                    // can safely ignore this exception
                }
            }
        };
        sentenceReader.addSentenceListener(positionListener);
        readerSupport.startReading();
        readerSupport.waitUntilAllMessagesHaveBeenRead();
        assertFalse(positions.isEmpty());
    }

    @Test
    public void testRegisterTimeListener() throws InterruptedException {
        final List<Time> times = new ArrayList<>();
        SentenceListener timeListener = new AbstractSentenceListener<TimeSentence>() {
            @Override
            public void sentenceRead(TimeSentence sentence) {
                try {
                    final Time time = sentence.getTime();
                    times.add(time);
                } catch (DataNotAvailableException e) {
                    // this can happen and simply means we don't have a position; so we
                    // can safely ignore this exception
                }
            }
        };
        sentenceReader.addSentenceListener(timeListener);
        readerSupport.startReading();
        readerSupport.waitUntilAllMessagesHaveBeenRead();
        assertFalse(times.isEmpty());
    }
    
    @Test
    public void testSimpleNmeaWindReceiverScenario() throws FileNotFoundException, InterruptedException {
        final Iterable<Wind> wind = NmeaFactory.INSTANCE.readWind(new FileInputStream("resources/LogSS.txt"));
        assertFalse(Util.isEmpty(wind));
    }
 
    private static class MyGenericSentenceListener<A, B extends Sentence> extends AbstractSentenceListener<B> {
        private B lastSentence;
        
        @Override
        public void sentenceRead(B sentence) {
            lastSentence = sentence;
        }

        public B getLastSentence() {
            return lastSentence;
        }
    }
    
    private static class MyOtherGenericSentenceListener<A> extends MyGenericSentenceListener<A, TimeSentence> {}
    
    private static class MyConcreteSentenceListener extends MyOtherGenericSentenceListener<String> {}
    
    private static class MyConcreteSentenceListenerSubclass extends MyConcreteSentenceListener {}
    
    @Test
    public void testComplicatedGenericSentenceListenerSubclass() throws InterruptedException {
        MyConcreteSentenceListenerSubclass listener = new MyConcreteSentenceListenerSubclass();
        sentenceReader.addSentenceListener(listener);
        readerSupport.startReading();
        readerSupport.waitUntilAllMessagesHaveBeenRead();
        assertNotNull(listener.getLastSentence());
    }
}