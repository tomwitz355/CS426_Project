package com.example.iotvoiceassistant;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.io.InputStream;

public class FileNameGrabberDialog extends AppCompatDialogFragment {
    private EditText editTextFileName;
    private FileNameGrabberDialog.DialogListener listener;
    private View view;
    public static int count = 0;
    public InputStream istream;

    public FileNameGrabberDialog(InputStream is) {
        super();
        istream = is;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.new_file_dialog, null);

        builder.setView(view).setTitle("Save File As...")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(view.getContext(), "Using default filename...", Toast.LENGTH_SHORT).show();
                        ++count;
                        listener.getFileName("file" + count + ".txt", istream);
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String Filename = editTextFileName.getText().toString();
                        listener.getFileName(Filename, istream);
                    }
                });
        editTextFileName = view.findViewById(R.id.filename);
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (FileNameGrabberDialog.DialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement DialogListener");
        }
    }

    public interface DialogListener {
        void getFileName(String filename, InputStream is);
    }
}
