/* 
 * TPVProviderTest.java
 * Copyright (C) 2012 Kimmo Tuukkanen
 * 
 * This file is part of Java Marine API.
 * <http://sourceforge.net/projects/marineapi/>
 * 
 * Java Marine API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * Java Marine API is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Marine API. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.marineapi.provider.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.test.GGATest;
import net.sf.marineapi.nmea.parser.test.GLLTest;
import net.sf.marineapi.nmea.parser.test.RMCTest;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.provider.TPVProvider;
import net.sf.marineapi.provider.event.TPVEvent;
import net.sf.marineapi.provider.event.TPVListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Kimmo Tuukkanen
 * @version $Revision$
 */
public class TPVProviderTest implements TPVListener {

    TPVEvent event;
    TPVProvider instance;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        SentenceReader r = new SentenceReader(new ByteArrayInputStream(new byte[]{}));
        instance = new TPVProvider(r);
        instance.addListener(this);
        event = null;
        r.start();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        instance.removeListener(this);
    }

    /**
     * Test method for
     * {@link net.sf.marineapi.provider.AbstractProvider#sentenceRead(net.sf.marineapi.nmea.event.SentenceEvent)}
     * .
     */
    @Test
    public void testSentenceReadWithGGA() {

        SentenceFactory sf = SentenceFactory.getInstance();
        Sentence gga = sf.createParser(GGATest.EXAMPLE);
        
        assertNull(event);
        instance.sentenceRead(new SentenceEvent(this, gga));
        assertNull(event);
        
        Sentence rmc = sf.createParser(RMCTest.EXAMPLE);
        
        assertNull(event);
        instance.sentenceRead(new SentenceEvent(this, rmc));
        assertNotNull(event);

    }

    /**
     * Test method for
     * {@link net.sf.marineapi.provider.AbstractProvider#sentenceRead(net.sf.marineapi.nmea.event.SentenceEvent)}
     * .
     */
    @Test
    public void testSentenceReadWithGLL() {

        SentenceFactory sf = SentenceFactory.getInstance();
        Sentence gll = sf.createParser(GLLTest.EXAMPLE);
        
        assertNull(event);
        instance.sentenceRead(new SentenceEvent(this, gll));
        assertNull(event);

        Sentence rmc = sf.createParser(RMCTest.EXAMPLE);
        instance.sentenceRead(new SentenceEvent(this, rmc));
        assertNotNull(event);

    }

    /*
     * (non-Javadoc)
     * @see
     * net.sf.marineapi.provider.event.TPVListener#providerUpdate(net.sf.marineapi
     * .provider.event.TPVEvent)
     */
    public void providerUpdate(TPVEvent evt) {
        event = evt;
    }

}
