package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.NationalityChangeListener;
import com.sap.sailing.domain.base.Person;
import com.sap.sailing.domain.base.WithNationality;
import com.sap.sailing.domain.common.impl.NamedImpl;
import com.sap.sailing.domain.common.impl.Util;

public class TeamImpl extends NamedImpl implements DynamicTeam {
    private static final long serialVersionUID = 4646922280429210183L;
    private final Iterable<? extends DynamicPerson> sailors;
    private final DynamicPerson coach;
    private final NationalityChangeListener personNationalityChangeForwarder;
    private transient Set<NationalityChangeListener> listeners;
    
    public TeamImpl(String name, Iterable<? extends DynamicPerson> sailors, DynamicPerson coach) {
        super(name);
        listeners = new HashSet<NationalityChangeListener>();
        this.sailors = sailors;
        this.coach = coach;
        this.personNationalityChangeForwarder = new NationalityChangeListener() {
            private static final long serialVersionUID = -6024941913105410444L;
            @Override
            public void nationalityChanged(WithNationality what, Nationality oldNationality, Nationality newNationality) {
                if (what == getNationalityDonor()) {
                    for (NationalityChangeListener listener : getNationalityChangeListeners()) {
                        listener.nationalityChanged(TeamImpl.this, oldNationality, newNationality);
                    }
                }
            }
        };
        setTransitiveListeners();
    }

    /**
     * The listeners on {@link DynamicPerson} are transient. Our {@link #personNationalityChangeForwarder} needs to
     * be restored after deserialization.
     */
    private void setTransitiveListeners() {
        if (this.sailors != null) {
            for (DynamicPerson sailor : this.sailors) {
                sailor.addNationalityChangeListener(personNationalityChangeForwarder);
            }
        }
        if (this.coach != null) {
            this.coach.addNationalityChangeListener(personNationalityChangeForwarder);
        }
    }
    
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        listeners = new HashSet<NationalityChangeListener>();
        setTransitiveListeners();
    }
    
    public void addNationalityChangeListener(NationalityChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    public void removeNationalityChangeListener(NationalityChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    private Iterable<NationalityChangeListener> getNationalityChangeListeners() {
        synchronized (listeners) {
            return new HashSet<NationalityChangeListener>(listeners);
        }
    }

    @Override
    public Iterable<? extends Person> getSailors() {
        return sailors;
    }
    
    @Override
    public DynamicPerson getCoach() {
        return coach;
    }

    @Override
    public Nationality getNationality() {
        return getNationalityDonor() == null ? null : getNationalityDonor().getNationality();
    }
    
    private WithNationality getNationalityDonor() {
        for (Person sailor : getSailors()) {
            if (sailor.getNationality() != null) {
                return sailor;
            }
        }
        if (getCoach() != null) {
            return getCoach();
        }
        return null;
    }

    @Override
    public void setNationality(Nationality newNationality) {
        Nationality oldNationality = getNationality();
        if (!Util.equalsWithNull(oldNationality, newNationality)) {
            for (Person sailor : getSailors()) {
                ((DynamicPerson) sailor).setNationality(newNationality);
            }
            if (getCoach() != null) {
                getCoach().setNationality(newNationality);
            }
            for (NationalityChangeListener listener : getNationalityChangeListeners()) {
                listener.nationalityChanged(this, oldNationality, newNationality);
            }
        }
    }
    
}
