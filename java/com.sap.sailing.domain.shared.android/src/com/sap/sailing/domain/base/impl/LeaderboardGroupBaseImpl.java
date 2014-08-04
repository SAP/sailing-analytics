package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.UUID;

import com.sap.sailing.domain.base.LeaderboardGroupBase;

public abstract class LeaderboardGroupBaseImpl implements LeaderboardGroupBase {
    private static final long serialVersionUID = 5769435569603360651L;

    private UUID id;
    private String name;
    private String displayName;
    private String description;
    
    public LeaderboardGroupBaseImpl(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescriptiom(String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
