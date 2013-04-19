package com.sap.sailing.gwt.ui.client;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;

public class LogoAndTitlePanel extends FlowPanel implements RequiresResize {
    private final String title;
    private final String subtitle;
    private Label titleLabel; 
    private Label subTitleLabel;
    private final Anchor sapLogo;
    private boolean lastIsSmallWidth;
    private final WindowSizeDetector windowSizeDetector;
    private final StringMessages stringMessages;

    public LogoAndTitlePanel(StringMessages stringConstants, WindowSizeDetector windowSizeDetector) {
        this(null, null, stringConstants, windowSizeDetector);
    }

    public LogoAndTitlePanel(String title, StringMessages stringConstants, WindowSizeDetector windowSizeDetector) {
        this(title, null, stringConstants, windowSizeDetector);
    }

    public LogoAndTitlePanel(String title, String subtitle, StringMessages stringMessages, WindowSizeDetector windowSizeDetector) {
        this.title = title;
        this.subtitle = subtitle;
        this.stringMessages = stringMessages;
        this.windowSizeDetector = windowSizeDetector;
        lastIsSmallWidth = windowSizeDetector.isSmallWidth();
        sapLogo = new Anchor(new SafeHtmlBuilder().appendHtmlConstant(
                "<img class=\"linkNoBorder\" src=\"/gwt/images/sap_66_transparent.png\"/>").toSafeHtml());
        sapLogo.setHref("http://www.sap.com");
        sapLogo.addStyleName("sapLogo");
        this.add(sapLogo);
        renderUI();
    }

    private void renderUI() {
        for (int i=getWidgetCount()-1; i>=0; i--) {
            if (getWidget(i) != sapLogo) {
                remove(i);
            }
        }
        if (!lastIsSmallWidth) {
            FlowPanel sailingAnalyticsLabelPanel = new FlowPanel();
            Label sailingAnalyticsLabel = new Label(stringMessages.sapSailingAnalytics());
            sailingAnalyticsLabelPanel.add(sailingAnalyticsLabel);
            sailingAnalyticsLabelPanel.addStyleName("sailingAnalyticsLabelPanel");
            sailingAnalyticsLabel.addStyleName("sailingAnalyticsLabel boldLabel");
            this.add(sailingAnalyticsLabelPanel);
        }
        if (!lastIsSmallWidth && title != null) {
            FlowPanel titleLabelWrapper = new FlowPanel();
            titleLabelWrapper.addStyleName("titleLabelWrapper");
            titleLabel = new Label(title);
            titleLabel.addStyleName("titleLabel");
            titleLabelWrapper.add(titleLabel);
            this.add(titleLabelWrapper);
        }
        if (subtitle != null) {
            subTitleLabel = new Label(subtitle);
            FlowPanel subTitleLabelWrapper = new FlowPanel();
            if (lastIsSmallWidth) {
                subTitleLabelWrapper.addStyleName("titleLabelWrapper");
                subTitleLabel.addStyleName("titleLabelRight");
            } else {
                subTitleLabelWrapper.addStyleName("subTitleLabelWrapper");
                subTitleLabel.addStyleName("subTitleLabel");
            }
            subTitleLabelWrapper.add(subTitleLabel);
            this.add(subTitleLabelWrapper);
        }
    }

    public String getTitle() {
        return titleLabel != null ? titleLabel.getText() : null;
    }

    public void setTitle(String title) {
        if(titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    public String getSubTitle() {
        return subTitleLabel != null ? subTitleLabel.getText() : null;
    }

    public void setSubTitle(String subTitle) {
        if(subTitleLabel != null) {
            subTitleLabel.setText(subTitle);
        }
    }

    @Override
    public void onResize() {
        if (windowSizeDetector.isSmallWidth() != lastIsSmallWidth) {
            lastIsSmallWidth = windowSizeDetector.isSmallWidth();
            renderUI();
        }
        
    }   
}
