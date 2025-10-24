package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.concurrent.CopyOnWriteHashMap;

public class CopyOnWriteHashMapSerializationTest {
    
    private CopyOnWriteHashMap<String, String> expected;
    
    @BeforeEach
    public void setUp() {
        expected = new CopyOnWriteHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
    }
    
    @Test
    public void testSerializationOfCopyOnWriteHashMap() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(expected);
        ous.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        
        @SuppressWarnings("unchecked")
        CopyOnWriteHashMap<String, String> deserializedInstance = (CopyOnWriteHashMap<String, String>) ois.readObject();
        assertEquals(expected, deserializedInstance);
        
        assertEquals(-1, ois.read(new byte[1]));
        ois.close();
    }

}
