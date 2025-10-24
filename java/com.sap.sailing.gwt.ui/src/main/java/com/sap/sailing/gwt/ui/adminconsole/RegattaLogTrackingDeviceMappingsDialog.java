package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.moxieapps.gwt.highcharts.client.Axis;
import org.moxieapps.gwt.highcharts.client.AxisTitle;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Global;
import org.moxieapps.gwt.highcharts.client.Highcharts;
import org.moxieapps.gwt.highcharts.client.Legend;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.ToolTip;
import org.moxieapps.gwt.highcharts.client.ToolTipData;
import org.moxieapps.gwt.highcharts.client.ToolTipFormatter;
import org.moxieapps.gwt.highcharts.client.events.PointMouseOverEvent;
import org.moxieapps.gwt.highcharts.client.events.PointMouseOverEventHandler;
import org.moxieapps.gwt.highcharts.client.plotOptions.SeriesPlotOptions;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.sap.sailing.domain.common.MailInvitationType;
import com.sap.sailing.gwt.ui.client.MappableToDeviceFormatter;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceMappingDTO;
import com.sap.sailing.gwt.ui.shared.TypedDeviceMappingDTO;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.ImagesBarColumn;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.ui.client.UserService;

public class RegattaLogTrackingDeviceMappingsDialog extends DataEntryDialog<Void> {
    private static final int HOURS_TO_EXPAND_FOR_OPEN_END = 2;

    protected final String leaderboardName;

    public static final double PERCENTAGE_OF_TIMESPAN_TO_EXTEND_OPEN_ENDS = 0.1;
    public static final String FIELD_INDEX = "index";
    public static final int CHART_WIDTH = 500;
    public static final String SERIES_COLOR = "#fcb913";
    
    private final  ErrorReporter errorReporter;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final UserService userService;
    private final StringMessages stringMessages;
    private List<DeviceMappingDTO> mappings = new ArrayList<>();
    private DeviceMappingTableWrapper deviceMappingTable;
    private LabeledAbstractFilterablePanel<DeviceMappingDTO> filterField;
    private CheckBox showPingMappingsCb;
    private BusyIndicator busyIndicator;
    
    private Point[] data;
    
    private Date latest;
    private Date earliest;
    private Chart chart;

    private String regattaRegistrationSecret;
    
    public RegattaLogTrackingDeviceMappingsDialog(final SailingServiceWriteAsync sailingServiceWrite, final UserService userService,
            final StringMessages stringMessages, final ErrorReporter errorReporter, final String leaderboardName,
            final String regattaRegistrationSecret, DialogCallback<Void> callback) {
        super(stringMessages.mapDevices(), /*message*/ null, stringMessages.ok(), stringMessages.cancel(), /*validator*/ null, callback);
        this.ensureDebugId("regattaLogTrackingDeviceMappingsDialog");
        this.stringMessages = stringMessages;
        this.sailingServiceWrite = sailingServiceWrite;
        this.userService = userService;
        this.errorReporter = errorReporter;
        this.regattaRegistrationSecret = regattaRegistrationSecret;
        this.leaderboardName = leaderboardName;
        this.busyIndicator = new SimpleBusyIndicator();
        refresh();
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        FlowPanel mainPanel = new FlowPanel();
        HorizontalPanel buttonPanel = new HorizontalPanel();
        Button addMappingButton = new Button(stringMessages.add());
        addMappingButton.ensureDebugId("addMappingButton");
        addMappingButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addMapping();
            }
        });
        buttonPanel.add(addMappingButton);
        Button importBtn = new Button(stringMessages.importFixes());
        importBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                importFixes();
            }
        });
        buttonPanel.add(importBtn);
        buttonPanel.add(new Button(stringMessages.importAdditionalSensorData(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                importFoiling();
            }
        }));
        buttonPanel.add(busyIndicator);
        mainPanel.add(buttonPanel);
        deviceMappingTable = new DeviceMappingTableWrapper(sailingServiceWrite, stringMessages, errorReporter);
        deviceMappingTable.getTable().addCellPreviewHandler(new CellPreviewEvent.Handler<DeviceMappingDTO>() {
            @Override
            public void onCellPreview(CellPreviewEvent<DeviceMappingDTO> event) {
                int i = mappings.indexOf(event.getValue());
                chart.getSeries()[0].getPoints()[i].select(true, false);
            }
        });
        final ImagesBarColumn<DeviceMappingDTO, RaceLogTrackingDeviceMappingsImagesBarCell> actionCol =
                new ImagesBarColumn<DeviceMappingDTO, RaceLogTrackingDeviceMappingsImagesBarCell>(
                new RaceLogTrackingDeviceMappingsImagesBarCell(stringMessages));
        actionCol.setFieldUpdater(getActionColFieldUpdater());
        deviceMappingTable.getTable().addColumn(actionCol, stringMessages.actions());
        final HorizontalPanel deviceMappingPanel = new HorizontalPanel();
        mainPanel.add(deviceMappingPanel);
        chart = new Chart()
        .setType(Series.Type.COLUMN_RANGE)
        .setChartTitleText(stringMessages.deviceMappings())
        .setLegend(new Legend().setEnabled(false))
        .setInverted(true);
        chart.setSeriesPlotOptions(new SeriesPlotOptions().setPointMouseOverEventHandler(new PointMouseOverEventHandler() {
            @Override
            public boolean onMouseOver(PointMouseOverEvent pointMouseOverEvent) {
                int i = (int) pointMouseOverEvent.getXAsLong();
                deviceMappingTable.getSelectionModel().setSelected(mappings.get(i), true);
                chart.getSeries()[0].getPoints()[i].select(true, false);
                return true;
            }
        }).setColor(SERIES_COLOR));
        Highcharts.setOptions(
                new Highcharts.Options().setGlobal(
                        new Global()
                        .setUseUTC(false)
                        ));
        chart.getXAxis().setOption("labels/enabled", false)
        .setGridLineWidth(0)
        .setMinorGridLineWidth(0);
        chart.getYAxis()
        .setAxisTitle(new AxisTitle().setText(stringMessages.time()))
        .setType(Axis.Type.DATE_TIME)
        .setGridLineWidth(0)
        .setMinorGridLineWidth(0);
        chart.setWidth(CHART_WIDTH + "px");
        chart.setHeight("400px");
        chart.setToolTip(new ToolTip().setFormatter(new ToolTipFormatter() {
            @Override
            public String format(ToolTipData toolTipData) {
                int index = (int) toolTipData.getXAsLong();
                DeviceMappingDTO mapping = mappings.get(index);
                final String itemType = MappableToDeviceFormatter.formatType(mapping.mappedTo, stringMessages);
                return "<b>" + stringMessages.device() + ":</b> " + mapping.deviceIdentifier.deviceType + " - " + mapping.deviceIdentifier.deviceId + "<br/>" +
                    "<b>" + stringMessages.mappedTo() + ":</b> " + itemType + " - " + MappableToDeviceFormatter.formatName(mapping.mappedTo) + "<br/>" +
                    "<b>" + stringMessages.from() + ":</b> " + DateAndTimeFormatterUtil.formatDateAndTime(mapping.from) + "<br/>" +
                    "<b>" + stringMessages.to() + ":</b> " + DateAndTimeFormatterUtil.formatDateAndTime(mapping.to);
            }
        }));
        deviceMappingPanel.add(chart);
        filterField = new LabeledAbstractFilterablePanel<DeviceMappingDTO>(
                new Label(stringMessages.filterDeviceMappings()),
                new ArrayList<DeviceMappingDTO>(), deviceMappingTable.getDataProvider(), stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(DeviceMappingDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(MappableToDeviceFormatter.formatType(t.mappedTo, stringMessages));
                strings.add(MappableToDeviceFormatter.formatName(t.mappedTo));
                strings.add(t.deviceIdentifier.deviceType);
                strings.add(t.deviceIdentifier.deviceId);
                return strings;
            }

            @Override
            public AbstractCellTable<DeviceMappingDTO> getCellTable() {
                return deviceMappingTable.getTable();
            }
        };
        filterField.addFilter(new Filter<DeviceMappingDTO>() {
            @Override
            public boolean matches(DeviceMappingDTO dm) {
                return showPingMappingsCb.getValue() || !"PING".equals(dm.deviceIdentifier.deviceType);
            }
            
            @Override
            public String getName() {
                return "ShowPingMappingsFilter";
            }
        });
        
        deviceMappingTable.registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        final VerticalPanel vp = new VerticalPanel();
        deviceMappingPanel.add(vp);
        vp.add(filterField);
        showPingMappingsCb = new CheckBox(stringMessages.showPingMarkMappings());
        showPingMappingsCb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                filterField.filter();
            }
        });
        vp.insert(showPingMappingsCb, 0);
        vp.add(deviceMappingTable);
        return mainPanel;
    }

    void updateChart() {
        earliest = null;
        latest = null;
        for (DeviceMappingDTO mapping : mappings) {
            updateExtremes(mapping);
        }
        data = new Point[mappings.size()];
        
        handleOpenEndedIntervalls();
        
        long earliestMillis = earliest.getTime();
        long latestMillis = latest.getTime();
        long range = latestMillis - earliestMillis;
        long extension = (long) (range * PERCENTAGE_OF_TIMESPAN_TO_EXTEND_OPEN_ENDS);
        long yMin = earliestMillis - extension;
        long yMax = latestMillis + extension;
        int i = 0;
        for (DeviceMappingDTO mapping : mappings) {
            JSONObject userData = new JSONObject();
            userData.put(FIELD_INDEX, userData);
            long from = mapping.from == null ? yMin - range : mapping.from.getTime();
            long to = mapping.to == null ? yMax + range : mapping.to.getTime();
            data[i] = new Point(i, from, to);
            i++;
        }
        chart.removeAllSeries(false);
        chart.addSeries(chart.createSeries()
                .setName(stringMessages.deviceMappings())
                .setPoints(data));
        chart.getYAxis().setExtremes(yMin, yMax);
    }

    private void handleOpenEndedIntervalls() {
        if (earliest == null && latest == null){
            //allMappingsAreOpenEnded in Both directions or no mappings present
            earliest = new Date(0);
            latest = new Date();
        } else if (earliest == null && latest != null){
            earliest = new Date(latest.getTime() - HOURS_TO_EXPAND_FOR_OPEN_END*1000*60*60);
        } else if (latest == null && earliest != null){
            latest = new Date(earliest.getTime() + HOURS_TO_EXPAND_FOR_OPEN_END*1000*60*60);
        }
    }
    
    private void updateExtremes(DeviceMappingDTO mapping) {
        if (mapping.from != null && (earliest == null || earliest.after(mapping.from))) {
            earliest = mapping.from;
        }
        if (mapping.to != null && (latest == null || latest.before(mapping.to))) {
            latest = mapping.to;
        }
    }
    
    private void addMapping() {
        showAddMappingDialog(null);
    }
    
    private void refresh() {
        busyIndicator.setBusy(true);
        sailingServiceWrite.getDeviceMappings(leaderboardName, new AsyncCallback<List<DeviceMappingDTO>>() {
            @Override
            public void onSuccess(List<DeviceMappingDTO> result) {
                busyIndicator.setBusy(false);
                mappings = result;
                updateChart();
                filterField.updateAll(mappings);
            }

            @Override
            public void onFailure(Throwable caught) {
                busyIndicator.setBusy(false);
                errorReporter.reportError("Could not load mappings for marks: " + caught.getMessage());
            }
        });
    }

    private void showAddMappingDialog(DeviceMappingDTO mapping) {
        sailingServiceWrite.getMailType(new AsyncCallback<MailInvitationType>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }

            @Override
            public void onSuccess(MailInvitationType mailInvitationType) {
                new RegattaLogAddDeviceMappingDialog(sailingServiceWrite, userService, errorReporter, stringMessages,
                        leaderboardName, regattaRegistrationSecret, mailInvitationType,
                        new DataEntryDialog.DialogCallback<DeviceMappingDTO>() {
                            @Override
                            public void ok(final DeviceMappingDTO mapping) {
                                sailingServiceWrite.addDeviceMappingToRegattaLog(leaderboardName, mapping,
                                        new AsyncCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                                refresh();
                                            }

                                            @Override
                                            public void onFailure(Throwable caught) {
                                                showAddMappingDialog(mapping);
                                                errorReporter
                                                        .reportError("Could not add mapping: " + caught.getMessage());
                                            }
                                        });
                            }

                            @Override
                            public void cancel() {
                                refresh();
                            }
                        }, mapping).show();
            }
        });
    }

    private void importFixes() {
        new RegattaLogImportFixesAndAddMappingsDialog(sailingServiceWrite, userService, errorReporter, stringMessages, leaderboardName,
                new DialogCallback<Collection<DeviceMappingDTO>>() {
                    @Override
                    public void ok(Collection<DeviceMappingDTO> editedObject) {
                        for (DeviceMappingDTO mapping : editedObject) {
                            sailingServiceWrite.addDeviceMappingToRegattaLog(leaderboardName, mapping,
                                    new AsyncCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            RegattaLogTrackingDeviceMappingsDialog.this.refresh();
                                        }

                                        @Override
                                        public void onFailure(Throwable caught) {
                                            errorReporter.reportError(caught.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }
    
    private void importFoiling() {
        new RegattaLogImportSensorDataAndAddMappingsDialog(sailingServiceWrite, userService, errorReporter, stringMessages, leaderboardName,
                new DialogCallback<Collection<TypedDeviceMappingDTO>>() {
            @Override
            public void ok(Collection<TypedDeviceMappingDTO> editedObject) {
                for (TypedDeviceMappingDTO mapping : editedObject) {
                    sailingServiceWrite.addTypedDeviceMappingToRegattaLog(leaderboardName, mapping, new AsyncCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            RegattaLogTrackingDeviceMappingsDialog.this.refresh();
                        }
                        
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(caught.getMessage());
                        }
                    });
                }
            }
            
            @Override
            public void cancel() {
            }
        }).show();
    }

    private FieldUpdater<DeviceMappingDTO, String> getActionColFieldUpdater() {
        return new FieldUpdater<DeviceMappingDTO, String>() {
            @Override
            public void update(int index, final DeviceMappingDTO dto, String value) {
                if (RaceLogTrackingDeviceMappingsImagesBarCell.ACTION_CLOSE.equals(value)) {
                    new SetTimePointDialog(stringMessages, stringMessages.setClosingTimePoint(),
                            new DataEntryDialog.DialogCallback<java.util.Date>() {
                                @Override
                                public void ok(java.util.Date editedObject) {
                                    sailingServiceWrite.closeOpenEndedDeviceMapping(leaderboardName, dto, editedObject,
                                            new AsyncCallback<Void>() {
                                                @Override
                                                public void onSuccess(Void result) {
                                                    refresh();
                                                }

                                                @Override
                                                public void onFailure(Throwable caught) {
                                                    errorReporter.reportError("Could not close open ended mapping: "
                                                            + caught.getMessage());
                                                }
                                            });
                                }

                                @Override
                                public void cancel() {
                                }
                            }).show();
                } else if (RaceLogTrackingDeviceMappingsImagesBarCell.ACTION_REMOVE.equals(value)) {
                    sailingServiceWrite.revokeRaceAndRegattaLogEvents(leaderboardName, dto.originalRaceLogEventIds,
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError("Could not remove mappings: " + caught.getMessage());
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    refresh();
                                }

                            });
                }

            }
        };
    }

    @Override
    protected Void getResult() {
        return null;
    }
    
}
