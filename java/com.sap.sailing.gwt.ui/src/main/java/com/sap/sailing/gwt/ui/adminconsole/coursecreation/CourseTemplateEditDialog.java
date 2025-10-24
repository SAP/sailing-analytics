package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.WaypointTemplateDTO;
import com.sap.sse.common.RepeatablePart;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.celltable.ImagesBarColumn;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.controls.listedit.StringListEditorComposite;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;

public class CourseTemplateEditDialog extends DataEntryDialog<CourseTemplateDTO> {
    private static AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);

    private final TextBox nameTextBox;
    private final TextBox shortNameTextBox;
    private final TextBox urlTextBox;
    private final IntegerBox numberOfLapsTextBox;
    private final IntegerBox zeroBasedIndexOfFirstWaypointOfRepeatablePart;
    private final IntegerBox zeroBasedIndexOfLastWaypointOfRepeatablePart;
    private final StringMessages stringMessages;
    private final List<MarkRoleDTO> allMarkRoles;
    private final List<String> allMarkRolesSelectionListPlusEmptyString;
    private final List<MarkTemplateDTO> allMarkTemplates;
    private final List<String> allMarkTemplatesSelectionListPlusEmptyString;
    
    /**
     * Contains the full set of mark roles resulting from the {@link WaypointTemplateDTO}s managed in {@link #waypointTemplates}.
     * For those, it is a requirement that valid, non-{@code null} {@link MarkTemplateDTO}s are associated.
     */
    private final CellTable<MarkTemplateDTOAndMarkRoleDTO> markTemplatesForMarkRolesTable;
    private final List<MarkTemplateDTOAndMarkRoleDTO> markTemplatesForMarkRoles;
    
    /**
     * Contains optional additional mark templates with an optional assignment of a default role.
     */
    private final CellTable<MarkTemplateDTOAndMarkRoleDTO> spareMarkTemplatesAndTheirDefaultMarkRolesTable;
    private final List<MarkTemplateDTOAndMarkRoleDTO> spareMarkTemplatesAndTheirDefaultMarkRoles;
    
    private final Button buttonAddMarkRoleToMarkTemplateMapping;
    private final Button buttonAddSpareMarkTemplate;
    private final CellTable<WaypointTemplateDTO> waypointTemplatesTable;
    private final Collection<WaypointTemplateDTO> waypointTemplates = new ArrayList<>();
    private final Button buttonAddWaypointTemplate;

    private final UUID currentUuid;

    private final StringListEditorComposite tagsEditor;

    private static final RegExp urlRegExp = RegExp
            .compile("^((ftp|http|https)://[\\w@.\\-\\_]+(:\\d{1,5})?(/[\\w#!:.?+=&%@!\\_\\-/]+)*){1}$");

    public CourseTemplateEditDialog(final SailingServiceAsync sailingService, final StringMessages stringMessages,
            CourseTemplateDTO courseTemplateToEdit, List<MarkRoleDTO> allMarkRoles,
            List<MarkTemplateDTO> allMarkTemplates, DialogCallback<CourseTemplateDTO> callback,
            final boolean isNew) {
        super(stringMessages.edit() + " " + stringMessages.courseTemplates(), null, stringMessages.ok(),
                stringMessages.cancel(), new Validator<CourseTemplateDTO>() {
                    @Override
                    public String getErrorMessage(CourseTemplateDTO valueToValidate) {
                        final StringBuilder sb = new StringBuilder();
                        boolean invalidName = valueToValidate.getName() == null || valueToValidate.getName().isEmpty();
                        if (invalidName) {
                            sb.append(stringMessages.pleaseEnterAName()).append(". ");
                        }
                        AtomicBoolean unAssignedMarkRoleUsed = new AtomicBoolean(false);
                        valueToValidate.getWaypointTemplates().forEach(wt -> {
                            if (wt.getPassingInstruction() == null) {
                                sb.append(stringMessages.wayPointRequiresPassingInstruction()).append(". ");
                            } else {
                                wt.getMarkRolesForControlPoint()
                                        .forEach(markRole -> unAssignedMarkRoleUsed.set(unAssignedMarkRoleUsed.get()
                                                || valueToValidate.getDefaultMarkTemplatesForMarkRoles().get(markRole) == null));
                                if (hasTwoMarks(wt)) {
                                    if (wt.getShortName() == null) {
                                        sb.append(stringMessages.wayPointRequiresShortName()).append(". ");
                                    }
                                    if (wt.getName() == null) {
                                        sb.append(stringMessages.wayPointRequiresName()).append(". ");
                                    }
                                    if (wt.getMarkRolesForControlPoint().size() == 2
                                            && wt.getMarkRolesForControlPoint().get(0)
                                                    .equals(wt.getMarkRolesForControlPoint().get(1))) {
                                        sb.append(stringMessages.wayPointMarkTemplatesAreTheSame());
                                    }
                                }
                            }
                        });
                        if (unAssignedMarkRoleUsed.get()) {
                            sb.append(stringMessages.wayPointMarkInSequenceMissing()).append(". ");
                        }
                        if (valueToValidate.getOptionalImageUrl().isPresent()) {
                            if (urlRegExp.exec(valueToValidate.getOptionalImageUrl().get()) == null) {
                                sb.append(stringMessages.invalidImageURL()).append(". ");
                            }
                        }
                        if (valueToValidate.getDefaultNumberOfLaps() != null && valueToValidate.getDefaultNumberOfLaps() < 0) {
                            sb.append(stringMessages.defaultNumberOfLapsMustNotBeNegative());
                        }
                        if (valueToValidate.getRepeatablePart() != null) {
                            if ((valueToValidate.getRepeatablePart().getZeroBasedIndexOfRepeatablePartEnd() == -1) !=
                                 (valueToValidate.getRepeatablePart().getZeroBasedIndexOfRepeatablePartStart() == -1)) {
                                sb.append(stringMessages.eitherNoneOrBothStartAndEndOfRepeatablePartMustBeSpecified());
                            } else if (valueToValidate.getRepeatablePart().getZeroBasedIndexOfRepeatablePartEnd() < valueToValidate.getRepeatablePart().getZeroBasedIndexOfRepeatablePartStart()) {
                                sb.append(stringMessages.endOfRepeatablePartMustBeAtOfAfterStart());
                            } else if (valueToValidate.getRepeatablePart().getZeroBasedIndexOfRepeatablePartEnd() > valueToValidate.getWaypointTemplates().size()) {
                                sb.append(stringMessages.endOfRepeatablePartIsGreaterThanNumberOfWaypoints());
                            }
                        }
                        return sb.toString();
                    }
                }, /* animationEnabled */true, callback);
        this.ensureDebugId("CourseTemplateToEditEditDialog");
        this.stringMessages = stringMessages;
        this.currentUuid = courseTemplateToEdit.getUuid();
        this.nameTextBox = createTextBox(courseTemplateToEdit.getName());
        this.shortNameTextBox = createTextBox(courseTemplateToEdit.getShortName());
        this.urlTextBox = createTextBox(courseTemplateToEdit.getOptionalImageUrl().orElse(""));
        this.numberOfLapsTextBox = createIntegerBox(courseTemplateToEdit.getDefaultNumberOfLaps() != null
                ? courseTemplateToEdit.getDefaultNumberOfLaps() : null, 2);
        this.zeroBasedIndexOfFirstWaypointOfRepeatablePart = createIntegerBox(courseTemplateToEdit.getRepeatablePart() == null ? null :
            courseTemplateToEdit.getRepeatablePart().getZeroBasedIndexOfRepeatablePartStart(), 2);
        this.zeroBasedIndexOfFirstWaypointOfRepeatablePart.setEnabled(isNew);
        this.zeroBasedIndexOfLastWaypointOfRepeatablePart = createIntegerBox(courseTemplateToEdit.getRepeatablePart() == null ? null :
            courseTemplateToEdit.getRepeatablePart().getZeroBasedIndexOfRepeatablePartEnd()-1, 2);
        this.zeroBasedIndexOfLastWaypointOfRepeatablePart.setEnabled(isNew);
        this.allMarkTemplates = allMarkTemplates;
        this.allMarkTemplatesSelectionListPlusEmptyString = new LinkedList<>(allMarkTemplates.stream().map(MarkTemplateDTO::getName).collect(Collectors.toList()));
        allMarkTemplatesSelectionListPlusEmptyString.add(0, ""); // prepend the empty selection
        this.allMarkRoles = allMarkRoles;
        this.allMarkRolesSelectionListPlusEmptyString = new LinkedList<>(allMarkRoles.stream().map(MarkRoleDTO::getName).collect(Collectors.toList()));
        allMarkRolesSelectionListPlusEmptyString.add(0, ""); // prepend the empty selection
        this.markTemplatesForMarkRoles = new ArrayList<>();
        markTemplatesForMarkRolesTable = createMarkTemplateAndMarkRoleTable(/* readOnly */ !isNew, markTemplatesForMarkRoles);
        buttonAddMarkRoleToMarkTemplateMapping = new Button(stringMessages.add());
        buttonAddMarkRoleToMarkTemplateMapping.addClickHandler(c -> {
            markTemplatesForMarkRoles.add(new MarkTemplateDTOAndMarkRoleDTO(allMarkTemplates.stream().findFirst().orElse(null),
                    allMarkRoles.stream().findFirst().orElse(null)));
            refreshTable(markTemplatesForMarkRolesTable, markTemplatesForMarkRoles);
            validateAndUpdate();
        });
        this.spareMarkTemplatesAndTheirDefaultMarkRoles = new ArrayList<>();
        spareMarkTemplatesAndTheirDefaultMarkRolesTable = createMarkTemplateAndMarkRoleTable(/* readOnly */ !isNew, spareMarkTemplatesAndTheirDefaultMarkRoles);
        buttonAddMarkRoleToMarkTemplateMapping.setEnabled(isNew);
        buttonAddSpareMarkTemplate = new Button(stringMessages.add());
        buttonAddSpareMarkTemplate.addClickHandler(c -> {
            spareMarkTemplatesAndTheirDefaultMarkRoles.add(new MarkTemplateDTOAndMarkRoleDTO(allMarkTemplates.stream().findFirst().orElse(null),
                    allMarkRoles.stream().findFirst().orElse(null)));
            refreshTable(spareMarkTemplatesAndTheirDefaultMarkRolesTable, spareMarkTemplatesAndTheirDefaultMarkRoles);
            validateAndUpdate();
        });
        buttonAddSpareMarkTemplate.setEnabled(isNew);
        markTemplatesForMarkRoles.addAll(courseTemplateToEdit.getDefaultMarkTemplatesForMarkRoles().entrySet().stream()
                .map(e -> new MarkTemplateDTOAndMarkRoleDTO(e.getValue(), e.getKey()))
                .collect(Collectors.toList()));
        refreshTable(markTemplatesForMarkRolesTable, markTemplatesForMarkRoles);
        spareMarkTemplatesAndTheirDefaultMarkRoles.addAll(courseTemplateToEdit.getDefaultMarkRolesForMarkTemplates().entrySet().stream()
                .map(e -> new MarkTemplateDTOAndMarkRoleDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));
        refreshTable(spareMarkTemplatesAndTheirDefaultMarkRolesTable, spareMarkTemplatesAndTheirDefaultMarkRoles);
        waypointTemplatesTable = createWaypointTemplateTable(/* readOnly */ !isNew);
        waypointTemplates.addAll(courseTemplateToEdit.getWaypointTemplates());
        refreshWaypointsTable();
        buttonAddWaypointTemplate = new Button(stringMessages.add());
        buttonAddWaypointTemplate.addClickHandler(c -> {
            final MarkRoleDTO firstMarkRole = allMarkRoles.stream().findFirst().orElse(null);
            waypointTemplates
                    .add(new WaypointTemplateDTO(/* name */ null, /* shortName */ null, Arrays.asList(firstMarkRole, firstMarkRole),
                            Arrays.stream(PassingInstruction.relevantValues()).findFirst().orElse(null)));
            refreshWaypointsTable();
            validateAndUpdate();
        });
        buttonAddWaypointTemplate.setEnabled(isNew);
        nameTextBox.addKeyUpHandler(e -> validateAndUpdate());
        urlTextBox.addKeyUpHandler(e -> validateAndUpdate());
        tagsEditor = new StringListEditorComposite(courseTemplateToEdit.getTags(), stringMessages,
                stringMessages.edit(stringMessages.tags()), IconResources.INSTANCE.removeIcon(),
                Collections.emptyList(), stringMessages.tag());
    }

    private <T> void refreshTable(CellTable<T> table, List<T> elements) {
        table.setRowCount(elements.size());
        table.setRowData(0, elements);
    }

    private CellTable<MarkTemplateDTOAndMarkRoleDTO> createMarkTemplateAndMarkRoleTable(final boolean readOnly,
            final List<MarkTemplateDTOAndMarkRoleDTO> markTemplatesAndMarkRoles) {
        final CellTable<MarkTemplateDTOAndMarkRoleDTO> table = new BaseCelltable<>(1000, tableResources);
        table.setWidth("100%");
        final SelectionCell markTemplateSelectionCell = new SelectionCell(allMarkTemplatesSelectionListPlusEmptyString);
        final HideableAndEditableCell<MarkTemplateDTOAndMarkRoleDTO, String, SelectionCell> hideableMarkTemplateSelectionCell = new HideableAndEditableCell<MarkTemplateDTOAndMarkRoleDTO, String, SelectionCell>(
                markTemplateSelectionCell, /* hidden */ null, /* editable */ wt -> !readOnly);
        Column<MarkTemplateDTOAndMarkRoleDTO, String> markTemplateColumn = new Column<MarkTemplateDTOAndMarkRoleDTO, String>(
                hideableMarkTemplateSelectionCell) {
            @Override
            public String getValue(MarkTemplateDTOAndMarkRoleDTO markTemplateAndMarkRole) {
                return markTemplateAndMarkRole.getMarkTemplate() != null ? markTemplateAndMarkRole.getMarkTemplate().getName() : "";
            }
        };
        markTemplateColumn.setFieldUpdater(new FieldUpdater<MarkTemplateDTOAndMarkRoleDTO, String>() {
            @Override
            public void update(final int index, final MarkTemplateDTOAndMarkRoleDTO markTemplateAndMarkRole, final String value) {
                final MarkTemplateDTOAndMarkRoleDTO newMarkTemplateAndMarkRole = new MarkTemplateDTOAndMarkRoleDTO(allMarkTemplates.stream().filter(mt -> mt.getName().equals(value))
                        .findFirst().get(), markTemplateAndMarkRole.getMarkRole());
                markTemplatesAndMarkRoles.set(index, newMarkTemplateAndMarkRole);
                table.setRowData(index, Collections.singletonList(newMarkTemplateAndMarkRole));
                validateAndUpdate();
            }
        });
        final SelectionCell associatedRoleSelectionCell = new SelectionCell(allMarkRolesSelectionListPlusEmptyString);
        final HideableAndEditableCell<MarkTemplateDTOAndMarkRoleDTO, String, SelectionCell> hideableAssociatedRoleSelectionCell = new HideableAndEditableCell<MarkTemplateDTOAndMarkRoleDTO, String, SelectionCell>(
                associatedRoleSelectionCell, /* hidden */ null, /* editable */ wt -> !readOnly);
        Column<MarkTemplateDTOAndMarkRoleDTO, String> markRoleColumn = new Column<MarkTemplateDTOAndMarkRoleDTO, String>(
                hideableAssociatedRoleSelectionCell) {
            @Override
            public String getValue(MarkTemplateDTOAndMarkRoleDTO markTemplate) {
                return markTemplate.getMarkRole() != null ? markTemplate.getMarkRole().getName() : "";
            }
        };
        markRoleColumn.setFieldUpdater(new FieldUpdater<MarkTemplateDTOAndMarkRoleDTO, String>() {
            @Override
            public void update(final int index, final MarkTemplateDTOAndMarkRoleDTO markTemplateAndMarkRole, final String value) {
                final MarkTemplateDTOAndMarkRoleDTO newMarkTemplateAndMarkRole = new MarkTemplateDTOAndMarkRoleDTO(markTemplateAndMarkRole.getMarkTemplate(),
                        allMarkRoles.stream().filter(mr -> mr.getName().equals(value))
                        .findFirst().get());
                markTemplatesAndMarkRoles.set(index, newMarkTemplateAndMarkRole);
                table.setRowData(index, Collections.singletonList(newMarkTemplateAndMarkRole));
                validateAndUpdate();
            }
        });
        DefaultActionsImagesBarCell imagesBarCell = new DefaultActionsImagesBarCell(stringMessages) {
            @Override
            protected Iterable<ImageSpec> getImageSpecs() {
                return readOnly ? Collections.emptyList() : Arrays.asList(getDeleteImageSpec());
            }
        };
        ImagesBarColumn<MarkTemplateDTOAndMarkRoleDTO, DefaultActionsImagesBarCell> actionsColumn = new ImagesBarColumn<>(
                imagesBarCell);
        actionsColumn.setFieldUpdater(new FieldUpdater<MarkTemplateDTOAndMarkRoleDTO, String>() {
            @Override
            public void update(int index, MarkTemplateDTOAndMarkRoleDTO markTemplate, String value) {
                markTemplatesAndMarkRoles.remove(index);
                validateAndUpdate();
                refreshTable(table, markTemplatesAndMarkRoles);
            }
        });
        table.addColumn(markTemplateColumn, stringMessages.markTemplate());
        table.addColumn(markRoleColumn, stringMessages.markRoles());
        table.addColumn(actionsColumn, stringMessages.actions());
        return table;
    }

    private void refreshWaypointsTable() {
        waypointTemplatesTable.setRowCount(waypointTemplates.size());
        waypointTemplatesTable.setRowData(0, (List<? extends WaypointTemplateDTO>) waypointTemplates);
    }

    private CellTable<WaypointTemplateDTO> createWaypointTemplateTable(final boolean readOnly) {
        final CellTable<WaypointTemplateDTO> waypointTemplatesTable = new BaseCelltable<>(1000, tableResources);
        waypointTemplatesTable.setWidth("100%");
        final HideableAndEditableCell<WaypointTemplateDTO, String, TextInputCell> hideableShortNameCell = new HideableAndEditableCell<>(
                new TextInputCell(), /* hidden */ wt -> !hasTwoMarks(wt), /* editable */ wt -> !readOnly);
        Column<WaypointTemplateDTO, String> shortNameColumn = new Column<WaypointTemplateDTO, String>(
                hideableShortNameCell) {
            @Override
            public String getValue(WaypointTemplateDTO waypointTemplate) {
                return waypointTemplate.getShortName();
            }
        };
        shortNameColumn.setFieldUpdater(new FieldUpdater<WaypointTemplateDTO, String>() {
            @Override
            public void update(int index, WaypointTemplateDTO waypointTemplate, String value) {
                waypointTemplate.setShortName(value);
                validateAndUpdate();
            }
        });
        final HideableAndEditableCell<WaypointTemplateDTO, String, TextInputCell> hideableNameCell = new HideableAndEditableCell<>(
                new TextInputCell(), /* hidden */ wt -> !hasTwoMarks(wt), /* editable */ wt -> !readOnly);
        Column<WaypointTemplateDTO, String> nameColumn = new Column<WaypointTemplateDTO, String>(hideableNameCell) {
            @Override
            public String getValue(WaypointTemplateDTO waypointTemplate) {
                return waypointTemplate.getName();
            }
        };
        nameColumn.setFieldUpdater(new FieldUpdater<WaypointTemplateDTO, String>() {
            @Override
            public void update(int index, WaypointTemplateDTO waypointTemplate, String value) {
                waypointTemplate.setName(value);
                validateAndUpdate();
            }
        });
        final SelectionCell markRole1SelectionCell = new SelectionCell(allMarkRolesSelectionListPlusEmptyString);
        final HideableAndEditableCell<WaypointTemplateDTO, String, SelectionCell> hideableMarkRole1Cell = new HideableAndEditableCell<WaypointTemplateDTO, String, SelectionCell>(
                markRole1SelectionCell, /* hidden */ null, /* editable */ wt -> !readOnly);
        Column<WaypointTemplateDTO, String> markRoleColumn1 = new Column<WaypointTemplateDTO, String>(
                hideableMarkRole1Cell) {
            @Override
            public String getValue(WaypointTemplateDTO waypointTemplate) {
                return waypointTemplate.getMarkRolesForControlPoint() != null
                        && waypointTemplate.getMarkRolesForControlPoint().size() >= 1
                                ? waypointTemplate.getMarkRolesForControlPoint().get(0).getName()
                                : "";
            }
        };
        markRoleColumn1.setFieldUpdater(new FieldUpdater<WaypointTemplateDTO, String>() {
            @Override
            public void update(int index, WaypointTemplateDTO waypointTemplate, String value) {
                final MarkRoleDTO selectedMarkRole = allMarkRoles.stream()
                        .filter(mr -> mr.getName().equals(value)).findFirst().get();
                if (waypointTemplate.getMarkRolesForControlPoint().size() == 0) {
                    waypointTemplate.getMarkRolesForControlPoint().add(selectedMarkRole);
                } else {
                    waypointTemplate.getMarkRolesForControlPoint().set(0, selectedMarkRole);
                }
                validateAndUpdate();
            }
        });
        final SelectionCell markRole2SelectionCell = new SelectionCell(allMarkRolesSelectionListPlusEmptyString);
        final HideableAndEditableCell<WaypointTemplateDTO, String, SelectionCell> hideableMarkRole2Cell = new HideableAndEditableCell<WaypointTemplateDTO, String, SelectionCell>(
                markRole2SelectionCell, /* hidden */ wt -> !hasTwoMarks(wt), /* editable */ wt -> !readOnly);
        Column<WaypointTemplateDTO, String> markRoleColumn2 = new Column<WaypointTemplateDTO, String>(
                hideableMarkRole2Cell) {
            @Override
            public String getValue(WaypointTemplateDTO waypointTemplate) {
                return waypointTemplate.getMarkRolesForControlPoint() != null
                        && waypointTemplate.getMarkRolesForControlPoint().size() == 2
                                ? waypointTemplate.getMarkRolesForControlPoint().get(1).getName()
                                : null;
            }
        };
        markRoleColumn2.setFieldUpdater(new FieldUpdater<WaypointTemplateDTO, String>() {
            @Override
            public void update(int index, WaypointTemplateDTO waypointTemplate, String value) {
                final MarkRoleDTO selectedMarkRole = allMarkRoles.stream()
                        .filter(mr -> mr.getName().equals(value)).findFirst().get();
                if (waypointTemplate.getMarkRolesForControlPoint().size() == 1) {
                    waypointTemplate.getMarkRolesForControlPoint().add(selectedMarkRole);
                } else {
                    waypointTemplate.getMarkRolesForControlPoint().set(1, selectedMarkRole);
                }
                validateAndUpdate();
            }
        });
        final HideableAndEditableCell<WaypointTemplateDTO, String, SelectionCell> hideablePassingInstructionCell = new HideableAndEditableCell<WaypointTemplateDTO, String, SelectionCell>(
                new SelectionCell(Arrays.stream(PassingInstruction.relevantValues()).map(PassingInstruction::name)
                        .collect(Collectors.toList())),
                /* hidden */ null, /* editable */ wt -> !readOnly);
        Column<WaypointTemplateDTO, String> passingInstructionColumn = new Column<WaypointTemplateDTO, String>(
                hideablePassingInstructionCell) {
            @Override
            public String getValue(WaypointTemplateDTO waypointTemplate) {
                return waypointTemplate.getPassingInstruction().name();
            }
        };
        passingInstructionColumn.setFieldUpdater(new FieldUpdater<WaypointTemplateDTO, String>() {
            @Override
            public void update(int index, WaypointTemplateDTO waypointTemplate, String value) {
                waypointTemplate.setPassingInstruction(Arrays.stream(PassingInstruction.relevantValues())
                        .filter(pi -> pi.name().equals(value)).findFirst().get().name());
                validateAndUpdate();
                refreshWaypointsTable();
            }
        });
        DefaultActionsImagesBarCell imagesBarCell = new DefaultActionsImagesBarCell(stringMessages) {
            @Override
            protected Iterable<ImageSpec> getImageSpecs() {
                return readOnly ? Collections.emptyList() : Arrays.asList(getDeleteImageSpec());
            }
        };
        ImagesBarColumn<WaypointTemplateDTO, DefaultActionsImagesBarCell> actionsColumn = new ImagesBarColumn<>(
                imagesBarCell);
        actionsColumn.setFieldUpdater(new FieldUpdater<WaypointTemplateDTO, String>() {
            @Override
            public void update(int index, WaypointTemplateDTO waypointTemplate, String value) {
                waypointTemplates.removeIf(r -> r == waypointTemplate);
                validateAndUpdate();
                refreshWaypointsTable();
            }
        });
        waypointTemplatesTable.addColumn(passingInstructionColumn, stringMessages.passingInstructions());
        waypointTemplatesTable.addColumn(shortNameColumn, stringMessages.shortName());
        waypointTemplatesTable.addColumn(nameColumn, stringMessages.name());
        waypointTemplatesTable.addColumn(markRoleColumn1, stringMessages.markRole1());
        waypointTemplatesTable.addColumn(markRoleColumn2, stringMessages.markRole2());
        waypointTemplatesTable.addColumn(actionsColumn, stringMessages.actions());
        return waypointTemplatesTable;
    }

    private static boolean hasTwoMarks(final WaypointTemplateDTO wt) {
        boolean hasTwoMarks = wt != null && wt.getPassingInstruction() != null
                && Arrays.stream(wt.getPassingInstruction().applicability).anyMatch(a -> a == 2);
        return hasTwoMarks;

    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return nameTextBox;
    }

    @Override
    protected CourseTemplateDTO getResult() {
        final UUID id = currentUuid == null ? UUID.randomUUID() : currentUuid;
        final ArrayList<MarkTemplateDTO> markTemplates = new ArrayList<MarkTemplateDTO>();
        final Map<MarkTemplateDTO, MarkRoleDTO> defaultMarkRolesForSpareMarkTemplates = new HashMap<>();
        final Map<MarkRoleDTO, MarkTemplateDTO> defaultMarkTemplatesForMarkRoles = new HashMap<>();
        final String optionalUrl = urlTextBox.getText() != null && !urlTextBox.getText().isEmpty()
                ? urlTextBox.getText() : null;
        this.markTemplatesForMarkRoles.stream().forEach(mt -> {
            markTemplates.add(mt.getMarkTemplate());
            if (mt.getMarkRole() != null) {
                defaultMarkTemplatesForMarkRoles.put(mt.getMarkRole(), mt.getMarkTemplate());
            }
        });
        this.spareMarkTemplatesAndTheirDefaultMarkRoles.stream().forEach(mt -> {
            markTemplates.add(mt.getMarkTemplate());
            if (mt.getMarkRole() != null) {
                defaultMarkRolesForSpareMarkTemplates.put(mt.getMarkTemplate(), mt.getMarkRole());
            }
        });
        final RepeatablePart repeatablePart = zeroBasedIndexOfFirstWaypointOfRepeatablePart.getValue() == null && zeroBasedIndexOfLastWaypointOfRepeatablePart.getValue() == null ? null :
            RepeatablePart.of(
                zeroBasedIndexOfFirstWaypointOfRepeatablePart.getValue() == null ? -1 : zeroBasedIndexOfFirstWaypointOfRepeatablePart.getValue(),
                        zeroBasedIndexOfLastWaypointOfRepeatablePart.getValue() == null ? -1 : zeroBasedIndexOfLastWaypointOfRepeatablePart.getValue()+1);
        final Integer defaultNumberOfLaps = numberOfLapsTextBox.getValue();
        waypointTemplates.forEach(wt -> {
            if (!hasTwoMarks(wt) && wt.getMarkRolesForControlPoint() != null
                    && wt.getMarkRolesForControlPoint().size() == 2) {
                wt.getMarkRolesForControlPoint().remove(1);
            }
        });
        return new CourseTemplateDTO(id, nameTextBox.getValue(), shortNameTextBox.getValue(), markTemplates,
                waypointTemplates, defaultMarkRolesForSpareMarkTemplates, defaultMarkTemplatesForMarkRoles, optionalUrl,
                tagsEditor.getValue(), repeatablePart, defaultNumberOfLaps);
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(10, 2);
        int row = 0;
        result.setWidget(row, 0, new Label(stringMessages.name()));
        result.setWidget(row++, 1, nameTextBox);
        result.setWidget(row, 0, new Label(stringMessages.shortName()));
        result.setWidget(row++, 1, shortNameTextBox);
        result.setWidget(row, 0, new Label(stringMessages.imageURL()));
        result.setWidget(row++, 1, urlTextBox);
        result.setWidget(row++, 1, new Label(stringMessages.markRoles()));
        result.setWidget(row, 0, buttonAddMarkRoleToMarkTemplateMapping);
        result.setWidget(row++, 1, markTemplatesForMarkRolesTable);
        result.setWidget(row, 0, buttonAddWaypointTemplate);
        result.setWidget(row++, 1, waypointTemplatesTable);
        result.setWidget(row++, 1, new Label(stringMessages.spareMarksAndTheirOptionalDefaultMarkRoles()));
        result.setWidget(row, 0, buttonAddSpareMarkTemplate);
        result.setWidget(row++, 1, spareMarkTemplatesAndTheirDefaultMarkRolesTable);
        result.setWidget(row, 0, new Label(stringMessages.tags()));
        result.setWidget(row++, 1, tagsEditor);
        result.setWidget(row, 0, new Label(stringMessages.defaultNumberOfLaps()));
        final HorizontalPanel panelForLapCountAndRepeatablePartDefinition = new HorizontalPanel();
        panelForLapCountAndRepeatablePartDefinition.add(numberOfLapsTextBox);
        panelForLapCountAndRepeatablePartDefinition.add(new Label(stringMessages.zeroBasedNumberOfWaypointForRepeatablePartStart()));
        panelForLapCountAndRepeatablePartDefinition.add(zeroBasedIndexOfFirstWaypointOfRepeatablePart);
        panelForLapCountAndRepeatablePartDefinition.add(new Label(stringMessages.zeroBasedNumberOfWaypointForRepeatablePartEnd()));
        panelForLapCountAndRepeatablePartDefinition.add(zeroBasedIndexOfLastWaypointOfRepeatablePart);
        result.setWidget(row++, 1, panelForLapCountAndRepeatablePartDefinition);
        return result;
    }

    /**
     * Used to describe connections between mark roles and mark templates in the context of a cell table's row type.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private static class MarkTemplateDTOAndMarkRoleDTO {
        private final MarkTemplateDTO markTemplate;
        private final MarkRoleDTO markRole;

        public MarkTemplateDTOAndMarkRoleDTO(final MarkTemplateDTO markTemplate, final MarkRoleDTO markRole) {
            this.markTemplate = markTemplate;
            this.markRole = markRole;
        }

        public MarkTemplateDTO getMarkTemplate() {
            return markTemplate;
        }

        public MarkRoleDTO getMarkRole() {
            return markRole;
        }
    }

    public static class HideableAndEditableCell<DTO, V, T extends Cell<V>> implements Cell<V> {
        private final T concreteCell;
        private final Predicate<DTO> hiddenPredicate;
        private final Predicate<DTO> editablePredicate;

        public HideableAndEditableCell(final T concreteCell, final Predicate<DTO> hiddenPredicate,
                final Predicate<DTO> editablePredicate) {
            this.concreteCell = concreteCell;
            this.hiddenPredicate = hiddenPredicate;
            this.editablePredicate = editablePredicate;
        }

        @Override
        public void render(final Context context, final V value, final SafeHtmlBuilder sb) {
            @SuppressWarnings("unchecked")
            DTO object = (DTO) context.getKey();
            if (hiddenPredicate == null || !hiddenPredicate.test(object)) {
                if (editablePredicate != null && !editablePredicate.test(object)) {
                    sb.appendHtmlConstant("<div contentEditable='false' unselectable='false' >" + value + "</div>");
                } else {
                    concreteCell.render(context, value, sb);
                }
            }
        }

        @Override
        public boolean dependsOnSelection() {
            return concreteCell.dependsOnSelection();
        }

        @Override
        public Set<String> getConsumedEvents() {
            return concreteCell.getConsumedEvents();
        }

        @Override
        public boolean handlesSelection() {
            return concreteCell.handlesSelection();
        }

        @Override
        public boolean isEditing(Context context, Element parent, V value) {
            return concreteCell.isEditing(context, parent, value);
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, V value, NativeEvent event,
                ValueUpdater<V> valueUpdater) {
            concreteCell.onBrowserEvent(context, parent, value, event, valueUpdater);
        }

        @Override
        public boolean resetFocus(Context context, Element parent, V value) {
            return concreteCell.resetFocus(context, parent, value);
        }

        @Override
        public void setValue(Context context, Element parent, V value) {
            concreteCell.setValue(context, parent, value);
        }

    }

}
