package com.sap.sailing.racecommittee.app.ui;

import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public enum NavigationEvents {
    INSTANCE;

    public void attach(Fragment fragment) {
        for (NavigationListener navigationListener : fragmentAttachListeners) {
            navigationListener.onFragmentAttach(fragment);
        }
    }

    public void detach(Fragment fragment) {
        for (NavigationListener navigationListener : fragmentAttachListeners) {
            navigationListener.onFragmentDetach(fragment);
        }
    }

    public interface NavigationListener {
        void onFragmentAttach(Fragment fragment);

        void onFragmentDetach(Fragment fragment);
    }

    private final List<NavigationListener> fragmentAttachListeners = new CopyOnWriteArrayList<>();

    public void subscribeFragmentAttachment(NavigationListener listener) {
        fragmentAttachListeners.add(listener);
    }

    public void unSubscribeFragmentAttachment(NavigationListener listener) {
        fragmentAttachListeners.remove(listener);
    }
}
