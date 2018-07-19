package com.sap.sailing.gwt.ui.client.shared.filter;

import java.util.List;

import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.TagDTO;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;

public class EditTagsFilterSetDialog extends AbstractTagsFilterSetDialog {

    public EditTagsFilterSetDialog(FilterSet<TagDTO, FilterWithUI<TagDTO>> tagsFilterSet, List<String> availableTagFilterNames, 
            List<String> existingFilterSetNames, StringMessages stringMessages, DialogCallback<FilterSet<TagDTO, FilterWithUI<TagDTO>>> callback) {
        super(tagsFilterSet, availableTagFilterNames, existingFilterSetNames, stringMessages.actionEditFilter(), stringMessages, callback);
        filterSetNameTextBox = createTextBox(tagsFilterSet.getName());
    }
}
