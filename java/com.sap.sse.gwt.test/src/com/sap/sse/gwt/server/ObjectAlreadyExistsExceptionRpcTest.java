package com.sap.sse.gwt.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy;
import com.sap.sse.common.ObjectAlreadyExistsException;
import com.sap.sse.common.ObjectAlreadyExistsException.ObjectType;
import com.sap.sse.common.ObjectAlreadyExistsException_CustomFieldSerializer;

public class ObjectAlreadyExistsExceptionRpcTest {
    private static final SerializationPolicy SERIALIZATION_POLICY = createSerializationPolicy();

    private static SerializationPolicy createSerializationPolicy() {
        final Map<Class<?>, Boolean> whitelist = new HashMap<>();
        whitelist.put(ObjectAlreadyExistsException.class, Boolean.TRUE);
        whitelist.put(TestObjectType.class, Boolean.TRUE);
        return new StandardSerializationPolicy(whitelist, whitelist, Collections.emptyMap());
    }

    public interface ConflictingService {
        void update() throws ObjectAlreadyExistsException;
    }

    public enum TestObjectType implements ObjectType {
        LEADERBOARD,
        LEADERBOARD_GROUP
    }

    @Test
    public void testDeclaredConflictIsEncodedAsRpcFailure() throws Exception {
        final ObjectAlreadyExistsException conflict = new ObjectAlreadyExistsException(TestObjectType.LEADERBOARD,
                "Humba Overall");
        assertSame(ObjectAlreadyExistsException_CustomFieldSerializer.class,
                SerializabilityUtil.hasCustomFieldSerializer(ObjectAlreadyExistsException.class));
        final String response = RPC.encodeResponseForFailure(ConflictingService.class.getMethod("update"), conflict,
                SERIALIZATION_POLICY);
        assertTrue(response.startsWith("//EX["), response);
        assertTrue(response.contains(ObjectAlreadyExistsException.class.getName()), response);
        assertTrue(response.contains(TestObjectType.class.getName()), response);
        assertTrue(response.contains("\"Humba Overall\""), response);
    }

    @Test
    public void testRpcDeserializationPreservesConflictDetails() throws Exception {
        assertRpcDeserialization(TestObjectType.LEADERBOARD, "Humba Overall");
        assertRpcDeserialization(TestObjectType.LEADERBOARD_GROUP, "Hümba \"Cup\"");
    }

    private void assertRpcDeserialization(final TestObjectType objectType, final String identifier) throws Exception {
        final String typeSignature = SerializabilityUtil.encodeSerializedInstanceReference(
                ObjectAlreadyExistsException.class, SERIALIZATION_POLICY);
        final String objectTypeSignature = SerializabilityUtil.encodeSerializedInstanceReference(
                TestObjectType.class, SERIALIZATION_POLICY);
        final ServerSerializationStreamReader reader = new ServerSerializationStreamReader(getClass().getClassLoader(),
                (moduleBaseURL, strongName) -> SERIALIZATION_POLICY);
        // GWT request format: header, string table, module/policy references, then the exception and its fields.
        reader.prepareToRead("7|0|5|http://localhost/|policy|" + typeSignature + "|" + objectTypeSignature + "|" + identifier
                + "|1|2|3|4|" + objectType.ordinal() + "|5|");
        final ObjectAlreadyExistsException restored = (ObjectAlreadyExistsException) reader.readObject();
        assertSame(objectType, restored.getObjectType());
        assertEquals(identifier, restored.getIdentifier());
        assertEquals(new ObjectAlreadyExistsException(objectType, identifier).getMessage(), restored.getMessage());
    }
}
