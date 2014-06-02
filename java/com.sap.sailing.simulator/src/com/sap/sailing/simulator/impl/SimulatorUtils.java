package com.sap.sailing.simulator.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.osgi.framework.FrameworkUtil;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tractracadapter.TracTracAdapterFactory;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sailing.simulator.windfield.impl.WindFieldGeneratorMeasured;
import com.sap.sse.common.UtilNew;

@SuppressWarnings("restriction")
public class SimulatorUtils {

    private static final Logger LOGGER = Logger.getLogger("com.sap.sailing.simulator");

    public static final String LEGSNAMES_DAT = "legsNames.dat";
    public static final String RACECOURSE_DAT = "racecourse.dat";
    public static final String COMPETITORSNAMES_DAT = "competitorsNames.dat";

    public static final String[] PATH_NAMES = new String[] { "1#Omniscient", "2#Opportunistic", "3#1-Turner Left",
        "4#1-Turner Right", "6#GPS Poly", "7#GPS Track" };

    public static String pathPrefix = null;

    public static String getPathPrefix() {
        String bundleName = null;
        try {
            bundleName = FrameworkUtil.getBundle(Class.forName("com.sap.sailing.simulator.impl.SimulatorUtils"))
                    .getSymbolicName();
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR][SerializationUtils][getPathPrefix][ClassNotFoundException]  " + e.getMessage());
            LOGGER.severe("[ERROR][SerializationUtils][getPathPrefix][ClassNotFoundException]  " + e.getMessage());
            return null;
        }
        String bundlesProperty = System.getProperty("osgi.bundles");

        int bundleNameStart = bundlesProperty.indexOf(bundleName);
        int bundleNameEnd = bundleNameStart + bundleName.length();

        String prependedBundlePath = bundlesProperty.substring(0, bundleNameEnd);

        int prefixPos = prependedBundlePath.lastIndexOf("reference:file:");

        if (prefixPos >= 0) {
            prependedBundlePath = prependedBundlePath.substring(prefixPos + 15, prependedBundlePath.length());
        }

        return prependedBundlePath;
    }

    public static RacesHandle loadRace(RacingEventServiceImpl service, TracTracAdapterFactory tracTracAdapterFactory, URL paramURL, URI liveURI,
            URI storedURI, RaceLogStore raceLogStore, long timeoutInMilliseconds) throws Exception {
        //TODO: TracTrac Username / Password
        String tractracUsername = "";
        String tractracPassword = "";
        RacesHandle raceHandle = tracTracAdapterFactory.getOrCreateTracTracAdapter(service.getBaseDomainFactory())
                .addTracTracRace(service, paramURL, liveURI, storedURI, null, raceLogStore,
                        timeoutInMilliseconds, tractracUsername, tractracPassword, TracTracConnectionConstants.ONLINE_STATUS);
        return raceHandle;
    }

    public static boolean savePathsToFiles(Map<String, Path> paths, Path raceCourse) {
        if (paths == null) {
            return false;
        }

        if (paths.isEmpty()) {
            return true;
        }

        if (pathPrefix == null) {
            pathPrefix = getPathPrefix();
        }

        String filePath = "";
        boolean result = true;

        for (String name : PATH_NAMES) {
            filePath = pathPrefix + "\\resources\\" + name + ".dat";
            result &= saveToFile(paths.get(name), filePath);
        }

        filePath = pathPrefix + "\\resources\\" + RACECOURSE_DAT;
        result &= saveToFile(raceCourse, filePath);

        return result;
    }

    public static boolean saveLegPathsToFiles(Map<String, Path> paths, Path raceCourse, int selectedRaceIndex,
            int selectedCompetitorIndex, int selectedLegIndex) {
        if (paths == null) {
            return false;
        }

        if (paths.isEmpty()) {
            return true;
        }

        if (pathPrefix == null) {
            pathPrefix = getPathPrefix();
        }

        String filePath = "";
        String fileName = "";
        boolean result = true;

        for (String name : PATH_NAMES) {

            fileName = SimulatorUtils.getFileName(selectedRaceIndex, selectedCompetitorIndex, selectedLegIndex,
                    name);
            filePath = pathPrefix + "\\resources\\" + fileName;
            result &= saveToFile(paths.get(name), filePath);
        }

        fileName = SimulatorUtils.getFileName(selectedRaceIndex, selectedCompetitorIndex, selectedLegIndex,
                "racecourse");
        filePath = pathPrefix + "\\resources\\" + fileName;
        result &= saveToFile(raceCourse, filePath);

        return result;
    }

    public static boolean saveToFile(Path path, String fileName) {
        boolean result = true;
        try {
            OutputStream file = new FileOutputStream(fileName);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);

            try {
                output.writeObject(path);
            } finally {
                output.close();
                buffer.close();
                file.close();
            }
        } catch (IOException ex) {
            System.err.println("[ERROR][SerializationUtils][saveToFile][IOException]  " + ex.getMessage());
            LOGGER.severe("[ERROR][SerializationUtils][saveToFile][IOException]  " + ex.getMessage());
            result = false;
        }

        return result;
    }

    public static UtilNew.Pair<Map<String, Path>, Path> readLegPathsFromResources(int selectedRaceIndex,
            int selectedCompetitorIndex, int selectedLegIndex) {
        HashMap<String, Path> paths = new HashMap<String, Path>();

        Path path = null;
        String filePath = "";
        String fileName = "";
        for (String pathName : PATH_NAMES) {

            fileName = SimulatorUtils.getFileName(selectedRaceIndex, selectedCompetitorIndex, selectedLegIndex,
                    pathName);
            filePath = fileName;

            path = (Path) readObjectFromResources(filePath);
            if (path == null) {
                System.err.println("[ERROR][SerializationUtils][readPathsFromResources] Cannot de-serialize path from"
                        + pathName);
                LOGGER.severe("[ERROR][SerializationUtils][readPathsFromResources] Cannot de-serialize path from"
                        + pathName);
            } else {
                paths.put(pathName, path);
            }
        }

        Path raceCourse = (Path) readObjectFromResources("resources/"
                + SimulatorUtils.getFileName(selectedRaceIndex, selectedCompetitorIndex, selectedLegIndex,
                        "racecourse"));

        return new UtilNew.Pair<Map<String, Path>, Path>(paths, raceCourse);
    }

    public static UtilNew.Pair<Map<String, Path>, Path> readPathsFromResources() {
        HashMap<String, Path> paths = new HashMap<String, Path>();
        Path path = null;
        String filePath = "";

        for (String pathName : PATH_NAMES) {
            filePath = "resources/" + pathName + ".dat";
            path = (Path) readObjectFromResources(filePath);
            if (path == null) {
                System.err.println("[ERROR][SerializationUtils][readPathsFromResources] Cannot de-serialize path from"
                        + pathName);
                LOGGER.severe("[ERROR][SerializationUtils][readPathsFromResources] Cannot de-serialize path from"
                        + pathName);
            } else {
                paths.put(pathName, path);
            }
        }

        Path raceCourse = (Path) readObjectFromResources("resources/" + RACECOURSE_DAT);

        return new UtilNew.Pair<Map<String, Path>, Path>(paths, raceCourse);
    }

    public static Object readObjectExternalFile(String fileName) {
        Object result = null;
        try {
            InputStream file = new FileInputStream(fileName);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);

            try {
                result = input.readObject();
            } finally {
                input.close();
                buffer.close();
                file.close();
            }
        } catch (ClassNotFoundException ex) {
            System.err.println("[ERROR][SerializationUtils][readFromExternalFile][ClassNotFoundException] "
                    + ex.getMessage());
            LOGGER.severe("[ERROR][SerializationUtils][readFromExternalFile][ClassNotFoundException] "
                    + ex.getMessage());
            result = null;
        } catch (IOException ex) {
            System.err.println("[ERROR][SerializationUtils][readFromExternalFile][IOException]  " + ex.getMessage());
            LOGGER.severe("[ERROR][SerializationUtils][readFromExternalFile][IOException]  " + ex.getMessage());
            result = null;
        }

        return result;
    }

    public static Object readObjectFromResources(String fileName) {
        Object result = null;

        try {
            ClassLoader classLoader = Class.forName("com.sap.sailing.simulator.impl.SimulatorUtils").getClassLoader();
            InputStream file = classLoader.getResourceAsStream(fileName);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);

            try {
                result = input.readObject();
            } finally {
                input.close();
                buffer.close();
                file.close();
            }
        } catch (ClassNotFoundException ex) {
            System.err.println("[ERROR][SerializationUtils][readFromResourcesFile][ClassNotFoundException] "
                    + ex.getMessage());
            LOGGER.severe("[ERROR][SerializationUtils][readFromResourcesFile][ClassNotFoundException] "
                    + ex.getMessage());
            result = null;
        } catch (IOException ex) {
            System.err.println("[ERROR][SerializationUtils][readFromResourcesFile][IOException]  " + ex.getMessage());
            LOGGER.severe("[ERROR][SerializationUtils][readFromResourcesFile][IOException]  " + ex.getMessage());
            result = null;
        }

        return result;
    }

    public static boolean saveStringListToFiles(List<String> legsNames, String fileName) {
        if (legsNames == null) {
            return false;
        }

        if (legsNames.isEmpty()) {
            return true;
        }

        if (pathPrefix == null) {
            pathPrefix = getPathPrefix();
        }

        String filePath = pathPrefix + "\\resources\\" + fileName;

        boolean result = true;

        try {
            OutputStream file = new FileOutputStream(filePath);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);

            try {
                output.writeObject(legsNames);
            } finally {
                output.close();
                buffer.close();
                file.close();
            }
        } catch (IOException ex) {
            System.err.println("[ERROR][SerializationUtils][saveLegsNamesToFiles][IOException]  " + ex.getMessage());
            LOGGER.severe("[ERROR][SerializationUtils][saveLegsNamesToFiles][IOException]  " + ex.getMessage());
            result = false;
        }

        return result;
    }

    public static String getRaceID(String raceURL) {

        String result = null;

        if (raceURL.contains("&race=")) {
            String[] parts = raceURL.split("&");
            for (String part : parts) {
                if (part.startsWith("race=")) {
                    result = part.replace("race=", "");
                }
            }
        } else if (raceURL.contains("?race=")) {
            String[] parts = raceURL.split("?");
            for (String part : parts) {
                if (part.startsWith("race=")) {
                    result = part.replace("race=", "");
                }
            }
        }

        return result;
    }

    public static String getFileName(int selectedRaceIndex, int selectedCompetitorIndex, int selectedLegIndex,
            String pathName) {
        return SimulatorUtils.getRaceID(ConfigurationManager.INSTANCE.getRaceURL(selectedRaceIndex)) + "_"
                + selectedCompetitorIndex + "_" + selectedLegIndex + "_" + pathName + ".dat";
    }

    public static Map<String, Path> getSimulationPaths(SimulationParameters parameters, Path gpsPath, Path raceCourse) {

        //
        // Initialize WindFields boundary
        //
        WindFieldGenerator wf = parameters.getWindField();
        // int[] gridRes = wf.getGridResolution();
        Position[] gridArea = wf.getGridAreaGps();
        if (parameters.getMode() == SailingSimulatorConstants.ModeMeasured) {
            ((WindFieldGeneratorMeasured) wf).setGPSWind(gpsPath);
            gridArea = new Position[2];
            gridArea[0] = raceCourse.getPathPoints().get(0).getPosition();
            gridArea[1] = raceCourse.getPathPoints().get(1).getPosition();
            List<Position> course = new ArrayList<Position>();
            course.add(gridArea[0]);
            course.add(gridArea[1]);
            parameters.setCourse(course);
        }

        Map<String, Path> paths = new HashMap<String, Path>();

        // get instance of heuristic searcher
        PathGeneratorTreeGrowWind3 genTreeGrow = new PathGeneratorTreeGrowWind3(parameters);

        // search best left-starting 1-turner
        genTreeGrow.setEvaluationParameters("L", 1, null);

        Path leftPath = genTreeGrow.getPath();
        PathCandidate leftBestCand = genTreeGrow.getBestCand();
        int left1TurnMiddle = 1000;
        if (leftBestCand != null) {
            left1TurnMiddle = leftBestCand.path.indexOf("LR");
        }

        // search best right-starting 1-turner
        genTreeGrow.setEvaluationParameters("R", 1, null);

        Path rightPath = genTreeGrow.getPath();
        PathCandidate rightBestCand = genTreeGrow.getBestCand();
        int right1TurnMiddle = 1000;
        if (rightBestCand != null) {
            right1TurnMiddle = rightBestCand.path.indexOf("RL");
        }

        // search best multi-turn course
        genTreeGrow.setEvaluationParameters(null, 0, null);

        Path optPath = genTreeGrow.getPath();

        // evaluate opportunistic heuristic
        PathGeneratorOpportunistEuclidian genOpportunistic = new PathGeneratorOpportunistEuclidian(parameters);

        // left-starting opportunist
        genOpportunistic.setEvaluationParameters(left1TurnMiddle, right1TurnMiddle, true);
        Path oppPathL = genOpportunistic.getPath();

        // right-starting opportunist
        genOpportunistic.setEvaluationParameters(left1TurnMiddle, right1TurnMiddle, false);
        Path oppPathR = genOpportunistic.getPath();

        // compare left- & right-starting opportunists
        Path oppPath = null;

        TimedPositionWithSpeed lastPathLPoint = oppPathL.getPathPoints().get(oppPathL.getPathPoints().size() - 1);
        TimedPositionWithSpeed lastPathRPoint = oppPathR.getPathPoints().get(oppPathR.getPathPoints().size() - 1);

        if (lastPathLPoint.getTimePoint().asMillis() <= lastPathRPoint.getTimePoint().asMillis()) {
            oppPath = oppPathL;
        } else {
            oppPath = oppPathR;
        }

        //
        // NOTE: pathName convention is: sort-digit + "#" + path-name
        // The sort-digit defines the sorting of paths in webbrowser
        //

        paths.put("4#1-Turner Right", rightPath);
        paths.put("3#1-Turner Left", leftPath);
        paths.put("2#Opportunistic", oppPath);
        paths.put("1#Omniscient", optPath);

        return paths;
    }

}
