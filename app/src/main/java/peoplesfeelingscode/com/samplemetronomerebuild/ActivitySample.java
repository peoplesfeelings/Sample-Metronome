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

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;

public class ActivitySample extends ActivityBase {

    ListView listView;
    FragmentSampleActivityEmptyList emptyListDialog;

    AdapterSampleActivityList adapter;
    ArrayList<ObjectFile> files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        listView = (ListView) findViewById(R.id.listView);
        emptyListDialog = new FragmentSampleActivityEmptyList();

        files = Storage.getFileList(Storage.path);

        setUpListView();

        checkForEmptyList();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        files = Storage.getFileList(Storage.path);

        setUpListView();

        checkForEmptyList();
    }

    void checkForEmptyList() {
        if (files.size() == 0) {
            emptyListDialog.show(getFragmentManager().beginTransaction(), "");
        }
    }

    void setUpListView() {
        adapter = new AdapterSampleActivityList(this, files);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int pos, long id) {
                CheckBox chk = (CheckBox) viewClicked.findViewById(R.id.itemChkBx);
                chk.performClick();
            }
        });
    }
}
