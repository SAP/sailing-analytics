package com.sap.sailing.gwt.ui.client;

import com.google.gwt.user.cellview.client.CellTable;

public interface AdminConsoleTableResources extends CellTable.Resources {
    interface AdminConsoleTableStyle extends CellTable.Style {
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

    }

    @Override
    @Source({ CellTable.Style.DEFAULT_CSS, "AdminConsoleTable.css" })
    AdminConsoleTableStyle cellTableStyle();
}
