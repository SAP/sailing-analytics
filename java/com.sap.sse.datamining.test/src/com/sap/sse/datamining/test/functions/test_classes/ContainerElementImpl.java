package com.sap.sse.datamining.test.functions.test_classes;

public class ContainerElementImpl implements ContainerElement {

    private String name;

    public ContainerElementImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
