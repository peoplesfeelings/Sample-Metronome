package peoplesfeelingscode.com.samplemetronomerebuild;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by User Name on 9/10/2016.
 */
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
        if (files.get(pos).name.equals(Storage.selectedFileName)) {
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

                    Storage.selectedFileName = files.get(pos).name;
                    Storage.fileNeedsToBeLoaded = true;
                    Storage.setSharedPrefString(Storage.selectedFileName, Storage.SHARED_PREF_SELECTED_FILE_KEY, activity);
                }
            }
        });

        return v;
    }

}
