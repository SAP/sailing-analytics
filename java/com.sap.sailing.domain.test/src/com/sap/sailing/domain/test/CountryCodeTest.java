package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.CountryCode;
import com.sap.sse.common.CountryCodeFactory;
import com.sap.sse.common.Util;

public class CountryCodeTest {
    private static final int NUMBER_OF_COUNTRY_CODES = 262;

    @Disabled
    @Test
    public void convertHTML() throws IOException {
        InputStream is = Util.class.getResourceAsStream("countrycodes.csv");
        assertNotNull(is);
        List<String[]> matrix = new LinkedList<String[]>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            String line;
            String[] row = null;
            int column = 8;
            Pattern p = Pattern.compile("<TD[^>]*> *(<[pP]>)?([^<]*)(</[pP]>)? *</TD>");
            while ((line = br.readLine()) != null) {
                if (line.contains("<TR>")) {
                    assertEquals(8, column);
                    column = 0;
                    row = new String[8];
                    matrix.add(row);
                } else {
                    if (line.contains("<TD")) {
                        Matcher m = p.matcher(line);
                        if (m.find()) {
                            String entry = m.group(2);
                            if (entry != null) {
                                entry = URLDecoder.decode(entry, "UTF-8").trim();
                                if (entry.startsWith(".")) {
                                    // IANA domain name will be stripped of leading "."
                                    entry = entry.substring(1);
                                }
                            }
                            row[column++] = entry == null || entry.length() == 0 ? null : entry;
                        }
                    }
                }
            }
        }
        assertEquals(NUMBER_OF_COUNTRY_CODES, matrix.size());
        for (String[] r : matrix) {
            StringBuilder sb = new StringBuilder("        add(new CountryCodeImpl(");
            boolean first = true;
            for (String s : r) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                if (s == null) {
                    sb.append("null");
                } else {
                    sb.append('\"');
                    sb.append(s.replaceAll("\\\"", "\\\\\""));
                    sb.append('\"');
                }
            }
            sb.append("));");
            System.out.println(sb);
        }
    }
    
    @Test
    public void testAFewCountryCodes() {
        CountryCodeFactory ccf = CountryCodeFactory.INSTANCE;
        CountryCode cc;
        assertEquals(NUMBER_OF_COUNTRY_CODES, Util.size(ccf.getAll()));
        cc = ccf.getFromThreeLetterIOCName("GER");
        assertEquals("DE", cc.getTwoLetterISOCode().toUpperCase());
        assertEquals("DEU", cc.getThreeLetterISOCode().toUpperCase());
        cc = ccf.getFromThreeLetterISOName(Locale.GERMANY.getISO3Country().toUpperCase());
        assertEquals("DE", cc.getTwoLetterISOCode().toUpperCase());
        assertEquals("DEU", cc.getThreeLetterISOCode().toUpperCase());
        cc = ccf.getFromIANAInternet("sd");
        assertEquals("SUD", cc.getUNVehicle().toUpperCase());
        assertEquals("SDN", cc.getThreeLetterISOCode().toUpperCase());
    }
    
    @Test
    public void testGermanyCountryCode() {
        for (String iso2 : Locale.getISOCountries()) {
            final CountryCode cc = CountryCodeFactory.INSTANCE.getFromTwoLetterISOName(iso2);
            assertNotNull(cc, "No country code found for two-letter ISO code "+iso2);
            assertEquals(iso2, cc.getTwoLetterISOCode());
        }
        assertEquals("DEU", Locale.GERMANY.getISO3Country());
        assertEquals("DE", Locale.GERMANY.getCountry());
    }
}
