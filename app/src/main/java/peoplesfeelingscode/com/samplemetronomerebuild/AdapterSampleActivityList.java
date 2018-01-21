/*

Sample Metronome
Copyright (C) 2017 People's Feelings

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

*/

package peoplesfeelingscode.com.samplemetronomerebuild;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

public class AdapterSampleActivityList extends ArrayAdapter<ObjectFile> {
    ActivitySample activity;
    ArrayList<ObjectFile> files;

    public AdapterSampleActivityList(ActivitySample activity, ArrayList<ObjectFile> files) {
        super(activity, R.layout.list_item_checkbox, files);

        this.activity = activity;
        this.files = files;
    }

    @Override
    public View getView(final int pos, View v, ViewGroup parent) {
        final int position = pos;

        ViewHolderSampleActivityList holder = null;

        if(v==null) {
            LayoutInflater vi = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_item_checkbox, null);

            holder = new ViewHolderSampleActivityList();
            holder.txt = (TextView) v.findViewById(R.id.itemTxt);
            holder.chk = (CheckBox) v.findViewById(R.id.itemChkBx);
            v.setTag(holder);

        } else {
            holder = (ViewHolderSampleActivityList) v.getTag();
        }

        holder.txt.setText(files.get(pos).name);

        if (files.get(pos).name.equals(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, activity))) {
            holder.chk.setChecked(true);
        } else {
            holder.chk.setChecked(false);
        }
        holder.chk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox cb = (CheckBox) view;
                if(cb.isChecked()) {
                    ViewHolderSampleActivityList otherHolder;
                    for (int i = 0; i < activity.listView.getChildCount(); i++) {
                        otherHolder = (ViewHolderSampleActivityList) activity.listView.getChildAt(i).getTag();

                        otherHolder.chk.setChecked(false);
                    }
                    cb.setChecked(true);

                    Storage.setSharedPrefString(files.get(pos).name, Storage.SHARED_PREF_SELECTED_FILE_KEY, activity);
                    Storage.fileNeedsToBeLoaded = true;
                }
            }
        });

        return v;
    }
}
