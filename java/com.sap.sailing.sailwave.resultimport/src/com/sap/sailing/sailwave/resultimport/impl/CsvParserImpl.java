package com.sap.sailing.sailwave.resultimport.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sap.sailing.resultimport.CompetitorEntry;
import com.sap.sailing.resultimport.CompetitorRow;
import com.sap.sailing.resultimport.RegattaResults;
import com.sap.sailing.resultimport.impl.CompetitorRowImpl;
import com.sap.sailing.resultimport.impl.DefaultCompetitorEntryImpl;
import com.sap.sailing.sailwave.resultimport.CsvParser;
import com.sap.sse.common.TimePoint;

/**
 * Parses CSV files. The input stream passed to the constructor is expected to be encoded with the "cp1252"
 * character set.
 */
public class CsvParserImpl implements CsvParser {

    private final InputStream inputStream;
    private final String filename;
    private final TimePoint lastModified;

    private final String SEPARATOR = ";";

    public CsvParserImpl(InputStream inputStream, String filename, TimePoint lastModified) {
        this.inputStream = inputStream;
        this.filename = filename;
        this.lastModified = lastModified;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getBoatClass() {
        String result = "Unknown Boatclass";
        // we assume the boat class name is in the filename
        int underScoreIndex = filename.lastIndexOf("_");
        if (underScoreIndex > 0) {
            result = filename.substring(0, underScoreIndex);
        }

        return result;
    }

    @Override
    public RegattaResults parseResults() throws Exception {
        final Pattern totalRankPattern = Pattern.compile("([0-9]+)((st)|(nd)|(rd)|(th))?");
        final List<CompetitorRow> competitorRows = new ArrayList<>();

        // Sample line for a couple of races
        // Rank;Nat;SailNo;Club;HelmName;CrewName;Q1;Q2;Q3;Q4;Q5;Q6;F1;F2;F3;F4;F5;F6;F7;F8;F9;Total;Nett;
        // 1;NZL;6;Victoria Cruising Club;Marcus Hansen;Josh Porebski;4;(22);2;1;1;2;;;;;;;;;;32;10;

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "cp1252"));
        String line = br.readLine();
        int startIndexForRacePoints = -1;
        int endIndexForRacePoints = -1;
        while (line != null) {
            int firstSeparator = line.indexOf(SEPARATOR);
            if (line.startsWith("DNQ")) {
                // ignore line
            } else if (firstSeparator > 0) {
                String firstEntry = line.substring(0, firstSeparator);
                if (firstEntry.startsWith("Rank")) {
                    // header line
                    // scan the number of races
                    String[] splittedHeadline = line.split(SEPARATOR);
                    for (int i = 0; i < splittedHeadline.length; i++) {
                        // find index of 'CrewName'
                        if (splittedHeadline[i].equals("CrewName")) {
                            startIndexForRacePoints = i + 1;
                        } else if (splittedHeadline[i].equals("Total")) {
                            endIndexForRacePoints = i - 1;
                        }
                    }
                } else {
                    // check for valid race row; SailWave supports two ways of representing the rank: either as
                    // 1st, 2nd, 3rd, 4th, ... or as 1, 2, 3, 4, ... We try to handle both:
                    final Matcher totalRankMatcher = totalRankPattern.matcher(firstEntry);
                    if (totalRankMatcher.matches()) {
                        String[] splittedRow = line.split(SEPARATOR);
                        String sailID = splittedRow[1] + " " + splittedRow[2];

                        Integer totalRank = Integer.parseInt(totalRankMatcher.group(1));
                        List<String> names = new ArrayList<String>();
                        List<CompetitorEntry> rankAndMaxPointsReasonAndPointsAndDiscarded = new ArrayList<>();

                        names.add(splittedRow[4]);
                        names.add(splittedRow[5]);

                        int index = startIndexForRacePoints;
                        while (index <= endIndexForRacePoints) {
                            String pointsAndMaxPointsReasonAndDiscard = splittedRow[index];
                            if (pointsAndMaxPointsReasonAndDiscard != null
                                    && !pointsAndMaxPointsReasonAndDiscard.isEmpty()) {
                                Integer rank = null;
                                String maxPointsReason;
                                Double points;
                                boolean discarded = false;

                                // sample for points and maxPointReason and discard in one field: (32,DNF)
                                if (pointsAndMaxPointsReasonAndDiscard.startsWith("(")
                                        && pointsAndMaxPointsReasonAndDiscard.endsWith(")")) {
                                    pointsAndMaxPointsReasonAndDiscard = pointsAndMaxPointsReasonAndDiscard.substring(1,
                                            pointsAndMaxPointsReasonAndDiscard.length() - 1);
                                    discarded = true;
                                }
                                if (pointsAndMaxPointsReasonAndDiscard.indexOf(' ') >= 0 || pointsAndMaxPointsReasonAndDiscard.indexOf(',') >= 0) {
                                    String[] splittedPointsAndMaxPointReason = pointsAndMaxPointsReasonAndDiscard.split(",| ");
                                    points = Double.valueOf(splittedPointsAndMaxPointReason[0]);
                                    maxPointsReason = splittedPointsAndMaxPointReason[1];
                                } else {
                                    points = Double.valueOf(pointsAndMaxPointsReasonAndDiscard);
                                    maxPointsReason = null;
                                }

                                if (points != 0.0) {
                                    CompetitorEntry entry = new DefaultCompetitorEntryImpl(rank, maxPointsReason,
                                            points, discarded);
                                    rankAndMaxPointsReasonAndPointsAndDiscarded.add(entry);
                                } else {
                                    rankAndMaxPointsReasonAndPointsAndDiscarded.add(null);
                                }
                            }
                            index++;
                        }
                        CompetitorRow competitorRow = new CompetitorRowImpl(totalRank, sailID, names, null, null,
                                rankAndMaxPointsReasonAndPointsAndDiscarded);
                        competitorRows.add(competitorRow);
                    }
                }
            } else {
                // probably last line
            }
            line = br.readLine();
        }
        return new RegattaResults() {
            @Override
            public Map<String, String> getMetadata() {
                Map<String, String> result = new HashMap<>();
                return result;
            }

            @Override
            public List<CompetitorRow> getCompetitorResults() {
                return competitorRows;
            }
        };
    }

    public TimePoint getLastModified() {
        return lastModified;
    }
}
