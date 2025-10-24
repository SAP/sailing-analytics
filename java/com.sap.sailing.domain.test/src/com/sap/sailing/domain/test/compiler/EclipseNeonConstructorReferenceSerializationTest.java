package com.sap.sailing.domain.test.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

public class EclipseNeonConstructorReferenceSerializationTest {
    
    /**
     * We have serialization problems due to a regression in Eclipse Neon:
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=497879
     */
    @Test
    public void testConstructorReferenceSerializationThatFailsOnEclipseNeon() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        
        SerializableSupplier factory = TypeImplementation::new;
        oos.writeObject(factory);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        ois.readObject();
        
    }

}
