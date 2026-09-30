package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.WindSourceTypeFormatter;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChartSettingsDialogCssResources.DialogCss;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;

public class WindChartSettingsDialogComponent implements SettingsDialogComponent<WindChartSettings> {

    private static final DialogCss CSS = WindChartSettingsDialogCssResources.INSTANCE.css();
    private static final Iterable<WindSourceType> ALL_SOURCE_TYPES = EnumSet.allOf(WindSourceType.class);

    private enum StatState { OFF, ON, INDETERMINATE }

    /**
     * A small clickable button-like label for toggling avg/min/max on or off.
     * Has three visual states: off (grey), on (colored), and indeterminate (used by bulk toggles
     * when only some sources have that stat enabled). Bulk toggles use a different color (yellow)
     * than per-source toggles (blue). When the section is disabled (show checkbox unchecked),
     * the button fades out to signal the selection is preserved but inactive.
     */
    private static final class StatToggle {
        private final Label label;
        private StatState state;
        private final DialogCss css;
        private final boolean bulk;
        private boolean sectionDisabled;

        StatToggle(final String text, final boolean initialOn, final DialogCss css, final boolean bulk) {
            this.css = css;
            this.bulk = bulk;
            css.ensureInjected();
            label = new Label(text);
            label.addStyleName(css.statButton());
            setState(initialOn ? StatState.ON : StatState.OFF);
        }

        void setState(final StatState newState) {
            state = newState;
            label.removeStyleName(css.statButtonActive());
            label.removeStyleName(css.statButtonIndeterminate());
            label.removeStyleName(css.statButtonBulkActive());
            label.removeStyleName(css.statButtonBulkIndeterminate());
            if (state == StatState.ON) {
                label.addStyleName(bulk ? css.statButtonBulkActive() : css.statButtonActive());
            } else if (state == StatState.INDETERMINATE) {
                label.addStyleName(bulk ? css.statButtonBulkIndeterminate() : css.statButtonIndeterminate());
            }
        }

        void setSectionDisabled(final boolean disabled) {
            sectionDisabled = disabled;
            if (sectionDisabled) {
                label.addStyleName(css.statButtonSectionDisabled());
            } else {
                label.removeStyleName(css.statButtonSectionDisabled());
            }
        }

        void toggle() {
            setState(state == StatState.ON ? StatState.OFF : StatState.ON);
        }

        boolean isOn() {
            return state == StatState.ON;
        }

        Widget asWidget() {
            return label;
        }

        void setShown(final boolean shown) {
            label.getElement().getStyle().setVisibility(
                shown ? Visibility.VISIBLE : Visibility.HIDDEN);
        }

        void addClickHandler(final ClickHandler handler) {
            label.addClickHandler(handler);
        }
    }

    private final WindChartSettings initialSettings;
    private IntegerBox resolutionInSecondsBox;
    private CheckBox showWindDirectionsSeriesCheckbox;
    private CheckBox showWindSpeedSeriesCheckbox;
    private final Map<WindSourceType, CheckBox> windDirectionSourceCheckboxes = new LinkedHashMap<WindSourceType, CheckBox>();
    private final Map<WindSourceType, CheckBox> windSpeedSourceCheckboxes = new LinkedHashMap<WindSourceType, CheckBox>();
    private final Map<WindSourceType, StatToggle> dirAvgToggles = new LinkedHashMap<WindSourceType, StatToggle>();
    private final Map<WindSourceType, StatToggle> dirMinToggles = new LinkedHashMap<WindSourceType, StatToggle>();
    private final Map<WindSourceType, StatToggle> dirMaxToggles = new LinkedHashMap<WindSourceType, StatToggle>();
    private final Map<WindSourceType, StatToggle> spdAvgToggles = new LinkedHashMap<WindSourceType, StatToggle>();
    private final Map<WindSourceType, StatToggle> spdMinToggles = new LinkedHashMap<WindSourceType, StatToggle>();
    private final Map<WindSourceType, StatToggle> spdMaxToggles = new LinkedHashMap<WindSourceType, StatToggle>();
    private StatToggle dirBulkAvg;
    private StatToggle dirBulkMin;
    private StatToggle dirBulkMax;
    private StatToggle spdBulkAvg;
    private StatToggle spdBulkMin;
    private StatToggle spdBulkMax;

    private final StringMessages stringMessages;

    public WindChartSettingsDialogComponent(final WindChartSettings initialSettings, final StringMessages stringMessages) {
        super();
        this.stringMessages = stringMessages;
        this.initialSettings = initialSettings;
    }

    @Override
    public Widget getAdditionalWidget(final DataEntryDialog<?> dialog) {
        final VerticalPanel vp = new VerticalPanel();
        resolutionInSecondsBox = dialog.createIntegerBox((int) (initialSettings.getResolutionInMilliseconds() / 1000), 5);
        final HorizontalPanel hp = new HorizontalPanel();
        hp.add(new Label(stringMessages.stepSizeInSeconds() + ":"));
        hp.add(resolutionInSecondsBox);
        vp.add(hp);
        final FlexTable dirTable = new FlexTable();
        dirTable.getElement().getStyle().setMarginLeft(15.0, Unit.PX);
        dirTable.setCellPadding(2);
        showWindDirectionsSeriesCheckbox = dialog.createCheckbox(stringMessages.showWindDirectionSeries());
        showWindDirectionsSeriesCheckbox.setTitle(stringMessages.showWindDirectionSeriesTooltip());
        showWindDirectionsSeriesCheckbox.setValue(initialSettings.isShowWindDirectionsSeries());
        showWindDirectionsSeriesCheckbox.addStyleName(CSS.accentCheckbox());
        dirTable.setWidget(0, 0, showWindDirectionsSeriesCheckbox);
        dirTable.getFlexCellFormatter().setColSpan(0, 0, 4);
        final StatToggle[] dirBulk = buildSourceSection(dialog, dirTable, 1,
                windDirectionSourceCheckboxes, dirAvgToggles, dirMinToggles, dirMaxToggles,
                initialSettings.getWindDirectionSourcesToDisplay(),
                initialSettings.getDirectionAvgSources(), initialSettings.getDirectionMinSources(), initialSettings.getDirectionMaxSources(),
                initialSettings.isDirectionAvgBulk(), initialSettings.isDirectionMinBulk(), initialSettings.isDirectionMaxBulk(),
                initialSettings.isShowWindDirectionsSeries(), /* directionSection */ true);
        dirBulkAvg = dirBulk[0];
        dirBulkMin = dirBulk[1];
        dirBulkMax = dirBulk[2];
        if (!initialSettings.isShowWindDirectionsSeries()) {
            setSectionTogglesDisabled(dirAvgToggles, dirMinToggles, dirMaxToggles, dirBulkAvg, dirBulkMin, dirBulkMax, /* disabled */ true);
        }
        showWindDirectionsSeriesCheckbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(final ValueChangeEvent<Boolean> event) {
                final boolean enabled = event.getValue();
                for (final CheckBox cb : windDirectionSourceCheckboxes.values()) {
                    cb.setEnabled(enabled);
                }
                setSectionTogglesDisabled(dirAvgToggles, dirMinToggles, dirMaxToggles, dirBulkAvg, dirBulkMin, dirBulkMax, !enabled);
            }
        });
        final FlexTable spdTable = new FlexTable();
        spdTable.getElement().getStyle().setMarginLeft(8.0, Unit.PX);
        spdTable.setCellPadding(2);
        showWindSpeedSeriesCheckbox = dialog.createCheckbox(stringMessages.showWindSpeedSeries());
        showWindSpeedSeriesCheckbox.setTitle(stringMessages.showWindSpeedSeriesTooltip());
        showWindSpeedSeriesCheckbox.setValue(initialSettings.isShowWindSpeedSeries());
        showWindSpeedSeriesCheckbox.addStyleName(CSS.accentCheckbox());
        spdTable.setWidget(0, 0, showWindSpeedSeriesCheckbox);
        spdTable.getFlexCellFormatter().setColSpan(0, 0, 4);
        final StatToggle[] spdBulk = buildSourceSection(dialog, spdTable, 1,
                windSpeedSourceCheckboxes, spdAvgToggles, spdMinToggles, spdMaxToggles,
                initialSettings.getWindSpeedSourcesToDisplay(),
                initialSettings.getSpeedAvgSources(), initialSettings.getSpeedMinSources(), initialSettings.getSpeedMaxSources(),
                initialSettings.isSpeedAvgBulk(), initialSettings.isSpeedMinBulk(), initialSettings.isSpeedMaxBulk(),
                initialSettings.isShowWindSpeedSeries(), /* directionSection */ false);
        spdBulkAvg = spdBulk[0];
        spdBulkMin = spdBulk[1];
        spdBulkMax = spdBulk[2];
        if (!initialSettings.isShowWindSpeedSeries()) {
            setSectionTogglesDisabled(spdAvgToggles, spdMinToggles, spdMaxToggles, spdBulkAvg, spdBulkMin, spdBulkMax, /* disabled */ true);
        }
        showWindSpeedSeriesCheckbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(final ValueChangeEvent<Boolean> event) {
                final boolean enabled = event.getValue();
                for (final CheckBox cb : windSpeedSourceCheckboxes.values()) {
                    cb.setEnabled(enabled);
                }
                setSectionTogglesDisabled(spdAvgToggles, spdMinToggles, spdMaxToggles, spdBulkAvg, spdBulkMin, spdBulkMax, !enabled);
            }
        });
        final SimplePanel divider = new SimplePanel();
        divider.addStyleName(CSS.sectionDivider());
        final HorizontalPanel sectionsPanel = new HorizontalPanel();
        sectionsPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        sectionsPanel.add(dirTable);
        sectionsPanel.add(divider);
        sectionsPanel.add(spdTable);
        vp.add(sectionsPanel);
        return vp;
    }

    /**
     * Dims or restores all stat toggles (both bulk and per-source) for a section.
     * Called when the "show wind direction/speed data" checkbox is toggled — the selection
     * is kept intact, just visually faded so it's clear it's not active right now.
     */
    private void setSectionTogglesDisabled(
            final Map<WindSourceType, StatToggle> avgToggles,
            final Map<WindSourceType, StatToggle> minToggles,
            final Map<WindSourceType, StatToggle> maxToggles,
            final StatToggle bulkAvg, final StatToggle bulkMin, final StatToggle bulkMax,
            final boolean disabled) {
        bulkAvg.setSectionDisabled(disabled);
        bulkMin.setSectionDisabled(disabled);
        bulkMax.setSectionDisabled(disabled);
        for (final StatToggle t : avgToggles.values()) {
            t.setSectionDisabled(disabled);
        }
        for (final StatToggle t : minToggles.values()) {
            t.setSectionDisabled(disabled);
        }
        for (final StatToggle t : maxToggles.values()) {
            t.setSectionDisabled(disabled);
        }
    }

    /**
     * Builds one section of the settings table — either the direction or speed half. Each section has a "to all
     * selected" bulk row at the top, followed by one row per wind source. Per-source avg/min/max buttons are hidden
     * while the corresponding bulk button is on. When a source is newly checked while a bulk button is on, that source
     * inherits the bulk state. Returns the three bulk toggles in the order {@code bulkAvg, bulkMin, bulkMax}, so the
     * caller can wire them up to the show/hide checkbox.
     */
    private StatToggle[] buildSourceSection(final DataEntryDialog<?> dialog, final FlexTable table, final int startRow,
            final Map<WindSourceType, CheckBox> sourceCheckboxes,
            final Map<WindSourceType, StatToggle> avgToggles,
            final Map<WindSourceType, StatToggle> minToggles,
            final Map<WindSourceType, StatToggle> maxToggles,
            final Set<WindSourceType> selectedSources,
            final Set<WindSourceType> avgSources,
            final Set<WindSourceType> minSources,
            final Set<WindSourceType> maxSources,
            final boolean initialBulkAvg, final boolean initialBulkMin, final boolean initialBulkMax,
            final boolean sectionEnabled,
            final boolean directionSection) {
        final StatToggle bulkAvg = new StatToggle(stringMessages.windStatAvg(), initialBulkAvg, CSS, true);
        final StatToggle bulkMin = new StatToggle(stringMessages.windStatMin(), initialBulkMin, CSS, true);
        final StatToggle bulkMax = new StatToggle(stringMessages.windStatMax(), initialBulkMax, CSS, true);
        final Label toAllLabel = new Label(stringMessages.toAllSelected());
        toAllLabel.addStyleName(CSS.toAllSelectedLabel());
        table.setWidget(startRow, 0, toAllLabel);
        table.setWidget(startRow, 1, bulkAvg.asWidget());
        table.setWidget(startRow, 2, bulkMin.asWidget());
        table.setWidget(startRow, 3, bulkMax.asWidget());
        int row = startRow + 1;
        for (final WindSourceType type : ALL_SOURCE_TYPES) {
            if (!directionSection && !type.useSpeed()) {
                table.getCellFormatter().addStyleName(row, 0, CSS.sourceCheckboxIndent());
                final Label filler = new Label(WindSourceTypeFormatter.format(type, stringMessages));
                filler.addStyleName(CSS.sourceFillerLabel());
                table.setWidget(row, 0, filler);
                row++;
                continue;
            }
            final boolean sourceSelected = selectedSources.contains(type);
            final CheckBox sourceBox = dialog.createCheckbox(WindSourceTypeFormatter.format(type, stringMessages));
            sourceBox.setTitle(WindSourceTypeFormatter.tooltipFor(type, stringMessages));
            sourceBox.setValue(sourceSelected);
            sourceBox.setEnabled(sectionEnabled);
            sourceCheckboxes.put(type, sourceBox);
            final StatToggle avgToggle = new StatToggle(stringMessages.windStatAvg(), avgSources.contains(type), CSS, false);
            final StatToggle minToggle = new StatToggle(stringMessages.windStatMin(), minSources.contains(type), CSS, false);
            final StatToggle maxToggle = new StatToggle(stringMessages.windStatMax(), maxSources.contains(type), CSS, false);
            avgToggle.setShown(sourceSelected && !bulkAvg.isOn());
            minToggle.setShown(sourceSelected && !bulkMin.isOn());
            maxToggle.setShown(sourceSelected && !bulkMax.isOn());
            avgToggles.put(type, avgToggle);
            minToggles.put(type, minToggle);
            maxToggles.put(type, maxToggle);
            avgToggle.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(final ClickEvent event) {
                    avgToggle.toggle();
                }
            });
            minToggle.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(final ClickEvent event) {
                    minToggle.toggle();
                }
            });
            maxToggle.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(final ClickEvent event) {
                    maxToggle.toggle();
                }
            });
            sourceBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(final ValueChangeEvent<Boolean> event) {
                    final boolean checked = event.getValue();
                    if (checked) {
                        if (bulkAvg.isOn()) {
                            avgToggle.setState(StatState.ON);
                        }
                        if (bulkMin.isOn()) {
                            minToggle.setState(StatState.ON);
                        }
                        if (bulkMax.isOn()) {
                            maxToggle.setState(StatState.ON);
                        }
                    }
                    avgToggle.setShown(checked && !bulkAvg.isOn());
                    minToggle.setShown(checked && !bulkMin.isOn());
                    maxToggle.setShown(checked && !bulkMax.isOn());
                }
            });
            table.getCellFormatter().addStyleName(row, 0, CSS.sourceCheckboxIndent());
            table.setWidget(row, 0, sourceBox);
            table.setWidget(row, 1, avgToggle.asWidget());
            table.setWidget(row, 2, minToggle.asWidget());
            table.setWidget(row, 3, maxToggle.asWidget());
            row++;
        }
        addBulkClickHandler(bulkAvg, avgToggles, sourceCheckboxes);
        addBulkClickHandler(bulkMin, minToggles, sourceCheckboxes);
        addBulkClickHandler(bulkMax, maxToggles, sourceCheckboxes);
        return new StatToggle[]{bulkAvg, bulkMin, bulkMax};
    }

    private void addBulkClickHandler(final StatToggle bulk, final Map<WindSourceType, StatToggle> perSourceToggles,
            final Map<WindSourceType, CheckBox> sourceCheckboxes) {
        bulk.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                bulk.toggle();
                for (final Map.Entry<WindSourceType, CheckBox> e : sourceCheckboxes.entrySet()) {
                    if (e.getValue().getValue()) {
                        final StatToggle toggle = perSourceToggles.get(e.getKey());
                        if (bulk.isOn()) {
                            toggle.setState(StatState.ON);
                            toggle.setShown(false);
                        } else {
                            toggle.setShown(true);
                        }
                    }
                }
            }
        });
    }

    @Override
    public WindChartSettings getResult() {
        final Set<WindSourceType> dirSources = new HashSet<WindSourceType>();
        for (final Map.Entry<WindSourceType, CheckBox> e : windDirectionSourceCheckboxes.entrySet()) {
            if (e.getValue().getValue()) {
                dirSources.add(e.getKey());
            }
        }
        final Set<WindSourceType> spdSources = new HashSet<WindSourceType>();
        for (final Map.Entry<WindSourceType, CheckBox> e : windSpeedSourceCheckboxes.entrySet()) {
            if (e.getValue().getValue()) {
                spdSources.add(e.getKey());
            }
        }
        return new WindChartSettings(
                showWindSpeedSeriesCheckbox.getValue(), spdSources,
                showWindDirectionsSeriesCheckbox.getValue(), dirSources,
                resolutionInSecondsBox.getValue() == null ? -1 : resolutionInSecondsBox.getValue() * 1000,
                collectEnabled(dirAvgToggles, dirBulkAvg, windDirectionSourceCheckboxes),
                collectEnabled(dirMinToggles, dirBulkMin, windDirectionSourceCheckboxes),
                collectEnabled(dirMaxToggles, dirBulkMax, windDirectionSourceCheckboxes),
                dirBulkAvg.isOn(), dirBulkMin.isOn(), dirBulkMax.isOn(),
                collectEnabled(spdAvgToggles, spdBulkAvg, windSpeedSourceCheckboxes),
                collectEnabled(spdMinToggles, spdBulkMin, windSpeedSourceCheckboxes),
                collectEnabled(spdMaxToggles, spdBulkMax, windSpeedSourceCheckboxes),
                spdBulkAvg.isOn(), spdBulkMin.isOn(), spdBulkMax.isOn());
    }

    private Set<WindSourceType> collectEnabled(final Map<WindSourceType, StatToggle> statToggles,
            final StatToggle bulkToggle, final Map<WindSourceType, CheckBox> sourceBoxes) {
        final Set<WindSourceType> result = new HashSet<WindSourceType>();
        for (final Map.Entry<WindSourceType, CheckBox> e : sourceBoxes.entrySet()) {
            if (e.getValue().getValue()) {
                final boolean bulkActive = bulkToggle != null && bulkToggle.isOn();
                final StatToggle t = statToggles.get(e.getKey());
                if (bulkActive || (t != null && t.isOn())) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    @Override
    public Validator<WindChartSettings> getValidator() {
        return new Validator<WindChartSettings>() {
            @Override
            public String getErrorMessage(final WindChartSettings valueToValidate) {
                String errorMessage = null;
                if (valueToValidate.getResolutionInMilliseconds() < 1) {
                    errorMessage = stringMessages.stepSizeMustBeGreaterThanNull();
                }
                return errorMessage;
            }
        };
    }

    @Override
    public FocusWidget getFocusWidget() {
        return windDirectionSourceCheckboxes.values().iterator().next();
    }
}
