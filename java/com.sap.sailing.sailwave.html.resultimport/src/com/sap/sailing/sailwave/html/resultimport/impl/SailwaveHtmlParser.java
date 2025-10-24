package com.sap.sailing.sailwave.html.resultimport.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import com.sap.sailing.resultimport.CompetitorEntry;
import com.sap.sailing.resultimport.CompetitorRow;
import com.sap.sailing.resultimport.RegattaResults;
import com.sap.sailing.resultimport.impl.CompetitorRowImpl;
import com.sap.sailing.resultimport.impl.DefaultCompetitorEntryImpl;

public class SailwaveHtmlParser {
    static final String CLASS_METADATA = "class";
    
    private static final Logger logger = Logger.getLogger(SailwaveHtmlParser.class.getName());
    
    public List<String> getRowContents(BufferedReader br) throws IOException {
        Pattern tr = Pattern.compile("<(tr|TR)([^<>]*)>");
        Pattern slashTr = Pattern.compile("</(tr|TR)([^<>]*)>");
        boolean inTr = false;
        final StringBuilder trContents = new StringBuilder();
        final List<String> tableRows = new ArrayList<String>(); // the contents of the <tr> tags found
        String readLine = br.readLine();
        int start = 0;
        int end = 0;
        while (readLine != null && !readLine.contains("</tbody>")) {
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
                    result.add(StringEscapeUtils.unescapeHtml(s.substring(start, end).trim()));
                } else {
                    result.add(StringEscapeUtils.unescapeHtml(s.substring(start).trim()));
                    finished = true;
                    logger.warning("unclosed "+tag+" tag in string \""+s+"\"");
                }
            }
        }
        return result;
    }
    
    /**
     * @param is closed before the method returns, also in case of exception
     */
    public RegattaResults getRegattaResults(InputStream is) throws IOException {
        try {
            Map<String, Integer> classesCounts = new HashMap<>(); // counts the occurrences of boat class names
            final List<CompetitorRow> result = new ArrayList<CompetitorRow>();
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            final Map<String, String> metadata = readMetadataBeforeTable(br);
            final LinkedHashMap<String, String> columnNamesAndStyles = readTableHeader(br);
            findTableBody(br);
            final List<String> rowContents = getRowContents(br);
            for (final String row : rowContents) {
                final List<String> tdContent = getTagContents(row, "td");
                result.add(createCompetitorRow(tdContent, columnNamesAndStyles, classesCounts));
            }
            addMostLikelyClassToMetadata(classesCounts, metadata);
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

    private void addMostLikelyClassToMetadata(Map<String, Integer> classesCounts, Map<String, String> metadata) {
        if (!classesCounts.isEmpty()) {
            // sort descending and pick first:
            metadata.put(CLASS_METADATA, classesCounts.entrySet().stream().sorted((e1, e2)->Integer.compare(e2.getValue(), e1.getValue())).findFirst().get().getKey());
        }
    }

    private void findTableBody(BufferedReader br) throws IOException {
        String line;
        while ((line=br.readLine()) != null && !line.contains("<tbody>"))
            ;
    }

    private LinkedHashMap<String, String> readTableHeader(BufferedReader br) throws IOException {
        final LinkedHashMap<String, String> result = new LinkedHashMap<>();
        final List<String> classes = new ArrayList<>();
        final Pattern colClassPattern = Pattern.compile("<col class=\"([^\"]*)\" */?>");
        final Pattern colTitlePattern = Pattern.compile("<th>([^<]*)</th>");
        String line;
        while ((line=br.readLine()) != null && !line.contains("<colgroup"))
            ;
        while (line != null && !line.contains("</colgroup")) {
            final Matcher colMatcher = colClassPattern.matcher(line);
            if (colMatcher.matches()) {
                classes.add(colMatcher.group(1));
            }
            line = br.readLine();
        }
        while ((line=br.readLine()) != null && !line.contains("<tr class=\"titlerow\">"))
            ;
        final Iterator<String> classIter = classes.iterator();
        while (line != null && !line.contains("</tr")) {
            final Matcher colTitleMatcher = colTitlePattern.matcher(line);
            if (colTitleMatcher.matches()) {
                result.put(colTitleMatcher.group(1), classIter.next());
            }
            line = br.readLine();
        }
        return result;
    }

    private Map<String, String> readMetadataBeforeTable(BufferedReader br) throws IOException {
        final Pattern h3MetadataElementPattern = Pattern.compile("<h3 class=\"([^\"]*)\"[^>]*>([^<]*)</h3>");
        final Pattern divMetadataElementPattern = Pattern.compile("<div class=\"([^\"]*)\"[^>]*>([^<]*)</div>");
        final Map<String, String> result = new HashMap<>();
        String line;
        while ((line=br.readLine()) != null && !line.contains("<h3"))
            ;
        while (line != null && !line.contains("<table")) {
            final Matcher h3Matcher = h3MetadataElementPattern.matcher(line);
            if (h3Matcher.matches()) {
                result.put(h3Matcher.group(1), h3Matcher.group(2));
            }
            final Matcher divMatcher = divMetadataElementPattern.matcher(line);
            if (divMatcher.matches()) {
                result.put(divMatcher.group(1), divMatcher.group(2));
            }
            line = br.readLine();
        }
        return result;
    }

    private CompetitorRow createCompetitorRow(List<String> trContent, LinkedHashMap<String, String> columnNamesAndStyles, Map<String, Integer> classesCounts) throws UnsupportedEncodingException {
        final Pattern rankPattern = Pattern.compile("([0-9]+)[A-Za-z]*"); // captures the 3 in "3rd"
        final Pattern nationalityPattern = Pattern.compile("^(<img .*\\btitle=\")?([A-Za-z][A-Za-z][A-Za-z])(\".*>)?$");
        final Iterator<String> columnValueIterator = trContent.iterator();
        String nationality = null;
        String sailNumber = null;
        Integer totalRank = null;
        List<String> names = null;
        List<CompetitorEntry> rankAndMaxPointsReasonAndPointsAndDiscarded = new ArrayList<CompetitorEntry>();
        Double scoreAfterDiscarding = null;
        Double totalPointsBeforeDiscarding = null;
        for (final Entry<String, String> columnNameAndStyle : columnNamesAndStyles.entrySet()) {
            final String columnValue = columnValueIterator.next();
            switch (columnNameAndStyle.getValue()) {
            case "rank":
                final Matcher rankMatcher = rankPattern.matcher(columnValue);
                if (rankMatcher.matches()) {
                    totalRank = Integer.parseInt(rankMatcher.group(1));
                }
                break;
            case "class":
                if (classesCounts.containsKey(columnValue)) {
                    classesCounts.put(columnValue, classesCounts.get(columnValue)+1);
                } else {
                    classesCounts.put(columnValue, 1);
                }
                break;
            case "nat":
                // example value: <img class="natflag" title="HKG" src="ILCA6_files/HKG.jpg">
                // but we should also be prepared for just the nationality string, such as "HKG"
                final Matcher natMatcher = nationalityPattern.matcher(columnValue);
                if (natMatcher.matches()) {
                    nationality = natMatcher.group(2);
                } else {
                    nationality = columnValue;
                }
                break;
            case "sailno":
                sailNumber = columnValue;
                break;
            case "helmname":
                names = getNames(columnValue);
                break;
            case "race":
                CompetitorEntry rankAndMaxPointsReasonAndPointsAndDiscardedForOnceRace = getRankAndMaxPointsReasonAndPointsAndDiscardedForOnceRace(columnValue.trim());
                rankAndMaxPointsReasonAndPointsAndDiscarded.add(rankAndMaxPointsReasonAndPointsAndDiscardedForOnceRace);
                break;
            case "total":
                totalPointsBeforeDiscarding = Double.parseDouble(columnValue);
                break;
            case "nett":
                scoreAfterDiscarding = Double.parseDouble(columnValue);
            }
        }
        return new CompetitorRowImpl(totalRank, nationality+" "+sailNumber, names, scoreAfterDiscarding, totalPointsBeforeDiscarding,
                rankAndMaxPointsReasonAndPointsAndDiscarded);
    }

    private CompetitorEntry getRankAndMaxPointsReasonAndPointsAndDiscardedForOnceRace(String cell) {
        final Pattern oneRaceScorePattern = Pattern.compile("^\\(?([0-9]+\\.[0-9]+)( ([A-Z][A-Z][A-Z]))?.*$");
        boolean isDiscarded = cell.trim().startsWith("(") && cell.trim().endsWith(")");
        final Matcher matcher = oneRaceScorePattern.matcher(cell);
        final CompetitorEntry result;
        if (matcher.matches()) {
            final String maxPointsReason = matcher.group(3);
            final Double points = Double.parseDouble(matcher.group(1));
            result = new DefaultCompetitorEntryImpl(/* rank */ null, maxPointsReason, points, isDiscarded);
        } else {
            result = null;
        }
        return result;
    }

    private List<String> getNames(String cell) {
        return Arrays.asList(cell.split(", *"));
    }
}
