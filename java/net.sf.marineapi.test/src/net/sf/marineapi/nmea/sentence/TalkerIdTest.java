package net.sf.marineapi.nmea.sentence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Kimmo Tuukkanen
 */
public class TalkerIdTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
	}

	/**
	 * Test method for
	 * {@link net.sf.marineapi.nmea.sentence.TalkerId#parse(java.lang.String)}.
	 */
	@Test
	public void testParse() {
		assertEquals(TalkerId.GP, TalkerId.parse("$GPGLL,,,,,,,"));
		assertEquals(TalkerId.GL, TalkerId.parse("$GLGSV,,,,,,,"));
		assertEquals(TalkerId.GN, TalkerId.parse("$GNGSV,,,,,,,"));
		assertEquals(TalkerId.II, TalkerId.parse("$IIDPT,,,,,,,"));
	}

	@Test
	public void testParseProprietary() {
		assertEquals(TalkerId.P, TalkerId.parse("$PRWIILOG,GGA,A,T,1,0"));
	}

	@Test
	public void testParseAIS() {
		assertEquals(TalkerId.AI, TalkerId.parse("!AIVDM,,,,,,,"));	
		assertEquals(TalkerId.AB, TalkerId.parse("!ABVDM,,,,,,,"));	
		assertEquals(TalkerId.BS, TalkerId.parse("!BSVDM,,,,,,,"));	
	}

	@Test
	public void testParseUnknown() {
		try {
			TalkerId.parse("$XXXXX,,,,,,");
			fail("Did not throw exception");
		} catch (Exception e) {
			// pass
		}		
	}

}
