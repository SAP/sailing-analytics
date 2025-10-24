package com.sap.sailing.freg.resultimport.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sap.sailing.resultimport.CompetitorEntry;
import com.sap.sailing.resultimport.CompetitorRow;
import com.sap.sailing.resultimport.RegattaResults;
import com.sap.sailing.resultimport.impl.CompetitorRowImpl;
import com.sap.sailing.resultimport.impl.DefaultCompetitorEntryImpl;

public class FregHtmlParser {
    private static final Logger logger = Logger.getLogger(FregHtmlParser.class.getName());
    
    public List<String> getRowContents(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        Pattern tr = Pattern.compile("<(tr|TR)([^<>]*)>");
        Pattern slashTr = Pattern.compile("</(tr|TR)([^<>]*)>");
        boolean inTr = false;
        StringBuilder trContents = new StringBuilder();
        List<String> tableRows = new ArrayList<String>(); // the contents of the <tr> tags found
        String readLine = br.readLine();
        int start = 0;
        int end = 0;
        while (readLine != null) {
            if (!inTr) {
                final Matcher trMatcher = tr.matcher(readLine);
                boolean foundTr = trMatcher.find(end);
                if (foundTr) {
                    start = trMatcher.end(0);
                    inTr = true;
                } else {
                    readLine = br.readLine();
                    end = 0;
                }
            } else { // we are within the contents of a <tr> tag here
                final Matcher slashTrMatcher = slashTr.matcher(readLine);
                boolean foundSlashTr = slashTrMatcher.find(start);
                if (foundSlashTr) {
                    end = slashTrMatcher.start();
                    inTr = false;
                } else {
                    end = readLine.length();
                }
                trContents.append(readLine.substring(start, end));
                if (!foundSlashTr) {
                    trContents.append('\n'); 
                    readLine = br.readLine();
                    start = 0;
                } else {
                    tableRows.add(trContents.toString());
                    trContents.delete(0, trContents.length());
                }
            }
        }
        return tableRows;
    }

    public List<String> getTagContents(String s, String tag) {
        Pattern tagPattern = Pattern.compile("<("+tag.toLowerCase()+"|"+tag.toUpperCase()+")([^<>]*)>");
        Pattern slashTag = Pattern.compile("</("+tag.toLowerCase()+"|"+tag.toUpperCase()+")([^<>]*)>");
        boolean inTag = false;
        List<String> result = new ArrayList<String>(); // the contents of the <td> tags found
        int start = 0;
        int end = 0;
        boolean finished = false;
        while (!finished) {
            if (!inTag) {
                final Matcher tagMatcher = tagPattern.matcher(s);
                boolean foundTag = tagMatcher.find(end);
                if (foundTag) {
                    start = tagMatcher.end(0);
                    inTag = true;
                } else {
                    finished = true;
                }
            } else { // we are within the contents of a <tr> tag here
                final Matcher slashTagMatcher = slashTag.matcher(s);
                boolean foundSlashTag = slashTagMatcher.find(start);
                if (foundSlashTag) {
                    end = slashTagMatcher.start();
                    inTag = false;
                    result.add(s.substring(start, end).trim());
                } else {
                    result.add(s.substring(start).trim());
                    finished = true;
                    logger.warning("unclosed "+tag+" tag in string \""+s+"\"");
                }
            }
        }
        return result;
    }
    
    /**
     * @param is closed before the method returns, also in case of exception
     * @throws IOException 
     */
    public RegattaResults getRegattaResults(InputStream is) throws IOException {
        try {
            final List<CompetitorRow> result = new ArrayList<CompetitorRow>();
            List<String> rowContents = getRowContents(is);
            is.close();
            final Map<String, String> metadata = new HashMap<String, String>();
            List<String> tagContents = getTagContents(rowContents.get(0), "b"); // header information like regatta name and dates are all in <B> tags
            int j = 1;
            for (String metaInfo : tagContents) {
                metadata.put("info" + j++, metaInfo);
			}
            for (int i = 2; i < rowContents.size() - 1; i++) {
                List<String> tdContent = getTagContents(rowContents.get(i), "td");
                result.add(createCompetitorRow(tdContent));
            }
            return new RegattaResults() {
                @Override
                public Map<String, String> getMetadata() {
                    return metadata;
                }
                @Override
                public List<CompetitorRow> getCompetitorResults() {
                    return result;
                }
            };
        } finally {
            is.close();
        }
    }

    private CompetitorRow createCompetitorRow(List<String> tdContent) {
        List<String> bold = getTagContents(tdContent.get(0), "b");
        Integer totalRank;
        if (bold != null && !bold.isEmpty()) {
            totalRank = getInt(bold.get(0).trim());
        } else {
            totalRank = getInt(tdContent.get(0).trim());
        }
        String sailID = getSailID(tdContent.get(1).trim()).replace("&nbsp;", " ").trim();
        List<String> names = getNames(tdContent.get(2).trim());
        Double scoreAfterDiscarding = getScore(tdContent.get(3).trim());
        Double totalPointsBeforeDiscarding = getScore(tdContent.get(4).trim());
        List<CompetitorEntry> rankAndMaxPointsReasonAndPointsAndDiscarded = new ArrayList<CompetitorEntry>();
        for (int i=5; i<tdContent.size()-1; i++) {
            CompetitorEntry rankAndMaxPointsReasonAndPointsAndDiscardedForOnceRace =
                    getRankAndMaxPointsReasonAndPointsAndDiscardedForOnceRace(tdContent.get(i).trim());
            rankAndMaxPointsReasonAndPointsAndDiscarded.add(rankAndMaxPointsReasonAndPointsAndDiscardedForOnceRace);
        }
        return new CompetitorRowImpl(totalRank, sailID, names, scoreAfterDiscarding, totalPointsBeforeDiscarding,
                rankAndMaxPointsReasonAndPointsAndDiscarded);
    }

    private CompetitorEntry getRankAndMaxPointsReasonAndPointsAndDiscardedForOnceRace(
            String cell) {
        boolean isDiscarded;
        String maxPointsReason;
        String results;
        cell = cell.replace("&nbsp;", "");
        List<String> discarded = getTagContents(cell, "strike");
        if (discarded.isEmpty()) {
            List<String> colorized = getTagContents(cell, "span"); // could be a background-color-styled span for 1st and 2nd rank
            if (colorized.isEmpty()) {
                results = cell;
            } else {
                results = colorized.get(0);
            }
            isDiscarded = false;
        } else {
            results = discarded.get(0);
            isDiscarded = true;
        }
        String[] lines = results.split("<(br|BR)>");
        Integer rank;
        try {
            rank = Integer.valueOf(lines[0]);
            maxPointsReason = null;
        } catch (NumberFormatException nfe) {
            // must have been a disqualification / max-points-reason
            rank = null;
            if (lines.length==0 || lines[0].trim().length() == 0 || lines[0].trim().contains("--")) {
                maxPointsReason = "DNC";
            } else {
                maxPointsReason = lines[0];
            }
        }
        Double points = lines.length < 2 ? null : Double.valueOf(getTagContents(lines[1], "i").get(0));
        return new DefaultCompetitorEntryImpl(rank, maxPointsReason, points, isDiscarded);
    }

    private Double getScore(String cell) {
        String scoreAsString;
        List<String> boldScore = getTagContents(cell, "b");
        if (boldScore.isEmpty()) {
            scoreAsString = cell.trim();
        } else {
            scoreAsString = boldScore.get(0);
        }
        return Double.valueOf(scoreAsString);
    }

    private List<String> getNames(String cell) {
        String pContents;
        List<String> pContentsList = getTagContents(cell, "p");
        if (pContentsList.isEmpty()) {
            pContents = cell;
        } else {
            pContents = pContentsList.get(0);
        }
        List<String> names = getTagContents(pContents, "span");
        List<String> result = new ArrayList<String>(names.size());
        for (String name : names) {
            result.add(name.replace("&nbsp;", " ").trim());
        }
        return result;
    }

    private String getSailID(String cell) {
        String imgAndSailID = getTagContents(cell, "p").get(0);
        String sailID = getTagContents(imgAndSailID+"</img>", "img").get(0); // closing img may be missing
        return sailID;
    }

    private Integer getInt(String cell) {
        Integer result;
        try {
            result = Integer.valueOf(cell);
        } catch (NumberFormatException nfe) {
            // probably there is a <b> </b> tag combination around the number
            List<String> contents = getTagContents(cell, "b");
            if (!contents.isEmpty()) {
                result = Integer.valueOf(contents.get(0));
            } else {
                result = null;
            }
        }
        return result;
    }
}
