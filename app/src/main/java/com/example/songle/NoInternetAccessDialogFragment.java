package com.example.songle;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by Paul Moore on 10-Oct-17.
 */
// adapted from https://developer.android.com/guide/topics/ui/dialogs.html
public class NoInternetAccessDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.no_internet_access)
                // TODO give option to switch on data/wifi
                // press okay to close the app.
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //closes app
                        System.exit(0);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}

