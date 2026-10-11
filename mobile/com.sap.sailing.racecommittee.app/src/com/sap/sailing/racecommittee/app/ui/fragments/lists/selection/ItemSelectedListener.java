package com.sap.sailing.racecommittee.app.ui.fragments.lists.selection;

import androidx.fragment.app.Fragment;

public interface ItemSelectedListener<T> {
    void itemSelected(Fragment sender, T item);
}
