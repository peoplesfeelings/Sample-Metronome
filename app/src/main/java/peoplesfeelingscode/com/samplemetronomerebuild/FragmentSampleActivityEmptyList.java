package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class FragmentSampleActivityEmptyList extends DialogFragment {

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(R.string.dialogInstruction);
        builder.setTitle(R.string.dialogNoFiles);

        builder.setPositiveButton(R.string.btnOk, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
                getActivity().finish();
            }
        });

        return builder.create();
    }
}
