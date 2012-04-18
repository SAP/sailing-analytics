package com.sap.sailing.gwt.ui.leaderboard;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.ActionCell.Delegate;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class ExpandCollapseButtonCell extends AbstractCell<SafeHtml> {

    private static final LeaderboardResources RESOURCES = GWT.create(LeaderboardResources.class);

    private final SafeHtml html;
    private final Delegate<SafeHtml> delegate;
    private final ExpandableSortableColumn<?> column;

    /**
     * Construct a new {@link ActionCell}.
     * 
     * @param message
     *            the message to display on the button
     * @param delegate
     *            the delegate that will handle events
     */
    public ExpandCollapseButtonCell(ExpandableSortableColumn<?> column, Delegate<SafeHtml> delegate) {
        super("click", "keydown");
        this.column = column;
        this.delegate = delegate;

        String src = column.isExpanded() ? RESOURCES.magnifierSlashedIcon().getSafeUri().asString() : RESOURCES
                .magnifierIcon().getSafeUri().asString();
        this.html = new SafeHtmlBuilder().appendHtmlConstant(
                "<input class=\"valignMiddle magnifier\" type=\"image\" src=\"" + src + "\" />").toSafeHtml();
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, SafeHtml value, NativeEvent event,
            ValueUpdater<SafeHtml> valueUpdater) {
        column.suppressSortingOnce();
        super.onBrowserEvent(context, parent, value, event, valueUpdater);
        if ("click".equals(event.getType())) {
            EventTarget eventTarget = event.getEventTarget();
            if (!Element.is(eventTarget)) {
                return;
            }
            if (parent.getFirstChildElement().isOrHasChild(Element.as(eventTarget))) {
                if (!column.isTogglingInProcess()) {
                    // Ignore clicks that occur outside of the main element.
                    onEnterKeyDown(context, parent, value, event, valueUpdater);
                }
            }
        }
    }

    @Override
    public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
        sb.append(html);
    }

    @Override
    protected void onEnterKeyDown(Context context, Element parent, SafeHtml value, NativeEvent event,
            ValueUpdater<SafeHtml> valueUpdater) {
        delegate.execute(value);
    }
}