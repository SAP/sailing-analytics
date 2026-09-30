package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.Date;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.charts.EditMarkPositionPanel.NotificationType;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardTableResources;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sse.common.InvertibleComparator;
import com.sap.sse.common.Position;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.common.impl.InvertibleComparatorAdapter;
import com.sap.sse.common.settings.AbstractSettings;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableSortedCellTableWithStylableHeaders;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;
import com.sap.sse.gwt.client.celltable.SortableColumn;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

public class MarksPanel extends AbstractCompositeComponent<AbstractSettings> {
    private static final LeaderboardTableResources tableResources = GWT.create(LeaderboardTableResources.class);
    
    private final ListDataProvider<MarkDTO> markDataProvider;    
    private final FlushableSortedCellTableWithStylableHeaders<MarkDTO> markTable;
    
    public MarksPanel(final EditMarkPositionPanel parent, ComponentContext<?> context, final StringMessages stringMessages) {
        super(parent, context);
        markTable = new FlushableSortedCellTableWithStylableHeaders<MarkDTO>(10000, tableResources);
        markDataProvider = markTable.getDataProvider();
        markTable.addStyleName("EditMarkPositionMarkTable");
        SortableColumn<MarkDTO, String> markNameColumn = new SortableColumn<MarkDTO, String>(new TextCell(), SortingOrder.ASCENDING) {
            @Override
            public String getValue(MarkDTO object) {
                return object.getName();
            }

            @Override
            public InvertibleComparator<MarkDTO> getComparator() {
                return new InvertibleComparatorAdapter<MarkDTO>() {
                    public int compare(MarkDTO m1, MarkDTO m2) {
                        return m1.getName().compareTo(m2.getName());
                    }
                };
            }

            @Override
            public Header<?> getHeader() {
                return new TextHeader(stringMessages.marks());
            }
        };
        markTable.addColumn(markNameColumn);
        SortableColumn<MarkDTO, String> addFixColumn = new SortableColumn<MarkDTO, String>(new ButtonCell(), SortingOrder.NONE) {
            @Override
            public String getValue(MarkDTO object) {
                return stringMessages.addNewFix();
            }

            @Override
            public InvertibleComparator<MarkDTO> getComparator() {
                return null;
            }

            @Override
            public Header<?> getHeader() {
                return new TextHeader("");
            }
        };
        addFixColumn.setFieldUpdater(new FieldUpdater<MarkDTO, String>() {
            @Override
            public void update(int index, final MarkDTO mark, String value) {
                final Date timePoint = parent.timer.getTime();
                parent.retrieveAndSelectMarkIfNecessary(mark, new Runnable() {

                    @Override
                    public void run() {
                        if (parent.hasFixAtTimePoint(mark, timePoint)) {
                            parent.showNotification(stringMessages.pleaseSelectOtherTimepoint(), NotificationType.ERROR);
                        } else {
                            parent.createFixPositionChooserToAddFixToMark(mark, new Callback<Position, Exception>() {
                                @Override
                                public void onFailure(final Exception reason) {
                                    parent.resetCurrentFixPositionChooser();
                                }
                                @Override
                                public void onSuccess(Position result) {
                                    parent.addMarkFix(mark, timePoint, result);
                                    parent.resetCurrentFixPositionChooser();
                                }
                            });
                        }
                    }
                });
            }
        });
        markTable.addColumn(addFixColumn);
        final SingleSelectionModel<MarkDTO> selectionModel = new RefreshableSingleSelectionModel<MarkDTO>(
                new EntityIdentityComparator<MarkDTO>() {
            @Override
            public boolean representSameEntity(MarkDTO dto1, MarkDTO dto2) {
                return dto1.getIdAsString().equals(dto2.getIdAsString());
            }
            @Override
            public int hashCode(MarkDTO t) {
                return t.getIdAsString().hashCode();
            }
        }, this.markDataProvider);
        markTable.setSelectionModel(selectionModel);
        markTable.getSelectionModel().addSelectionChangeHandler(parent);

        final FlowPanel mainPanel = new FlowPanel();
        final Widget clearSelection = createClearSelection(event -> selectionModel.clear(), stringMessages);
        markTable.getSelectionModel().addSelectionChangeHandler(
                event -> clearSelection.setVisible(!selectionModel.getSelectedSet().isEmpty()));
        mainPanel.add(clearSelection);
        mainPanel.add(markTable);
        initWidget(mainPanel);
        setTitle(stringMessages.marks());
    }

    private Widget createClearSelection(final ClickHandler clearSelectionHandler, final StringMessages stringMessages) {
        final FlowPanel clearSelectionPanel = new FlowPanel();
        clearSelectionPanel.addStyleName("EditMarkPositionHintPanel");
        clearSelectionPanel.add(new Label(stringMessages.pleaseClearSelectionToSeeFullCourse()));
        clearSelectionPanel.add(new Button(stringMessages.clearSelection(), clearSelectionHandler));
        clearSelectionPanel.add(new Label());
        clearSelectionPanel.setVisible(false);
        return clearSelectionPanel;
    }

    void updateMarks(final Iterable<MarkDTO> marks) {
        markTable.getDataProvider().getList().clear();
        for (final MarkDTO mark : marks) {
            markTable.getDataProvider().getList().add(mark);
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }
    
    @Override
    public String getLocalizedShortName() {
        return null;
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public SettingsDialogComponent<AbstractSettings> getSettingsDialogComponent(AbstractSettings settings) {
        return null;
    }

    @Override
    public void updateSettings(AbstractSettings newSettings) {
    }

    @Override
    public String getDependentCssClassName() {
        return null;
    }

    public MarkDTO getSelectedMark() {
        for (MarkDTO mark : markDataProvider.getList()) {
            if (markTable.getSelectionModel().isSelected(mark)) {
               	return mark;
            }
        }
        return null;
    }
    
    public void deselectMark() {
        markTable.getSelectionModel().setSelected(getSelectedMark(), false);
    }

    public void select(MarkDTO mark) {
        markTable.getSelectionModel().setSelected(mark, true);
    }
    
    @Override
    public AbstractSettings getSettings() {
        return null;
    }

    @Override
    public String getId() {
        return "MarksPanel";
    }

}