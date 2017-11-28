package com.echotwin.android;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.zagum.speechrecognitionview.RecognitionProgressView;
import com.github.zagum.speechrecognitionview.adapters.RecognitionListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by kristo.prifti on 2017/11/26.
 */

public class RecordFragment extends Fragment implements View.OnClickListener, MediaPlayer.OnCompletionListener {

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;

    private Chronometer chronometer;
    private ImageView imageViewRecord, imageViewStop, imageViewPlay;
    private int RECORD_AUDIO_REQUEST_CODE = 123;
    private String fileName;

    private SpeechRecognizer speechRecognizer;
    private RecognitionProgressView recognitionProgressView;

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
        View inflaterView = inflater.inflate(R.layout.fragment_record, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio();
        }

        int[] colors = {
                ContextCompat.getColor(getActivity(), R.color.color1),
                ContextCompat.getColor(getActivity(), R.color.color2),
                ContextCompat.getColor(getActivity(), R.color.color3),
                ContextCompat.getColor(getActivity(), R.color.color4),
                ContextCompat.getColor(getActivity(), R.color.color5)
        };

        int[] heights = {20, 24, 18, 23, 16};

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());

        recognitionProgressView = inflaterView.findViewById(R.id.recognition_view);
        recognitionProgressView.setSpeechRecognizer(speechRecognizer);
        recognitionProgressView.setRecognitionListener(new RecognitionListenerAdapter() {
            @Override
            public void onResults(Bundle results) {
                showResults(results);
            }
        });
        recognitionProgressView.setColors(colors);
        recognitionProgressView.setBarMaxHeightsInDp(heights);
        recognitionProgressView.setCircleRadiusInDp(2);
        recognitionProgressView.setSpacingInDp(2);
        recognitionProgressView.setIdleStateAmplitudeInDp(2);
        recognitionProgressView.setRotationRadiusInDp(10);
        recognitionProgressView.play();

        chronometer = inflaterView.findViewById(R.id.chronometerTimer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        imageViewRecord = inflaterView.findViewById(R.id.imageViewRecord);
        imageViewStop = inflaterView.findViewById(R.id.imageViewStop);
        imageViewPlay = inflaterView.findViewById(R.id.imagePlayRecord);

        imageViewRecord.setOnClickListener(this);
        imageViewStop.setOnClickListener(this);
        imageViewPlay.setOnClickListener(this);

        return inflaterView;
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
        // Make sure it's our original READ_CONTACTS request
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
        stopRecognition();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        if (view == imageViewRecord) {
            prepareForRecording();
            startRecording();
            startRecognition();
            recognitionProgressView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startRecognition();
                }
            }, 50);
        } else if (view == imageViewStop) {
            prepareForStop();
            stopRecording();
            stopRecognition();
        } else if (view == imageViewPlay) {
            startPlaying();
            startRecognition();
            recognitionProgressView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startRecognition();
                }
            }, 50);
        }
    }

    private void showResults(Bundle results) {
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Toast.makeText(getActivity(), matches.get(0), Toast.LENGTH_LONG).show();
    }

    private void prepareForRecording() {
        imageViewRecord.setVisibility(View.GONE);
        imageViewPlay.setVisibility(View.GONE);
        imageViewStop.setVisibility(View.VISIBLE);
    }

    private void startRecording() {
        stopAudioPlay();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        /*In the lines below, we create a directory VoiceRecorderSimplifiedCoding/Audios in the phone storage
         * and the audios are being stored in the Audios folder **/
        File root = android.os.Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios");
        if (!file.exists()) {
            file.mkdirs();
        }

        fileName = root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios/" +
                String.valueOf(System.currentTimeMillis() + ".mp3");
        Log.d("filename", fileName);
        mRecorder.setOutputFile(fileName);
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

    private void startRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getActivity().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
        speechRecognizer.startListening(intent);
    }

    private void stopRecognition() {
        speechRecognizer.stopListening();
        recognitionProgressView.stop();
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
            mPlayer.setDataSource(fileName);
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
