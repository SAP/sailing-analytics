package com.sap.sailing.declination.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.io.UTFDataFormatException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.impl.DeclinationImporter;
import com.sap.sailing.declination.impl.DeclinationRecordImpl;
import com.sap.sailing.declination.impl.DeclinationStore;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.quadtree.QuadTree;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class DeclinationStoreTest<I extends DeclinationImporter> extends AbstractDeclinationTest<I> {
    private DeclinationStore store;
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    
    @BeforeEach
    public void setUp() {
        store = new DeclinationStore(importer);
    }
    
    @Test
    public void testLoad2011() throws IOException, ClassNotFoundException, ParseException {
        QuadTree<Declination> declinationsFor2011 = store.getStoredDeclinations(2011);
        assertNotNull(declinationsFor2011);
        Declination declinationAt54N9E = declinationsFor2011.get(new DegreePosition(54, 9));
        assertEquals(1.+29./60., declinationAt54N9E.getBearing().getDegrees(), 0.00000001);
        assertEquals(0.+08./60., declinationAt54N9E.getAnnualChange().getDegrees(), 0.00000001);
    }

    @Test
    public void test1800IsNull() throws IOException, ClassNotFoundException, ParseException {
        QuadTree<Declination> declinationsFor1800 = store.getStoredDeclinations(1800);
        assertNull(declinationsFor1800);
    }
    
    @Test
    public void demonstrateThatConcatenatingObjectOutputStreamsIsABadIdea() throws IOException {
        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
        oos1.writeUTF("Humba Humba");
        oos1.close();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        ObjectOutputStream oos2 = new ObjectOutputStream(bos2);
        oos2.writeUTF("T�t�r���");
        oos2.close();
        byte[] bos1Arr = bos1.toByteArray();
        byte[] bos2Arr = bos2.toByteArray();
        byte[] both = new byte[bos1Arr.length + bos2Arr.length];
        System.arraycopy(bos1Arr, 0, both, 0, bos1Arr.length);
        System.arraycopy(bos2Arr, 0, both, bos1Arr.length, bos2Arr.length);
        ByteArrayInputStream bis = new ByteArrayInputStream(both);
        ObjectInputStream ois = new ObjectInputStream(bis);
        String s1 = ois.readUTF();
        assertEquals("Humba Humba", s1);
        try {
            ois.readUTF();
            fail("Expected StreamCorruptedException");
        } catch (StreamCorruptedException sce) {
            // this is expected
        }
        ois.close();
    }

    @Test
    public void concatenatingObjectOutputStreamsWorksForSimpleStrings() throws IOException {
        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
        oos1.writeUTF("Humba Humba");
        oos1.close();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        ObjectOutputStream oos2 = new ObjectOutputStream(bos2) {
            @Override
            protected void writeStreamHeader() throws IOException {
                // don't write a second header
            }
        };
        oos2.writeUTF("T�t�r���");
        oos2.close();
        byte[] bos1Arr = bos1.toByteArray();
        byte[] bos2Arr = bos2.toByteArray();
        byte[] both = new byte[bos1Arr.length + bos2Arr.length];
        System.arraycopy(bos1Arr, 0, both, 0, bos1Arr.length);
        System.arraycopy(bos2Arr, 0, both, bos1Arr.length, bos2Arr.length);
        ByteArrayInputStream bis = new ByteArrayInputStream(both);
        ObjectInputStream ois = new ObjectInputStream(bis);
        String s1 = ois.readUTF();
        assertEquals("Humba Humba", s1);
        String s2 = ois.readUTF();
        assertEquals("T�t�r���", s2);
        ois.close();
    }

    @Test
    public void concatenatingObjectOutputStreamsWorksForDeclinationRecordFormat() throws IOException, ClassNotFoundException, ParseException {
        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
        oos1.writeUTF("Humba Humba");
        oos1.writeDouble(1.);
        oos1.writeDouble(2.);
        oos1.writeDouble(3.);
        oos1.writeDouble(4.);
        oos1.close();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        ObjectOutputStream oos2 = new ObjectOutputStream(bos2) {
            @Override
            protected void writeStreamHeader() throws IOException {
                // don't write a second header
            }
        };
        oos2.writeUTF("T�t�r���");
        oos2.writeDouble(1.);
        oos2.writeDouble(2.);
        oos2.writeDouble(3.);
        oos2.writeDouble(4.);
        oos2.close();
        byte[] bos1Arr = bos1.toByteArray();
        byte[] bos2Arr = bos2.toByteArray();
        byte[] both = new byte[bos1Arr.length + bos2Arr.length];
        System.arraycopy(bos1Arr, 0, both, 0, bos1Arr.length);
        System.arraycopy(bos2Arr, 0, both, bos1Arr.length, bos2Arr.length);
        ByteArrayInputStream bis = new ByteArrayInputStream(both);
        ObjectInputStream ois = new ObjectInputStream(bis);
        String s1 = ois.readUTF();
        assertEquals("Humba Humba", s1);
        assertEquals(1., ois.readDouble(), 0.000000001);
        assertEquals(2., ois.readDouble(), 0.000000001);
        assertEquals(3., ois.readDouble(), 0.000000001);
        assertEquals(4., ois.readDouble(), 0.000000001);
        String s2 = ois.readUTF();
        assertEquals("T�t�r���", s2);
        assertEquals(1., ois.readDouble(), 0.000000001);
        assertEquals(2., ois.readDouble(), 0.000000001);
        assertEquals(3., ois.readDouble(), 0.000000001);
        assertEquals(4., ois.readDouble(), 0.000000001);
        ois.close();
    }


    @Test
    public void testReadingBeyondEOF() throws IOException, ClassNotFoundException, ParseException {
        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
        oos1.writeUTF("Humba Humba");
        oos1.writeDouble(1.);
        oos1.writeDouble(2.);
        oos1.writeDouble(3.);
        oos1.writeDouble(4.);
        oos1.close();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        ObjectOutputStream oos2 = new ObjectOutputStream(bos2) {
            @Override
            protected void writeStreamHeader() throws IOException {
                // don't write a second header
            }
        };
        oos2.writeUTF("T�t�r���");
        oos2.writeDouble(1.);
        oos2.writeDouble(2.);
        oos2.writeDouble(3.);
        oos2.writeDouble(4.);
        oos2.close();
        byte[] bos1Arr = bos1.toByteArray();
        byte[] bos2Arr = bos2.toByteArray();
        byte[] both = new byte[bos1Arr.length + bos2Arr.length];
        System.arraycopy(bos1Arr, 0, both, 0, bos1Arr.length);
        System.arraycopy(bos2Arr, 0, both, bos1Arr.length, bos2Arr.length);
        ByteArrayInputStream bis = new ByteArrayInputStream(both);
        ObjectInputStream ois = new ObjectInputStream(bis);
        String s1 = ois.readUTF();
        assertEquals("Humba Humba", s1);
        assertEquals(1., ois.readDouble(), 0.000000001);
        assertEquals(2., ois.readDouble(), 0.000000001);
        assertEquals(3., ois.readDouble(), 0.000000001);
        assertEquals(4., ois.readDouble(), 0.000000001);
        String s2 = ois.readUTF();
        assertEquals("T�t�r���", s2);
        assertEquals(1., ois.readDouble(), 0.000000001);
        assertEquals(2., ois.readDouble(), 0.000000001);
        assertEquals(3., ois.readDouble(), 0.000000001);
        assertEquals(4., ois.readDouble(), 0.000000001);
        try {
            ois.readUTF();
            fail("Expected EOF");
        } catch (EOFException eofex) {
            // this was to be expected
        }
        ois.close();
    }

    @Test
    public void testAbortingAndReadingBeyondEOF() throws IOException, ClassNotFoundException, ParseException {
        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
        oos1.writeUTF("Humba Humba");
        oos1.writeDouble(1.);
        oos1.writeDouble(2.);
        oos1.writeDouble(3.);
        oos1.writeDouble(4.);
        oos1.flush();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        ObjectOutputStream oos2 = new ObjectOutputStream(bos2) {
            @Override
            protected void writeStreamHeader() throws IOException {
                // don't write a second header
            }
        };
        oos2.writeUTF("T�t�r���");
        oos2.writeDouble(1.);
        oos2.writeDouble(2.);
        oos2.writeDouble(3.);
        oos2.writeDouble(4.);
        oos2.close();
        byte[] bos1Arr = bos1.toByteArray();
        byte[] bos2Arr = bos2.toByteArray();
        byte[] both = new byte[bos1Arr.length + bos2Arr.length];
        System.arraycopy(bos1Arr, 0, both, 0, bos1Arr.length);
        System.arraycopy(bos2Arr, 0, both, bos1Arr.length, bos2Arr.length);
        ByteArrayInputStream bis = new ByteArrayInputStream(both);
        ObjectInputStream ois = new ObjectInputStream(bis);
        String s1 = ois.readUTF();
        assertEquals("Humba Humba", s1);
        assertEquals(1., ois.readDouble(), 0.000000001);
        assertEquals(2., ois.readDouble(), 0.000000001);
        assertEquals(3., ois.readDouble(), 0.000000001);
        assertEquals(4., ois.readDouble(), 0.000000001);
        String s2 = ois.readUTF();
        assertEquals("T�t�r���", s2);
        assertEquals(1., ois.readDouble(), 0.000000001);
        assertEquals(2., ois.readDouble(), 0.000000001);
        assertEquals(3., ois.readDouble(), 0.000000001);
        assertEquals(4., ois.readDouble(), 0.000000001);
        try {
            ois.readUTF();
            fail("Expected EOF");
        } catch (EOFException eofex) {
            // this was to be expected
        }
        ois.close();
    }

    @Test
    public void testAbortingAndReadingBeyondEOFOnRealFile() throws IOException, ClassNotFoundException, ParseException {
        File tmpFile = File.createTempFile("declination-test", ".txt");
        try {
            FileOutputStream bos1 = new FileOutputStream(tmpFile);
            ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
            oos1.writeUTF("2011-10-08");
            oos1.writeDouble(1.);
            oos1.writeDouble(2.);
            oos1.writeDouble(3.);
            oos1.writeDouble(4.);
            oos1.flush();
            FileOutputStream bos2 = new FileOutputStream(tmpFile, /* append */ true);
            ObjectOutputStream oos2 = new ObjectOutputStream(bos2) {
                @Override
                protected void writeStreamHeader() throws IOException {
                    // don't write a second header
                }
            };
            oos2.writeUTF("2011-10-09");
            oos2.writeDouble(1.);
            oos2.writeDouble(2.);
            oos2.writeDouble(3.);
            oos2.writeDouble(4.);
            oos2.close();
            FileInputStream bis = new FileInputStream(tmpFile);
            ObjectInputStream ois = new ObjectInputStream(bis);
            String s1 = ois.readUTF();
            assertEquals("2011-10-08", s1);
            assertEquals(1., ois.readDouble(), 0.000000001);
            assertEquals(2., ois.readDouble(), 0.000000001);
            assertEquals(3., ois.readDouble(), 0.000000001);
            assertEquals(4., ois.readDouble(), 0.000000001);
            String s2 = ois.readUTF();
            assertEquals("2011-10-09", s2);
            assertEquals(1., ois.readDouble(), 0.000000001);
            assertEquals(2., ois.readDouble(), 0.000000001);
            assertEquals(3., ois.readDouble(), 0.000000001);
            assertEquals(4., ois.readDouble(), 0.000000001);
            try {
                ois.readUTF();
                fail("Expected EOF");
            } catch (EOFException eofex) {
                // this was to be expected
            }
            ois.close();
            
            oos1.close();
            oos2.close();
        } finally {
            tmpFile.delete();
        }
    }
    
    @Disabled("This was a one-time-only conversion")
    @Test
    public void copyExistingDeclinationsToNewFormat() throws IOException, ClassNotFoundException, ParseException {
        File resourcesDir = new File("../com.sap.sailing.declination/resources/");
        DeclinationStore store = new DeclinationStore(importer);
        assertTrue(resourcesDir.isDirectory());
        for (File f : resourcesDir.listFiles()) {
            if (f.getName().startsWith("declination-") && f.getName().endsWith(".txt") &&
                    f.getName().length() == "declination-".length()+4+".txt".length()) {
                ObjectInput in = new ObjectInputStream(new FileInputStream(f));
                Writer out = new FileWriter(new File(f.getParentFile(), f.getName().replace(".txt", "")));
                Declination record;
                while ((record = readExternal(in)) != null) {
                    store.writeExternal(record, out);
                }
                in.close();
                out.close();
            }
        }
    }
    
    private Declination readExternal(ObjectInput in) throws IOException, ClassNotFoundException, ParseException {
        try {
            TimePoint timePoint = new MillisecondsTimePoint(dateFormatter.parse(in.readUTF()).getTime());
            double lat = in.readDouble();
            double lng = in.readDouble();
            Position position = new DegreePosition(lat, lng);
            Bearing bearing = new DegreeBearingImpl(in.readDouble());
            Bearing annualChange = new DegreeBearingImpl(in.readDouble());
            return new DeclinationRecordImpl(position, timePoint, bearing, annualChange);
        } catch (EOFException e) {
            return null;
        } catch (UTFDataFormatException dfe) {
            // this probably means that a previous write was aborted
            return null;
        }
    }

    
}
