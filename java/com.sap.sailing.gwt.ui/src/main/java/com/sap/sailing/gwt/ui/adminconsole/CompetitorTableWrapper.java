package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.Callback;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ImageResourceRenderer;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.adminconsole.ColorColumn.ColorRetriever;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;

public class CompetitorTableWrapper<S extends RefreshableSelectionModel<CompetitorDTO>> extends TableWrapper<CompetitorDTO, S> {
    private final LabeledAbstractFilterablePanel<CompetitorDTO> filterField;
    
    public CompetitorTableWrapper(SailingServiceAsync sailingService, StringMessages stringMessages,ErrorReporter errorReporter,
            boolean multiSelection, boolean enablePager) {
        super(sailingService, stringMessages, errorReporter, multiSelection, enablePager,
                new EntityIdentityComparator<CompetitorDTO>() {
                    @Override
                    public boolean representSameEntity(CompetitorDTO dto1, CompetitorDTO dto2) {
                        return dto1.getIdAsString().equals(dto2.getIdAsString());
                    }
                    @Override
                    public int hashCode(CompetitorDTO t) {
                        return t.getIdAsString().hashCode();
                    }
                });
        ListHandler<CompetitorDTO> competitorColumnListHandler = getColumnSortHandler();
        
        // competitors table
        TextColumn<CompetitorDTO> competitorNameColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getName();
            }
        };
        competitorNameColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorNameColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        TextColumn<CompetitorDTO> competitorShortNameColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getShortName();
            }
        };
        competitorShortNameColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorShortNameColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* case sensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return comparator.compare(o1.getShortName(), o2.getShortName());
            }
        });

        TextColumn<CompetitorDTO> boatClassColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getBoatClass() != null ? competitor.getBoatClass().getName() : "";
            }
        };
        boatClassColumn.setSortable(true);
        competitorColumnListHandler.setComparator(boatClassColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* caseSensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return comparator.compare(o1.getBoatClass().getName(), o2.getBoatClass().getName());
            }
        });
        
        Column<CompetitorDTO, SafeHtml> sailIdColumn = new Column<CompetitorDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(CompetitorDTO competitor) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                ImageResourceRenderer renderer = new ImageResourceRenderer();
                final String twoLetterIsoCountryCode = competitor.getTwoLetterIsoCountryCode();
                final String flagImageURL = competitor.getFlagImageURL();
                if (flagImageURL != null && !flagImageURL.isEmpty()) {
                    sb.appendHtmlConstant("<img src=\"" + flagImageURL + "\" width=\"18px\" height=\"12px\" title=\"" + competitor.getName() + "\"/>");
                    sb.appendHtmlConstant("&nbsp;");
                } else {
                    final ImageResource flagImageResource;
                    if (twoLetterIsoCountryCode==null || twoLetterIsoCountryCode.isEmpty()) {
                        flagImageResource = FlagImageResolver.getEmptyFlagImageResource();
                    } else {
                        flagImageResource = FlagImageResolver.getFlagImageResource(twoLetterIsoCountryCode);
                    }
                    if (flagImageResource != null) {
                        sb.append(renderer.render(flagImageResource));
                        sb.appendHtmlConstant("&nbsp;");
                    }
                }
                sb.appendEscaped(competitor.getSailID());
                return sb.toSafeHtml();
            }
        };
        sailIdColumn.setSortable(true);
        competitorColumnListHandler.setComparator(sailIdColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* case sensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return comparator.compare(o1.getSailID(), o2.getSailID());
            }
        });

        Column<CompetitorDTO, SafeHtml> displayColorColumn = new ColorColumn<>(new ColorRetriever<CompetitorDTO>() {
            @Override
            public Color getColor(CompetitorDTO t) {
                return t.getColor();
            }
        });
        
        Column<CompetitorDTO, SafeHtml> imageColumn = new Column<CompetitorDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(CompetitorDTO competitor) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                if (competitor.getImageURL() != null && !competitor.getImageURL().isEmpty()) {
                    sb.appendHtmlConstant("<img src=\"" + competitor.getImageURL() + "\" height=\"40px\" title=\""
                            + competitor.getImageURL() + "\"/>");
                }
                return sb.toSafeHtml();
            }
        };
        imageColumn.setSortable(true);
        competitorColumnListHandler.setComparator(imageColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* case sensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return comparator.compare(o1.getImageURL(), o2.getImageURL());
            }
        });

        TextColumn<CompetitorDTO> competitorIdColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getIdAsString();
            }
        };
        competitorIdColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorIdColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return new NaturalComparator(false).compare(o1.getIdAsString(), o2.getIdAsString());
            }
        });

        TextColumn<CompetitorDTO> competitorEMailColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getEmail();
            }
        };
        competitorEMailColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorEMailColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return new NaturalComparator(false).compare(o1.getEmail(), o2.getEmail());
            }
        });

        TextColumn<CompetitorDTO> competitorSearchTagColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getSearchTag();
            }
        };
        competitorSearchTagColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorSearchTagColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return new NaturalComparator(false).compare(o1.getSearchTag(), o2.getSearchTag());
            }
        });

        TextColumn<CompetitorDTO> timeOnTimeFactorColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getTimeOnTimeFactor()==null?"":(""+competitor.getTimeOnTimeFactor());
            }
        };
        timeOnTimeFactorColumn.setSortable(true);
        competitorColumnListHandler.setComparator(timeOnTimeFactorColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return o1.getTimeOnTimeFactor()==null?o2.getTimeOnTimeFactor()==null?0:-1:o2.getTimeOnTimeFactor()==null?1:
                    o1.getTimeOnTimeFactor().compareTo(o2.getTimeOnTimeFactor());
            }
        });
        TextColumn<CompetitorDTO> timeOnDistanceAllowancePerNauticalMileColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getTimeOnDistanceAllowancePerNauticalMile()==null?"":(""+competitor.getTimeOnDistanceAllowancePerNauticalMile());
            }
        };
        timeOnTimeFactorColumn.setSortable(true);
        competitorColumnListHandler.setComparator(timeOnDistanceAllowancePerNauticalMileColumn, new Comparator<CompetitorDTO>() {
            @Override
                    public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                        return o1.getTimeOnDistanceAllowancePerNauticalMile() == null ? o2
                                .getTimeOnDistanceAllowancePerNauticalMile() == null ? 0 : -1 : o2
                                .getTimeOnDistanceAllowancePerNauticalMile() == null ? 1 : o1
                                .getTimeOnDistanceAllowancePerNauticalMile().compareTo(
                                        o2.getTimeOnDistanceAllowancePerNauticalMile());
                    }
        });

        
        filterField = new LabeledAbstractFilterablePanel<CompetitorDTO>(new Label(stringMessages.filterCompetitors()),
                new ArrayList<CompetitorDTO>(), table, dataProvider) {
            @Override
            public Iterable<String> getSearchableStrings(CompetitorDTO t) {
                List<String> string = new ArrayList<String>();
                string.add(t.getName());
                string.add(t.getSailID());
                string.add(t.getBoatClass().getName());
                string.add(t.getIdAsString());
                string.add(t.getSearchTag());
                return string;
            }
        };
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        
        //CompetitorTableEditFeatures
        ImagesBarColumn<CompetitorDTO, CompetitorConfigImagesBarCell> competitorActionColumn = new ImagesBarColumn<CompetitorDTO, CompetitorConfigImagesBarCell>(
                new CompetitorConfigImagesBarCell(stringMessages));
        competitorActionColumn.setFieldUpdater(new FieldUpdater<CompetitorDTO, String>() {
            @Override
            public void update(int index, final CompetitorDTO competitor, String value) {
                if (CompetitorConfigImagesBarCell.ACTION_EDIT.equals(value)) {
                    openEditCompetitorDialog(competitor, competitor.getBoatClass().getName());
                } else if (CompetitorConfigImagesBarCell.ACTION_REFRESH.equals(value)) {
                    allowUpdate(Collections.singleton(competitor));
                }
            }

        });
        
        mainPanel.insert(filterField, 0);
        table.addColumnSortHandler(competitorColumnListHandler);
        table.addColumn(competitorNameColumn, stringMessages.name());
        table.addColumn(competitorShortNameColumn, stringMessages.shortName());
        table.addColumn(sailIdColumn, stringMessages.sailNumber());
        table.addColumn(boatClassColumn, stringMessages.boatClass());
        table.addColumn(timeOnTimeFactorColumn, stringMessages.timeOnTimeFactor());
        table.addColumn(timeOnDistanceAllowancePerNauticalMileColumn, stringMessages.timeOnDistanceAllowanceInSecondsPerNauticalMile());
        table.addColumn(displayColorColumn, stringMessages.color());
        table.addColumn(imageColumn, stringMessages.image());
        table.addColumn(competitorEMailColumn, stringMessages.email());
        table.addColumn(competitorSearchTagColumn, stringMessages.searchTag());
        table.addColumn(competitorIdColumn, stringMessages.id());
        table.addColumn(competitorActionColumn, stringMessages.actions());
        table.ensureDebugId("CompetitorsTable");
    }
    
    public Iterable<CompetitorDTO> getAllCompetitors() {
        return filterField.getAll();
    }
    
    public LabeledAbstractFilterablePanel<CompetitorDTO> getFilterField() {
        return filterField;
    }
    
    public void refreshCompetitorList(Iterable<CompetitorDTO> competitors) {
        getFilteredCompetitors(competitors);
    }
    
    public void refreshCompetitorList(String leaderboardName) {
        refreshCompetitorList(leaderboardName, null);
    }
    
    /**
     * @param leaderboardName If null, all existing competitors are loaded
     */
    public void refreshCompetitorList(String leaderboardName, final Callback<Iterable<CompetitorDTO>,
            Throwable> callback) {
        if (leaderboardName != null) {
            sailingService.getCompetitorsOfLeaderboard(leaderboardName, new AsyncCallback<Iterable<CompetitorDTO>>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Remote Procedure Call getCompetitors() - Failure: " + caught.getMessage());
                    if (callback != null) {
                        callback.onFailure(caught);
                    }
                }

                @Override
                public void onSuccess(Iterable<CompetitorDTO> result) {
                    refreshCompetitorList(result);
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                }
            });
        } else {
            sailingService.getCompetitors(new AsyncCallback<Iterable<CompetitorDTO>>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Remote Procedure Call getCompetitors() - Failure: " + caught.getMessage());
                    if (callback != null) {
                        callback.onFailure(caught);
                    }
                }

                @Override
                public void onSuccess(Iterable<CompetitorDTO> result) {
                    getFilteredCompetitors(result);
                    refreshCompetitorList(result);
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                }
            });
        }
    }

    private void getFilteredCompetitors(Iterable<CompetitorDTO> result) {
        filterField.updateAll(result);
    }
    
    void openEditCompetitorDialog(final CompetitorDTO originalCompetitor, String boatClass) {
        final CompetitorEditDialog dialog = new CompetitorEditDialog(stringMessages, originalCompetitor, new DialogCallback<CompetitorDTO>() {
            @Override
            public void ok(final CompetitorDTO competitor) {
                sailingService.addOrUpdateCompetitor(competitor, new AsyncCallback<CompetitorDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to update competitor: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(CompetitorDTO updatedCompetitor) {
                        //only reload selected competitors reloading with refreshCompetitorList(leaderboardName)
                        //would not work in case the list is not based on a leaderboard e.g. AbstractCompetitorRegistrationDialog
                        int editedCompetitorIndex = getFilterField().indexOf(originalCompetitor);
                        getFilterField().remove(originalCompetitor);
                        if (editedCompetitorIndex >= 0){
                            getFilterField().add(editedCompetitorIndex, updatedCompetitor);
                        } else {
                            //in case competitor was not present --> not edit, but create
                            getFilterField().add(updatedCompetitor);
                        }
                        getDataProvider().refresh();
                    }  
                });
            }

            @Override
            public void cancel() {
            }
        },  boatClass);
        dialog.ensureDebugId("CompetitorEditDialog");
        dialog.show();
    }

    protected void allowUpdate(final Iterable<CompetitorDTO> competitors) {
        List<CompetitorDTO> serializableSingletonList = new ArrayList<CompetitorDTO>();
        Util.addAll(competitors, serializableSingletonList);
        sailingService.allowCompetitorResetToDefaults(serializableSingletonList, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to allow resetting competitors " + competitors
                        + " to defaults: " + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                Window.alert(stringMessages.successfullyAllowedCompetitorReset(competitors.toString()));
            }
        });
    }
}
