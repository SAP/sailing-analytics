package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import androidx.fragment.app.Fragment;

public abstract class FragmentAttachedDialogFragment extends AttachedDialogFragment {

    @Override
    protected DialogListenerHost getListenerHost() {
        Fragment targetFragment = getTargetFragment();
        if (targetFragment instanceof DialogListenerHost) {
            return (DialogListenerHost) targetFragment;
        }
        throw new IllegalStateException(String.format("Instance of %s must be attached to instances of %s.",
                targetFragment.getClass().getName(), DialogListenerHost.class.getName()));
    }

}
