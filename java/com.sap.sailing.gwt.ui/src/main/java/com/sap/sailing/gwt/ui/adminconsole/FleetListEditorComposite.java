package com.sap.sailing.gwt.ui.adminconsole;

import java.util.List;

import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.Color;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.listedit.ExpandedListEditorUi;
import com.sap.sailing.gwt.ui.client.shared.controls.listedit.ListEditorComposite;

public class FleetListEditorComposite extends ListEditorComposite<FleetDTO> {
    public FleetListEditorComposite(List<FleetDTO> initialValues, StringMessages stringMessages, ImageResource removeImage) {
        super(initialValues, new ExpandedUi(stringMessages, removeImage));
    }

    private static class ExpandedUi extends ExpandedListEditorUi<FleetDTO> {

        public ExpandedUi(StringMessages stringMessages, ImageResource removeImage) {
            super(stringMessages, removeImage);
        }

        @Override
        protected Widget createAddWidget() {
            CaptionPanel captionPanel = new CaptionPanel(stringMessages.addFleet());

            HorizontalPanel panel = new HorizontalPanel();
            panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

            final TextBox nameBox = createNameBox();
            final IntegerBox orderNoBox = createOrderNoBox();
            final ListBox colorListBox = createColorListBox(nameBox, orderNoBox);
            final Button addButton = new Button(stringMessages.add());
            addButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    addValue(new FleetDTO(nameBox.getValue(), orderNoBox.getValue(), getSelectedColor(colorListBox)));
                }
            });

            Grid fleetsGrid = new Grid(2, 3);
            fleetsGrid.setCellSpacing(4);
            fleetsGrid.setHTML(0, 0, stringMessages.color());
            fleetsGrid.setHTML(0, 1, stringMessages.name());
            fleetsGrid.setHTML(0, 2, stringMessages.rank());
            fleetsGrid.setWidget(1, 0, colorListBox);
            fleetsGrid.setWidget(1, 1, nameBox);
            fleetsGrid.setWidget(1, 2, orderNoBox);

            panel.add(fleetsGrid);
            panel.add(addButton);
            captionPanel.add(panel);
            return captionPanel;
        }

        @Override
        protected Widget createValueWidget(int row, FleetDTO fleet) {
            HorizontalPanel hPanel = new HorizontalPanel();
            hPanel.setSpacing(4);
            Label fleetLabel = new Label(row + ". " + stringMessages.fleet() + ":");
            fleetLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
            hPanel.add(fleetLabel);
            
            String valueText = fleet.getName();
            if(fleet.getColor() != null) {
                valueText += ", " + stringMessages.color() + " '" + fleet.getColor().toString() + "'";
            } else {
                valueText += ", " + stringMessages.noColor();
            }
            valueText += ", " + stringMessages.rank() + " " + fleet.getOrderNo();
            
            hPanel.add(new Label(valueText));
            
            return hPanel;
        }

        private IntegerBox createOrderNoBox() {
            IntegerBox orderNoBox = new IntegerBox();
            orderNoBox.setVisibleLength(3);
            orderNoBox.setValue(0);
            return orderNoBox;
        }

        private TextBox createNameBox() {
            final TextBox nameBox = new TextBox();
            nameBox.setVisibleLength(40);
            nameBox.setWidth("175px");
            return nameBox;
        }

        private ListBox createColorListBox(final TextBox nameBox, final IntegerBox orderNoBox) {
            final ListBox colorListBox = new ListBox(false);
            colorListBox.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    // set default order no of the selected color
                    int selIndex = colorListBox.getSelectedIndex();
                    if (selIndex == 0) {
                        orderNoBox.setValue(0);
                    } else {
                        String value = colorListBox.getValue(selIndex);
                        final FleetColors color = FleetColors.valueOf(value);
                        if (color != null) {
                            orderNoBox.setValue(color.getDefaultOrderNo());
                            nameBox.setValue(getColorText(color));
                        }
                    }
                }
            });
            colorListBox.addItem(stringMessages.noColor());
            for (FleetColors value : FleetColors.values()) {
                colorListBox.addItem(value.name());
            }
            colorListBox.setSelectedIndex(0);
            return colorListBox;
        }

        private Color getSelectedColor(ListBox colorListBox) {
            Color result = null;
            int selIndex = colorListBox.getSelectedIndex();
            // the zero index represents the 'no color' option
            if (selIndex > 0) {
                String value = colorListBox.getValue(selIndex);
                for (FleetColors color : FleetColors.values()) {
                    if (color.name().equals(value)) {
                        result = color.getColor();
                        break;
                    }
                }
            }
            return result;
        }

        private String getColorText(FleetColors color) {
            if (color == null) {
                return stringMessages.noColor();
            }
            return color.name().charAt(0) + color.name().toLowerCase().substring(1);
        }

    }

};
