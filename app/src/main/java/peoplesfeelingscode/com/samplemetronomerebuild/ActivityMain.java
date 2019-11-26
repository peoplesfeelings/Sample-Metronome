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

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.WarwickWestonWright.HGDialV2.HGDialInfo;
import com.WarwickWestonWright.HGDialV2.HGDialV2;

import java.io.File;

import peoplesfeelingscode.com.pfseq.PFSeqMessage;

import static peoplesfeelingscode.com.pfseq.PFSeqMessage.MESSAGE_TYPE_ERROR;

public class ActivityMain extends ActivityBase {
    final static int TEMPO_CHANGE_POLLING_MS = 100;

    HGDialV2 hgDialV2;
    HGDialV2.IHGDial ihgDial;

    TextView txtBpm;
    Button btnSamples;
    Button btnStartStop;
    Spinner rateSpinner;
    TextWatcher textWatcher;
    ArrayAdapter<String> spinnerAdapter;

    private long lastBpmChangeMillis;

    // activity life cycle stuff
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(Dry.TAG, "onCreate ");

        setContentView(R.layout.activity_main);

        hgDialV2 = (HGDialV2) findViewById(R.id.hgDialV2);
        txtBpm = (TextView) findViewById(R.id.txtBpm);
        btnSamples = (Button) findViewById(R.id.btnSamples);
        btnStartStop = (Button) findViewById(R.id.btnStartStop);
        rateSpinner = (Spinner) findViewById(R.id.rateSpinner);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isBound() && !getSeq().isPlaying()) {
            stopService(new Intent(this, getServiceClass()));
        }
    }

    // pfseq stuff
    @Override
    public void onConnect() {
        super.onConnect();

        // first confirm content has been set up (may not have been if first run and user did not grant write permission yet, in which case this will be called again after setup)
        if (getSeq().getTracks() != null) {
            // set up dial
            ihgDial = new HGDialV2.IHGDial() {
                @Override
                public void onDown(HGDialInfo hgDialInfo) {
                    //
                }
                @Override
                public void onPointerDown(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
                @Override
                public void onMove(HGDialInfo hgDialInfo) {
                    if(System.currentTimeMillis() > lastBpmChangeMillis + TEMPO_CHANGE_POLLING_MS) {
                        double acceptedBpm = getSeq().setBpm(Storage.ftaToBpm(preventNegative(hgDialV2.getFullTextureAngle())));
                        lastBpmChangeMillis = System.currentTimeMillis();
                        txtBpm.removeTextChangedListener(textWatcher);
                        txtBpm.setText(Double.toString(acceptedBpm));
                        txtBpm.addTextChangedListener(textWatcher);
                    }
                }
                @Override
                public void onPointerUp(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
                @Override
                public void onUp(HGDialInfo hgDialInfo) {
                    double bpm = Storage.ftaToBpm(preventNegative(hgDialV2.getFullTextureAngle()));

                    double acceptedBpm = getSeq().setBpm(bpm);
                    lastBpmChangeMillis = System.currentTimeMillis();
                    txtBpm.removeTextChangedListener(textWatcher);
                    txtBpm.setText(Double.toString(acceptedBpm));
                    txtBpm.addTextChangedListener(textWatcher);

                    double acceptedFta = Storage.bpmToFta(acceptedBpm);
                    Storage.setSharedPrefDouble(editor, acceptedFta, Storage.SHARED_PREF_FTA_KEY, ActivityMain.this);
                }
            };
            hgDialV2.registerCallback(ihgDial);

            // load stored fta
            double fta = Storage.getSharedPrefDouble(sharedPrefs, Storage.SHARED_PREF_FTA_KEY, this);
            /* after first install, ui set-up methods run before appVersionSetUp runs */
            if (fta == Storage.DEFAULT_SHARED_PREF_INT) {
                fta = Storage.bpmToFta(Storage.DEFAULT_BPM);
            }
            hgDialV2.doRapidDial(fta);
            hgDialV2.doManualGestureDial(fta);

            // set up edit text
            txtBpm.setText(Double.toString(Storage.ftaToBpm(Storage.getSharedPrefDouble(sharedPrefs, Storage.SHARED_PREF_FTA_KEY, this))));
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    String input = txtBpm.getText().toString();
                    if (input.length() > 0 && !input.equals(".")) {
                        double fta = Storage.bpmToFta(Double.parseDouble(txtBpm.getText().toString()));

                        hgDialV2.doRapidDial(fta);
                        hgDialV2.doManualGestureDial(fta);

                        getSeq().setBpm(Storage.ftaToBpm(hgDialV2.getFullTextureAngle()));

                        Storage.setSharedPrefDouble(editor, fta, Storage.SHARED_PREF_FTA_KEY, ActivityMain.this);
                    }
                }
            };
            txtBpm.addTextChangedListener(textWatcher);

            // set up spinner
            spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.rates)) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    ((TextView) v).setGravity(Gravity.CENTER);
                    return v;
                }
                public View getDropDownView(int position, View convertView,ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView,parent);
                    ((TextView) v).setGravity(Gravity.CENTER);
                    return v;
                }
            };
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            rateSpinner.setAdapter(spinnerAdapter);
            int tickRate = Storage.getSharedPrefInt(Storage.SHARED_PREF_RATE_KEY, this);
            /* after first install, ui set-up methods run before appVersionSetUp runs */
            if (tickRate == Storage.DEFAULT_SHARED_PREF_INT) {
                tickRate = Storage.DEFAULT_RATE;
            }
            rateSpinner.setSelection(tickRate);
            rateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                    setSeqRate(pos);

                    Storage.setSharedPrefInt(pos, Storage.SHARED_PREF_RATE_KEY, ActivityMain.this);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

            // button listeners
            btnSamples.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(ActivityMain.this, ActivitySample.class));
                }
            });
            if (getSeq().isPlaying()) {
                btnStartStop.setText(getResources().getString(R.string.btnStop));
            }
            btnStartStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!getSeq().isPlaying()) {
                        if (!new File(Storage.path, Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, ActivityMain.this)).exists()) {
                            Toast toast = Toast.makeText(ActivityMain.this, R.string.toastSampleNotSelected, Toast.LENGTH_LONG);
                            toast.show();
                            return;
                        }
                        if (isBound()) {
                            getSeq().setBpm(Storage.ftaToBpm(hgDialV2.getFullTextureAngle()));
                            lastBpmChangeMillis = System.currentTimeMillis();
                            getSeq().play();
                            btnStartStop.setText(getResources().getString(R.string.btnStop));
                        }
                    } else {
                        if (isBound()) {
                            getSeq().stop();
                            btnStartStop.setText(getResources().getString(R.string.btnStart));
                        }
                    }
                }
            });
        }
    }
    @Override
    public void receiveMessage(PFSeqMessage pfSeqMessage) {
        super.receiveMessage(pfSeqMessage);

        switch (pfSeqMessage.getType()) {
            case MESSAGE_TYPE_ERROR:
                if (isBound()) {
                    if (getSeq().isSetUp() && getSeq().isPlaying()) {
                        getSeq().stop();
                    }
                    btnStartStop.setText(getResources().getString(R.string.btnStart));
                }
                break;
        }
    }

    double preventNegative(double fta) {
        if (fta < 0) {
            hgDialV2.setFullTextureAngle(0);

            return 0;
        }

        return fta;
    }
}
