package com.example.iotvoiceassistant;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tapadoo.alerter.Alerter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity implements NewItemDialog.DialogListener, EditItemDialog.DialogListener, FileNameGrabberDialog.DialogListener {

    private static final int SPEECH_REQUEST_CODE = 0;
    public Dialog itemDialogBox;
    public Animation rotateAnim;
    // MAIN CONTROL VARIABLES
    private ArrayList<Item> ITEMLIST; // may want to prevent duplicates at some point by checking this list
    private int last_clicked_position = -1; // index of item currently clicked
    private Item CURRENT_ITEM;
    private static int count = 0;
    // MAIN UI
    private RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private TcpClient mTcpClient;
    // MISC UI RELATED
    private FloatingActionButton addButton;
    private boolean fileDone = false;
    /********************************** INIT *********************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        setContentView(R.layout.activity_main);

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
    /* METHOD CALLED WHEN 'Save' BUTTON CLICKED*/
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
    /********************************** ITEM STORING AND EDITING *********************************/
    /* DIALOG BOX FOR CREATING NEW ITEM */
    public void openNewItemDialog() {

        NewItemDialog dialog = new NewItemDialog();
        dialog.show(getSupportFragmentManager(), "dialog_box_new");
    }

    /* DIALOG BOX FOR EDITING OLD ITEM */
    public void openEditItemDialog() {

        EditItemDialog dialog = new EditItemDialog();
        dialog.show(getSupportFragmentManager(), "dialog_box_edit");
    }

    /* DIALOG BOX FOR GETTING FILE NAME FROM USER */
    public void openFileNameGrabberDialog(InputStream istream) {

        FileNameGrabberDialog dialog = new FileNameGrabberDialog(istream);
        dialog.show(getSupportFragmentManager(), "dialog_box_file");

    }

    /* NAME A FILE TO SAVE */
    @Override
    public void getFileName(String filename, InputStream istream) {
        final String f = filename;
        final InputStream is = istream;

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                writeFIleToStorage(f, is);
                fileDone = true;
            }
        });
        t.start();


    }

    /* METHOD CALLED ON '+' BUTTON PRESS TO ADD A NEW ITEM*/
    public void insertItem() {
        // open the prompt to give the new item its IP and PORT values
        openNewItemDialog();
    }

    /* REMOVE THE ITEM AT POSITION position AND UPDATE THE VIEW*/
    public void removeItem(int position) {
        ITEMLIST.remove(position);
        mAdapter.notifyItemRemoved(position);

    }
    /* METHOD CALLED ON ITEM CLICK */
    public void deviceItemClicked(int position) {
        // called from the adapter onClickListener in 'buildRecyclerView()'
        CURRENT_ITEM = ITEMLIST.get(position); // @SET_CURRENT_ITEM
        last_clicked_position = position; // store the index of the item that was selected
        ShowItemPopup();

    }
    /* INSERT A NEW ITEM */
    @Override
    public void createNewItemWithDialogValues(String IP, String Port) {

        Item newDevice = new Item(R.drawable.ic_desktop, IP, Port);
        Toast.makeText(MainActivity.this, "New Device Added.", Toast.LENGTH_SHORT).show();
        ITEMLIST.add(newDevice); // adds item to end of list
        mAdapter.notifyItemInserted(ITEMLIST.size() - 1);

    }
    /* EDIT AN OLD ITEM */
    @Override
    public void editItemWithDialogValues(String IP, String Port) {

        Toast.makeText(getApplicationContext(), "Changing item with index " + last_clicked_position, Toast.LENGTH_SHORT).show();
        if (last_clicked_position != -1) {
            changeItemText(last_clicked_position, IP, Port);
            changeItemData(last_clicked_position, IP, Port);
        } else {
            Toast.makeText(getApplicationContext(), "error trying to edit item, index is undefined", Toast.LENGTH_SHORT).show();
        }
    }

    /* METHOD CALLED TO DISPLAY THE MENU WHEN AN ITEM IS CLICKED */
    public void ShowItemPopup() {

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
                openEditItemDialog();
            }
        });
        Connect = itemDialogBox.findViewById(R.id.connect_button);
        Connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ConnectTask().execute(""); // START CONNECTION HERE
                voiceButtonStart();
            }
        });

        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemDialogBox.dismiss();
                CURRENT_ITEM = null; // forget the last item saved since the prompt is closed
                last_clicked_position = -1; //@SET_CURRENT_ITEM
            }
        });
        Objects.requireNonNull(itemDialogBox.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        itemDialogBox.show();
    }
    /**** Utility ****/
    /* UTILITY FUNCTION TO EDIT AND UPDATE TEXT OF AN ITEM*/
    public void changeItemText(int position, String t1, String t2) {
        // position is the item index in the list ITEMLIST
        ITEMLIST.get(position).changeText1(t1);
        ITEMLIST.get(position).changeText2(t2);
        mAdapter.notifyItemChanged(position);
    }
    /* UTILITY FUNCTION TO EDIT AND UPDATE IP AND PORT OF AN ITEM*/
    public void changeItemData(int position, String t1, String t2){
        ITEMLIST.get(position).changeIP(t1);
        ITEMLIST.get(position).changePort(t2);
        // don't need to notify adapter since this is internal data
    }
    /* UTILITY FUNCTION TO DISPLAY ALERT AT THE TOP OF THE SCREEN*/
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
    /********************************** VOICE AND CONNECTIONS *********************************/
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

    /* METHOD CALLED TO SHARE A STRING AS A TEXT FILE*/
    private void shareTextAsFile(String string) {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, string);
        startActivity(Intent.createChooser(shareIntent, "Share response text file..."));
    }


    public void writeFIleToStorage(String filename, InputStream is) {
        File theDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "received_files");
        if (!theDir.exists()) {
            theDir.mkdirs();
        }
        File NewFile = new File(theDir, filename);
        try (OutputStream fos = new FileOutputStream(NewFile)) {

            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] aByte = new byte[1024];

            int bytesRead;
            while ((bytesRead = is.read(aByte)) != -1) {
                System.out.println(bytesRead + " bytes read");
                bos.write(aByte);
            }
            bos.flush();
            bos.close();

        } catch (FileNotFoundException e) {
            System.out.println("F N F");

        } catch (IOException e) {
            System.out.println("IO Exception");

        }


    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {
        //
        @Override
        protected TcpClient doInBackground(String... message) {
            // ONLY CREATE THIS IF AN ITEM HAS BEEN CLICKED AND VALUES ARE STORED PROPERLY
            // MIGHT WANT THIS TO OCCUR IN NEW ACTIVITY
            // IF THIS RUNS IN BACKGROUND, WHAT DO WE DISPLAY?
            if (CURRENT_ITEM == null) {
                showAlerter("Error", "No item selected");
                return null;
            }
            try{Integer.parseInt(CURRENT_ITEM.getPort());}
            catch (NumberFormatException e){
                showAlerter("Port Error", "Please Enter a Valid Numerical Port");
            }

            //Create TCP Client
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(InputStream istream) {
                    //this method calls the onProgressUpdate
                    //publishProgress(message);

                    byte[] b = new byte[1];
                    try {
                        int bytes_read = istream.read(b);
                        if (bytes_read != 1) {
                            // error
                            showAlerter("Error: ", "couldn't parse response");
                        } else {
                            // @here
                            String str = new String(b);
                            char c = str.charAt(0);
                            switch (c) {
                                case '0':
                                    // invalid command
                                    showAlerter("Response: ", "invalid command");
                                    mTcpClient.stopClient();
                                    break;
                                case '1':
                                    showAlerter("Response: ", "command successfully executed");
                                    mTcpClient.stopClient();
                                    break;
                                case '2':
                                    // file received
                                    showAlerter("Response: ", "file received");
                                    openFileNameGrabberDialog(istream);

                                    while (!fileDone) {
                                        //wait
                                        try {
                                            sleep(1000);
                                        } catch (InterruptedException e) {
                                            showAlerter("Error", "interrupted");
                                        }
                                    }
                                    mTcpClient.stopClient();
                                    fileDone = false;
                                    // tcp client closed when file written
                                    break;
                                case '3':
                                    // ping test
                                    writeFIleToStorage("results" + count + ".txt", istream);
                                    count++;
                                    mTcpClient.stopClient();
                                    break;
                                case '4':
                                    // file not found
                                    showAlerter("Response: ", "file not found");
                                    mTcpClient.stopClient();
                                    break;
                                case '5':
                                    // get clipboard
                                    showAlerter("Response: ", "clipboard received (in file)");
                                    writeFIleToStorage("results" + count + ".txt", istream);
                                    mTcpClient.stopClient();
                                    break;
                                default:
                                    // err
                                    showAlerter("Message Received Error", "Code = " + str);
                                    mTcpClient.stopClient();
                                    break;
                            }
                        }
                    } catch (IOException e) {
                        showAlerter("Error", "IO Exception");
                    }
                }
            }, CURRENT_ITEM.getIP(), Integer.parseInt(CURRENT_ITEM.getPort()));
            mTcpClient.run();
            return null;
        }

        /* unused, called when 'publishprogress' is called */
        @Override
        protected void onProgressUpdate(String... values) {

            super.onProgressUpdate(values);
            if (values.length == 0) {
                // empty response ? or values[0].length? @TODO
                Toast.makeText(getApplicationContext(), "Empty Response", Toast.LENGTH_SHORT).show();
                return;
            }
            //response received from server
            Log.d("test", "response " + values[0]);
            showAlerter("RESPONSE ", values[0]); // maybe add failure and success for title?
            shareTextAsFile(values[0]);
        }

    }

}