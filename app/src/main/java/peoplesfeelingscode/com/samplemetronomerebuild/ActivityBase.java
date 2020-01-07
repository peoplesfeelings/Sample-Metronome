

package peoplesfeelingscode.com.samplemetronomerebuild;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;

import peoplesfeelingscode.com.pfseq.PFSeq;
import peoplesfeelingscode.com.pfseq.PFSeqActivity;
import peoplesfeelingscode.com.pfseq.PFSeqClip;
import peoplesfeelingscode.com.pfseq.PFSeqConfig;
import peoplesfeelingscode.com.pfseq.PFSeqMessage;
import peoplesfeelingscode.com.pfseq.PFSeqPianoRollItem;
import peoplesfeelingscode.com.pfseq.PFSeqTimeOffset;
import peoplesfeelingscode.com.pfseq.PFSeqTrack;

import static peoplesfeelingscode.com.pfseq.PFSeq.LOG_TAG;
import static peoplesfeelingscode.com.pfseq.PFSeqConfig.ONGOING_NOTIF_ID;
import static peoplesfeelingscode.com.pfseq.PFSeqConfig.TIME_SIG_LOWER;
import static peoplesfeelingscode.com.pfseq.PFSeqConfig.TIME_SIG_UPPER;
import static peoplesfeelingscode.com.pfseq.PFSeqMessage.ALERT_MSG_PREFIX;
import static peoplesfeelingscode.com.pfseq.PFSeqMessage.ERROR_MSG_PREFIX;
import static peoplesfeelingscode.com.pfseq.PFSeqMessage.MESSAGE_TYPE_ALERT;
import static peoplesfeelingscode.com.pfseq.PFSeqMessage.MESSAGE_TYPE_ERROR;
import static peoplesfeelingscode.com.pfseq.PFSeqTimeOffset.MODE_FRACTIONAL;
import static peoplesfeelingscode.com.samplemetronomerebuild.Storage.SHARED_PREF_RATE_KEY;

public abstract class ActivityBase extends PFSeqActivity {

    final static int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT = 3476;

    final static String FIRST_QUARTER_NOTE = "first quarter note";
    final static String FIRST_SIXTEENTH_NOTE = "first sixteenth note";
    final static String FIRST_EIGHTH_NOTE = "first eighth note";
    final static String THIRD_SIXTEENTH_NOTE = "third sixteenth note";
    final static String SECOND_QUARTER_NOTE = "second quarter note";
    final static String FIFTH_SIXTEENTH_NOTE = "fifth sixteenth note";
    final static String THIRD_EIGHTH_NOTE = "third eighth note";
    final static String SEVENTH_SIXTEENTH_NOTE = "seventh sixteenth note";
    final static String TRACK_NAME = "metronome";

    FragmentMainActivityWelcome welcomeDialog;
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    int permissionCheck;

    // audio sequencer stuff
    @Override
    public void onConnect() {
        if (!getSeq().isSetUp()) {
            getPermissionForWrite();
        }
    }
    @Override
    public void receiveMessage(PFSeqMessage pfSeqMessage) {
        String prefix = "";
        switch (pfSeqMessage.getType()) {
            case MESSAGE_TYPE_ALERT:
                prefix = ALERT_MSG_PREFIX;
                break;
            case MESSAGE_TYPE_ERROR:
                prefix = ERROR_MSG_PREFIX;
                break;
        }

        Log.d(LOG_TAG, "receiveMessage call - " + prefix + pfSeqMessage.getMessage());

        FragmentMainActivityProblem problemDialog = new FragmentMainActivityProblem();
        problemDialog.setArgs(prefix + pfSeqMessage.getMessage());
        problemDialog.show(getFragmentManager().beginTransaction(), "");
    }
    @Override
    public Class getServiceClass() {
        return mypfseq.class;
    }
    protected void setUpSequencerAndContent() {
        // app setup must have run first
        if (getLastAppVersionSetUp() > 0) {
            if (isBound()) {
                if (!getSeq().isSetUp()) {
                    Log.d(Dry.TAG, "setting up sequencer");
                    boolean success = configureSequecer(getSeq());
                    if (success) {
                        setUpTracks((mypfseq) getSeq());
                        setSeqRate(Storage.getSharedPrefInt(SHARED_PREF_RATE_KEY, getApplicationContext()));
                        onConnect();
                    }
                }
            } else {
                Log.d(Dry.TAG, "not bound in setUpSequencerAndContent");
//                receiveMessage(new PFSeqMessage(MESSAGE_TYPE_ERROR, "not bound in setUpSequencerAndContent"));
            }
        }
    }
    private boolean configureSequecer(PFSeq seq) {
        HashMap<String, Integer> myConfigInts = new HashMap<String, Integer>() {{
//            put(BUFFER_SIZE_BYTES, 200000);
//            put(CONTROL_THREAD_POLLING_MILLIS, 50);
//            put(MIN_MILLIS_AHEAD_TO_WRITE, 300);
            put(ONGOING_NOTIF_ID, 4345);
            put(TIME_SIG_UPPER, 2);
            put(TIME_SIG_LOWER, 4);
        }};

        PFSeqConfig config = new PFSeqConfig(myConfigInts, null, null, null);
        boolean seqSetupSuccess = seq.setUpSequencer(config);

        if (!seqSetupSuccess) {
            receiveMessage(new PFSeqMessage(MESSAGE_TYPE_ALERT, "failed to set Up Sequencer"));
            return false;
        }
        return true;
    }
    private void setUpTracks(mypfseq seq) {
        // assumes two over four time signature

        // set clip
        String fileName = Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, getApplicationContext());
        if (fileName.equals(Storage.DEFAULT_SHARED_PREF_STRING)) {
            fileName = Storage.DEFAULT_SELECTED_FILE_STRING;
        }
        File audFile = new File(Storage.path + File.separator + fileName);
        if (!audFile.exists()) {
            receiveMessage(new PFSeqMessage(MESSAGE_TYPE_ERROR, "file " + audFile.getName() + " doesn't exist. go to Samples screen"));
        }
        PFSeqClip clip = new PFSeqClip(seq, audFile);

        // create piano roll items
        PFSeqTimeOffset zeroQuarterNotesFromBarStart = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, -1, 1, 0, false, -1);
        PFSeqPianoRollItem zeroQuarterNote = new PFSeqPianoRollItem(getSeq(), clip, FIRST_QUARTER_NOTE, zeroQuarterNotesFromBarStart, null);
        PFSeqTimeOffset oneSixteenthNoteFromBarStart = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, -1, 4, 1, false, -1);
        PFSeqPianoRollItem oneSixteenthNote = new PFSeqPianoRollItem(getSeq(), clip, FIRST_SIXTEENTH_NOTE, oneSixteenthNoteFromBarStart, null);
        PFSeqTimeOffset oneEigthNoteFromBarStart = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, -1, 2, 1, false, -1);
        PFSeqPianoRollItem oneEighthNote = new PFSeqPianoRollItem(getSeq(), clip, FIRST_EIGHTH_NOTE, oneEigthNoteFromBarStart, null);
        PFSeqTimeOffset threeSixteenthNotesFromBarStart = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, -1, 4, 3, false, -1);
        PFSeqPianoRollItem threeSixteenthNotes = new PFSeqPianoRollItem(getSeq(), clip, THIRD_SIXTEENTH_NOTE, threeSixteenthNotesFromBarStart, null);
        PFSeqTimeOffset oneQuarterNoteFromBarStart = PFSeqTimeOffset.make(1, MODE_FRACTIONAL, -1, 1, 0, false, -1);
        PFSeqPianoRollItem oneQuarterNote = new PFSeqPianoRollItem(getSeq(), clip, SECOND_QUARTER_NOTE, oneQuarterNoteFromBarStart, null);
        PFSeqTimeOffset fiveSixteenthNotesFromBarStart = PFSeqTimeOffset.make(1, MODE_FRACTIONAL, -1, 4, 1, false, -1);
        PFSeqPianoRollItem fiveSixteenthNotes = new PFSeqPianoRollItem(getSeq(), clip, FIFTH_SIXTEENTH_NOTE, fiveSixteenthNotesFromBarStart, null);
        PFSeqTimeOffset threeEighthNotesFromBarStart = PFSeqTimeOffset.make(1, MODE_FRACTIONAL, -1, 2, 1, false, -1);
        PFSeqPianoRollItem threeEighthNotes = new PFSeqPianoRollItem(getSeq(), clip, THIRD_EIGHTH_NOTE, threeEighthNotesFromBarStart, null);
        PFSeqTimeOffset sevenSixteenthNotesFromBarStart = PFSeqTimeOffset.make(1, MODE_FRACTIONAL, -1, 4, 3, false, -1);
        PFSeqPianoRollItem sevenSixteenthNotes = new PFSeqPianoRollItem(getSeq(), clip, SEVENTH_SIXTEENTH_NOTE, sevenSixteenthNotesFromBarStart, null);

        // add pr items to track
        PFSeqTrack metronomeTrack = new PFSeqTrack(seq, TRACK_NAME);
        metronomeTrack.addPianoRollItem(zeroQuarterNote);
        metronomeTrack.addPianoRollItem(oneSixteenthNote);
        metronomeTrack.addPianoRollItem(oneEighthNote);
        metronomeTrack.addPianoRollItem(threeSixteenthNotes);
        metronomeTrack.addPianoRollItem(oneQuarterNote);
        metronomeTrack.addPianoRollItem(fiveSixteenthNotes);
        metronomeTrack.addPianoRollItem(threeEighthNotes);
        metronomeTrack.addPianoRollItem(sevenSixteenthNotes);

        // add track to seq
        seq.addTrack(metronomeTrack);
    }
    protected void setSeqRate(int spinnerPos) {
        // assumes two over four time signature

        if (isBound()) {
            PFSeqTrack track = getSeq().getTrackByName(TRACK_NAME);
            if (track == null) {
                Log.d(LOG_TAG, "setSegRate failed, track null");
                return;
            }

            switch(spinnerPos) {
                case 0:
                    Log.d(LOG_TAG, "setSegRate ticks per beat: 0.5");
                    // ticks per beat: 0.5
                    track.getPrItemByName(FIRST_QUARTER_NOTE).setEnabled(true);
                    track.getPrItemByName(FIRST_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(FIRST_EIGHTH_NOTE).setEnabled(false);
                    track.getPrItemByName(THIRD_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(SECOND_QUARTER_NOTE).setEnabled(false);
                    track.getPrItemByName(FIFTH_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(THIRD_EIGHTH_NOTE).setEnabled(false);
                    track.getPrItemByName(SEVENTH_SIXTEENTH_NOTE).setEnabled(false);
                    break;
                case 1:
                    Log.d(LOG_TAG, "setSegRate ticks per beat: 1");
                    // ticks per beat: 1
                    track.getPrItemByName(FIRST_QUARTER_NOTE).setEnabled(true);
                    track.getPrItemByName(FIRST_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(FIRST_EIGHTH_NOTE).setEnabled(false);
                    track.getPrItemByName(THIRD_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(SECOND_QUARTER_NOTE).setEnabled(true);
                    track.getPrItemByName(FIFTH_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(THIRD_EIGHTH_NOTE).setEnabled(false);
                    track.getPrItemByName(SEVENTH_SIXTEENTH_NOTE).setEnabled(false);
                    break;
                case 2:
                    Log.d(LOG_TAG, "setSegRate ticks per beat: 2");
                    // ticks per beat: 2
                    track.getPrItemByName(FIRST_QUARTER_NOTE).setEnabled(true);
                    track.getPrItemByName(FIRST_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(FIRST_EIGHTH_NOTE).setEnabled(true);
                    track.getPrItemByName(THIRD_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(SECOND_QUARTER_NOTE).setEnabled(true);
                    track.getPrItemByName(FIFTH_SIXTEENTH_NOTE).setEnabled(false);
                    track.getPrItemByName(THIRD_EIGHTH_NOTE).setEnabled(true);
                    track.getPrItemByName(SEVENTH_SIXTEENTH_NOTE).setEnabled(false);
                    break;
                case 3:
                    Log.d(LOG_TAG, "setSegRate ticks per beat: 4");
                    // ticks per beat: 4
                    track.getPrItemByName(FIRST_QUARTER_NOTE).setEnabled(true);
                    track.getPrItemByName(FIRST_SIXTEENTH_NOTE).setEnabled(true);
                    track.getPrItemByName(FIRST_EIGHTH_NOTE).setEnabled(true);
                    track.getPrItemByName(THIRD_SIXTEENTH_NOTE).setEnabled(true);
                    track.getPrItemByName(SECOND_QUARTER_NOTE).setEnabled(true);
                    track.getPrItemByName(FIFTH_SIXTEENTH_NOTE).setEnabled(true);
                    track.getPrItemByName(THIRD_EIGHTH_NOTE).setEnabled(true);
                    track.getPrItemByName(SEVENTH_SIXTEENTH_NOTE).setEnabled(true);
                    break;
            }
        }
    }

    // app setup stuff
    int getLastAppVersionSetUp() {
        boolean hasRunBefore = Storage.getSharedPrefBool(Storage.SHARED_PREF_VERSION_1_WAS_SET_UP_KEY, this);
        int lastVersionSetUp = Storage.getSharedPrefInt(Storage.SHARED_PREF_LAST_VERSION_SET_UP, this);

        if (lastVersionSetUp == Storage.DEFAULT_SHARED_PREF_INT) {
            if (hasRunBefore) {
                /* 8 is last before version-check was implemented */
                lastVersionSetUp = 8;
            } else {
                lastVersionSetUp = 0;
            }
        }

        return lastVersionSetUp;
    }
    void appVersionSetUp() {
        int lastVersionSetUp = getLastAppVersionSetUp();
        int currentVersion = BuildConfig.VERSION_CODE;
        Log.d(Dry.TAG, "checking version set up. last version set up: " + lastVersionSetUp + " current version: " + currentVersion);

        if (currentVersion > lastVersionSetUp) {
            switch (lastVersionSetUp) {
                case 0:
                    setDefaultSettings();
                    setUpDirectory();
                    welcomeDialog.show(getFragmentManager().beginTransaction(), "");
                default:
                    Storage.writeSamplePack(this, lastVersionSetUp);
                    Storage.setSharedPrefInt(currentVersion, Storage.SHARED_PREF_LAST_VERSION_SET_UP, this);
            }
        }
    }
    void setUpDirectory() {
        Storage.makeDirectoryIfNeeded();
        Storage.writeNoMediaFile(this);
    }
    void setDefaultSettings() {
        //set default settings
        Storage.setSharedPrefDouble(editor, Storage.bpmToFta(Storage.DEFAULT_BPM), Storage.SHARED_PREF_FTA_KEY, this);
        Storage.setSharedPrefInt(Storage.DEFAULT_RATE, Storage.SHARED_PREF_RATE_KEY, this);
        Storage.setSharedPrefString(Storage.DEFAULT_SELECTED_FILE_STRING, Storage.SHARED_PREF_SELECTED_FILE_KEY, this);

        Storage.setSharedPref(true, Storage.SHARED_PREF_VERSION_1_WAS_SET_UP_KEY, this);
    }

    // permission stuff
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    appVersionSetUp();
                    setUpSequencerAndContent();
                } else {
                    Toast toast = new Toast(this);
                    toast.setView(getLayoutInflater().inflate(R.layout.toast, null));
                    toast.setDuration(Toast.LENGTH_LONG);
                    TextView text = (TextView) toast.getView().findViewById(R.id.txt);
                    text.setText(getResources().getString(R.string.toastWritePermissionNotGranted));
                    toast.show();

                    finish();
                }
                return;
            }
        }
    }
    void getPermissionForWrite() {
        Log.d(Dry.TAG, "checking for write permission");
        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.d(Dry.TAG, "write permission not yet granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT);
        } else {
            appVersionSetUp();
            setUpSequencerAndContent();
        }
    }

    // activity overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        welcomeDialog = new FragmentMainActivityWelcome();
        sharedPrefs = getSharedPreferences(Storage.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
        editor = sharedPrefs.edit();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_item:
                startActivity(new Intent(ActivityBase.this, ActivityAbout.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
