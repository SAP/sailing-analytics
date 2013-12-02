/**
 * 
 */
package com.sap.sailing.racecommittee.app.ui.fragments.dialogs.prerequisite;

import java.util.Set;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;

import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.racelog.state.racingprocedure.rrs26.RRS26RacingProcedure;
import com.sap.sailing.domain.racelog.state.racingprocedure.rrs26.impl.StartmodePrerequisite;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.utils.FlagsBitmapCache;

public class RaceChooseStartModeDialog extends PrerequisiteRaceDialog<StartmodePrerequisite, Flags> {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.race_choose_start_mode_view, container);
        getDialog().setTitle(getText(R.string.choose_startmode));
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Set<Flags> flags = preferences.getRRS26StartmodeFlags();
        GridView container = (GridView) getView().findViewById(R.id.race_choose_startmode_container);
        container.setNumColumns(flags.size() <= 4 ? 2 : 3);
        container.setAdapter(new FlagImageAdapter(getActivity(), flags));
    }
    
    @Override
    protected void onNormalChosen(Flags flag) {
        getRaceState().getTypedRacingProcedure(RRS26RacingProcedure.class).setStartModeFlag(
                MillisecondsTimePoint.now(), flag);
    }
    
    @Override
    protected void onPrerequisiteChosen(StartmodePrerequisite prerequisite, Flags flag) {
        prerequisite.fulfill(flag);
    }
    
    private class FlagImageAdapter extends ArrayAdapter<Flags> {

        private final FlagsBitmapCache bitmapCache;

        public FlagImageAdapter(Context context, Set<Flags> flags) {
            super(context, 0, flags.toArray(new Flags[flags.size()]));
            this.bitmapCache = new FlagsBitmapCache(context);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new ImageButton(getContext());
            }
            
            final Flags flag = getItem(position);
            
            ImageButton button = (ImageButton) convertView;
            button.setImageBitmap(bitmapCache.getBitmap(flag, Flags.NONE));
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    onChosen(flag);
                    dismiss();
                }
            });
            
            return convertView;
        }
        
    }
}
