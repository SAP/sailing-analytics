package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SHARED_SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import com.google.protobuf.CodedOutputStream;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.Authenticator;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.TimePoint;
import com.sun.jersey.core.util.Base64;

/**
 * To obtain a Base64 message log of protobuf Windbot messages, try something like this:
 * <pre>
 *  mongosh --quiet --eval 'EJSON.stringify(db.IGTIMI_MESSAGES.find({IGTIMI_MESSAGES_DEVICE_SERIAL_NUMBER: "DC-GD-AAED", IGTIMI_MESSAGES_TIMESTAMP: {$gt: ISODate("2025-01-07T16:49:30.000Z")}}).sort({IGTIMI_MESSAGES_TIMESTAMP: 1}).toArray())' \
 *      "mongodb://localhost/winddb" | \
 *      jq -r '.[].IGTIMI_MESSAGES_PROTOBUF_MESSAGE."$binary".base64' \
 *      >java/com.sap.sailing.domain.igtimiadapter.test/resources/windbot_session_20250107.base64
 * </pre>
 * 
 * @author Axel Uhl (d043530)
 *
 */
@Timeout(value = 2, unit = TimeUnit.MINUTES)
public abstract class AbstractTestWithIgtimiConnection extends AbstractSeleniumTest {
    protected IgtimiConnection connection;
    
    protected ApiContext ctx;

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
        super.setUp();
        ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        final Authenticator authenticator = new Authenticator(getContextRoot());
        String token = authenticator.authForToken(ApiContext.ADMIN_USERNAME, ApiContext.ADMIN_PASSWORD);
        try {
            connection = IgtimiConnectionFactory.create(new URL(getContextRoot()), token).getOrCreateConnection();
            final int riotPort = connection.getRiotPort();
            final Socket socket = new Socket("127.0.0.1", riotPort);
            final OutputStream sos = socket.getOutputStream();
            final CodedOutputStream cos = CodedOutputStream.newInstance(sos);
            final InputStream is = getClass().getResourceAsStream("/windbot_session_20250107.base64");
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String base64;
            while ((base64=br.readLine()) != null) {
                final byte[] msgBytes = Base64.decode(base64);
                cos.writeUInt32NoTag(msgBytes.length);
                cos.writeRawBytes(msgBytes);
            }
            is.close();
            cos.flush();
            sos.close();
            socket.close();
            final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
            connection.createDataAccessWindow("DC-GD-AAED",
                    TimePoint.of(df.parse("2025-01-07T08:00:00Z")),
                    TimePoint.of(df.parse("2025-01-07T22:00:00Z")));
            Thread.sleep(2000); // give RiotServer time to process and store messages
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
