package com.sap.sailing.server.test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.RegattaNameAndRaceName;

public class IdentifierSerializationTest {

    @Test
    public void identifierSerializationTest() throws IOException, ClassNotFoundException {
        RegattaNameAndRaceName original = new RegattaNameAndRaceName("TestEvent", "Test Race");
        PipedInputStream pis = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(pis);
        
        //Don't change order of stream creation, to prevent deadlock
        ObjectOutputStream oos = new ObjectOutputStream(pos);
        ObjectInputStream ois = new ObjectInputStream(pis);
        
        oos.writeObject(original);
        oos.flush();
        
        RegattaNameAndRaceName streamed = (RegattaNameAndRaceName) ois.readObject();
        
        ois.close();
        oos.close();
        
        Assertions.assertEquals(original, streamed);
    }
    
}
