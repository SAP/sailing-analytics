/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sap.sailing.android.shared.ui.utils;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

import android.content.Context;
import androidx.annotation.UiThread;

/**
 * Generic tracker which is used for tracking or reading a barcode (and can really be used for any type of item). This
 * is used to receive newly detected items, add a graphical representation to an overlay, update the graphics as the
 * item changes, and remove the graphics when the item goes away.
 */
public class BarcodeGraphicTracker extends Tracker<Barcode> {

    private BarcodeUpdateListener mBarcodeUpdateListener;

    /**
     * Consume the item instance detected from an Activity or Fragment level by implementing the BarcodeUpdateListener
     * interface method onBarcodeDetected.
     */
    public interface BarcodeUpdateListener {
        @UiThread
        void onBarcodeDetected(Barcode barcode);
    }

    BarcodeGraphicTracker(Context context) {
        if (context instanceof BarcodeUpdateListener) {
            this.mBarcodeUpdateListener = (BarcodeUpdateListener) context;
        } else {
            throw new RuntimeException("Hosting activity must implement BarcodeUpdateListener");
        }
    }

    /**
     * Start tracking the detected item instance within the item overlay.
     */
    @Override
    public void onNewItem(int id, Barcode item) {
        mBarcodeUpdateListener.onBarcodeDetected(item);
    }
}