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

public class EditItemDialog extends AppCompatDialogFragment {
    private EditText editTextIP;
    private EditText editTextPort;
    private EditItemDialog.DialogListener listener;
    private View view;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.layout_dialog, null);

        builder.setView(view).setTitle("Edit connection")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(view.getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String IP = editTextIP.getText().toString();
                        String Port = editTextPort.getText().toString();
                        listener.editItemWithDialogValues(IP, Port);
                    }
                });
        editTextIP = view.findViewById(R.id.edit_IP);
        editTextPort = view.findViewById(R.id.edit_portNumber);
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (EditItemDialog.DialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement DialogListener");
        }
    }

    public interface DialogListener {
        void editItemWithDialogValues(String IP, String Port);
    }
}
