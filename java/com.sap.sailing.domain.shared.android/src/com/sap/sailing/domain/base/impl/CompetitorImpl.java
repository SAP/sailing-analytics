package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorChangeListener;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;

public class CompetitorImpl implements DynamicCompetitor {
    private static final long serialVersionUID = 294603681016643157L;
    private final DynamicTeam team;
    private final DynamicBoat boat;
    private final Serializable id;
    private String name;
    private Color color;
    private transient Set<CompetitorChangeListener> listeners;
    private String email;
    private URI flagImage;
    private Double timeOnTimeFactor;
    private Duration timeOnDistanceAllowancePerNauticalMile;
    
    public CompetitorImpl(Serializable id, String name, Color color, String email, URI flagImage, DynamicTeam team, DynamicBoat boat) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.boat = boat;
        this.color = color;
        this.email = email;
        this.flagImage = flagImage;
        this.listeners = new HashSet<CompetitorChangeListener>();
    }
    
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        listeners = new HashSet<CompetitorChangeListener>();
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public void setName(String newName) {
        final String oldName = this.name;
        if (!Util.equalsWithNull(oldName, newName)) {
            this.name = newName;
            for (CompetitorChangeListener listener : getListeners()) {
                listener.nameChanged(oldName, newName);
            }
        }
    }
    
    @Override
    public Serializable getId() {
        return id;
    }
    
    @Override
    public DynamicTeam getTeam() {
        return team;
    }

    @Override
    public DynamicBoat getBoat() {
        return boat;
    }

    @Override
    public Competitor resolve(SharedDomainFactory domainFactory) {
        Competitor result = domainFactory.getOrCreateCompetitor(getId(), getName(), getColor(), getEmail(), getFlagImage(), getTeam(), getBoat());
        return result;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        final Color oldColor = this.color;
        if (!Util.equalsWithNull(oldColor, color)) {
            this.color = color;
            for (CompetitorChangeListener listener : getListeners()) {
                listener.colorChanged(oldColor, color);
            }
        }
    }

    @Override
    public void addCompetitorChangeListener(CompetitorChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        getBoat().addBoatChangeListener(listener);
        getTeam().addNationalityChangeListener(listener);
    }

    @Override
    public void removeCompetitorChangeListener(CompetitorChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
        getBoat().removeCompetitorChangeListener(listener);
        getTeam().removeNationalityChangeListener(listener);
    }
    
    private Iterable<CompetitorChangeListener> getListeners() {
        synchronized (listeners) {
            return new HashSet<CompetitorChangeListener>(listeners);
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String newEmail) {
        final String oldEmail = this.email;
        if (!Util.equalsWithNull(oldEmail, newEmail)) {
            this.email = newEmail;
            for (CompetitorChangeListener listener : getListeners()) {
                listener.emailChanged(oldEmail, newEmail);
            }
        }
    }
    
    public boolean hasEmail(){
        return email != null && !email.isEmpty();
    }

    @Override
    public URI getFlagImage() {
        return flagImage;
    }

    @Override
    public void setFlagImage(URI flagImage) {
        final URI oldFlagImage = this.flagImage;
        this.flagImage = flagImage;
        if (!Util.equalsWithNull(oldFlagImage, flagImage)) {
            for (CompetitorChangeListener listener : getListeners()) {
                listener.flagImageChanged(oldFlagImage, flagImage);
            }
        }
    }

    @Override
    public Double getTimeOnTimeFactor() {
        return timeOnTimeFactor;
    }

    @Override
    public Duration getTimeOnDistanceAllowancePerNauticalMile() {
        return timeOnDistanceAllowancePerNauticalMile;
    }

    @Override
    public void setTimeOnTimeFactor(Double timeOnTimeFactor) {
        Double oldTimeOnTimeFactor = this.timeOnTimeFactor;
        this.timeOnTimeFactor = timeOnTimeFactor;
        if (!Util.equalsWithNull(oldTimeOnTimeFactor, timeOnTimeFactor)) {
            for (CompetitorChangeListener listener : getListeners()) {
                listener.timeOnTimeFactorChanged(oldTimeOnTimeFactor, timeOnTimeFactor);
            }
        }
    }

    @Override
    public void setTimeOnDistanceAllowancePerNauticalMile(Duration timeOnDistanceAllowancePerNauticalMile) {
        Duration oldTimeOnDistanceAllowancePerNauticalMile = this.timeOnDistanceAllowancePerNauticalMile;
        this.timeOnDistanceAllowancePerNauticalMile = timeOnDistanceAllowancePerNauticalMile;
        if (!Util.equalsWithNull(oldTimeOnDistanceAllowancePerNauticalMile, timeOnDistanceAllowancePerNauticalMile)) {
            for (CompetitorChangeListener listener : getListeners()) {
                listener.timeOnDistanceAllowancePerNauticalMileChanged(oldTimeOnDistanceAllowancePerNauticalMile, timeOnDistanceAllowancePerNauticalMile);
            }
        }
    }

}
