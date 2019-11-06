

package peoplesfeelingscode.com.samplemetronomerebuild;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import static peoplesfeelingscode.com.pfseq.PFSeqConfig.TIME_SIG_UPPER;
import static peoplesfeelingscode.com.pfseq.PFSeqMessage.ALERT_MSG_PREFIX;
import static peoplesfeelingscode.com.pfseq.PFSeqMessage.ERROR_MSG_PREFIX;
import static peoplesfeelingscode.com.pfseq.PFSeqMessage.MESSAGE_TYPE_ALERT;
import static peoplesfeelingscode.com.pfseq.PFSeqMessage.MESSAGE_TYPE_ERROR;

public class ActivityBase extends PFSeqActivity {

    @Override
    public void onConnect() {
        if (!getService().isSetUp()) {
            boolean success = configureSequecer(getService());
            if (success) {
                setUpTracks(getService());
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
            put(TIME_SIG_UPPER, 1);
        }};

        PFSeqConfig config = new PFSeqConfig(myConfigInts, null, null, null);
        boolean seqSetupSuccess = seq.setUpSequencer(config);

        if (!seqSetupSuccess) {
            receiveMessage(new PFSeqMessage(MESSAGE_TYPE_ALERT, "failed to set Up Sequencer"));
            return false;
        }
        return true;
    }

    private boolean setUpTracks(PFSeq seq) {
        File audFile;
        try {
            audFile = File.createTempFile("test_file", "");
            InputStream ins = getResources().openRawResource(R.raw.guitar_hit_5);
            OutputStream out = new FileOutputStream(audFile);

            byte[] buffer = new byte[1024];
            int read;
            while((read = ins.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            receiveMessage(new PFSeqMessage(MESSAGE_TYPE_ERROR, "error creating file object \n" + e.getStackTrace().toString()));
            return false;
        }
        PFSeqTrack metronomeTrack = new PFSeqTrack(seq, "metronome");
        PFSeqClip clip = new PFSeqClip(seq, audFile);
        PFSeqTimeOffset timeOffset = PFSeqTimeOffset.make(0, PFSeqTimeOffset.MODE_FRACTIONAL, 0, 1, 0, false, 0);
        PFSeqPianoRollItem item = new PFSeqPianoRollItem(seq, clip, "the tick", timeOffset);
        metronomeTrack.addPianoRollItem(item);

        seq.addTrack(metronomeTrack);

        return true;
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
