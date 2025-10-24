package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.HasPermissionsImpl;
import com.sap.sse.security.shared.impl.QualifiedObjectIdentifierImpl;
import com.sap.sse.security.shared.impl.WildcardPermissionEncoder;

public class HasPermissionsTest {

    private final static String OBJECT_NAME = " Regatta :2018///\"@::";
    private final static String OBJECT_NAME_ENCODED = WildcardPermissionEncoder.encode(OBJECT_NAME);
    private final static Action READ_ACTION = HasPermissions.DefaultActions.READ;

    @Test
    public void getQualifiedObjectIdentifierFromTypedObjectIdentifer() {
        final QualifiedObjectIdentifier q = QualifiedObjectIdentifierImpl.fromDBWithoutEscaping("type/" + OBJECT_NAME_ENCODED);
        final HasPermissions h = new HasPermissionsImpl("type");
        final TypeRelativeObjectIdentifier t = new TypeRelativeObjectIdentifier(OBJECT_NAME);
        assertEquals(q, h.getQualifiedObjectIdentifier(t));
        assertEquals(q.getTypeRelativeObjectIdentifier(), t);
        assertEquals(OBJECT_NAME_ENCODED, q.getTypeRelativeObjectIdentifier().toString());
        assertEquals(OBJECT_NAME_ENCODED, t.toString());
        assertEquals(q.getTypeIdentifier(), "type");
    }
    
    @Test
    public void checkPermissions() {
        final HasPermissions h = new HasPermissionsImpl("type");
        final WildcardPermission permission = h.getPermission(READ_ACTION);
        assertEquals(new WildcardPermission("type:READ"), permission);
    }
    
    @Test
    public void checkPermissionsForObject() {
        final HasPermissions h = new HasPermissionsImpl("type");
        final WithQualifiedObjectIdentifier object = createObject(h, OBJECT_NAME);
        final WildcardPermission permission = h.getPermissionForObject(READ_ACTION, object);
        assertEquals(new WildcardPermission("type:READ:" + OBJECT_NAME_ENCODED), permission);
        assertEquals("type:READ:" + OBJECT_NAME_ENCODED, h.getStringPermissionForObject(READ_ACTION, object));
    }
    
    @Test
    public void checkPermissionsForTypeRelativeObjectIdentifiers() {
        final HasPermissions h = new HasPermissionsImpl("type");
        final TypeRelativeObjectIdentifier id = new TypeRelativeObjectIdentifier(OBJECT_NAME);
        final WildcardPermission permission = h.getPermissionForTypeRelativeIdentifier(READ_ACTION, id);
        assertEquals(new WildcardPermission("type:READ:" + OBJECT_NAME_ENCODED), permission);
        assertEquals("type:READ:" + OBJECT_NAME_ENCODED, h.getStringPermissionForTypeRelativeIdentifier(READ_ACTION, id));
    }

    private WithQualifiedObjectIdentifier createObject (HasPermissions h, String name) {
        return new WithQualifiedObjectIdentifier() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getName() {
                return name;
            }

            @Override
            public QualifiedObjectIdentifier getIdentifier() {
                return new QualifiedObjectIdentifierImpl(h.getName(), new TypeRelativeObjectIdentifier(name));
            }

            @Override
            public HasPermissions getPermissionType() {
                return h;
            }
        };
    }
}
