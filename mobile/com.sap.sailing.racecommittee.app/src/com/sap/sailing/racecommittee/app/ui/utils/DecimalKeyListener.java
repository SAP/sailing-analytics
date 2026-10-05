package com.sap.sailing.racecommittee.app.ui.utils;

import androidx.annotation.NonNull;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.NumberKeyListener;

public class DecimalKeyListener extends NumberKeyListener {

    @NonNull
    @Override
    protected char[] getAcceptedChars() {
        return new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ',', '.'};
    }

    @Override
    public int getInputType() {
        return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        CharSequence out = super.filter(source, start, end, dest, dstart, dend);

        if (out != null) {
            source = out;
            start = 0;
            end = out.length();
        }

        int decimal = -1;
        int dlen = dest.length();

        /*
         * Find out if the existing text has a decimal point characters.
         */

        for (int i = 0; i < dstart; i++) {
            char c = dest.charAt(i);

            if (isDecimalPointChar(c)) {
                decimal = i;
            }
        }
        for (int i = dend; i < dlen; i++) {
            char c = dest.charAt(i);

            if (isDecimalPointChar(c)) {
                decimal = i;
            }
        }

        /*
         * If it does, we must strip them out from the source.
         * Go in reverse order so the offsets are stable.
         */

        SpannableStringBuilder stripped = null;

        for (int i = end - 1; i >= start; i--) {
            char c = source.charAt(i);
            boolean strip = false;

            if (isDecimalPointChar(c)) {
                if (decimal >= 0) {
                    strip = true;
                } else {
                    decimal = i;
                }
            }

            if (strip) {
                if (end == start + 1) {
                    return "";  // Only one character, and it was stripped.
                }

                if (stripped == null) {
                    stripped = new SpannableStringBuilder(source, start, end);
                }

                stripped.delete(i - start, i + 1 - start);
            }
        }

        if (stripped != null) {
            return stripped;
        } else if (out != null) {
            return out;
        } else {
            return null;
        }
    }

    private boolean isDecimalPointChar(final char c) {
        return c == ',' || c == '.';
    }
}
