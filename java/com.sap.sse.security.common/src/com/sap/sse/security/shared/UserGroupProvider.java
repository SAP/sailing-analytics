package com.sap.sse.security.shared;

import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

public interface UserGroupProvider {
    Iterable<UserGroup> getUserGroupsOfUser(User user);
}
