package net.sf.marineapi.nmea.parser;

import net.sf.marineapi.nmea.sentence.MTASentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.TalkerId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MTATest {

	public static final String EXAMPLE = "$IIMTA,21.5,C";

	private MTASentence mta;

	@BeforeEach
	public void setUp() throws Exception {
		mta = new MTAParser(EXAMPLE);
	}

	@Test
	public void testMTAParserString() {
		assertEquals(TalkerId.II, mta.getTalkerId());
		assertEquals(SentenceId.MTA.name(), mta.getSentenceId());
	}

	@Test
	public void testMTAParserTalkerId() {
		MTAParser empty = new MTAParser(TalkerId.WI);
		assertEquals(TalkerId.WI, empty.getTalkerId());
		assertEquals(SentenceId.MTA.name(), empty.getSentenceId());
		assertTrue(empty.getCharValue(1) == 'C');
	}

	@Test
	public void testGetTemperature() {
		assertEquals(21.5, mta.getTemperature(), 0.01);
	}

	@Test
	public void testSetTemperature() {
		mta.setTemperature(15.3335);
		assertEquals(15.33, mta.getTemperature(), 0.01);
	}

}
