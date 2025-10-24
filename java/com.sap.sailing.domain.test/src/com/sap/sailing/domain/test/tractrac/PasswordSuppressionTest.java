package com.sap.sailing.domain.test.tractrac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PasswordSuppressionTest {
    @Test
    public void testPasswordSuppression() {
        assertEquals("https://em.paris1.tractrac.com/update_race_status?eventid=cbdd7370-daea-013c-d54d-342e996a9920&raceid=a90b8e50-e24a-013c-39f7-0eeb2fba6aa7&username=john.doe%40example.com&password=****&race_status=ABORTED",
                "https://em.paris1.tractrac.com/update_race_status?eventid=cbdd7370-daea-013c-d54d-342e996a9920&raceid=a90b8e50-e24a-013c-39f7-0eeb2fba6aa7&username=john.doe%40example.com&password=Humba&race_status=ABORTED"
                    .replaceAll("password=([^&]*)&", "password=****&"));
    }
}
