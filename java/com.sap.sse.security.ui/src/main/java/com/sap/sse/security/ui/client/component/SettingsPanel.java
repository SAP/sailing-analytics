package com.sap.sse.security.ui.client.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ImageResourceRenderer;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.security.ui.client.UserManagementImageResources;
import com.sap.sse.security.ui.client.resources.IconResources;
import com.sap.sse.security.ui.shared.UserManagementServiceAsync;

public class SettingsPanel extends FlowPanel {

    private UserManagementServiceAsync userManagementService;
    private Map<String, String> settings = null;
    private Map<String, String> settingTypes = null;
    private Map<String, FlexTable> savedTabs = new HashMap<>();

    public SettingsPanel(UserManagementServiceAsync userManagementService) {
        super();
        this.userManagementService = userManagementService;
        initComponents();
    }

    private void initComponents() {
        clear();
        Label settingsTitle = new Label("Settings");
        add(settingsTitle);
        userManagementService.getSettingTypes(new AsyncCallback<Map<String, String>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Map<String, String> result) {
                settingTypes = result;
                updateSettings();
            }
        });
        userManagementService.getSettings(new AsyncCallback<Map<String, String>>() {

            @Override
            public void onSuccess(Map<String, String> result) {
                settings = result;
                updateSettings();
            }

            @Override
            public void onFailure(Throwable caught) {
            }
        });
    }

    private void updateSettings() {
        if (settings == null || settingTypes == null) {
            return;
        }
        TabLayoutPanel tabPanel = new TabLayoutPanel(30, Unit.PX);
        tabPanel.setHeight("95%");

        Map<String, Integer> numberOfSettings = new HashMap<>();
        for (Entry<String, String> e : settingTypes.entrySet()) {
            String[] split = e.getKey().split("_");
            FlexTable flexTable = savedTabs.get(split[0]);
            Integer row = numberOfSettings.get(split[0]);
            if (row == null) {
                row = 0;
            }
            if (flexTable == null) {
                flexTable = new FlexTable();
                savedTabs.put(split[0], flexTable);
                ScrollPanel scrollPanel = new ScrollPanel(flexTable);
                scrollPanel.setHeight("100%");
                scrollPanel.addStyleName("settingsPanel-grid");
                tabPanel.add(scrollPanel, split[0]);
            }

            if (split[0].equals("URLS")) {
                if (!split[1].equals("AUTH")) {
                    createUrlRowEntry(e.getKey(), e.getValue(), flexTable, row);
                }
            } else {
                createSettingRowEntry(e.getKey(), e.getValue(), flexTable, row);
            }
            row++;
            numberOfSettings.put(split[0], row);
        }

        if (savedTabs.get("URLS") != null) {
            FlexTable table = savedTabs.get("URLS");
            int newline = table.getRowCount()+1;
            final TextBox key = new TextBox();
            final TextBox url = new TextBox();
            final TextBox filter = new TextBox();
            table.setWidget(newline, 0, key);
            table.setWidget(newline, 1, url);
            table.setWidget(newline, 2, filter);
            
            Button add = new Button("Add url filter", new ClickHandler() {
                
                @Override
                public void onClick(ClickEvent event) {
                    userManagementService.addSetting("URLS_" + key.getText(), String.class.getName(), url.getText(), new AsyncCallback<Void>() {

                        @Override
                        public void onFailure(Throwable caught) {
                            Window.alert(caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Void result) {
                            Window.alert("Added url!");
                        }
                    });
                    userManagementService.addSetting("URLS_AUTH_" + key.getText(), String.class.getName(), filter.getText(), new AsyncCallback<Void>() {

                        @Override
                        public void onFailure(Throwable caught) {
                            Window.alert(caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Void result) {
                            Window.alert("Added url filter!");
                        }
                    });
                }
            });
            table.setWidget(newline+1, 0, add);
        }

        add(tabPanel);
    }

    private void createSettingRowEntry(final String key, String sValue, final FlexTable flexTable, final Integer row) {
        Label keyLabel = new Label(key.substring(key.indexOf('_') + 1));
        flexTable.setWidget(row, 0, keyLabel);
        
        ImageResourceRenderer renderer = new ImageResourceRenderer();
        final ImageResource statusRedImageResource = IconResources.INSTANCE.statusRed();
        final HTML statusRed =  new HTML(renderer.render(statusRedImageResource));
        statusRed.setTitle("Could not safe property!");
        
        final ImageResource statusGreenImageResource = IconResources.INSTANCE.statusGreen();
        final HTML statusGreen =  new HTML(renderer.render(statusGreenImageResource));
        statusGreen.setTitle("Saved property!");
        
        final ImageResource statusYellowImageResource = IconResources.INSTANCE.statusYellow();
        final HTML statusYellow =  new HTML(renderer.render(statusYellowImageResource));
        statusYellow.setTitle("Trying to safe property...");
        
        flexTable.setWidget(row, 2, statusGreen);
        
        final ImageResource deleteImageResource = IconResources.INSTANCE.delete();
        HTML delete = new HTML(renderer.render(deleteImageResource));
        delete.addClickHandler(new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                boolean deleteS = Window.confirm("Are you sure you want to delete this setting?");
                
                if (deleteS){
                    Window.alert("Not implemented yet");
                }
            }
        });
        flexTable.setWidget(row, 3, delete);

        if (sValue.equals(Boolean.class.getName())) {
            final CheckBox value = new CheckBox();
            value.setValue(Boolean.parseBoolean(settings.get(key)));
            value.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    flexTable.setWidget(row, 2, statusYellow);
                    userManagementService.setSetting(key, Boolean.class.getName(), value.getValue().toString(),
                            new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    flexTable.setWidget(row, 2, statusRed);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    flexTable.setWidget(row, 2, statusGreen);
                                }
                            });
                }
            });
            flexTable.setWidget(row, 1, value);
        } else if (sValue.equals(Integer.class.getName())) {
            final IntegerBox value = new IntegerBox();
            value.setValue(Integer.parseInt(settings.get(key)));
            value.addValueChangeHandler(new ValueChangeHandler<Integer>() {

                @Override
                public void onValueChange(ValueChangeEvent<Integer> event) {
                    flexTable.setWidget(row, 2, statusYellow);
                    userManagementService.setSetting(key, Integer.class.getName(), value.getValue().toString(),
                            new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    flexTable.setWidget(row, 2, statusRed);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    flexTable.setWidget(row, 2, statusGreen);
                                }
                            });
                }
            });
            flexTable.setWidget(row, 1, value);
        } else {
            final TextBox value = new TextBox();
            value.setText(settings.get(key));
            value.addChangeHandler(new ChangeHandler() {

                @Override
                public void onChange(ChangeEvent event) {
                    flexTable.setWidget(row, 2, statusYellow);
                    userManagementService.setSetting(key, String.class.getName(), value.getText(),
                            new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    flexTable.setWidget(row, 2, statusRed);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    flexTable.setWidget(row, 2, statusGreen);
                                }
                            });
                }
            });
            flexTable.setWidget(row, 1, value);
        }
    }

    private void createUrlRowEntry(final String key, String sValue, final FlexTable flexTable, final int row) {
        final String labelKey = key.substring(key.indexOf('_') + 1);
        Label keyLabel = new Label(labelKey);
        flexTable.setWidget(row, 0, keyLabel);
        
        ImageResourceRenderer renderer = new ImageResourceRenderer();
        final ImageResource statusRedImageResource = IconResources.INSTANCE.statusRed();
        final HTML statusRed =  new HTML(renderer.render(statusRedImageResource));
        statusRed.setTitle("Could not safe property!");
        
        final ImageResource statusGreenImageResource = IconResources.INSTANCE.statusGreen();
        final HTML statusGreen =  new HTML(renderer.render(statusGreenImageResource));
        statusGreen.setTitle("Saved property!");
        
        final ImageResource statusYellowImageResource = IconResources.INSTANCE.statusYellow();
        final HTML statusYellow =  new HTML(renderer.render(statusYellowImageResource));
        statusYellow.setTitle("Trying to safe property...");
        
        flexTable.setWidget(row, 3, statusGreen);
        
        final ImageResource deleteImageResource = IconResources.INSTANCE.delete();
        HTML delete = new HTML(renderer.render(deleteImageResource));
        delete.addClickHandler(new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                boolean deleteS = Window.confirm("Are you sure you want to delete this setting?");
                
                if (deleteS){
                    Window.alert("Not implemented yet");
                }
            }
        });
        flexTable.setWidget(row, 4, delete);

        final TextBox value1 = new TextBox();
        value1.setText(settings.get(key));
        value1.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                flexTable.setWidget(row, 3, statusYellow);
                userManagementService.setSetting(key, String.class.getName(), value1.getText(),
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                flexTable.setWidget(row, 3, statusRed);
                            }

                            @Override
                            public void onSuccess(Void result) {
                                flexTable.setWidget(row, 3, statusGreen);
                            }
                        });
            }
        });
        flexTable.setWidget(row, 1, value1);

        final TextBox value2 = new TextBox();
        value2.setText(settings.get("URLS_AUTH_" + labelKey));
        value2.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                flexTable.setWidget(row, 3, statusYellow);
                userManagementService.setSetting("URLS_AUTH_" + labelKey, String.class.getName(), value2.getText(),
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                flexTable.setWidget(row, 3, statusRed);
                            }

                            @Override
                            public void onSuccess(Void result) {
                                flexTable.setWidget(row, 3, statusGreen);
                            }
                        });
            }
        });
        flexTable.setWidget(row, 2, value2);
    }
}
