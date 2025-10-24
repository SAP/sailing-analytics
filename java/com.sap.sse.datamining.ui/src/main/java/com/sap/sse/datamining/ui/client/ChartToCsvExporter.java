package com.sap.sse.datamining.ui.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;

import com.google.gwt.i18n.client.NumberFormat;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.shared.TextExporter;

/**
 * Provides a service for export of chart data into CSV format.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ChartToCsvExporter {

    private final String exportedMessage;

    public ChartToCsvExporter(String exportedMessage) {
        this.exportedMessage = exportedMessage;
    }

    /**
     * Converts the data of the chart into CSV format and stores it in clipboard. Finally, window alert gets shown
     * to the user with information that there is CSV content in his clipboard.
     * 
     * @param chartToExport
     *            Chart with data to export
     */
    public void exportChartAsCsvToClipboard(Chart chartToExport) {
        String csvContentExport = createCsvExportContentForStatisticsCurve(chartToExport);
        TextExporter.exportToClipboard(csvContentExport);
        Notification.notify(exportedMessage, NotificationType.INFO);
    }

    private static String createCsvExportContentForStatisticsCurve(Chart chartToExport) {
        if (chartToExport != null && chartToExport.getSeries().length > 0) {
            final StringBuilder csvStr = new StringBuilder("Series name");
            final Map<Number, Integer> columnIndexByXValue = new HashMap<>();
            {
                final TreeMap<Number, String> orderedColumnNames = new TreeMap<>((a, b) -> Double.compare(a.doubleValue(), b.doubleValue()));
                // collect column names across all series; some may not have points for all columns
                for (final Series series : chartToExport.getSeries()) {
                    for (Point point : series.getPoints()) {
                        final String columnName = getColumnName(point);
                        orderedColumnNames.put(point.getX(), columnName);
                    }
                }
                for (final String columnName : orderedColumnNames.values()) {
                    csvStr.append(';');
                    if (columnName != null && !columnName.isEmpty()) {
                        csvStr.append(columnName);
                    }
                }
                int columnIndex = 0;
                for (final Entry<Number, String> e : orderedColumnNames.entrySet()) {
                    columnIndexByXValue.put(e.getKey(), columnIndex++);
                }
            }
            csvStr.append("\r\n");
            for (Series series : chartToExport.getSeries()) {
                csvStr.append(series.getName());
                int lastXIndex = -1;
                for (Point point : series.getPoints()) {
                    while (lastXIndex < columnIndexByXValue.get(point.getX())) {
                        csvStr.append(';');
                        lastXIndex++;
                    }
                    csvStr.append(NumberFormat.getDecimalFormat().format(point.getY()));
                }
                csvStr.append("\r\n");
            }
            String csvContentExport = csvStr.toString();
            return csvContentExport;
        }
        return "Statistics are empty";
    }

    private static String getColumnName(Point point) {
        final String pointName = point.getName();
        final String columnName = pointName != null && !pointName.isEmpty() ? pointName : point.getX().toString();
        return columnName;
    }

}
