package com.echotwin.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

public class TTSActivity extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<String> voicesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();
        voicesList = new ArrayList<>();

        AppCompatSpinner voicesSpinner = findViewById(R.id.voices_spinner);
        TextInputEditText mEditText = findViewById(R.id.text);
        FloatingActionButton floatingActionButton = findViewById(R.id.fab);

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, voicesList);
        voicesSpinner.setAdapter(adapter);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                voicesList.addAll(collectUsers((Map<String, Object>) dataSnapshot.getValue()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TTSActivity.this.getLocalClassName(), "Failed to read value.", error.toException());
            }
        });

        floatingActionButton.setOnClickListener(this);
    }

    private ArrayList<String> collectUsers(Map<String, Object> users) {
        ArrayList<String> usersSet = new ArrayList<>();
        //iterate through each user, ignoring their UID
        for (Map.Entry<String, Object> entry : users.entrySet()) {
            //Get user map
            Map singleUser = (Map) entry.getValue();
            //Get phone field and append to list
            if (singleUser.get("Username") != null && singleUser.get("Username") != "") {
                usersSet.add((String) singleUser.get("Username"));
            } else {
                usersSet.add("No username");
            }
        }

        return usersSet;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                Intent intent = new Intent(TTSActivity.this, RecordActivity.class);
                startActivity(intent);
            default:
                break;
        }
    }
}
