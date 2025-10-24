package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CompetitorChangeListener;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Team;
import com.sap.sailing.domain.base.WithNationality;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.RGBColor;

/**
 * Tests the {@link CompetitorChangeListener} stuff including the serialization of the transitive listener
 * pattern on the {@link Team} structure.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class CompetitorListenerTest extends AbstractSerializationTest {
    private CompetitorChangeListener listener;
    private boolean nationalityChanged;
    private Object whatChangedNationality;
    private Object oldNationality;
    private Object newNationality;
    private boolean nameChanged;
    private String oldName;
    private String newName;
    private boolean shortNameChanged;
    private String oldShortName;
    private String newShortName;
    private boolean colorChanged;
    private Color oldColor;
    private Color newColor;
    private DynamicCompetitor competitor;
    private DomainFactoryImpl baseDomainFactory;
    private boolean emailChanged;
    private String oldEmail;
    private String newEmail;
    private boolean searchTagChanged;
    private String oldSearchTag;
    private String newSearchTag;
    private boolean flagImageChanged;
    private URI oldFlagImageURL;
    private URI newFlagImageURL;
    private boolean timeOnTimeFactorChanged;
    private Double oldTimeOnTimeFactor;
    private Double newTimeOnTimeFactor;
    private boolean timeOnDistanceAllowancePerNauticalMileChanged;
    private Duration oldTimeOnDistanceAllowancePerNauticalMile;
    private Duration newTimeOnDistanceAllowancePerNauticalMile;
    
    @BeforeEach
    public void setUp() {
        baseDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        competitor = (DynamicCompetitor) TrackBasedTest.createCompetitorWithBoat("Hasso");
        nationalityChanged = false;
        whatChangedNationality = null;
        oldNationality = null;
        newNationality = null;
        nameChanged = false;
        oldName = null;
        newName = null;
        shortNameChanged = false;
        oldShortName = null;
        newShortName = null;
        colorChanged = false;
        oldColor = null;
        newColor = null;
        emailChanged = false;
        oldEmail = null;
        newEmail = null;
        searchTagChanged = false;
        oldSearchTag = null;
        newSearchTag = null;
        flagImageChanged = false;
        oldFlagImageURL = null;
        newFlagImageURL = null;
        timeOnTimeFactorChanged = false;
        oldTimeOnTimeFactor = null;
        newTimeOnTimeFactor = null;
        timeOnDistanceAllowancePerNauticalMileChanged = false;
        oldTimeOnDistanceAllowancePerNauticalMile = null;
        newTimeOnDistanceAllowancePerNauticalMile = null;
        listener = new CompetitorChangeListener() {
            private static final long serialVersionUID = 4581029778988240209L;

            @Override
            public void nationalityChanged(WithNationality what, Nationality oldNationality, Nationality newNationality) {
                nationalityChanged = true;
                whatChangedNationality = what;
                CompetitorListenerTest.this.oldNationality = oldNationality;
                CompetitorListenerTest.this.newNationality = newNationality;
            }
            
            @Override
            public void nameChanged(String oldName, String newName) {
                nameChanged = true;
                CompetitorListenerTest.this.oldName = oldName;
                CompetitorListenerTest.this.newName = newName;
            }

            @Override
            public void shortNameChanged(String oldShortName, String newShortName) {
                shortNameChanged = true;
                CompetitorListenerTest.this.oldShortName = oldShortName;
                CompetitorListenerTest.this.newShortName = newShortName;
            }

            @Override
            public void colorChanged(Color oldColor, Color newColor) {
                colorChanged = true;
                CompetitorListenerTest.this.oldColor = oldColor;
                CompetitorListenerTest.this.newColor = newColor;
            }

            @Override
            public void emailChanged(String oldEmail, String newEmail) {
                emailChanged = true;
                CompetitorListenerTest.this.oldEmail = oldEmail;
                CompetitorListenerTest.this.newEmail = newEmail;
            }

            @Override
            public void searchTagChanged(String oldSearchTag, String newSearchTag) {
                searchTagChanged = true;
                CompetitorListenerTest.this.oldSearchTag = oldSearchTag;
                CompetitorListenerTest.this.newSearchTag = newSearchTag;
            }

            @Override
            public void flagImageChanged(URI oldFlagImageURL, URI newFlagImageURL) {
                flagImageChanged = true;
                CompetitorListenerTest.this.oldFlagImageURL = oldFlagImageURL;
                CompetitorListenerTest.this.newFlagImageURL = newFlagImageURL;
            }

            @Override
            public void timeOnTimeFactorChanged(Double oldTimeOnTimeFactor, Double newTimeOnTimeFactor) {
                timeOnTimeFactorChanged = true;
                CompetitorListenerTest.this.oldTimeOnTimeFactor = oldTimeOnTimeFactor;
                CompetitorListenerTest.this.newTimeOnTimeFactor = newTimeOnTimeFactor;
            }

            @Override
            public void timeOnDistanceAllowancePerNauticalMileChanged(
                    Duration oldTimeOnDistanceAllowancePerNauticalMile,
                    Duration newTimeOnDistanceAllowancePerNauticalMile) {
                timeOnDistanceAllowancePerNauticalMileChanged = true;
                CompetitorListenerTest.this.oldTimeOnDistanceAllowancePerNauticalMile = oldTimeOnDistanceAllowancePerNauticalMile;
                CompetitorListenerTest.this.newTimeOnDistanceAllowancePerNauticalMile = newTimeOnDistanceAllowancePerNauticalMile;
            }
        };
        competitor.addCompetitorChangeListener(listener);
    }
    
    @Test
    public void testNullNationality() {
        competitor.getTeam().setNationality(null);
        assertNull(competitor.getTeam().getNationality());
        competitor.getTeam().setNationality(null);
        assertNull(competitor.getTeam().getNationality());
        competitor.getTeam().setNationality(baseDomainFactory.getOrCreateNationality("GER"));
        assertEquals(baseDomainFactory.getOrCreateNationality("GER"), competitor.getTeam().getNationality());
        competitor.getTeam().setNationality(null);
        assertNull(competitor.getTeam().getNationality());
    }
    
    @Test
    public void testChangeTimeOnTimeFactor() {
        competitor.setTimeOnTimeFactor(1.209);
        assertTrue(timeOnTimeFactorChanged);
        assertNull(oldTimeOnTimeFactor);
        assertEquals(1.209, newTimeOnTimeFactor, 0.00000001);
    }
    
    @Test
    public void testChangeTimeOnDistanceAllowancePerNauticalMile() {
        competitor.setTimeOnDistanceAllowancePerNauticalMile(Duration.ONE_HOUR);
        assertTrue(timeOnDistanceAllowancePerNauticalMileChanged);
        assertNull(oldTimeOnDistanceAllowancePerNauticalMile);
        assertEquals(Duration.ONE_HOUR.asMillis(), newTimeOnDistanceAllowancePerNauticalMile.asMillis());
    }
    
    @Test
    public void testSimpleCompetitorListenerPatternForName() {
        competitor.setName("Dr. Hasso Plattner");
        assertTrue(nameChanged);
        assertEquals("Hasso", oldName);
        assertEquals("Dr. Hasso Plattner", newName);
        assertFalse(colorChanged);
        assertFalse(nationalityChanged);
    }

    @Test
    public void testShortNameChange() throws URISyntaxException {
        competitor.setShortName("Dr. HP");
        assertTrue(shortNameChanged);
        assertEquals("HP", oldShortName);
        assertEquals("Dr. HP", newShortName);
    }
    
    @Test
    public void testFlagChange() throws URISyntaxException {
        competitor.setFlagImage(new URI("http://www.something.de/pic.png"));
        assertTrue(flagImageChanged);
        assertNull(oldFlagImageURL);
        assertEquals(new URI("http://www.something.de/pic.png"), newFlagImageURL);
    }

    @Test
    public void testEmailChange() throws URISyntaxException {
        competitor.setEmail("hasso.plattner@sap.com");
        assertTrue(emailChanged);
        assertNull(oldEmail);
        assertEquals("hasso.plattner@sap.com", newEmail);
    }

    @Test
    public void testSearchTagChange() throws URISyntaxException {
        competitor.setSearchTag("hasso");
        assertTrue(searchTagChanged);
        assertNull(oldSearchTag);
        assertEquals("hasso", newSearchTag);
    }

    @Test
    public void testSimpleCompetitorListenerPatternForColor() {
        final Color myOldColor = competitor.getColor();
        final RGBColor myNewColor = new RGBColor(123, 12, 234);
        competitor.setColor(myNewColor);
        assertFalse(nameChanged);
        assertTrue(colorChanged);
        assertEquals(myOldColor, oldColor);
        assertEquals(myNewColor, newColor);
        assertFalse(nationalityChanged);
    }

    @Test
    public void testSimpleCompetitorListenerPatternForNationality() {
        final Nationality myOldNationality = competitor.getTeam().getNationality();
        final Nationality myNewNationality = DomainFactory.INSTANCE.getOrCreateNationality("POR");
        competitor.getTeam().setNationality(myNewNationality);
        assertFalse(nameChanged);
        assertFalse(colorChanged);
        assertTrue(nationalityChanged);
        assertEquals(myOldNationality, oldNationality);
        assertEquals(myNewNationality, newNationality);
        assertSame(competitor.getTeam(), whatChangedNationality);
    }
    
    @Test
    public void testCompetitorSerializationLosesAllExternalListeners() throws ClassNotFoundException, IOException {
        DomainFactory baseDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        DynamicCompetitor clonedCompetitor = cloneBySerialization(competitor, baseDomainFactory);
        clonedCompetitor.setName("Dr. Hasso Plattner");
        final RGBColor myNewColor = new RGBColor(123, 12, 234);
        clonedCompetitor.setColor(myNewColor);
        final Nationality myNewNationality = baseDomainFactory.getOrCreateNationality("POR");
        clonedCompetitor.getTeam().setNationality(myNewNationality);
        assertFalse(nameChanged);
        assertFalse(colorChanged);
        assertFalse(nationalityChanged);
    }

    @Test
    public void testCompetitorSerializationListenerPatternForNameOnSerializedCompetitor() throws ClassNotFoundException, IOException {
        DynamicCompetitor clonedCompetitor = cloneBySerialization(competitor, baseDomainFactory);
        competitor = clonedCompetitor;
        competitor.addCompetitorChangeListener(listener);
        testSimpleCompetitorListenerPatternForName();
    }

    @Test
    public void testCompetitorSerializationListenerPatternForColorOnSerializedCompetitor() throws ClassNotFoundException, IOException {
        DynamicCompetitor clonedCompetitor = cloneBySerialization(competitor, baseDomainFactory);
        competitor = clonedCompetitor;
        competitor.addCompetitorChangeListener(listener);
        testSimpleCompetitorListenerPatternForColor();
    }
    
    @Test
    public void testCompetitorSerializationListenerPatternForSailIdOnSerializedCompetitor() throws ClassNotFoundException, IOException {
        DynamicCompetitor clonedCompetitor = cloneBySerialization(competitor, baseDomainFactory);
        competitor = clonedCompetitor;
        competitor.addCompetitorChangeListener(listener);
    }
    
    @Test
    public void testCompetitorSerializationListenerPatternForNationalityOnSerializedCompetitor() throws ClassNotFoundException, IOException {
        DynamicCompetitor clonedCompetitor = cloneBySerialization(competitor, baseDomainFactory);
        competitor = clonedCompetitor;
        competitor.addCompetitorChangeListener(listener);
        testSimpleCompetitorListenerPatternForNationality();
    }
}
