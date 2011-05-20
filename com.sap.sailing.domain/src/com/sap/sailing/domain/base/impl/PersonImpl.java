package com.sap.sailing.domain.base.impl;

import java.io.InputStream;
import java.util.Date;

import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Person;

public class PersonImpl extends NamedImpl implements Person {
    private final Nationality nationality;
    private final Date dateOfBirth;
    private final String description;
    
    public PersonImpl(String name, Nationality nationality, Date dateOfBirth, String description) {
        super(name);
        this.nationality = nationality;
        this.dateOfBirth = dateOfBirth;
        this.description = description;
    }

    @Override
    public InputStream getImage() {
        throw new UnsupportedOperationException("Image for person not yet implemented");
    }

    @Override
    public Nationality getNationality() {
        return nationality;
    }

    @Override
    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
