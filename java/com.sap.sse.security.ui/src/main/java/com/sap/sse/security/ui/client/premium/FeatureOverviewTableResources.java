package com.sap.sse.security.ui.client.premium;

import com.google.gwt.user.cellview.client.CellTable;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxStyle;

public interface FeatureOverviewTableResources extends CellTableWithCheckboxResources, CellTable.Resources {
    interface AdminConsoleTableStyle extends CellTableWithCheckboxStyle, CellTable.Style {
        /**
         * Applied to header cells of race columns
         */
        String cellTableRaceColumnHeader();

        /**
         * Applied to header cells of race columns
         */
        String cellTableLegColumnHeader();

        /**
         * Applied to header cells of race columns
         */
        String cellTableLegDetailColumnHeader();

        /**
         * Applied to detail columns
         */
        String cellTableLegDetailColumn();

        /**
         * Applied to race columns
         */
        String cellTableRaceColumn();

        /**
         * Applied to leg columns
         */
        String cellTableLegColumn();

        /**
         * Applied to the totals columns
         */
        String cellTableTotalColumn();

        /**
         * Applied to checkbox column
         */
        String cellTableCheckboxColumnCell();

        /**
         * Applied to the checkbox div when the checkbox is selected
         */
        String cellTableCheckboxSelected();
        
        /**
         * Applied to the checkbox div when the checkbox is deselected
         */
        String cellTableCheckboxDeselected();
        
        /**
         * Applied to the disabled rows
         */
        String cellTableDisabledRow();
    }

    @Override
    @Source({ CellTable.Style.DEFAULT_CSS, "FeatureOverviewTable.css" })
    AdminConsoleTableStyle cellTableStyle();
}
