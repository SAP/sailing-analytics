package com.sap.sailing.gwt.ui.leaderboard;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.ImageCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Header;
import com.sap.sailing.gwt.ui.client.StringConstants;

/**
 * A {@link CellTable} {@link Header} implementation that uses a {@link CompositeCell} containing a
 * {@link TextCell} and optionally an {@link ActionCell} for an expand/close button and an {@link ImageCell}
 * for a medal displayed for medal races.
 */
public class SortableExpandableColumnHeader extends Header<SafeHtml> {
    private static class ExpandCollapseButtonAction implements ActionCell.Delegate<SafeHtml> {
        private final ExpandableSortableColumn<?> column;

        private ExpandCollapseButtonAction(ExpandableSortableColumn<?> column) {
            this.column = column;
        }

        @Override
        public void execute(SafeHtml object) {
            column.toggleExpansion();
        }
    }

    interface RaceCellTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<span class=\"race-name\">{0}</span>")
        SafeHtml cell(SafeHtml value);
    }
    
    private static RaceCellTemplates template = GWT.create(RaceCellTemplates.class);
    
    public SortableExpandableColumnHeader(String title, String iconURL, LeaderboardPanel leaderboardPanel,
            ExpandableSortableColumn<?> column, StringConstants stringConstants) {
        super(constructCell(title, iconURL, column.isExpansionEnabled(), leaderboardPanel, column, stringConstants));
    }

    private static <T> Cell<SafeHtml> constructCell(final String title, final String iconURL, boolean isExpansionEnabled,
            final LeaderboardPanel leaderboardPanel, final ExpandableSortableColumn<?> column, final StringConstants stringConstants) {
        final List<HasCell<SafeHtml, ?>> cells = new ArrayList<HasCell<SafeHtml, ?>>(3);
        // if it's a medal race, add the cell rendering the medal image
        // add the cell rendering the expand/collapse button:
        if (isExpansionEnabled) {
            cells.add(new HasCell<SafeHtml, SafeHtml>() {
                @Override
                public Cell<SafeHtml> getCell() {
                    return new ExpandCollapseButtonCell(column, new ExpandCollapseButtonAction(column));
                }

                @Override
                public FieldUpdater<SafeHtml, SafeHtml> getFieldUpdater() {
                    return null; // no updates possible in a header cell
                }

                @Override
                public SafeHtml getValue(SafeHtml object) {
                    return null;
                }
            });
        }
        if (iconURL != null) {
            cells.add(new HasCell<SafeHtml, String>() {
                @Override
                public Cell<String> getCell() {
                    return new ImageCell();
                }

                @Override
                public FieldUpdater<SafeHtml, String> getFieldUpdater() {
                    return null; // no updates possible in a header cell
                }

                @Override
                public String getValue(SafeHtml object) {
                    return iconURL;
                }
            });
        }
        // add the cell rendering the race name:
        cells.add(new HasCell<SafeHtml, SafeHtml>() {
            @Override
            public Cell<SafeHtml> getCell() {
                return new SafeHtmlCell();
            }
            @Override
            public FieldUpdater<SafeHtml, SafeHtml> getFieldUpdater() {
                return null; // no updates possible in a header cell
            }
            @Override
            public SafeHtml getValue(SafeHtml object) {
                return template.cell(SafeHtmlUtils.fromString(title));
            }
        });
        
        CompositeCell<SafeHtml> abc = new CompositeCell<SafeHtml>(cells) {
            /**
             * Redefining this method because when a table column is sorted, GWT wraps a div element
             * around the column header. Subsequently, the div's index no longer corresponds with the
             * cell indexes and a isOrHasChild doesn't make sense. We need to drill into the div's
             * elements and skip the sort indicator
             */
            @Override
            public void onBrowserEvent(Context context, Element parent, SafeHtml value, NativeEvent event,
                    ValueUpdater<SafeHtml> valueUpdater) {
                int index = 0;
                EventTarget eventTarget = event.getEventTarget();
                if (Element.is(eventTarget)) {
                    Element target = eventTarget.cast();
                    Element container = getContainerElement(parent);
                    Element wrapper = container.getFirstChildElement();
                    try {
                        DivElement.as(wrapper);
                        // this must be a div inserted by the table after the column was sorted;
                        // delegate on to the div's second child's child; note that this is highly
                        // implementation-dependant and may easily break. We should probably file
                        // a bug with Google...
                        wrapper = wrapper.getFirstChildElement().getNextSiblingElement().getFirstChildElement();
                    } catch (Throwable t) {
                        // wrapper was no div, so no action necessary
                    }
                    while (wrapper != null) {
                        if (wrapper.isOrHasChild(target)) {
                            @SuppressWarnings("unchecked")
                            Cell<Object> cell = (Cell<Object>) cells.get(index).getCell();
                            cell.onBrowserEvent(context, wrapper, cells.get(index).getValue(value), event, null); // tempUpdater
                        }
                        index++;
                        wrapper = wrapper.getNextSiblingElement();
                    }
                }
            }
        };
        return abc;
    }

    @Override
    public SafeHtml getValue() {
        return null;
    }
    
}

