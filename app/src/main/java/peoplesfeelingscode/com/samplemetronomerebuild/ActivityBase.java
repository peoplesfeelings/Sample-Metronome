

package peoplesfeelingscode.com.samplemetronomerebuild;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;
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

public class ActivityBase extends PFSeqActivity {

    @Override
    public void onConnect() {
        if (!getService().isSetUp()) {
            boolean success = configureSequecer(getService());
            if (success) {
                setUpTracks((mypfseq) getService());
            }
        }
    }

    @Override
    public void receiveMessage(PFSeqMessage pfSeqMessage) {
        String prefix = "";

        switch (pfSeqMessage.getType()) {
            case MESSAGE_TYPE_ALERT: prefix = ALERT_MSG_PREFIX;
                break;
            case MESSAGE_TYPE_ERROR: prefix = ERROR_MSG_PREFIX;
                break;
        }

        Log.d(LOG_TAG, "receiveMessage call - " + prefix + pfSeqMessage.getMessage());
    }

    @Override
    public Class getServiceClass() {
        return mypfseq.class;
    }

    private boolean configureSequecer(PFSeq seq) {
        HashMap<String, Integer> myConfigInts = new HashMap<String, Integer>() {{
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

    private boolean setUpTracks(mypfseq seq) {
        //clip stuff
        String fileName = Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, getApplicationContext());
        File audFile = new File(Storage.path + File.separator + fileName);
        if (!audFile.exists()) {
            receiveMessage(new PFSeqMessage(MESSAGE_TYPE_ERROR, "file doesn't exist \n"));
            return false;
        }
        seq.setTheClip(new PFSeqClip(seq, audFile));

        // rate stuff
        int rateSpinnerPos = Storage.getSharedPrefInt(SHARED_PREF_RATE_KEY, getApplicationContext());

        // track stuff
        PFSeqTrack metronomeTrack = new PFSeqTrack(seq, "metronome");
        PFSeqTimeOffset timeOffset = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, 0, 1, 0, false, 0);
        PFSeqPianoRollItem item = new PFSeqPianoRollItem(seq, seq.getTheClip(), "the tick", timeOffset);
        metronomeTrack.addPianoRollItem(item);

        seq.addTrack(metronomeTrack);

        return true;
    }
    protected void setSeqRate(ArrayList<PFSeqPianoRollItem> pianoroll, int spinnerPos) {
        // assumes two over four time signature

        int rollCount = pianoroll.size();
        if (rollCount > 0) {
            pianoroll.clear();
        }
        PFSeqClip clip = ((mypfseq) getService()).getTheClip();


        PFSeqTimeOffset zeroQuarterNotesFromBarStart = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, -1, 1, 0, false, -1);
        PFSeqPianoRollItem zeroQuarterNote = new PFSeqPianoRollItem(getService(), clip, "first quarter", zeroQuarterNotesFromBarStart);

        PFSeqTimeOffset oneSixteenthNoteFromBarStart = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, -1, 4, 1, false, -1);
        PFSeqPianoRollItem oneSixteenthNote = new PFSeqPianoRollItem(getService(), clip, "first sixteenth", oneSixteenthNoteFromBarStart);

        PFSeqTimeOffset oneEigthNoteFromBarStart = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, -1, 2, 1, false, -1);
        PFSeqPianoRollItem oneEighthNote = new PFSeqPianoRollItem(getService(), clip, "first eighth", oneEigthNoteFromBarStart);

        PFSeqTimeOffset threeSixteenthNotesFromBarStart = PFSeqTimeOffset.make(0, MODE_FRACTIONAL, -1, 4, 3, false, -1);
        PFSeqPianoRollItem threeSixteenthNotes = new PFSeqPianoRollItem(getService(), clip, "first sixteenth", threeSixteenthNotesFromBarStart);

        PFSeqTimeOffset oneQuarterNoteFromBarStart = PFSeqTimeOffset.make(1, MODE_FRACTIONAL, -1, 1, 0, false, -1);
        PFSeqPianoRollItem oneQuarterNote = new PFSeqPianoRollItem(getService(), clip, "second quarter", oneQuarterNoteFromBarStart);

        PFSeqTimeOffset fiveSixteenthNotesFromBarStart = PFSeqTimeOffset.make(1, MODE_FRACTIONAL, -1, 4, 1, false, -1);
        PFSeqPianoRollItem fiveSixteenthNotes = new PFSeqPianoRollItem(getService(), clip, "first sixteenth", fiveSixteenthNotesFromBarStart);

        PFSeqTimeOffset threeEighthNotesFromBarStart = PFSeqTimeOffset.make(1, MODE_FRACTIONAL, -1, 2, 1, false, -1);
        PFSeqPianoRollItem threeEighthNotes = new PFSeqPianoRollItem(getService(), clip, "second eighth", threeEighthNotesFromBarStart);

        PFSeqTimeOffset sevenSixteenthNotesFromBarStart = PFSeqTimeOffset.make(1, MODE_FRACTIONAL, -1, 4, 3, false, -1);
        PFSeqPianoRollItem sevenSixteenthNotes = new PFSeqPianoRollItem(getService(), clip, "first sixteenth", sevenSixteenthNotesFromBarStart);

        switch(spinnerPos) {
            case 0:
                // ticks per beat: 0.5
                pianoroll.add(zeroQuarterNote);
                break;
            case 1:
                // ticks per beat: 1
                pianoroll.add(zeroQuarterNote);
                pianoroll.add(oneQuarterNote);
                break;
            case 2:
                // ticks per beat: 2
                pianoroll.add(zeroQuarterNote);
                pianoroll.add(oneEighthNote);
                pianoroll.add(oneQuarterNote);
                pianoroll.add(threeEighthNotes);
                break;
            case 3:
                // ticks per beat: 4
                pianoroll.add(zeroQuarterNote);
                pianoroll.add(oneSixteenthNote);
                pianoroll.add(oneEighthNote);
                pianoroll.add(threeSixteenthNotes);
                pianoroll.add(oneQuarterNote);
                pianoroll.add(fiveSixteenthNotes);
                pianoroll.add(threeEighthNotes);
                pianoroll.add(sevenSixteenthNotes);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
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
