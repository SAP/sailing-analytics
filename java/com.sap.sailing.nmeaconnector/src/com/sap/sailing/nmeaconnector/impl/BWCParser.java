/* 
 * MWVParser.java
 * Copyright (C) 2011 Kimmo Tuukkanen
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
package com.sap.sailing.nmeaconnector.impl;

import com.sap.sailing.nmeaconnector.BWCSentence;

import net.sf.marineapi.nmea.parser.SentenceParser;
import net.sf.marineapi.nmea.sentence.TalkerId;

public class BWCParser extends SentenceParser implements BWCSentence {
    /**
     * Creates a new instance of BWCParser.
     * 
     * @param nmea BWC sentence String
     */
    public BWCParser(String nmea) {
        super(nmea, "BWC");
    }

    /**
     * Creates a new empty instance of BWCParser.
     * 
     * @param talker Talker id to set
     */
    public BWCParser(TalkerId talker) {
        super(talker, "BWC", 12);
    }
}
