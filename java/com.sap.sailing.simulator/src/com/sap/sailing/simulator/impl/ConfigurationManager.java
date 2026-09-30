package com.sap.sailing.simulator.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.simulator.BoatClassProperties;
import com.sap.sailing.simulator.RaceProperties;
import com.sap.sse.common.impl.MeterDistance;

public enum ConfigurationManager {
    INSTANCE;

    private static final String ENVIRONMENT_VARIABLE_NAME = "STG_CONFIG";
    private static final String ENVIRONMENT_RACES_VARIABLE_NAME = "STG_CONFIG";

    private static final String CONFIG_FILE_LOCATION = "STG_configuration.csv";
    private static final String RACES_FILE_LOCATION = "races.csv";

    private List<BoatClassProperties> _boatClassesInfo = new ArrayList<>();
    private List<RaceProperties> _racesInfo = new ArrayList<>();

    private ReadingConfigurationFileStatus status = ReadingConfigurationFileStatus.SUCCESS;
    private String errorMessage = "";

    private ConfigurationManager() {
        this.initFromResources(ConfigurationManager.ENVIRONMENT_VARIABLE_NAME, ConfigurationManager.CONFIG_FILE_LOCATION, true);
        this.initFromResources(ConfigurationManager.ENVIRONMENT_RACES_VARIABLE_NAME, ConfigurationManager.RACES_FILE_LOCATION, false);
    }

    private void initFromResources(String envKeyName, String fileLocation, boolean polarDiagramConfig) {
        String configFileLocation = System.getenv(envKeyName);
        InputStream inputStream = null;
        try {
            if (configFileLocation == null || configFileLocation == "") {
                configFileLocation = fileLocation;
                inputStream = this.getClass().getClassLoader().getResourceAsStream(configFileLocation);
                this.status = ReadingConfigurationFileStatus.SUCCESS;
                this.errorMessage = "";
            } else if (new File(configFileLocation).exists()) {
                final URL csvFileURL = new URL("file:///" + configFileLocation);
                inputStream = csvFileURL.openStream();
                this.status = ReadingConfigurationFileStatus.SUCCESS;
                this.errorMessage = "";
            } else {
                configFileLocation = fileLocation;
                inputStream = this.getClass().getClassLoader().getResourceAsStream(configFileLocation);
                this.status = ReadingConfigurationFileStatus.ERROR_FINDING_CONFIG_FILE;
                this.errorMessage = "Invalid configuration file path ( " + configFileLocation + ")! Using default configuration values!";
            }
            final InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            final BufferedReader buffer = new BufferedReader(reader);
            String line = null;
            String[] elements = null;
            int index = 0;
            while (true) {
                line = buffer.readLine();
                if (line == null) {
                    break;
                }
                elements = line.split(",");
                if (polarDiagramConfig) {
                    this._boatClassesInfo
                    .add(new BoatClassPropertiesImpl(elements[0], new MeterDistance(Double.parseDouble(elements[1])), elements[2], index++));
                } else {
                    this._racesInfo.add(new RacePropertiesImpl(elements[0], elements[1], elements[2], index++));
                }
            }
            buffer.close();
            reader.close();
            inputStream.close();
        } catch (final IOException exception) {
            this.status = ReadingConfigurationFileStatus.IO_ERROR;
            this.errorMessage = "An IO error occured when parsing the configuration file ( " + fileLocation
                    + ")! The original error message is " + exception.getMessage();
        }
    }

    public List<BoatClassProperties> getBoatClassesInfo() {
        return this._boatClassesInfo;
    }

    public List<RaceProperties> getRacesInfo() {
        return this._racesInfo;
    }

    public int getRacesInfoCount() {
        return this._racesInfo.size();
    }

    public int getBoatClassesInfoCount() {
        return this._boatClassesInfo.size();
    }

    public String getRaceURL(int index) {
        return this._racesInfo.get(index).getURL();
    }

    /**
     * @param index relative to the list returned by {@link #getBoatClassesInfo()}, starting with 0
     */
    public String getPolarDiagramFileLocation(int index) {
        return this._boatClassesInfo.get(index).getPolar();
    }

    public ReadingConfigurationFileStatus getStatus() {
        return this.status;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}