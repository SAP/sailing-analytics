package com.sap.sailing.domain.igtimiadapter;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;

/**
 * Represents the authorization of a {@link Client} to access the data of some {@link #getUser}.
 */
public interface Account extends WithQualifiedObjectIdentifier {
    User getUser();

    @Override
    default HasPermissions getType() {
        return SecuredDomainType.IGTIMI_ACCOUNT;
    }

    @Override
    default String getName() {
        return getUser().getFirstName() + " " + getUser().getSurname();
    }

    @Override
    default QualifiedObjectIdentifier getIdentifier() {
        return getType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    default TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(this);
    }

    static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(Account account) {
        return new TypeRelativeObjectIdentifier(account.getUser().getEmail());
    }

    static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(String email) {
        return new TypeRelativeObjectIdentifier(email);
    }
}
