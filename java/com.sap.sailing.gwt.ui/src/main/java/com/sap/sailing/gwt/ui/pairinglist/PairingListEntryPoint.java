package com.sap.sailing.gwt.ui.pairinglist;

import java.util.List;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.PairingListDTO;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.common.communication.routing.ProvidesLeaderboardRouting;
import com.sap.sailing.gwt.ui.adminconsole.PairingListPreviewDialog;
import com.sap.sailing.gwt.ui.client.AbstractSailingReadEntryPoint;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Color;
import com.sap.sse.gwt.settings.SettingsToUrlSerializer;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class PairingListEntryPoint extends AbstractSailingReadEntryPoint implements ProvidesLeaderboardRouting {

    private PairingListContextDefinition pairingListContextDefinition;

    private StringMessages stringMessages = StringMessages.INSTANCE;
    private StrippedLeaderboardDTO strippedLeaderboardDTO;
    private String leaderboardName;
    
    @Override
    protected void doOnModuleLoad() {
        super.doOnModuleLoad();
        pairingListContextDefinition = new SettingsToUrlSerializer()
                .deserializeFromCurrentLocation(new PairingListContextDefinition());
        leaderboardName = pairingListContextDefinition.getLeaderboardName();
        getSailingService().getLeaderboard(leaderboardName,
                new AsyncCallback<StrippedLeaderboardDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        strippedLeaderboardDTO = null;
                    }

                    @Override
                    public void onSuccess(StrippedLeaderboardDTO result) {
                        strippedLeaderboardDTO = result;
                        createUI();
                    }
                });
    }

    private void createUI() {
        DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.PX);
        ScrollPanel scrollPanel = new ScrollPanel();
        RootLayoutPanel.get().add(mainPanel);
        mainPanel.setWidth("100%");
        mainPanel.setHeight("100%");
        SAPSailingHeaderWithAuthentication header = new SAPSailingHeaderWithAuthentication(
                pairingListContextDefinition.getLeaderboardName());
        PaywallResolver paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
        new FixedSailingAuthentication(getUserService(), paywallResolver, header.getAuthenticationMenuView());
        mainPanel.addNorth(header, 75);
        VerticalPanel contentPanel = new VerticalPanel();
        contentPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        contentPanel.setWidth("100%");
        contentPanel.getElement().getStyle().setProperty("marginTop", "15px");
        contentPanel.getElement().getStyle().setProperty("marginBottom", "15px");
        scrollPanel.add(contentPanel);
        getSailingService().getPairingListFromRaceLogs(pairingListContextDefinition.getLeaderboardName(),
                new AsyncCallback<PairingListDTO>() {
                    @Override
                    public void onSuccess(PairingListDTO result) {
                        if (strippedLeaderboardDTO != null) {
                            getSailingService().getRaceDisplayNamesFromLeaderboard(strippedLeaderboardDTO.getName(),
                                    result.getRaceColumnNames(), new AsyncCallback<List<String>>() {
                                        @Override
                                        public void onFailure(Throwable caught) {
                                            HTML lbl = new HTML(
                                                    "<h2>" + stringMessages.noPairingListAvailable() + "</h2>");
                                            lbl.getElement().getStyle().setColor(Color.BLACK.toString());
                                            contentPanel.add(lbl);
                                        }

                                        @Override
                                        public void onSuccess(List<String> raceDisplayNames) {
                                            Button btn = new Button(getStringMessages().print());
                                            contentPanel.add(btn);
                                            Widget pairingListPanel = createPairingListPanel(result, raceDisplayNames);
                                            contentPanel.add(pairingListPanel);
                                            btn.addClickHandler(new ClickHandler() {
                                                @Override
                                                public void onClick(ClickEvent event) {
                                                    printPairingListGrid(
                                                            "<div class='printHeader'><img src='images/home/logo-small@2x.png' />"
                                                                    + "<b class='title'>"
                                                                    + SafeHtmlUtils.fromString(pairingListContextDefinition.getLeaderboardName())
                                                                            .asString()
                                                                    + "</b></div>" + pairingListPanel.asWidget().getElement().getInnerHTML());
                                                }
                                            });

                                        }

                                    });
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        try {
                            throw caught;
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }

                    }
                });
        mainPanel.add(scrollPanel);
    }

    private Widget createPairingListPanel(PairingListDTO pairingListDTO, final List<String> raceDisplayNames) {
        final Widget pairingListPanel = new PairingListPreviewDialog(pairingListDTO, raceDisplayNames, stringMessages, pairingListContextDefinition.getLeaderboardName()).
                getPairingListGrid();
        pairingListPanel.getElement().getStyle().setProperty("marginTop", "15px");
        return pairingListPanel;
    }

    private native void printPairingListGrid(String pageHTMLContent) /*-{
		var frameID = '__gwt_historyFrame';
		var frame = $doc.getElementById(frameID);
		if (!frame) {
			$wnd.alert("Error: Can not find frame '" + frameID + "'");
			return;
		}
		frame = frame.contentWindow;
		var document = frame.document;
		document.open();
		document.write(pageHTMLContent);

		//adding style to doc
		var css = "body { background: #fff; font-family: 'Open Sans', Arial, Verdana, sans-serif;"
				+ "line-height: 1; font-weight: 400; border: 0 }"
				+ ".title { font-size: 18px; text-align: center; float: right; color: #f6f9fc; margin-bottom: 0.466666666666667em; margin-right: 0.466666666666667em }"
				+ "img { max-height: 2em; float:left; margin-top: 0.466666666666667em; margin-left: 0.466666666666667em }"
				+ ".printHeader { font-size: 1rem; background: #333; border-bottom: 0.333333333333333em solid #f0ab00;"
				+ "height: 3.333333333333333em; line-height: 3em; width: 100%; overflow: hidden;}"
				+ "table { border-collapse: collapse; border: 1px solid black; margin: auto; width: 100%}"
				+ "td { font-size: 13px; }"
		head = document.head || document.getElementsByTagName('head')[0];
		style = document.createElement('style');
		style.type = 'text/css';
		if (style.styleSheet) {
			style.styleSheet.cssText = css;
		} else {
			style.appendChild(document.createTextNode(css));
		}
		head.appendChild(style);

		document.close();

		//Timeout for assets loading
		setTimeout(function() {
			frame.focus();
			frame.print();
		}, 100);
    }-*/;

    @Override
    public String getLeaderboardName() {
        return leaderboardName;
    }
}
