package com.sap.sailing.gwt.ui.datamining;

import static com.sap.sse.common.HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER;
import static com.sap.sse.common.HttpRequestHeaderConstants.HEADER_FORWARD_TO_REPLICA;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.ui.client.AbstractSailingReadEntryPoint;
import com.sap.sailing.gwt.ui.client.RemoteServiceMappingConstants;
import com.sap.sailing.gwt.ui.datamining.presentation.TabbedSailingResultsPresenter;
import com.sap.sailing.gwt.ui.shared.settings.SailingSettingsConstants;
import com.sap.sse.datamining.shared.DataMiningSession;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.UUIDDataMiningSession;
import com.sap.sse.datamining.shared.impl.dto.ModifiableStatisticQueryDefinitionDTO;
import com.sap.sse.datamining.ui.client.AnchorDataMiningSettingsControl;
import com.sap.sse.datamining.ui.client.CompositeResultsPresenter;
import com.sap.sse.datamining.ui.client.DataMiningService;
import com.sap.sse.datamining.ui.client.DataMiningServiceAsync;
import com.sap.sse.datamining.ui.client.DataMiningSettingsControl;
import com.sap.sse.datamining.ui.client.DataMiningSettingsInfoManager;
import com.sap.sse.datamining.ui.client.DataMiningWriteService;
import com.sap.sse.datamining.ui.client.DataMiningWriteServiceAsync;
import com.sap.sse.datamining.ui.client.execution.SimpleQueryRunner;
import com.sap.sse.datamining.ui.client.selection.QueryDefinitionProviderWithControls;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.gwt.client.shared.components.ComponentResources;
import com.sap.sse.gwt.client.xdstorage.CrossDomainStorage;
import com.sap.sse.gwt.resources.Highcharts;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.ui.authentication.decorator.AuthorizedContentDecorator;
import com.sap.sse.security.ui.authentication.decorator.WidgetFactory;
import com.sap.sse.security.ui.authentication.generic.GenericAuthentication;
import com.sap.sse.security.ui.authentication.generic.GenericAuthorizedContentDecorator;
import com.sap.sse.security.ui.authentication.generic.sapheader.SAPHeaderWithAuthentication;

public class DataMiningEntryPoint extends AbstractSailingReadEntryPoint {

    public static final ComponentResources resources = GWT.create(ComponentResources.class);
    private static final Logger LOG = Logger.getLogger(DataMiningEntryPoint.class.getName());

    private static final DataMiningResources dataMiningResources = GWT.create(DataMiningResources.class);

    private final DataMiningServiceAsync dataMiningService = GWT.create(DataMiningService.class);
    private final DataMiningWriteServiceAsync dataMiningWriteService = GWT.create(DataMiningWriteService.class);
    private DataMiningSession session;

    private QueryDefinitionProviderWithControls queryDefinitionProvider;
    private CompositeResultsPresenter<?> resultsPresenter;
    private Integer resultsPresenterSouthHeight = 350;
    private Integer resultsPresenterEastWidth = 750;

    private SplitLayoutPanel queryAndResultSplitPanel;
    private boolean queryAndResultAreVertical = true;

    @Override
    protected void doOnModuleLoad() {
        Highcharts.ensureInjectedWithMore();
        super.doOnModuleLoad();
        session = new UUIDDataMiningSession(UUID.randomUUID());
        EntryPointHelper.registerASyncService((ServiceDefTarget) dataMiningService,
                RemoteServiceMappingConstants.dataMiningServiceRemotePath, HEADER_FORWARD_TO_REPLICA);
        EntryPointHelper.registerASyncService((ServiceDefTarget) dataMiningWriteService,
                RemoteServiceMappingConstants.dataMiningServiceRemotePath, HEADER_FORWARD_TO_MASTER);
        getUserService().executeWithServerInfo(s -> createDataminingPanel(s, Window.Location.getParameter("q")));
    }

    private void createDataminingPanel(ServerInfoDTO serverInfo, final String queryIdentifier) {
        removeUrlParameter();;
        SAPHeaderWithAuthentication header = new SAPSailingHeaderWithAuthentication(getStringMessages().dataMining());
        GenericAuthentication genericSailingAuthentication = new FixedSailingAuthentication(getUserService(),
                header.getAuthenticationMenuView());
        AuthorizedContentDecorator authorizedContentDecorator = new GenericAuthorizedContentDecorator(
                genericSailingAuthentication);
        authorizedContentDecorator.setPermissionToCheck(serverInfo, ServerActions.DATA_MINING);
        authorizedContentDecorator.setContentWidgetFactory(new WidgetFactory() {

            private SimpleQueryRunner queryRunner;
            private final DataMiningSettingsInfoManager settingsManager = new DataMiningSettingsInfoManagerImpl(
                    getStringMessages());

            @Override
            public Widget get() {
                DataMiningSettingsControl settingsControl = new AnchorDataMiningSettingsControl(null, null);
                resultsPresenter = new TabbedSailingResultsPresenter(/* parent */ null,
                        /* context */ null, /* drillDownCallback */ groupKey -> {
                            queryDefinitionProvider.drillDown(groupKey, () -> {
                                queryRunner.run(queryDefinitionProvider.getQueryDefinition());
                            });
                        }, getStringMessages());
                resultsPresenter.addCurrentPresenterChangedListener(presenterId -> {
                    StatisticQueryDefinitionDTO queryDefinition = resultsPresenter.getQueryDefinition(presenterId);
                    if (queryDefinition != null) {
                        queryDefinitionProvider.applyQueryDefinition(queryDefinition);
                    }
                });

                queryDefinitionProvider = new QueryDefinitionProviderWithControls(null, null, session,
                        dataMiningService, DataMiningEntryPoint.this, settingsControl, settingsManager,
                        queryDefinition -> queryRunner.run(queryDefinition));

                queryRunner = new SimpleQueryRunner(null, null, session, dataMiningService, DataMiningEntryPoint.this,
                        queryDefinitionProvider, resultsPresenter);
                queryDefinitionProvider.addControl(queryRunner.getEntryWidget());
                if (getUserService().hasServerPermission(ServerActions.DATA_MINING)) {
                    StoredDataMiningQueryDataProvider dataProvider = new StoredDataMiningQueryDataProvider(
                            queryDefinitionProvider, dataMiningService, dataMiningWriteService, queryRunner, getStringMessages());
                    queryDefinitionProvider.addControl(new StoredDataMiningQueryPanel(dataProvider));
                }

                Anchor orientationAnchor = new Anchor(AbstractImagePrototype.create(
                        dataMiningResources.orientationIcon()).getSafeHtml());
                orientationAnchor.addStyleName("orientationAnchor");
                orientationAnchor.setTitle(getStringMessages().changeOrientation());
                orientationAnchor.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        setQueryAndResultOrientation();
                    }
                });
                queryDefinitionProvider.addControl(orientationAnchor);
                /*
                 * Running queries automatically when they've been changed is currently unnecessary, if not even
                 * counterproductive. This removes the query runner settings to prevent that the user can enable the
                 * automatic execution of queries. Re-enable this, when this functionality is desired again.
                 */
                // settingsControl.addSettingsComponent(queryRunner);
                queryAndResultSplitPanel = new SplitLayoutPanel(10);
                addDefinitionProviderAndResultPresenter();
                if (queryIdentifier != null) {
                    CrossDomainStorage store = getUserService().getStorage();
                    store.getItem(SailingSettingsConstants.DATAMINING_QUERY, storedElem->{
                        JSONArray arr = JSONParser.parseStrict(storedElem).isArray();
                        for (int i = 0; i < arr.size(); i++) {
                            JSONObject json = arr.get(i).isObject();
                            if (queryIdentifier.equals(json.get("uuid").isString().stringValue())) {
                                String serializedQuery = json.get("payload").isString().stringValue();
                                dataMiningService.getDeserializedQuery(serializedQuery,
                                        new AsyncCallback<ModifiableStatisticQueryDefinitionDTO>() {
                                            @Override
                                            public void onSuccess(ModifiableStatisticQueryDefinitionDTO result) {
                                                queryDefinitionProvider.applyQueryDefinition(result);
                                                queryRunner.run(result);
                                            }
    
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                LOG.log(Level.SEVERE, caught.getMessage(), caught);
                                            }
                                        });
                                break;
                            }
                        }
                    });
                }
                return queryAndResultSplitPanel;
            }
        });

        RootLayoutPanel rootPanel = RootLayoutPanel.get();
        DockLayoutPanel panel = new DockLayoutPanel(Unit.PX);
        panel.addNorth(header, 75);
        panel.add(authorizedContentDecorator);
        panel.ensureDebugId("DataMiningPanel");
        rootPanel.add(panel);
    }

    /**
     * Toggles the position of the {@link #resultsPresenter} from south to east and vice versa.
     */
    public void setQueryAndResultOrientation() {
        setQueryAndResultOrientation(!queryAndResultAreVertical);
    }
    /**
     * Sets the position of the {@link #resultsPresenter}.
     * @param vertical {@code boolean} {@code true} places it in the south position
     * and {@code false} in the east position.
     */
    public void setQueryAndResultOrientation(boolean vertical) {
        if (vertical != queryAndResultAreVertical) {
            // Store current size for the next orientation change
            Double size = queryAndResultSplitPanel.getWidgetSize(resultsPresenter.getEntryWidget());
            if (size != null) {
                if (queryAndResultAreVertical) {
                    resultsPresenterSouthHeight = size.intValue();
                } else {
                    resultsPresenterEastWidth = size.intValue();
                }
            }

            queryAndResultAreVertical = vertical;

            queryAndResultSplitPanel.remove(resultsPresenter.getEntryWidget());
            // You can't add panels as long as there is a center panel so temporarily remove it
            queryAndResultSplitPanel.remove(queryDefinitionProvider.getEntryWidget());

            // Add the center panel back once the changes are made
            addDefinitionProviderAndResultPresenter();
        }
    }

    private void addDefinitionProviderAndResultPresenter() {
        if (queryAndResultAreVertical) {
            queryAndResultSplitPanel.addSouth(resultsPresenter.getEntryWidget(), resultsPresenterSouthHeight);
        } else {
            queryAndResultSplitPanel.addEast(resultsPresenter.getEntryWidget(), resultsPresenterEastWidth);
        }
        queryAndResultSplitPanel.add(queryDefinitionProvider.getEntryWidget());
    }

    private void removeUrlParameter() {
        try {
            UrlBuilder builder = Window.Location.createUrlBuilder().setHost(Window.Location.getHost())
                    .setPath(Window.Location.getPath()).setProtocol(Window.Location.getProtocol());

            String port = Window.Location.getPort();
            if (port != null && !"".equals(port.trim()) && !"0".equals(port)) {
                builder.setPort(Integer.parseInt(port));
            }
            String newUrl = builder.buildString();
            newUrl = newUrl.replaceAll("\\?.*$", "");

            updateUrl(Window.getTitle(), newUrl);
        } catch (Exception e) {
            LOG.severe("Could not update URL: " + e.getMessage());
            // In the worst case, the URL is not updated. This should not impact the user experience of
            // data mining.
        }
    }

    /*
     * using JSNI because GWT-History-Mapper does currently not support updating an URL in the history without reloading
     * the page
     */
    private native void updateUrl(String title, String newUrl)/*-{
        $wnd.history.replaceState(null, title, newUrl);
    }-*/;

}
