package com.sap.sailing.gwt.ui.datamining;

public class QueryBenchmarkResult {
    
    private String identifier;
    private int numberOfGPSFixes;
    private double serverTime;
    private double overallTime;
    
    public QueryBenchmarkResult(String identifier, int numberOfGPSFixes, double serverTime, double overallTime) {
        this.identifier = identifier;
        this.numberOfGPSFixes = numberOfGPSFixes;
        this.serverTime = serverTime;
        this.overallTime = overallTime;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getNumberOfGPSFixes() {
        return numberOfGPSFixes;
    }
    
    public double getServerTime() {
        return serverTime;
    }
    
    public double getOverallTime() {
        return overallTime;
    }

}
