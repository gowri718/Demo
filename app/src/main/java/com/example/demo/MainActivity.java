package com.example.demo;

import static android.nfc.NdefRecord.TNF_WELL_KNOWN;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    public static final String Error_Detected = "No NFC tag detected";
    public static final String Write_Success = "Text written Successfully";
    public static final String Write_Error = "Error during writing, Try Again";

    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;
    IntentFilter[] writingTagFilter;
    boolean writeMode;
    Tag myTag;
    Context context;
    TextView edit_message;
    TextView nfc_contents;
    Button ActivateButton;

    @SuppressLint({"MissingInflatedId", "UnspecifiedImmutableFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edit_message = (TextView) findViewById(R.id.edit_message);
        nfc_contents = (TextView) findViewById(R.id.nfc_contents);
        ActivateButton = findViewById(R.id.ActivateButton);
        context = this;
        ActivateButton.setOnClickListener(view -> {
            try {
                if(myTag == null){
                    Toast.makeText(context, Error_Detected,Toast.LENGTH_LONG).show();
                }
                else{
                    write("PlainText"+ edit_message.getText().toString(), myTag);
                    Toast.makeText(context,Write_Success,Toast.LENGTH_LONG).show();
                }

            }catch (Exception e){
                Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if(mAdapter == null){
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
            finish();
        }
        readfromIntent(getIntent());

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_MUTABLE);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilter = new IntentFilter[] { tagDetected };
    }

    private void readfromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ){
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }


    @SuppressLint("SetTextI18n")
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
        // String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; //Get the Text Encoding
        int languageCodeLength = payload[0] & 51; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength -1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding",e.toString());
        }
        nfc_contents.setText("NFC Content: " + text);
    }
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }
    private NdefRecord createRecord(String text) {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes(StandardCharsets.US_ASCII);
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        // Set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // Copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        return new NdefRecord(TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readfromIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn() {
        writeMode = true;
        mAdapter.enableForegroundDispatch(this, mPendingIntent, writingTagFilter, null);
    }

    private void WriteModeOff() {
        writeMode = false;
        mAdapter.disableForegroundDispatch(this);
    }


}
