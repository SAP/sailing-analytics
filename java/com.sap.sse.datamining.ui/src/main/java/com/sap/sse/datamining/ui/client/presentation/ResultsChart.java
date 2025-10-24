package com.sap.sse.datamining.ui.client.presentation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.ChartSubtitle;
import org.moxieapps.gwt.highcharts.client.ChartTitle;
import org.moxieapps.gwt.highcharts.client.Color;
import org.moxieapps.gwt.highcharts.client.Credits;
import org.moxieapps.gwt.highcharts.client.Exporting;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.Series.Type;
import org.moxieapps.gwt.highcharts.client.ToolTip;
import org.moxieapps.gwt.highcharts.client.events.SeriesClickEvent;
import org.moxieapps.gwt.highcharts.client.events.SeriesClickEventHandler;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsData;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsFormatter;
import org.moxieapps.gwt.highcharts.client.labels.DataLabels;
import org.moxieapps.gwt.highcharts.client.labels.DataLabelsData;
import org.moxieapps.gwt.highcharts.client.labels.DataLabelsFormatter;
import org.moxieapps.gwt.highcharts.client.labels.YAxisLabels;
import org.moxieapps.gwt.highcharts.client.plotOptions.SeriesPlotOptions;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.CompoundGroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.ui.client.ChartToCsvExporter;
import com.sap.sse.datamining.ui.client.StringMessages;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.controls.SimpleObjectRenderer;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

public class ResultsChart extends AbstractNumericResultsPresenter<Settings> {
    @FunctionalInterface
    public static interface DrillDownCallback {
        void drillDown(GroupKey groupKey);
    }

    private final Comparator<GroupKey> standardKeyComparator = new Comparator<GroupKey>() {
        @Override
        public int compare(GroupKey key1, GroupKey key2) {
            return key1.compareTo(key2);
        }

        @Override
        public String toString() {
            return getDataMiningStringMessages().groupName();
        };
    };
    private final Comparator<GroupKey> ascendingByValueKeyComparator = new Comparator<GroupKey>() {
        @Override
        public int compare(GroupKey key1, GroupKey key2) {
            double doubleValue1 = currentResultValues.get(key1).doubleValue();
            double doubleValue2 = currentResultValues.get(key2).doubleValue();
            return Double.compare(doubleValue1, doubleValue2);
        }

        @Override
        public String toString() {
            return getDataMiningStringMessages().valueAscending();
        };
    };
    private final Comparator<GroupKey> descendingByValueKeyComparator = new Comparator<GroupKey>() {
        @Override
        public int compare(GroupKey key1, GroupKey key2) {
            return -1 * ascendingByValueKeyComparator.compare(key1, key2);
        }

        @Override
        public String toString() {
            return getDataMiningStringMessages().valueDescending();
        };
    };
    private final Comparator<GroupKey> ascendingByGroupAverageComparator = new Comparator<GroupKey>() {
        @Override
        public int compare(GroupKey key1, GroupKey key2) {
            GroupKey mainKey1 = GroupKey.Util.getMainKey(key1);
            GroupKey mainKey2 = GroupKey.Util.getMainKey(key2);
            return Double.compare(averagePerMainKey.get(mainKey1), averagePerMainKey.get(mainKey2));
        }

        public String toString() {
            return getDataMiningStringMessages().groupAverageAscending();
        };
    };
    private final Comparator<GroupKey> descendingByGroupAverageComparator = new Comparator<GroupKey>() {
        @Override
        public int compare(GroupKey key1, GroupKey key2) {
            return -1 * ascendingByGroupAverageComparator.compare(key1, key2);
        }

        public String toString() {
            return getDataMiningStringMessages().groupAverageDescending();
        };
    };
    private final Comparator<GroupKey> ascendingByGroupMedianComparator = new Comparator<GroupKey>() {
        @Override
        public int compare(GroupKey key1, GroupKey key2) {
            GroupKey mainKey1 = GroupKey.Util.getMainKey(key1);
            GroupKey mainKey2 = GroupKey.Util.getMainKey(key2);
            return Double.compare(medianPerMainKey.get(mainKey1), medianPerMainKey.get(mainKey2));
        }

        public String toString() {
            return getDataMiningStringMessages().groupMedianAscending();
        };
    };
    private final Comparator<GroupKey> descendingByGroupMedianComparator = new Comparator<GroupKey>() {
        @Override
        public int compare(GroupKey key1, GroupKey key2) {
            return -1 * ascendingByGroupMedianComparator.compare(key1, key2);
        }

        public String toString() {
            return getDataMiningStringMessages().groupMedianDescending();
        };
    };

    private final HorizontalPanel sortByPanel;
    private final ValueListBox<Comparator<GroupKey>> keyComparatorListBox;
    private final ValueListBox<Integer> decimalsListBox;
    private final CheckBox showDataLabelsCheckBox;

    private final SimpleLayoutPanel chartPanel;
    private final Chart chart;
    private final DrillDownCallback drillDownCallback;

    /**
     * The series showing the numerical results
     */
    private final Map<GroupKey, Series> seriesMappedByGroupKey;

    /**
     * The optional series visualizing error bars, if available based on the data type; see
     */
    private final Map<GroupKey, Series> errorSeriesMappedByGroupKey;

    private final GroupKey simpleResultSeriesKey;
    private final Map<GroupKey, Integer> mainKeyToXValueMap;
    private final Map<Integer, GroupKey> xValueToMainKeyMap;
    private Map<GroupKey, Number> currentResultValues;
    private Map<GroupKey, Triple<Number, Number, Long>> currentResultErrorMargins;

    private final Map<GroupKey, Double> averagePerMainKey;
    private final Map<GroupKey, Double> medianPerMainKey;
    private final boolean showErrorBars;

    public ResultsChart(Component<?> parent, ComponentContext<?> context, boolean showErrorBars,
            DrillDownCallback drillDownCallback) {
        super(parent, context);
        this.showErrorBars = showErrorBars;
        this.drillDownCallback = drillDownCallback;
        chartPanel = new SimpleLayoutPanel() {
            @Override
            public void onResize() {
                chart.setSizeToMatchContainer();
                chart.redraw();
            }
        };
        chart = createChart();
        chartPanel.setWidget(chart);
        sortByPanel = new HorizontalPanel();
        sortByPanel.setSpacing(5);
        sortByPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        sortByPanel.add(new Label(getDataMiningStringMessages().sortBy() + ":"));
        keyComparatorListBox = new ValueListBox<>(new AbstractRenderer<Comparator<?>>() {
            @Override
            public String render(Comparator<?> object) {
                return object.toString();
            }
        });
        keyComparatorListBox.addValueChangeHandler(e -> {
            resetChartSeries();
            showResultData();
        });
        sortByPanel.add(keyComparatorListBox);
        sortByPanel.setVisible(false);
        addControl(sortByPanel);
        HorizontalPanel decimalsPanel = new HorizontalPanel();
        decimalsPanel.setSpacing(5);
        decimalsPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        decimalsPanel.add(new Label(getDataMiningStringMessages().shownDecimals() + ":"));
        decimalsListBox = new ValueListBox<Integer>(new SimpleObjectRenderer<Integer>());
        decimalsPanel.add(decimalsListBox);
        decimalsListBox.setValue(0);
        decimalsListBox.setAcceptableValues(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        decimalsListBox.addValueChangeHandler(e -> {
            updateChartLabels();
            resetChartSeries();
            showResultData();
        });
        addControl(decimalsPanel);
        HorizontalPanel showDataLabelsPanel = new HorizontalPanel();
        showDataLabelsPanel.setSpacing(5);
        showDataLabelsPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        showDataLabelsPanel.add(new Label(getDataMiningStringMessages().showDataLabels() + ":"));
        showDataLabelsCheckBox = new CheckBox();
        showDataLabelsPanel.add(showDataLabelsCheckBox);
        showDataLabelsCheckBox.setValue(true);
        showDataLabelsCheckBox.addValueChangeHandler(e -> {
            resetChartSeries();
            showResultData();
        });
        addControl(showDataLabelsPanel);
        final StringMessages stringMessages = getDataMiningStringMessages();
        final ChartToCsvExporter csvExporter = new ChartToCsvExporter(stringMessages.csvCopiedToClipboard());
        final Button exportButton = new Button(stringMessages.csvExport());
        exportButton.addClickHandler(e -> csvExporter.exportChartAsCsvToClipboard(chart));
        addControl(exportButton);
        seriesMappedByGroupKey = new HashMap<>();
        errorSeriesMappedByGroupKey = new HashMap<>();
        simpleResultSeriesKey = new GenericGroupKey<>(getDataMiningStringMessages().results());
        mainKeyToXValueMap = new HashMap<>();
        xValueToMainKeyMap = new HashMap<>();
        averagePerMainKey = new HashMap<>();
        medianPerMainKey = new HashMap<>();
    }

    @Override
    protected Widget getPresentationWidget() {
        return chartPanel;
    }

    @Override
    protected void internalShowNumericResults(Map<GroupKey, Number> resultValues,
            Map<GroupKey, Triple<Number, Number, Long>> errorMargins) {
        this.currentResultValues = resultValues;
        this.currentResultErrorMargins = errorMargins;
        decimalsListBox.setValue(getCurrentResult().getValueDecimals(), false);
        updateKeyComparatorListBox();
        resetChartSeries();
        updateChartLabels();
        // see bug 5812: if showResultData() is invoked immediately, the clean-up issued above isn't flushed properly into the chart
        Scheduler.get().scheduleDeferred(this::showResultData);
    }

    private void updateKeyComparatorListBox() {
        boolean visible = false;
        Comparator<GroupKey> valueToBeSelected = standardKeyComparator;
        Collection<Comparator<GroupKey>> acceptableValues = new ArrayList<>();
        acceptableValues.add(valueToBeSelected);
        if (getCurrentResult() != null) {
            if (isCurrentResultSimple()) {
                acceptableValues.add(ascendingByValueKeyComparator);
                acceptableValues.add(descendingByValueKeyComparator);
            } else {
                acceptableValues.add(ascendingByGroupAverageComparator);
                acceptableValues.add(descendingByGroupAverageComparator);
                acceptableValues.add(ascendingByGroupMedianComparator);
                acceptableValues.add(descendingByGroupMedianComparator);
            }
            visible = true;
        }
        keyComparatorListBox.setValue(valueToBeSelected);
        keyComparatorListBox.setAcceptableValues(acceptableValues);
        sortByPanel.setVisible(visible);
    }

    private void resetChartSeries() {
        chart.removeAllSeries(false);
        seriesMappedByGroupKey.clear();
        errorSeriesMappedByGroupKey.clear();
    }

    private void updateChartLabels() {
        chart.getYAxis().setAxisTitleText(getCurrentResult().getResultSignifier());
        chart.setToolTip(new ToolTip().setValueDecimals(decimalsListBox.getValue()));
    }

    private void showResultData() {
        buildMainKeyMapAndSetXAxisCategories();
        createAndAddSeriesToChart();
        Map<GroupKey, List<Number>> valuesPerMainKey = new HashMap<>();
        // sort the stream to avoid Highcharts error 15 (points not sorted by X value in series)
        currentResultValues.entrySet().stream().sorted(
                (e1, e2)->mainKeyToXValueMap.get(GroupKey.Util.getMainKey(e1.getKey())).compareTo(mainKeyToXValueMap.get(GroupKey.Util.getMainKey(e2.getKey()))))
                .forEach(resultEntry->{
            final GroupKey mainKey = GroupKey.Util.getMainKey(resultEntry.getKey());
            final Number value = resultEntry.getValue();
            if (!isCurrentResultSimple()) {
                if (!valuesPerMainKey.containsKey(mainKey)) {
                    valuesPerMainKey.put(mainKey, new ArrayList<Number>());
                }
                valuesPerMainKey.get(mainKey).add(value);
            }
            final Point point = new Point(mainKeyToXValueMap.get(mainKey), value);
            point.setName(mainKey.asString());
            seriesMappedByGroupKey.get(groupKeyToSeriesKey(resultEntry.getKey())).addPoint(point, false, false, false);
            if (showErrorBars) {
                final Triple<Number, Number, Long> errorMargins = currentResultErrorMargins == null ? null
                        : currentResultErrorMargins.get(mainKey);
                if (errorMargins != null) {
                    final Point errorMarginsPoint = new Point(mainKeyToXValueMap.get(mainKey), errorMargins.getA(),
                            errorMargins.getB());
                    errorMarginsPoint.setName(
                            mainKey.asString() + ", " + getDataMiningStringMessages().elements(errorMargins.getC()));
                    errorSeriesMappedByGroupKey.get(groupKeyToSeriesKey(resultEntry.getKey()))
                            .addPoint(errorMarginsPoint, false, false, false);
                }
            }
        });
        averagePerMainKey.clear();
        medianPerMainKey.clear();
        if (!isCurrentResultSimple()) {
            for (GroupKey mainKey : valuesPerMainKey.keySet()) {
                List<Number> values = valuesPerMainKey.get(mainKey);
                averagePerMainKey.put(mainKey, getAverageFromValues(values));
                medianPerMainKey.put(mainKey, getMedianFromValues(values));
            }
        }
        chart.redraw();
    }

    private Double getAverageFromValues(Collection<Number> values) {
        double sum = 0;
        for (Number value : values) {
            sum += value.doubleValue();
        }
        return sum / values.size();
    }

    private Double getMedianFromValues(List<Number> values) {
        Collections.sort(values, new Comparator<Number>() {
            @Override
            public int compare(Number n1, Number n2) {
                return Double.compare(n1.doubleValue(), n2.doubleValue());
            }
        });
        if (values.size() % 2 == 0) {
            int index1 = (values.size() / 2) - 1;
            int index2 = index1 + 1;
            return (values.get(index1).doubleValue() + values.get(index2).doubleValue()) / 2;
        } else {
            int index = ((values.size() + 1) / 2) - 1;
            return values.get(index).doubleValue();
        }
    }

    private void buildMainKeyMapAndSetXAxisCategories() {
        List<GroupKey> sortedMainKeys = getSortedMainKeys();
        String[] categories = new String[sortedMainKeys.size()];
        mainKeyToXValueMap.clear();
        xValueToMainKeyMap.clear();
        for (int i = 0; i < sortedMainKeys.size(); i++) {
            GroupKey mainKey = sortedMainKeys.get(i);
            categories[i] = mainKey.asString();
            mainKeyToXValueMap.put(mainKey, i);
            xValueToMainKeyMap.put(i, mainKey);
        }
        chart.getXAxis().setCategories(false, categories);
    }

    private List<GroupKey> getSortedMainKeys() {
        Collection<GroupKey> mainKeySet = new HashSet<>();
        for (GroupKey groupKey : getCurrentResult().getResults().keySet()) {
            mainKeySet.add(GroupKey.Util.getMainKey(groupKey));
        }
        List<GroupKey> sortedKeys = new ArrayList<>(mainKeySet);
        Collections.sort(sortedKeys, getKeyComparator());
        return sortedKeys;
    }

    private Comparator<GroupKey> getKeyComparator() {
        return keyComparatorListBox.getValue();
    }

    private void createAndAddSeriesToChart() {
        for (final GroupKey groupKey : getCurrentResult().getResults().keySet()) {
            final GroupKey seriesKey = groupKeyToSeriesKey(groupKey);
            if (!seriesMappedByGroupKey.containsKey(seriesKey)) {
                seriesMappedByGroupKey.put(seriesKey, chart.createSeries().setName(seriesKey.asString()));
                if (showErrorBars) {
                    errorSeriesMappedByGroupKey.put(seriesKey, chart.createSeries().setType(Type.ERRORBAR).setName(
                            seriesKey.asString() + " " + getDataMiningStringMessages().dataMiningErrorMargins()));
                }
            }
        }
        final List<GroupKey> sortedSeriesKeys = new ArrayList<>(seriesMappedByGroupKey.keySet());
        Collections.sort(sortedSeriesKeys);
        for (final GroupKey seriesKey : sortedSeriesKeys) {
            chart.addSeries(seriesMappedByGroupKey.get(seriesKey), false, false);
            if (showErrorBars) {
                chart.addSeries(errorSeriesMappedByGroupKey.get(seriesKey), false, false);
            }
        }
    }

    private GroupKey groupKeyToSeriesKey(GroupKey groupKey) {
        if (groupKey.hasSubKeys()) {
            List<? extends GroupKey> subKeys = GroupKey.Util.getSubKeys(groupKey);
            return subKeys.size() == 1 ? subKeys.get(0) : new CompoundGroupKey(subKeys);
        } else {
            return simpleResultSeriesKey;
        }
    }

    private class SeriesClickHandler implements SeriesClickEventHandler {
        @Override
        public boolean onClick(SeriesClickEvent seriesClickEvent) {
            final double xAxisValue = seriesClickEvent.getNearestXAsDouble();
            final GroupKey groupKey = xValueToMainKeyMap.get((int) Math.round(xAxisValue));
            drillDown(groupKey);
            return true;
        }
    }

    private Chart createChart() {
        Chart chart = new Chart().setType(Series.Type.COLUMN).setMarginLeft(100).setMarginRight(45).setWidth100()
                .setHeight100().setBorderColor(new Color("#F0AB00")).setPlotBorderWidth(0)
                .setTitle(new ChartTitle().setText(""), new ChartSubtitle().setText(""))
                .setCredits(new Credits().setEnabled(false));
        chart.setExporting(new Exporting().setEnabled(false));
        chart.getXAxis().setAllowDecimals(false);
        chart.getYAxis().setAxisTitleText("Result").setLabels(new YAxisLabels().setFormatter(new AxisLabelsFormatter() {
            @Override
            public String format(AxisLabelsData axisLabelsData) {
                try {
                    return axisLabelsData.getValueAsDouble() + "";
                } catch (Exception e) {
                    return "";
                }
            }
        }));
        chart.setSeriesPlotOptions(new SeriesPlotOptions()
                .setDataLabels(new DataLabels().setEnabled(true).setFormatter(new DataLabelsFormatter() {
                    @Override
                    public String format(DataLabelsData dataLabelsData) {
                        String dataLabel = String.valueOf(BigDecimal.valueOf(dataLabelsData.getYAsDouble())
                                .setScale(decimalsListBox.getValue(), RoundingMode.HALF_UP).doubleValue());
                        return showDataLabelsCheckBox.getValue() ? dataLabel : null;
                    }
                })).setSeriesClickEventHandler(new SeriesClickHandler()));
        return chart;
    }

    /**
     * Attempts a drill-down for the {@code groupKey}. If a drill-down callback has been provided, it will be invoked;
     * otherwise, this is a no-op.
     * 
     * @return whether or not a drill-down was issued
     */
    public void drillDown(GroupKey groupKey) {
        if (drillDownCallback != null) {
            drillDownCallback.drillDown(groupKey);
        }
    }

    @Override
    public String getLocalizedShortName() {
        return getDataMiningStringMessages().resultsChart();
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public SettingsDialogComponent<Settings> getSettingsDialogComponent(Settings settings) {
        return null;
    }

    @Override
    public void updateSettings(Settings newSettings) {
        // no-op
    }

    @Override
    public String getDependentCssClassName() {
        return "resultsChart";
    }

    @Override
    public Settings getSettings() {
        return null;
    }

    @Override
    public String getId() {
        return "rc";
    }
}
