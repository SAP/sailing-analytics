package com.sap.sailing.gwt.ui.leaderboard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Header;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.InvertibleComparator;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.domain.common.impl.InvertibleComparatorAdapter;
import com.sap.sailing.gwt.ui.client.DetailTypeFormatter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.AbstractSortableColumnWithMinMax;
import com.sap.sailing.gwt.ui.leaderboard.DetailTypeColumn.LegDetailField;

public class TimeTraveledRaceColumn extends ExpandableSortableColumn<String> implements HasStringAndDoubleValue {
    
    private static final DetailType DETAIL_TYPE = DetailType.RACE_TIME_TRAVELED;

    private RaceNameProvider raceNameProvider;
    private StringMessages stringMessages;

    private String columnStyle;
    private String headerStyle;
    private MinMaxRenderer minmaxRenderer;

    public TimeTraveledRaceColumn(UnStyledLeaderboardPanel leaderboardPanel, RaceNameProvider raceNameProvider,
            StringMessages stringMessages, String headerStyle, String columnStyle,
            String detailHeaderStyle, String detailColumnStyle) {
        super(leaderboardPanel, /* expandable */true, new TextCell(), DETAIL_TYPE.getDefaultSortingOrder(), 
                stringMessages, detailHeaderStyle, detailColumnStyle,
                Arrays.asList(DetailType.RACE_TIME_TRAVELED_UPWIND, DetailType.RACE_TIME_TRAVELED_DOWNWIND, DetailType.RACE_TIME_TRAVELED_REACHING), leaderboardPanel);
        setHorizontalAlignment(ALIGN_CENTER);
        this.raceNameProvider = raceNameProvider;
        this.stringMessages = stringMessages;
        this.columnStyle = columnStyle;
        this.headerStyle = headerStyle;
        this.minmaxRenderer = new MinMaxRenderer(this, getComparator());
    }

    @Override
    public Double getDoubleValue(LeaderboardRowDTO row) {
        Long result = null;
        LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
        if (fieldsForRace != null && fieldsForRace.legDetails != null) {
            for (LegEntryDTO legDetail : fieldsForRace.legDetails) {
                if (legDetail != null) {
                    if (legDetail.timeInMilliseconds != null) {
                        if (result == null) {
                            result = 0l;
                        }
                        result += legDetail.timeInMilliseconds;
                    }
                }
            }
        }
        return result == null ? null : new Long(result / 1000).doubleValue();
    }

    private Double getTimeTraveledFor(LeaderboardRowDTO row, LegType legType) {
        Long result = null;
        LeaderboardEntryDTO fieldsForRace = row.fieldsByRaceColumnName.get(getRaceColumnName());
        if (fieldsForRace != null && fieldsForRace.legDetails != null) {
            for (LegEntryDTO legDetail : fieldsForRace.legDetails) {
                if (legDetail != null && legDetail.legType == legType && legDetail.timeInMilliseconds != null) {
                    if (result == null) {
                        result = 0l;
                    }
                    result += legDetail.timeInMilliseconds;
                }
            }
        }
        return result == null ? null : new Long(result / 1000).doubleValue();
    }
    
    @Override
    protected Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> getDetailColumnMap(
            UnStyledLeaderboardPanel leaderboardPanel, StringMessages stringConstants, String detailHeaderStyle,
            String detailColumnStyle) {
        Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> result = new HashMap<>();

        result.put(DetailType.RACE_TIME_TRAVELED_UPWIND,
                new FormattedDoubleDetailTypeColumn(DetailType.RACE_TIME_TRAVELED_UPWIND, new RaceTimeTraveledUpwindInSeconds(),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.RACE_TIME_TRAVELED_DOWNWIND,
                new FormattedDoubleDetailTypeColumn(DetailType.RACE_TIME_TRAVELED_DOWNWIND, new RaceTimeTraveledDownwindInSeconds(),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));
        result.put(DetailType.RACE_TIME_TRAVELED_REACHING,
                new FormattedDoubleDetailTypeColumn(DetailType.RACE_TIME_TRAVELED_REACHING, new RaceTimeTraveledReachingInSeconds(),
                        detailHeaderStyle, detailColumnStyle, leaderboardPanel));

        return result;
    }
    
    private class RaceTimeTraveledUpwindInSeconds implements LegDetailField<Double> {
        @Override
        public Double get(LeaderboardRowDTO row) {
            return getTimeTraveledFor(row, LegType.UPWIND);
        }
    }
    
    private class RaceTimeTraveledDownwindInSeconds implements LegDetailField<Double> {
        @Override
        public Double get(LeaderboardRowDTO row) {
            return getTimeTraveledFor(row, LegType.DOWNWIND);
        }
    }
    
    private class RaceTimeTraveledReachingInSeconds implements LegDetailField<Double> {
        @Override
        public Double get(LeaderboardRowDTO row) {
            return getTimeTraveledFor(row, LegType.REACHING);
        }
    }

    private String getRaceColumnName() {
        return raceNameProvider.getRaceColumnName();
    }

    @Override
    public String getValue(LeaderboardRowDTO object) {
        Double result = getDoubleValue(object);
        if (result == null) {
            return "";
        } else {
            Integer intResult = ((int) (double) result);
            return intResult.toString();
        }
    }

    @Override
    public Header<SafeHtml> getHeader() {
        SortableExpandableColumnHeader result = new SortableExpandableColumnHeader(
        /* title */DetailTypeFormatter.format(DETAIL_TYPE), /* tooltip */ DetailTypeFormatter.getTooltip(DETAIL_TYPE),
        DetailTypeFormatter.getUnit(DETAIL_TYPE), /* iconURL */null, getLeaderboardPanel(), this, stringMessages);
        return result;
    }

    @Override
    public InvertibleComparator<LeaderboardRowDTO> getComparator() {
        return new InvertibleComparatorAdapter<LeaderboardRowDTO>(getPreferredSortingOrder().isAscending()) {
            @Override
            public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                Double val1 = getDoubleValue(o1);
                Double val2 = getDoubleValue(o2);
                return val1 == null ? val2 == null ? 0 : isAscending() ? 1 : -1 : val2 == null ? isAscending() ? -1 : 1 : val1
                        .compareTo(val2);
            }
        };
    }

    @Override
    public String getStringValueToRender(LeaderboardRowDTO row) {
        String result = getValue(row);
        if (!result.equals("")) {
            return result;
        } else {
            return null;
        }
    }

    @Override
    public void render(Context context, LeaderboardRowDTO row, SafeHtmlBuilder sb) {
        minmaxRenderer.render(context, row, getTitle(row), sb);
    }

    @Override
    public void updateMinMax() {
        minmaxRenderer.updateMinMax(getDisplayedLeaderboardRowsProvider());
    }

    /**
     * Computes a tool-tip text to add to the table cell's content as rendered by
     * {@link #render(Context, LeaderboardRowDTO, SafeHtmlBuilder)}.
     * 
     * @return This default implementation returns <code>null</code> for no tool tip / title
     */
    protected String getTitle(LeaderboardRowDTO row) {
        return null;
    }

    @Override
    public String getColumnStyle() {
        return columnStyle;
    }

    @Override
    public String getHeaderStyle() {
        return headerStyle;
    }

}
