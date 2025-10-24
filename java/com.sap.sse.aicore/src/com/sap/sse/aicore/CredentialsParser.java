package com.sap.sse.aicore;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;

import org.json.simple.parser.ParseException;

import com.sap.sse.aicore.impl.CredentialsParserImpl;
import com.sap.sse.common.Util.Pair;

public interface CredentialsParser {
    static CredentialsParser create() {
        return new CredentialsParserImpl();
    }
    
    Credentials parse(Reader r) throws IOException, ParseException;
    
    Credentials parse(CharSequence s) throws ParseException, MalformedURLException;

    /**
     * @return a pair of which the {@link Pair#getA() first} component represents the encoded credentials, using a
     *         random "key" returned as the {@link Pair#getB() second} component of the pair returned. These can be
     *         used as the two arguments to {@link #parseFromEncoded(CharSequence)} to obtain {@link Credentials}
     *         equivalent to the {@code credentials} passed to this method again.
     */
    Pair<String, String> getAsEncodedString(Credentials credentials);
    
    /**
     * The inverse for {@link #getAsEncodedString(Credentials)}
     */
    Credentials parseFromEncoded(CharSequence encoded, String key);
}
