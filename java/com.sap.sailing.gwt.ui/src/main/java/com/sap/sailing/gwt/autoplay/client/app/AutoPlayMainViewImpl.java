package com.sap.sailing.gwt.autoplay.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.DefaultErrorReporter;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.sapheader.SAPHeader;

public class AutoPlayMainViewImpl extends ResizeComposite
        implements ApplicationTopLevelView, AcceptsOneWidget {
    public static final int SAP_HEADER_IN_PX = 75;

    private static SixtyInchViewImplUiBinder uiBinder = GWT.create(SixtyInchViewImplUiBinder.class);

    @UiField
    protected LayoutPanel mainPanel;

    protected AnimationPanel animationPanel = new AnimationPanel();

    protected SAPHeader sapHeader = new SAPHeader(SAPSailingHeaderWithAuthentication.SAP_SAILING_APP_NAME,
            SAPSailingHeaderWithAuthentication.SAP_SAILING_URL);

    private static ErrorReporter errorReporter = new DefaultErrorReporter<StringMessages>(StringMessages.INSTANCE);

    interface SixtyInchViewImplUiBinder extends UiBinder<Widget, AutoPlayMainViewImpl> {
    }

    public AutoPlayMainViewImpl(EventBus eventBus) {
        initWidget(uiBinder.createAndBindUi(this));
        sapHeader.setHeaderTitle("Initializing");
        mainPanel.add(sapHeader);
        mainPanel.setWidgetTopHeight(sapHeader, 0, Unit.PX, SAP_HEADER_IN_PX, Unit.PX);
        eventBus.addHandler(AutoPlayHeaderEvent.TYPE, new AutoPlayHeaderEvent.Handler() {

            @Override
            public void onHeaderChanged(AutoPlayHeaderEvent event) {
                sapHeader.setHeaderTitle(event.getHeaderText());
                sapHeader.setHeaderSubTitle(event.getHeaderSubText());
            }
        });
        mainPanel.add(animationPanel);
        mainPanel.setWidgetTopBottom(animationPanel, 75, Unit.PX, 0, Unit.PX);
    }

    @Override
    public void setWidget(IsWidget widgetToShow) {
        animationPanel.add(widgetToShow);
    }

    @Override
    public AcceptsOneWidget getContent() {
        return this;
    }

    @Override
    public void showLoading(boolean visible) {
    }

    @Override
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }
}
