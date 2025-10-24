package com.sap.sailing.landscape.ui.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sailing.landscape.ui.shared.AvailabilityZoneDTO;
import com.sap.sailing.landscape.ui.shared.ReverseProxyDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Creates a dialog box for adding a reverse proxy to the cluster.
 * 
 * @author Thomas Stokes
 *
 */
public class CreateReverseProxyInClusterDialog
        extends DataEntryDialog<CreateReverseProxyInClusterDialog.CreateReverseProxyInstructions> {

    public static class CreateReverseProxyInstructions implements IsSerializable {
        private final String instanceType;
        private final String name;
        private final String region;
        private String keyName;
        private final AvailabilityZoneDTO availabilityZoneDTO;

        /**
         * @param name
         *            The name of the reverse proxy to spawn.
         * @param instanceType
         *            The new instance type.
         * @param availabilityZoneNameListBox
         *            The id of the AZ with the fewest reverse proxies.
         * @param region
         */
        public CreateReverseProxyInstructions(String name, String instanceType,
                String region, AvailabilityZoneDTO availabilityZoneDTO) {
            this.name = name;
            this.instanceType = instanceType;
            this.region = region;
            this.availabilityZoneDTO = availabilityZoneDTO;
        }

        public String getName() {
            return name;
        }

        public String getInstanceType() {
            return instanceType;
        }

        public String getRegion() {
            return region;
        }

        public String getKey() {
            return keyName;
        }

        public void setKey(String key) {
            keyName = key;
        }

        public AvailabilityZoneDTO getAvailabilityZoneDTO() {
            return availabilityZoneDTO;
        }
    }

    private final TextBox proxyName;
    private final ListBox dedicatedInstanceTypeListBox;
    private final ListBox availabilityZoneNameListBox;
    private final Map<String, String> availabilityZoneNameToId;
    private final String region;
    private final Label nameLabel;
    private final Label instanceTypeLabel;
    private final Label availabilityZoneLabel;
    private Label instancesIdLabel;
    
    /**
     * A list of all labels in the dialog box.
     */
    private ArrayList<Label> labels;

    /**
     * The dialog box allows users to choose the name, instance type and az.
     * 
     * @param existingReverseProxies A list of reverse proxy DTOs that exist in the region.
     * @param availabilityZones A list of the availability zone (in DTO format) in the region.
     * @param callback
     *            This will call after the user selects the "ok" in the box, if they create a reverse proxy.
     */
    public CreateReverseProxyInClusterDialog(StringMessages stringMessages, ErrorReporter errorReporter,
            LandscapeManagementWriteServiceAsync landscapeManagementService, String region,
            List<ReverseProxyDTO> existingReverseProxies, List<AvailabilityZoneDTO> availabilityZones,
            DialogCallback<CreateReverseProxyInClusterDialog.CreateReverseProxyInstructions> callback) {
        super(stringMessages.reverseProxies(), stringMessages.reverseProxies(), stringMessages.ok(),
                stringMessages.cancel(),
                new Validator<CreateReverseProxyInClusterDialog.CreateReverseProxyInstructions>() {

                    @Override
                    public String getErrorMessage(
                            CreateReverseProxyInClusterDialog.CreateReverseProxyInstructions valueToValidate) {
                        if (!Util.hasLength(valueToValidate.getName())) {
                            return stringMessages.pleaseProvideNonEmptyNameAndAZ();
                        } else if (valueToValidate.availabilityZoneDTO == null) {
                            return stringMessages.pleaseProvideNonEmptyNameAndAZ();
                        } else {
                            return null;
                        }
                    }
                }, callback);
        this.region = region;
        availabilityZoneNameToId = availabilityZones.stream().collect(Collectors.toMap(entry -> entry.getAzName(), entry -> entry.getAzId()));
        availabilityZoneNameListBox = setupAZChoiceListBox(landscapeManagementService, errorReporter,
                existingReverseProxies);
        proxyName = createTextBox("", 20);
        proxyName.setValue(SharedLandscapeConstants.DEFAULT_DISPOSABLE_REVERSE_PROXY_INSTANCE_NAME);
        dedicatedInstanceTypeListBox = LandscapeDialogUtil.createInstanceTypeListBox(this, landscapeManagementService,
                stringMessages, SharedLandscapeConstants.DEFAULT_REVERSE_PROXY_INSTANCE_TYPE, errorReporter, /* canBeDeployedInNlbInstanceBasedTargetGroup */ true);
        // setup labels
        nameLabel = new Label(stringMessages.instanceName());
        instanceTypeLabel = new Label(stringMessages.instanceType());
        availabilityZoneLabel = new Label(stringMessages.availabilityZone());
        instancesIdLabel = new Label(stringMessages.instanceId());
        labels = new ArrayList<>(4);
        labels.add(nameLabel);
        labels.add(instanceTypeLabel);
        labels.add(availabilityZoneLabel);
        labels.add(instancesIdLabel);
        instancesIdLabel.setVisible(false);
        validateAndUpdate();
    }

    /**
     * 
     * This method assumes that all AZs are being served. If an instance is deployed in an AZ not served by a target group, it will be "unused".
     * @param existingReverseProxies A list of all existing reverse proxies in a region.
     * @return A listbox with the reverse proxies. The least populated az is selected by default.
     */
    private ListBox setupAZChoiceListBox(LandscapeManagementWriteServiceAsync landscapeManagementService,
            ErrorReporter errorReporter, List<ReverseProxyDTO> existingReverseProxies) {
        final ListBox availabilityZoneBox = createListBox(false);
        if (!availabilityZoneNameToId.isEmpty()) {
            final Map<String, Long> azCounts = existingReverseProxies.stream() // Maps the AZs name to the number of times a reverse proxy is in that AZ.
                    .collect(Collectors.groupingBy(w -> w.getAvailabilityZoneName(), Collectors.counting()));
            availabilityZoneNameToId.keySet().forEach(azName -> azCounts.merge(azName, 0L, (a, b) -> a + b));  // Merges in any AZ which has no reverse proxies. 
            final String leastPopulatedAzName = azCounts.entrySet().stream()
                    .min((a, b) -> Long.compare(a.getValue(), b.getValue())).get().getKey();
            int i = 0;
            for (String az : availabilityZoneNameToId.keySet().stream().sorted().collect(Collectors.toList())) {
                availabilityZoneBox.addItem(az, az);
                if (az.equals(leastPopulatedAzName)) {
                    availabilityZoneBox.setSelectedIndex(i);
                }
                i++;
            }
        }
        return availabilityZoneBox;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final FormPanel result = new FormPanel();
        final VerticalPanel verticalPanel = new VerticalPanel();
        result.add(verticalPanel);
        verticalPanel.add(nameLabel);
        verticalPanel.add(proxyName);
        verticalPanel.add(instanceTypeLabel);
        verticalPanel.add(dedicatedInstanceTypeListBox);
        verticalPanel.add(availabilityZoneLabel);
        verticalPanel.add(availabilityZoneNameListBox);
        return result;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return proxyName;
    }

    @Override
    protected CreateReverseProxyInClusterDialog.CreateReverseProxyInstructions getResult() {
        return new CreateReverseProxyInstructions(proxyName.getValue(), dedicatedInstanceTypeListBox.getSelectedValue(),
                region,
                new AvailabilityZoneDTO(availabilityZoneNameListBox.getSelectedValue(), region,
                        availabilityZoneNameToId.get(availabilityZoneNameListBox.getSelectedValue())));
    }
}
