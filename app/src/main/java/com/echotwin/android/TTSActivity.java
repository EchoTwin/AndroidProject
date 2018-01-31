package com.echotwin.android;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class TTSActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, MediaPlayer.OnPreparedListener {

    private ArrayList<User> voicesList;
    private UserAdapter adapter;
    private String sampleVoiceFilePath = "";
    private MediaPlayer mMediaplayer;
    private AppCompatSpinner voicesSpinner;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            signInAnonymously();
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseRef = database.getReference();

        voicesList = new ArrayList<>();

        voicesSpinner = findViewById(R.id.voices_spinner);
        ImageButton playSampleVoice = findViewById(R.id.play_sample_voice);
        TextInputEditText mEditText = findViewById(R.id.text);
        FloatingActionButton floatingActionButton = findViewById(R.id.fab);

        adapter = new UserAdapter(this, voicesList);
        voicesSpinner.setAdapter(adapter);

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                collectUsers(dataSnapshot);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TTSActivity.this.getLocalClassName(), "Failed to read value.", error.toException());
            }
        });

        floatingActionButton.setOnClickListener(this);
        playSampleVoice.setOnClickListener(this);
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                // do your stuff
            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e("TAG", "signInAnonymously:FAILURE", exception);
            }
        });
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    private void collectUsers(DataSnapshot dataSnapshot) {

        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            Object userName = ds.child("userName").getValue();
            Object userAvatar = ds.child("pictureFileUrl").getValue();
            Object userVoice = ds.child("voiceFileUrl").getValue();

            User uInfo = new User();
            uInfo.setId(ds.getKey());
            if (userName != null && !Objects.equals(userName.toString(), "")) {
                uInfo.setUserName(userName.toString());
            } else {
                uInfo.setUserName("No Username");
            }
            if (userAvatar != null && !Objects.equals(userAvatar.toString(), "")) {
                uInfo.setUserAvatar(userAvatar.toString());
            } else {
                uInfo.setUserAvatar("No Avatar");
            }
            if (userVoice != null && !Objects.equals(userVoice.toString(), "")) {
                uInfo.setUserVoice(userVoice.toString());
            } else {
                uInfo.setUserVoice("No Voice");
            }

            voicesList.add(uInfo);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        User c = adapter.getItem(position);
        sampleVoiceFilePath = c.getUserVoice();
        Toast.makeText(this, "You've selected " + c.getUserName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                Intent intent = new Intent(TTSActivity.this, RecordActivity.class);
                startActivity(intent);
                break;
            case R.id.play_sample_voice:
                sampleVoiceFilePath = adapter.getItem(voicesSpinner.getSelectedItemPosition()).getUserVoice();
                Log.d("kot", sampleVoiceFilePath);
                playSampleVoice(sampleVoiceFilePath);
                break;
            default:
                break;
        }
    }

    private void playSampleVoice(String sampleVoiceFilePath) {
        if (mMediaplayer != null && mMediaplayer.isPlaying()) {
            mMediaplayer.stop();
            mMediaplayer.reset();
            mMediaplayer.release();
            mMediaplayer = null;
        }
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://echotwin-a8d76.appspot.com");
        final StorageReference pathReference = storageRef.child("Voices/" + sampleVoiceFilePath);
        // Create a storage reference from our app
        Log.d("pathreference", pathReference.toString());
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                try {
                    // Download url of file
                    final String url = uri.toString();
                    mMediaplayer = new MediaPlayer();
                    mMediaplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaplayer.setDataSource(url);
                    // wait for media player to get prepare
                    mMediaplayer.setOnPreparedListener(TTSActivity.this);
                    mMediaplayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("TAG", e.getMessage());
            }
        });
    }
}
