package com.example.iotvoiceassistant;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tapadoo.alerter.Alerter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NewItemDialog.DialogListener {

    private static final int SPEECH_REQUEST_CODE = 0;
    public Animation rotateAnim; // button animation
    public Dialog itemDialogBox;
    private String FILE_NAME; // TO BE USED w/ DOWNLOADING TEXT FILE
    // MISC UI RELATED
    private FloatingActionButton addButton;
    // LIST OF ALL DEVICES
    private ArrayList<Item> ITEMLIST; // may want to prevent duplicates at some point by checking this list
    // MAIN UI
    private RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private TcpClient mTcpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // INIT
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar); // create toolbar
        setSupportActionBar(toolbar);
        loadData();  // loads Devices list from memory
        buildRecyclerView();

        // BACKGROUND ANIMATION
        RelativeLayout relativeLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) relativeLayout.getBackground();
        animationDrawable.setEnterFadeDuration(3500);
        animationDrawable.setExitFadeDuration(3500);
        animationDrawable.start();

        // BUTTONS and Dialog
        addButton = findViewById(R.id.addButton);   // <-------- ADD NEW ITEM BUTTON
        rotateAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate_anim);
        addButton.setOnClickListener(new View.OnClickListener() { // <------- ADD BUTTON's ON CLICK
            @Override
            public void onClick(View v) {
                addButton.startAnimation(rotateAnim);
                insertItem();
            }
        });

        // VOICE
        new ConnectTask().execute("");

    }

    /*

     * ITEM RELATED

     */
    public void openNewItemDialog() {
        // DIALOG BOX
        NewItemDialog dialog = new NewItemDialog();
        dialog.show(getSupportFragmentManager(), "dialog_box_1");
    }

    public void insertItem() {
        // method called when the '+' button is pressed, attempts to add item to list

        //Toast.makeText(MainActivity.this, "message when + clicked", Toast.LENGTH_SHORT).show();
        openNewItemDialog();
    }

    public void removeItem(int position) {
        ITEMLIST.remove(position);
        mAdapter.notifyItemRemoved(position);

    }

    public void changeText(int position, String t1, String t2) {
        // position is the item index
        ITEMLIST.get(position).changeText1(t1);
        ITEMLIST.get(position).changeText2(t2);
        mAdapter.notifyItemChanged(position);
    }

    public void deviceItemClicked(int position) {
        // changes TEXT of an item that is clicked // TODO change / add edit functionality ^1
        // called from the adapter onClickListener in 'buildRecyclerView()'
        if (ITEMLIST.get(position).getText1().equals("Clicked")) { // new text to old
            changeText(position, ITEMLIST.get(position).getIP(), ITEMLIST.get(position).getPort());
        } else {
            changeText(position, "Clicked", "tap to undo"); // old text to new
        }
        ShowItemPopup();

    }

    @Override
    public void applyTexts(String IP, String Port) {
        // called when inserting a new item
        Item newDevice = new Item(R.drawable.ic_desktop, IP, Port);
        Toast.makeText(MainActivity.this, "New Device Added.", Toast.LENGTH_SHORT).show();
        ITEMLIST.add(newDevice); // adds item to end of list
        mAdapter.notifyItemInserted(ITEMLIST.size() - 1);

    }

    /* SAVE ITEM LIST TO SHARED PREFERENCES */
    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(ITEMLIST);
        editor.putString("DEVICES", json);
        editor.apply();
    }

    /* LOAD ITEM LIST FROM SHARED PREFERENCES */
    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("DEVICES", null);
        Type type = new TypeToken<ArrayList<Item>>() {
        }.getType();
        ITEMLIST = gson.fromJson(json, type);


        if (ITEMLIST == null) {
            ITEMLIST = new ArrayList<>();
        } else {
            for (Item i : ITEMLIST) i.setImResource(R.drawable.ic_desktop); // fix for icon bug

        }
    }

    /* BUILD LIST AND VIEW */
    public void buildRecyclerView() {
        // builds the list of items
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new Adapter(ITEMLIST);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        // On Click Listener for items in the List
        mAdapter.setOnItemClickListener(new Adapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                deviceItemClicked(position);
            }

            @Override
            public void onDeleteClick(int position) {
                removeItem(position);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.save) {

            saveData();
            Toast.makeText(this, "Devices List Saved", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /* POP UP THAT SHOWS UP WHEN AN ITEM IS SELECTED */
    public void ShowItemPopup() {
        // @TODO add edit and connect functionality, add text fields, clean up later
        itemDialogBox = new Dialog(this);
        TextView txtclose;
        ImageButton Edit_Fields;
        ImageButton Connect;
        itemDialogBox.setContentView(R.layout.item_popup);
        txtclose = itemDialogBox.findViewById(R.id.txtclose);
        Edit_Fields = itemDialogBox.findViewById(R.id.edit_button);
        Edit_Fields.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Edit Fields on click listener
                // call a new dialog to edit text, same dialog box?

                showAlerter("Edit clicked", "test");
                // testing share text file feature // shareText("Clicked The Edit Button!");
            }
        });
        Connect = itemDialogBox.findViewById(R.id.connect_button);
        Connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceButtonStart();
                //Toast.makeText(getApplicationContext(), "Connect Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemDialogBox.dismiss();
            }
        });
        Objects.requireNonNull(itemDialogBox.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        itemDialogBox.show();
    }

    /* VOICE RELATED */
    public void voiceButtonStart() {
        // Called to initialize voice recognition
        displaySpeechRecognizer();
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (results == null) {
                Toast.makeText(getApplicationContext(), "No command detected", Toast.LENGTH_SHORT).show();
                return;
            }
            String spokenText = results.get(0);
            // Do something with spokenText
            if (mTcpClient != null) {
                mTcpClient.sendMessage(spokenText);
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Share a text file containing 'string'
    // Can copy to clipboard
    private void shareText(String string) {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, string);
        startActivity(Intent.createChooser(shareIntent, "Share response text file..."));
    }

    public void showAlerter(String title, String msg) {
        Alerter.create(this)
                .setTitle(title) // can add custom fonts and buttons
                .setText(msg)
                .setDuration(5000)
                .setIcon(R.drawable.ic_chat)
                .enableIconPulse(true)
                .setBackgroundColorRes(R.color.navy)
                .enableSwipeToDismiss()
                .enableVibration(true)
                .enableProgress(true)
                .setProgressColorRes(R.color.colorAccent)
                .show();
    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... message) {

            //we create a TCPClient object
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            mTcpClient.run();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if (values.length == 0) {
                // empty response
                Toast.makeText(getApplicationContext(), "Empty Response", Toast.LENGTH_SHORT).show();
                return;
            }
            //response received from server
            Log.d("test", "response " + values[0]);
            showAlerter("RESPONSE ", values[0]); // maybe add failure and success for title?
            //TODO process server response here....

            // EXAMPLE: WRITE RESPONSE STRING TO FILE 'test.txt'
            shareText(values[0]);

        }

    }

}