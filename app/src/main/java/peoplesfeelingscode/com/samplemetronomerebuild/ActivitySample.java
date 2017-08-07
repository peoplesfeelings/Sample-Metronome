package peoplesfeelingscode.com.samplemetronomerebuild;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
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

        files = Storage.getFileList();

        setUpListView();

        checkForEmptyList();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        files = Storage.getFileList();

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
                //
            }
        });
    }
}
