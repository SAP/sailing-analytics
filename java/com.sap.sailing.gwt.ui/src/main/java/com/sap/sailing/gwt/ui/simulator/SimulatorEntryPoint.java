package com.sap.sailing.gwt.ui.simulator;

import static com.sap.sse.common.HttpRequestHeaderConstants.HEADER_FORWARD_TO_REPLICA;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.ui.client.AbstractSailingReadEntryPoint;
import com.sap.sailing.gwt.ui.client.SimulatorService;
import com.sap.sailing.gwt.ui.client.SimulatorServiceAsync;
import com.sap.sailing.landscape.common.RemoteServiceMappingConstants;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.gwt.resources.Highcharts;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.NamedSecuredObjectDTO;
import com.sap.sse.security.ui.authentication.decorator.AuthorizedContentDecorator;
import com.sap.sse.security.ui.authentication.decorator.WidgetFactory;
import com.sap.sse.security.ui.authentication.generic.GenericAuthentication;
import com.sap.sse.security.ui.authentication.generic.GenericAuthorizedContentDecorator;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class SimulatorEntryPoint extends AbstractSailingReadEntryPoint {
    private final SimulatorServiceAsync simulatorService = GWT.create(SimulatorService.class);
    private int xRes = 40;
    private int yRes = 20;
    private int border = 0;
    
    private StreamletParameters streamletPars = new StreamletParameters();

    private boolean autoUpdate = false;
    private char mode = SailingSimulatorConstants.ModeEvent;  // default mode: 'e'vent
    private char event = SailingSimulatorConstants.EventKielerWoche; // default event: 'k'ieler woche

    private boolean showArrows = false; // show the wind arrows in wind display and replay modes.    
    private boolean showGrid = false;   // show the "heat map" in the wind display and replay modes.
    private boolean showLines = false;  // show the wind lines in the wind display and replay modes.
    private boolean showMapControls = true; // show the map controls such as zoom and pan
    private char seedLines = 'b';  // seed lines at: 'b'ack, 'f'ront
    private boolean showLineGuides = false; // show the wind streamlets in the wind display and replay modes.
    private boolean showStreamlets = true; // show animated wind streamlets in the wind display and replay modes.
    
    
    private static Logger logger = Logger.getLogger(SimulatorEntryPoint.class.getName());

    @Override
    protected void doOnModuleLoad() {
        Highcharts.ensureInjectedWithExport();
        super.doOnModuleLoad();
        EntryPointHelper.registerASyncService((ServiceDefTarget) simulatorService, RemoteServiceMappingConstants.simulatorServiceRemotePath, HEADER_FORWARD_TO_REPLICA);
        checkUrlParameters();
        getUserService().executeWithServerInfo(serverInfo->createSimulatorPanel(serverInfo));
    }

    private void checkUrlParameters() {
        String horizontalRes = Window.Location.getParameter("horizontalRes");
        if (horizontalRes == null || horizontalRes.isEmpty()) {
           logger.config("Using default horizontal resolution " + xRes);
        } else {
            xRes = Integer.parseInt(horizontalRes);
        }
        String verticalRes = Window.Location.getParameter("verticalRes");
        if (verticalRes == null || verticalRes.isEmpty()) {
            logger.config("Using default horizontal resolution " + yRes);
        } else {
            yRes = Integer.parseInt(verticalRes);
        }
        String border = Window.Location.getParameter("border");
        if (border == null || border.isEmpty()) {
           logger.config("Using default border " + this.border);
        } else {
            this.border = Integer.parseInt(border);
        }
        streamletPars.macroWeather = false;
        streamletPars.motionScale = 1.0;
        streamletPars.swarmScale = 1.0;
        streamletPars.detailZoom = 15;
        String tmpStr = Window.Location.getParameter("motionScale");
        if (tmpStr == null || tmpStr.isEmpty()) {
           logger.config("Using default motionScale.");
        } else {
            this.streamletPars.motionScale = Double.parseDouble(tmpStr);
        }
        tmpStr = Window.Location.getParameter("swarmScale");
        if (tmpStr == null || tmpStr.isEmpty()) {
           logger.config("Using default swarmScale.");
        } else {
            this.streamletPars.swarmScale = Double.parseDouble(tmpStr);
        }
        tmpStr = Window.Location.getParameter("lineBase");
        if (tmpStr == null || tmpStr.isEmpty()) {
           logger.config("Using default lineBase.");
        } else {
            this.streamletPars.lineBase = Double.parseDouble(tmpStr);
        }
        tmpStr = Window.Location.getParameter("lineScale");
        if (tmpStr == null || tmpStr.isEmpty()) {
           logger.config("Using default lineScale.");
        } else {
            this.streamletPars.lineScale = Double.parseDouble(tmpStr);
        }
        tmpStr = Window.Location.getParameter("detailZoom");
        if (tmpStr == null || tmpStr.isEmpty()) {
           logger.config("Using default detailZoom.");
        } else {
            this.streamletPars.detailZoom = Integer.parseInt(tmpStr);
        }
        String autoUpdateStr = Window.Location.getParameter("autoUpdate");
        if (autoUpdateStr == null || autoUpdateStr.isEmpty()) {
            logger.config("Using default auto update " + autoUpdate);
        } else {
            autoUpdate = Boolean.parseBoolean(autoUpdateStr);
        }
        String showMapControlsStr = Window.Location.getParameter("showMapControls");
        if (showMapControlsStr == null || showMapControlsStr.isEmpty()) {
            logger.config("Using default showMapControls " + showMapControls);
        } else {
            showMapControls = Boolean.parseBoolean(showMapControlsStr);
        }
        String modeStr = Window.Location.getParameter("mode");
        if (modeStr == null || modeStr.isEmpty()) {
            logger.config("Using default mode " + mode);
        } else {
            mode = modeStr.charAt(0);
            if (mode == SailingSimulatorConstants.ModeMeasured) {
                showArrows = true; // show the wind arrows in wind display and replay modes.    
                showGrid = false;   // show the "heat map" in the wind display and replay modes.
                showLines = false;  // show the wind lines in the wind display and replay modes.
                showLineGuides = false; // show the wind streamlets in the wind display and replay modes.
            }
        }
        String eventStr = Window.Location.getParameter("event");
        if (eventStr == null || eventStr.isEmpty()) {
            logger.config("Using default event: " + event);
        } else {
            event = eventStr.charAt(0);
        }
        String windDisplayStr = Window.Location.getParameter("windDisplay");
        if (windDisplayStr == null || windDisplayStr.isEmpty()) {
            logger.config("Using default showGrid " + showGrid + " & default showArrows " + showArrows);
        } else {
            if (windDisplayStr.contains("g")) {
                showGrid = true;
            } else {
                showGrid = false;
            }
            if (windDisplayStr.contains("l")) {
                showLines = true;
            } else {
                showLines = false;
            }
            if (windDisplayStr.contains("a")) {
                showArrows = true;
            } else {
                showArrows = false;
            }
            if (windDisplayStr.contains("s")) {
                showLineGuides = true;
            } else {
                showLineGuides = false;
            }
            if (windDisplayStr.contains("z")) {
                showStreamlets = true;
            } else {
                showStreamlets = false;
            }
            if (windDisplayStr.contains("b")) {
                seedLines = 'b';
            }
            if (windDisplayStr.contains("f")) {
                seedLines = 'f';
            }
            if (windDisplayStr.contains("m")) {
                streamletPars.macroWeather = true;
            }
        }
        if ((showStreamlets)&&(border==null)) {
            this.border = 10;
        }
    }

    private void createSimulatorPanel(ServerInfoDTO serverInfo) {
        SAPSailingHeaderWithAuthentication header  = new SAPSailingHeaderWithAuthentication(getStringMessages().strategySimulatorTitle());
        PaywallResolver paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
        GenericAuthentication genericSailingAuthentication = new FixedSailingAuthentication(getUserService(), paywallResolver, header.getAuthenticationMenuView());
        AuthorizedContentDecorator authorizedContentDecorator = new GenericAuthorizedContentDecorator(genericSailingAuthentication);
        final String serverName = serverInfo.getName();
        authorizedContentDecorator.setPermissionToCheck(NamedSecuredObjectDTO.create(serverName,
                SecuredDomainType.SIMULATOR, new TypeRelativeObjectIdentifier(serverName)), DefaultActions.READ);
        authorizedContentDecorator.setContentWidgetFactory(new WidgetFactory() {
            @Override
            public Widget get() {
                SimulatorMainPanel simulatorPanel = new SimulatorMainPanel(simulatorService, getStringMessages(), SimulatorEntryPoint.this, xRes, yRes, border, streamletPars,
                        autoUpdate, mode, event, showGrid, showLines, seedLines, showArrows, showLineGuides, showStreamlets, showMapControls, getUserService());
                return simulatorPanel;
            }
        });
        RootLayoutPanel rootPanel = RootLayoutPanel.get();
        DockLayoutPanel panel = new DockLayoutPanel(Unit.PX);
        panel.addNorth(header, 75);
        panel.add(authorizedContentDecorator);
        panel.addStyleName("dockLayoutPanel");
        rootPanel.add(panel);
    }
}
