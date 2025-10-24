package com.sap.sailing.domain.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.tractracadapter.impl.DomainFactoryImpl;

public class DomainFactoryImplTest {
    private DomainFactoryImpl domainFactory;
    
    @BeforeEach
    public void setUp() {
        domainFactory  = new DomainFactoryImpl(DomainFactory.INSTANCE);        
    }
    
    
    @Test
    public void testGetDominantBoatClass_TestNullCollection() {
        Assertions.assertEquals(null, domainFactory.getDominantBoatClass(null));
    }
    
    @Test
    public void testGetDominantBoatClass_TestEmptyCollection() {
        List<String> emptyList = new ArrayList<>();
        Assertions.assertEquals(null, domainFactory.getDominantBoatClass(emptyList));
    }
    @Test
    public void testGetDominantBoatClass_TestWithOneObject() {
        List<String> list = new ArrayList<>();
        list.add("LSR");
        Assertions.assertEquals("Laser Int.", domainFactory.getDominantBoatClass(list).getName());
    }
    
    @Test
    public void testGetDominantBoatClass_TestWithTheSameCount() {
        List<String> list = new ArrayList<>();
        list.add("LSR");
        list.add("18.Footer");
        String result = domainFactory.getDominantBoatClass(list).getName();
        if(!(result == "Laser Int." || result == "18Footer")) {
            Assertions.assertEquals("Laser Int.", result);
        }
    }
    
    @Test
    public void testGetDominantBoatClass_TestWithMoreThanOneObject() {
        List<String> list = new ArrayList<>();
        list.add("LSR");
        list.add("18.Footer");
        list.add("LSR");
        String result = domainFactory.getDominantBoatClass(list).getName();
        Assertions.assertEquals("Laser Int.", result);
    }
    
    @Test
    public void testGetDominantBoatClass_TestWithSynonymNames() {
        List<String> list = new ArrayList<>();
        list.add("LSR");
        list.add("18.Footer");
        list.add("18.Footer");
        list.add("Laser Int.");
        list.add("Laser");
        String result = domainFactory.getDominantBoatClass(list).getName();
        Assertions.assertEquals("Laser Int.", result);
    }
}
