package com.sap.sailing.gwt.ui.polarsheets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.impl.PolarSheetsWindStepping;

public class WindSteppingConfiguratorPanel extends HorizontalPanel {
    
    private List<TextBox> textBoxes = new ArrayList<TextBox>();
    private Button minusButton;
    private Button plusButton;

    public WindSteppingConfiguratorPanel(PolarSheetsWindStepping windStepping) {
        setupPlusAndMinusButtons();
        Integer[] levels = windStepping.getRawStepping();
        for (int i = 0; i < levels.length; i++) {
            TextBox textBox = createSingleBox(levels[i]);
            textBoxes.add(textBox);
        }
        updateTextBoxes();
    }

    private void setupPlusAndMinusButtons() {
        minusButton = new Button("-");
        minusButton.addClickHandler(new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                textBoxes.remove(textBoxes.size() - 1);
                updateTextBoxes();
            }
        });
        plusButton = new Button("+");
        plusButton.addClickHandler(new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                TextBox newBox = createSingleBox(Integer.parseInt(textBoxes.get(textBoxes.size() - 1).getText()) + 2);
                textBoxes.add(newBox);
                updateTextBoxes();
            }
        });
    }

    private void updateTextBoxes() {
        this.clear();
        for (Widget widget : textBoxes) {
            this.add(widget);
        }
        if (textBoxes.size() > 1) {
            this.add(minusButton);
        }
        this.add(plusButton);
    }

    public PolarSheetsWindStepping getStepping() {
        List<Integer> levelList = new ArrayList<Integer>(); 
        for (TextBox box : textBoxes) {
            levelList.add(Integer.parseInt(box.getValue()));
        }
        Collections.sort(levelList);
        Integer[] levels = levelList.toArray(new Integer[levelList.size()]);
        return new PolarSheetsWindStepping(levels);
    }
    
    private TextBox createSingleBox(int level) {
        TextBox textBox = new TextBox();
        textBox.setMaxLength(2);
        textBox.setVisibleLength(2);
        textBox.setText(Integer.toString(level));
        return textBox;
    }
    
    

}
