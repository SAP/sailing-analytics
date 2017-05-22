package com.sap.sailing.gwt.home.desktop.partials.racelist;

import static com.sap.sailing.gwt.ui.common.client.DateAndTimeFormatterUtil.shortTimeFormatter;

import java.util.Date;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.sap.sailing.domain.common.SortingOrder;
import com.sap.sailing.gwt.home.communication.event.LiveRaceDTO;
import com.sap.sailing.gwt.home.communication.event.RaceListRaceDTO;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sailing.gwt.home.communication.race.FlagStateDTO;
import com.sap.sailing.gwt.home.communication.race.FleetMetadataDTO;
import com.sap.sailing.gwt.home.communication.race.RaceMetadataDTO;
import com.sap.sailing.gwt.home.communication.race.RaceProgressDTO;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO.RaceViewState;
import com.sap.sailing.gwt.home.communication.race.wind.AbstractWindDTO;
import com.sap.sailing.gwt.home.communication.race.wind.WindStatisticsDTO;
import com.sap.sailing.gwt.home.desktop.partials.racelist.RaceListResources.LocalCss;
import com.sap.sailing.gwt.home.desktop.partials.raceviewerlaunchpad.RaceviewerLaunchPadCell;
import com.sap.sailing.gwt.home.desktop.partials.raceviewerlaunchpad.RaceviewerLaunchPadController;
import com.sap.sailing.gwt.home.desktop.places.event.EventView;
import com.sap.sailing.gwt.home.shared.utils.HomeSailingFlagsBuilder;
import com.sap.sailing.gwt.regattaoverview.client.FlagsMeaningExplanator;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.common.client.DateAndTimeFormatterUtil;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceListColumnFactory {
    
    private static final LocalCss CSS = RaceListResources.INSTANCE.css(); 
    private static final StringMessages I18N = StringMessages.INSTANCE;
    private static final CellTemplates TEMPLATE = GWT.create(CellTemplates.class);

    interface CellTemplates extends SafeHtmlTemplates {
        @Template("<div style=\"{1}\" class=\"{0}\"></div>")
        SafeHtml fleetCorner(String styleNames, SafeStyles color);

        @Template("<div>{3}</div><div class=\"{0}\"><div style=\"{2}\" class=\"{1}\"></div></div>")
        SafeHtml viewStateRunning(String styleNamesBar, String styleNamesProgress, SafeStyles width, String text);

        @Template("<img style=\"{0}\" src=\"images/home/windkompass_nord.svg\"/>")
        SafeHtml windDirection(SafeStyles rotation);

        @Template("<img src=\"{3}\" class=\"{0}\" /><span class=\"{1}\">{4}</span><div class=\"{2}\" title=\"{5}\">{5}</div>")
        SafeHtml winner(String styleNamesFlag, String styleNamesSailId, String styleNamesText, SafeUri flagImageURL, String sailId, String name);
        
        @Template("<img src=\"{1}\" class=\"{0}\" />")
        SafeHtml imageHeader(String styleNames, SafeUri imageURL);
    }
    
    public static <T extends RaceMetadataDTO<?>> SortableRaceListColumn<T, FleetMetadataDTO> getFleetCornerColumn() {
        Cell<FleetMetadataDTO> cell = new AbstractCell<FleetMetadataDTO>() {
            @Override
            public void render(Context context, FleetMetadataDTO value, SafeHtmlBuilder sb) {
                if (value != null) {
                    SafeStyles style = SafeStylesUtils.fromTrustedNameAndValue("border-top-color", value.getFleetColor());
                    sb.append(TEMPLATE.fleetCorner(CSS.race_fleetcorner_icon(), style));
                }
            }
        };
        return new SortableRaceListColumn<T, FleetMetadataDTO>("", cell, null) {
            @Override
            public String getColumnStyle() {
                return CSS.race_fleetcorner();
            }
            
            @Override
            public FleetMetadataDTO getValue(T object) {
                return object.getFleet();
            }
        };
    }
    
    public static <T extends RaceMetadataDTO<?>> SortableRaceListColumn<T, String> getRegattaNameColumn() {
        Cell<String> cell = new TextCell();
        RaceListColumnComparator<T, String> comparator = new RaceListColumnComparator<T, String>() {
            @Override
            public String getValue(T object) {
                return object.getRegattaDisplayName();
            }
        };
        return new SortableRaceListColumn<T, String>(I18N.regatta(), cell, comparator) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }

            @Override
            public String getColumnStyle() {
                return getStyleNamesString(CSS.race_item(), CSS.race_itemname());
            }

            @Override
            public String getValue(T object) {
                return object.getRegattaDisplayName();
            }
        };
    }
    
    public static <T extends RaceMetadataDTO<?>> SortableRaceListColumn<T, String> getRaceNameColumn() {
        Cell<String> cell = new TextCell();
        DefaultRaceListColumnComparator<T> comparator = new DefaultRaceListColumnComparator<>();
        return new SortableRaceListColumn<T, String>(I18N.race(), cell, comparator, SortingOrder.DESCENDING) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }
            
            @Override
            public String getColumnStyle() {
                return getStyleNamesString(CSS.race_item(), CSS.race_itemname());
            }
            
            @Override
            public String getValue(T object) {
                return object.getRaceName();
            }
        };
    }
    
    public static <T extends RaceMetadataDTO<?>> SortableRaceListColumn<T, String> getFleetNameColumn() {
        Cell<String> cell = new TextCell();
        RaceListColumnComparator<T, String> comparator = new RaceListColumnComparator<T, String>() {
            @Override
            public String getValue(T object) {
                return object.getFleet() != null ? object.getFleet().getFleetName() : null;
            }
        };
        return new SortableRaceListColumn<T, String>(I18N.fleet(), cell, comparator) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }
            
            @Override
            public String getColumnStyle() {
                return CSS.race_item();
            }
            
            @Override
            public String getValue(T object) {
                return object.getFleet() != null ? object.getFleet().getFleetName() : null;
            }
        };
    }
    
    public static <T extends RaceMetadataDTO<?>> SortableRaceListStartTimeColumn<T> getStartTimeColumn() {
        RaceListColumnComparator<T, Date> comparator = new RaceListColumnComparator<T, Date>() {

            @Override
            public Date getValue(T object) {
                return object.getStart();
            }
        };
        return new SortableRaceListStartTimeColumn<T>(comparator);
    }
    
    public static <T extends RaceListRaceDTO> SortableRaceListColumn<T, String> getDurationColumn() {
        Cell<String> cell = new TextCell();
        RaceListColumnComparator<T, Duration> comparator = new RaceListColumnComparator<T, Duration>() {

            @Override
            public Duration getValue(T object) {
                return object.getDuration();
            }
        };
        return new SortableRaceListColumn<T, String>(I18N.durationPlain(), cell, comparator) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }
            
            @Override
            public String getColumnStyle() {
                return CSS.race_item();
            }
            
            @Override
            public String getValue(T object) {
                return object.getDuration() != null ? DateAndTimeFormatterUtil.formatElapsedTime(object.getDuration().asMillis()) : null;
            }
        };
    }
    
    public static <T extends LiveRaceDTO> SortableRaceListColumn<T, FlagStateDTO> getFlagsColumn() {
        Cell<FlagStateDTO> cell = new AbstractCell<FlagStateDTO>() {
            @Override
            public void render(Context context, FlagStateDTO value, SafeHtmlBuilder sb) {
                if (value != null) {
                    sb.append(HomeSailingFlagsBuilder.render(value, 0.55, FlagsMeaningExplanator.getFlagsMeaning(I18N,
                            value.getLastUpperFlag(), value.getLastLowerFlag(), value.isLastFlagsAreDisplayed())));
                }
            }
        };
        return new SortableRaceListColumn<T, FlagStateDTO>(I18N.flags(), cell, null) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }

            @Override
            public String getColumnStyle() {
                return getStyleNamesString(CSS.race_item(), CSS.racesListIcon());
            }

            @Override
            public FlagStateDTO getValue(T object) {
                return object.getFlagState();
            }
        };
    }
    
    public static <T extends LiveRaceDTO> SortableRaceListColumn<T, String> getWindSpeedColumn() {
        RaceListColumnComparator<T, Double> comparator = new RaceListColumnComparator<T, Double>() {
            @Override
            public Double getValue(T object) {
                return object.getWind() != null ? object.getWind().getTrueWindSpeedInKnots() : null;
            }
        };
        return new WindSpeedOrRangeColumn<T>(comparator) {
            @Override
            public String getValue(T object) {
                return object.getWind() != null ? I18N.knotsValue(object.getWind().getTrueWindSpeedInKnots()) : null;
            }
        };
    }
    
    public static <T extends RaceListRaceDTO> SortableRaceListColumn<T, String> getWindRangeColumn() {
        RaceListColumnComparator<T, Double> comparator = new RaceListColumnComparator<T, Double>() {
            @Override
            public Double getValue(T object) {
                WindStatisticsDTO wind = object.getWind();
                return wind != null ? (wind.getTrueUpperboundWindInKnots() + wind .getTrueLowerboundWindInKnots()) : null;
            }
        };
        return new WindSpeedOrRangeColumn<T>(comparator) {
            @Override
            public String getValue(T object) {
                WindStatisticsDTO wind = object.getWind();
                if (wind == null) {
                    return "";
                } else if (wind.getRoundedDifference() <= 0d) {
                    return I18N.knotsValue(wind.getTrueLowerboundWindInKnots());
                }
                return I18N.knotsRange(wind.getTrueLowerboundWindInKnots(), wind.getTrueUpperboundWindInKnots());
            }
        };
    }

    public static <T extends RaceMetadataDTO<?>> SortableRaceListColumn<T, AbstractWindDTO> getWindDirectionColumn() {
        Cell<AbstractWindDTO> cell = new AbstractCell<AbstractWindDTO>() {
            @Override
            public void render(Context context, AbstractWindDTO value, SafeHtmlBuilder sb) {
                if (value != null) {
                    SafeStylesBuilder safeStyles = new SafeStylesBuilder();
                    safeStyles.trustedNameAndValue("-webkit-transform", "rotate(" + value.getTrueWindFromDeg() + "deg)");
                    safeStyles.trustedNameAndValue("transform", "rotate(" + value.getTrueWindFromDeg() + "deg)");
                    safeStyles.width(2.75, Unit.EM).height(2.75, Unit.EM);
                    sb.append(TEMPLATE.windDirection(safeStyles.toSafeStyles()));
                }
            }
        };
        RaceListColumnComparator<T, Double> comparator = new RaceListColumnComparator<T, Double>() {
            @Override
            public Double getValue(T object) {
                return object.getWind() != null ? object.getWind().getTrueWindFromDeg() : null;
            }
        };
        return new SortableRaceListColumn<T, AbstractWindDTO>(I18N.from(), cell, comparator) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }

            @Override
            public String getColumnStyle() {
                return getStyleNamesString(CSS.race_item(), CSS.racesListIcon());
            }

            @Override
            public AbstractWindDTO getValue(T object) {
                return object.getWind();
            }
        };
    }
    
    public static <T extends RaceMetadataDTO<?>> SortableRaceListColumn<T, String> getCourseAreaColumn() {
        Cell<String> cell = new TextCell();
        RaceListColumnComparator<T, String> comparator = new RaceListColumnComparator<T, String>() {
            @Override
            public String getValue(T object) {
                return object.getCourseArea();
            }
        };
        return new SortableRaceListColumn<T, String>(I18N.courseArea(), cell, comparator) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }
            
            @Override
            public String getColumnStyle() {
                return CSS.race_item();
            }
            
            @Override
            public String getValue(T object) {
                return object.getCourseArea();
            }
        };
    }
    
    public static <T extends RaceMetadataDTO<?>> SortableRaceListColumn<T, String> getCourseColumn() {
        Cell<String> cell = new TextCell();
        RaceListColumnComparator<T, String> comparator = new RaceListColumnComparator<T, String>() {
            @Override
            public String getValue(T object) {
                return object.getCourse();
            }
        };
        return new SortableRaceListColumn<T, String>(I18N.course(), cell, comparator) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }
            
            @Override
            public String getColumnStyle() {
                return CSS.race_item();
            }
            
            @Override
            public String getValue(T object) {
                return object.getCourse();
            }
        };
    }
    
    public static <T extends RaceListRaceDTO> SortableRaceListColumn<T, Number> getWindSourcesCountColumn() {
        RaceListColumnComparator<T, Integer> comparator = new RaceListColumnComparator<T, Integer>() {
            @Override
            public Integer getValue(T object) {
                return object.getWindSourcesCount();
            }
        };
        return new DataCountColumn<T>(DataCountColumn.ICON_WIND, comparator) {
            @Override
            public Number getValue(T object) {
                return object.getWindSourcesCount();
            }
        };
    }
    
    public static <T extends RaceListRaceDTO> SortableRaceListColumn<T, Number> getVideoCountColumn() {
        RaceListColumnComparator<T, Integer> comparator = new RaceListColumnComparator<T, Integer>() {
            @Override
            public Integer getValue(T object) {
                return object.getVideoCount();
            }
        };
        return new DataCountColumn<T>(DataCountColumn.ICON_VIDEO, comparator) {
            @Override
            public Number getValue(T object) {
                return object.getVideoCount();
            }
        };
    }
    
    public static <T extends RaceListRaceDTO> SortableRaceListColumn<T, Number> getAudioCountColumn() {
        RaceListColumnComparator<T, Integer> comparator = new RaceListColumnComparator<T, Integer>() {
            @Override
            public Integer getValue(T object) {
                return object.getAudioCount();
            }
        };
        return new DataCountColumn<T>(DataCountColumn.ICON_AUDIO, comparator) {
            @Override
            public Number getValue(T object) {
                return object.getAudioCount();
            }
        };
    }
    
    public static <T extends LiveRaceDTO> SortableRaceListColumn<T, T> getRaceViewStateColumn() {
        Cell<T> cell = new AbstractCell<T>() {
            @Override
            public void render(Context context, T value, SafeHtmlBuilder sb) {
                RaceViewState state = value.getViewState();
                RaceProgressDTO progress = value.getProgress();
                if (state == RaceViewState.SCHEDULED) {
                    double min = MillisecondsTimePoint.now().until(new MillisecondsTimePoint(value.getStart())).asMinutes();
                    sb.appendEscaped(I18N.startingInMinutes((int) min));
                } else if (state == RaceViewState.RUNNING && progress != null) {
                    SafeStyles width = SafeStylesUtils.forWidth(progress.getPercentageProgress(), Unit.PCT);
                    String text = I18N.currentOfTotalLegs(progress.getCurrentLeg(), progress.getTotalLegs());
                    sb.append(TEMPLATE.viewStateRunning(CSS.race_itemstatus_progressbar(),
                            CSS.race_itemstatus_progressbar_progress(), width, text));
                } else {
                    sb.appendEscaped(state.getLabel());
                }
            }
        };
        RaceListColumnComparator<T, RaceViewState> comparator = new RaceListColumnComparator<T, RaceViewState>() {
            @Override
            public RaceViewState getValue(T object) {
                return object.getViewState();
            }
        };
        return new SortableRaceListColumn<T, T>(I18N.status(), cell, comparator) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }
            
            @Override
            public String getColumnStyle() {
                return CSS.race_item();
            }
            
            @Override
            public T getValue(T object) {
                return object;
            }
        };
    }
    
    public static <T extends RaceListRaceDTO> SortableRaceListColumn<T, SimpleCompetitorDTO> getWinnerColumn() {
        Cell<SimpleCompetitorDTO> cell = new AbstractCell<SimpleCompetitorDTO>() {
            @Override
            public void render(Context context, SimpleCompetitorDTO value, SafeHtmlBuilder sb) {
                if (value != null) {
                    SafeUri flagImageUri = FlagImageResolver.getFlagImageUri(value.getFlagImageURL(), value.getTwoLetterIsoCountryCode());
                    String flagStyle = CSS.race_item_flag(), sailIdStyle = CSS.race_item_sailid(), nameStyle = CSS.race_item_winner();
                    sb.append(TEMPLATE.winner(flagStyle, sailIdStyle, nameStyle, flagImageUri, value.getSailID(), value.getName()));
                }
            }
        };
        RaceListColumnComparator<T, String> comparator = new RaceListColumnComparator<T, String>() {
            @Override
            public String getValue(T object) {
                return object.getWinner() != null ? object.getWinner().getName() : null;
            }
        };
        return new SortableRaceListColumn<T, SimpleCompetitorDTO>(I18N.winner(), cell, comparator) {
            @Override
            public String getHeaderStyle() {
                return CSS.raceslist_head_item();
            }
            
            @Override
            public String getColumnStyle() {
                return getStyleNamesString(CSS.race_item(), CSS.race_itemwinner());
            }
            
            @Override
            public SimpleCompetitorDTO getValue(T object) {
                return object.getWinner();
            }
        };
    }
    
    public static <T extends RaceMetadataDTO<?>> SortableRaceListColumn<T, T> getRaceViewerButtonColumn(
            final EventView.Presenter presenter, final boolean showNotTracked) {
        final RaceviewerLaunchPadController lpPresenter = new RaceviewerLaunchPadController(presenter::getRaceViewerURL);
        final RaceviewerLaunchPadCell<T> lpadCell = new RaceviewerLaunchPadCell<T>(lpPresenter, showNotTracked);
        DefaultRaceListColumnComparator<T> comparator = new DefaultRaceListColumnComparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                int compareResult = lpPresenter.getRenderingStyle(o1).compareTo(lpPresenter.getRenderingStyle(o2));
                if (compareResult == 0) {
                    compareResult = super.compare(o1, o2);
                }
                return compareResult;
            }
        };
        return new SortableRaceListColumn<T, T>("", lpadCell, comparator) {
            @Override
            public String getHeaderStyle() {
                return getStyleNamesString(CSS.raceslist_head_item(), CSS.raceslist_head_itembutton());
            }
            
            @Override
            public String getColumnStyle() {
                return getStyleNamesString(CSS.race_item(), CSS.race_itemright());
            }
            
            @Override
            public T getValue(T object) {
                return object;
            }
        };
    }
    
    public static class SortableRaceListStartTimeColumn<T extends RaceMetadataDTO<?>> extends SortableRaceListColumn<T, Date> {
        protected SortableRaceListStartTimeColumn(RaceListColumnComparator<T, ?> comparator) {
            super(I18N.start(), new StartTimeCell(), comparator, SortingOrder.DESCENDING);
        }
        
        public void setShowTimeOnly(boolean showTimeOnly) {
            ((StartTimeCell) getCell()).showTimeOnly = showTimeOnly;
        }
        
        @Override
        public String getHeaderStyle() {
            return CSS.raceslist_head_item();
        }

        @Override
        public String getColumnStyle() {
            return CSS.race_item();
        }

        @Override
        public Date getValue(T object) {
            return object.getStart();
        }
        
        private static class StartTimeCell extends AbstractCell<Date> {
            private static final DateTimeFormat DATE_FORMAT = DateTimeFormat.getFormat(PredefinedFormat.MONTH_NUM_DAY);
            private boolean showTimeOnly = true;
            @Override
            public void render(Context context, Date value, SafeHtmlBuilder sb) {
                if (value != null) {
                    sb.appendEscaped((showTimeOnly ? "" : DATE_FORMAT.format(value) + " ") + shortTimeFormatter.render(value));
                }
            }
        }
    }
    
    
    private static abstract class WindSpeedOrRangeColumn<T extends RaceMetadataDTO<?>> extends SortableRaceListColumn<T, String> {
        protected WindSpeedOrRangeColumn(RaceListColumnComparator<T, ?> comparator) {
            super(I18N.wind(), new TextCell(), comparator);
        }

        @Override
        public String getHeaderStyle() {
            return CSS.raceslist_head_item();
        }
        
        @Override
        public String getColumnStyle() {
            return CSS.race_item();
        }
    }
    
    private static abstract class DataCountColumn<T extends RaceListRaceDTO> extends SortableRaceListColumn<T, Number> {
        private static final SafeUri ICON_WIND = UriUtils.fromTrustedString("images/home/icon-wind.png"); 
        private static final SafeUri ICON_VIDEO = UriUtils.fromTrustedString("images/home/icon-video.png"); 
        private static final SafeUri ICON_AUDIO = UriUtils.fromTrustedString("images/home/icon-audio.png"); 
        
        private DataCountColumn(final SafeUri imageUri, RaceListColumnComparator<T, ?> comparator) {
            super(new SafeHtmlHeader(TEMPLATE.imageHeader(CSS.raceslist_head_itemflag(), imageUri)), new AbstractCell<Number>() {
                @Override
                public void render(Context context, Number value, SafeHtmlBuilder sb) {
                    if (value != null && value.longValue() > 0) {
                        sb.append(value.longValue());
                    } else {
                        sb.appendHtmlConstant("&mdash;");
                    }
                }
            }, comparator, SortingOrder.DESCENDING);
        }

        @Override
        public String getHeaderStyle() {
            return getStyleNamesString(CSS.raceslist_head_item(), CSS.raceslist_head_itemcenter());
        }
        
        @Override
        public String getColumnStyle() {
            return getStyleNamesString(CSS.race_item(), CSS.race_itemcenter());
        }
    }
}
