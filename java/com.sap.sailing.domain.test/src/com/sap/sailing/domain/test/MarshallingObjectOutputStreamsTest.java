package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * See bug 3015. How sure can we be that multiple {@link ObjectOutputStream}s can be concatenated in one
 * {@link OutputStream} and then be read from one {@link InputStream} through multiple {@link ObjectInputStream}s?
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class MarshallingObjectOutputStreamsTest {
    @Test
    public void testTwoObjectOutputStreams() throws IOException, ClassNotFoundException {
        final Random random = new Random();
        String[][] strings = new String[100][];
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int stream = 0; stream < strings.length; stream++) {
            final int count = random.nextInt(100);
            strings[stream] = createRandomArrayOfStrings(count);
            {
                ObjectOutputStream oos1 = new ObjectOutputStream(bos);
                for (int i=0; i<strings[stream].length; i++) {
                    oos1.writeObject(strings[stream][i]);
                }
            }
        }
        bos.close();
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        for (int stream = 0; stream<strings.length; stream++) {
            final ObjectInputStream ois1 = new ObjectInputStream(bis);
            for (int i=0; i<strings[stream].length; i++) {
                final String o1Read = (String) ois1.readObject();
                assertEquals(strings[stream][i], o1Read);
            }
        }
        bis.close();
    }

    private String[] createRandomArrayOfStrings(final int count1) {
        final Random random = new Random();
        String[] o1 = new String[count1];
        for (int i=0; i<o1.length; i++) {
            final StringBuilder sb = new StringBuilder();
            final int strlen = random.nextInt(500);
            for (int j=0; j<strlen; j++) {
                sb.append((char) (65+random.nextInt(26)));
            }
            o1[i] = sb.toString();
        }
        return o1;
    }
}
