package net.sf.marineapi.nmea.event;

import net.sf.marineapi.nmea.parser.BODTest;
import net.sf.marineapi.nmea.parser.GGATest;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.BODSentence;
import net.sf.marineapi.nmea.sentence.Sentence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractSentenceListenerTest {

	private BODSentence result;
	private TestListener listener;
	private SentenceFactory factory;
	
	
	@BeforeEach
	public void setUp() throws Exception {
		result = null;
		listener = new TestListener();
		factory = SentenceFactory.getInstance();
	}

	@Test
	public void testExpectedSentenceRead() {
		Sentence bod = factory.createParser(BODTest.EXAMPLE);
		SentenceEvent evt = new SentenceEvent(this, bod);
		listener.sentenceRead(evt);
		assertNotNull(result);
		assertEquals(BODTest.EXAMPLE, result.toSentence());
	}
	
	@Test
	public void testUnexpectedSentenceRead() {
		Sentence gga = factory.createParser(GGATest.EXAMPLE);
		SentenceEvent evt = new SentenceEvent(this, gga);
		listener.sentenceRead(evt);
		assertNull(result);
	}
	
	
	private class TestListener extends AbstractSentenceListener<BODSentence> {
		@Override
		public void sentenceRead(BODSentence sentence) {
			result = sentence;						
		}
	}

}
