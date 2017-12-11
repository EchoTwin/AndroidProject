package com.echotwin.android;

import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

/**
 * Created by kristo.prifti on 2017/11/26.
 */

public class RecordFragment extends Fragment implements View.OnClickListener, MediaPlayer.OnCompletionListener {

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;

    private Chronometer chronometer;
    private EditText usernameEditText;
    private ImageView imageViewRecord, imageViewStop, imageViewPlay;
    private int RECORD_AUDIO_REQUEST_CODE = 123;
    private String filePath;
    private Button mSendData;
    private Firebase mRef;
    private StorageReference mStorageRef;
    private String fileName;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio();
        }

        chronometer = view.findViewById(R.id.chronometerTimer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        imageViewRecord = view.findViewById(R.id.imageViewRecord);
        imageViewStop = view.findViewById(R.id.imageViewStop);
        imageViewPlay = view.findViewById(R.id.imagePlayRecord);
        usernameEditText = (EditText) view.findViewById(R.id.username_edit_text);
        mSendData = (Button) view.findViewById(R.id.send_data);

        imageViewRecord.setOnClickListener(this);
        imageViewStop.setOnClickListener(this);
        imageViewPlay.setOnClickListener(this);

        Firebase.setAndroidContext(getActivity());
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mRef = new Firebase("https://echotwin-a8d76.firebaseio.com/");

        mSendData.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Firebase userId = mRef.child("12345");
                Firebase username = userId.child("Username");
                username.setValue(usernameEditText.getText().toString());
                uploadFile();
                Firebase voiceName = userId.child("VoiceName");
                voiceName.setValue(fileName);
//                Firebase pictureUrl = userId.child("PictureUrl");
//                pictureUrl.setValue("picture");

            }
        });
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToRecordAudio() {
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, RECORD_AUDIO_REQUEST_CODE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length == 3 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "Record Audio permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        stopRecording();
        stopAudioPlay();

        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
       if (view == imageViewRecord) {
           prepareForRecording();
           startRecording();

           mSendData.setVisibility(View.GONE);
       } else if (view == imageViewStop) {
           prepareForStop();
           stopRecording();

           mSendData.setVisibility(View.VISIBLE);
       } else if (view == imageViewPlay) {
           startPlaying();
       }
 }

    private void prepareForRecording() {
        imageViewRecord.setVisibility(View.GONE);
        imageViewPlay.setVisibility(View.GONE);
        imageViewStop.setVisibility(View.VISIBLE);
    }

    private void uploadFile(){
        File file = new File(filePath);

        if (file.exists()) {
            Log.d("RecordFragment", file.getName());
            Uri fileUri = Uri.fromFile(file);
            StorageReference riversRef = mStorageRef.child("Voices/"+ fileName);

            riversRef.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.d("RecordFragment", "Stana");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.d("RecordFragment", "Ne Stana");
                        }
                    });
        }
    }

    private void startRecording() {
        stopAudioPlay();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        File root = android.os.Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios");
        if (!file.exists()) {
            file.mkdirs();
        }

        fileName = String.valueOf(System.currentTimeMillis() + ".mp3");
        filePath = root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios/" +
                fileName;
        Log.d("filename", filePath);
        mRecorder.setOutputFile(filePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    private void prepareForStop() {
        imageViewRecord.setVisibility(View.VISIBLE);
        imageViewPlay.setVisibility(View.VISIBLE);
        imageViewPlay.setEnabled(true);
        imageViewStop.setVisibility(View.GONE);
    }

    private void stopRecording() {
        try {
            mRecorder.stop();
            mRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mRecorder = null;

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();

        Toast.makeText(getActivity(), "Recording saved successfully.", Toast.LENGTH_SHORT).show();
    }

    private void startPlaying() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        playLastStoredAudioMusic();
        mediaPlayerPlaying();
    }

    private void playLastStoredAudioMusic() {
        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(this);
        try {
            mPlayer.setDataSource(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.start();
        imageViewPlay.setEnabled(false);
    }

    private void mediaPlayerPlaying() {
        if (!mPlayer.isPlaying()) {
            stopAudioPlay();
        }
    }

    private void stopAudioPlay() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();
        imageViewPlay.setEnabled(true);
    }
}
