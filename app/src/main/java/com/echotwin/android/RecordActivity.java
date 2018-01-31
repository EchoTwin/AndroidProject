package com.echotwin.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecordActivity extends AppCompatActivity implements
        View.OnClickListener,
        MediaPlayer.OnCompletionListener {

    private static final int RECORD_AUDIO_REQUEST_CODE = 123;
    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private String userChoosenTask;

    private TextView chronometer;
    private EditText userNameEditText;
    private TextInputLayout userNameTextInput;
    private ImageButton recordStopImageButton, playImageButton;
    private CircleImageView userAvatar;
    private Button uploadUserInfo;
    private CountDownTimer countDownTimer;

    private long totalTimeCountInMilliseconds;
    private long timeBlinkInMilliseconds;
    private boolean blink;
    private boolean isRecording = false;

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;

    private String voiceFilePath, avatarFilePath;
    private String voiceFileName, avatarFileName;
    private Firebase mRef;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio();
        }

        userAvatar = findViewById(R.id.userAvatar);
        chronometer = findViewById(R.id.chronometerTimer);
        recordStopImageButton = findViewById(R.id.recordStopImageButton);
        playImageButton = findViewById(R.id.playImageButton);
        userNameEditText = findViewById(R.id.userNameEditText);
        uploadUserInfo = findViewById(R.id.uploadVoiceAndInfoButton);
        userNameTextInput = findViewById(R.id.userNameTextInput);
        userNameTextInput.setError("Username is required"); // show error
        userNameTextInput.setError(null); // hide error

        recordStopImageButton.setOnClickListener(this);
        playImageButton.setOnClickListener(this);
        userAvatar.setOnClickListener(this);

        Firebase.setAndroidContext(this);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mRef = new Firebase("https://echotwin-a8d76.firebaseio.com/");

        uploadUserInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userNameEditText.getText().toString().matches("")) {
                    userNameTextInput.setError("Username is required!");
                } else {
                    Firebase userId = mRef.push();
                    Firebase username = userId.child("userName");
                    username.setValue(userNameEditText.getText().toString());
                    Firebase voiceFileURL = userId.child("voiceFileURL");
                    voiceFileURL.setValue(voiceFileName);
                    Firebase pictureUrl = userId.child("pictureFileURL");
                    pictureUrl.setValue(avatarFileName);
                    uploadUserVoice();
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToRecordAudio() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

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
        switch (requestCode) {
            case RECORD_AUDIO_REQUEST_CODE:
                if (grantResults.length == 3 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Record Audio permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show();
                }
                break;
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if (userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    //code for deny
                    Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show();
                }
                break;
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
        switch (view.getId()) {
            case R.id.userAvatar:
                selectImage();
                break;
            case R.id.recordStopImageButton:
                if (!isRecording) {
                    prepareForRecording();
                    startRecording();

                    uploadUserInfo.setVisibility(View.INVISIBLE);
                } else {
                    prepareForStop();
                    stopRecording();

                    uploadUserInfo.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.playImageButton:
                prepareForPlaying();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        countDownTimer.cancel();
    }

    private void setTimer() {
        totalTimeCountInMilliseconds = 60 * 1000;
        timeBlinkInMilliseconds = 10 * 1000;
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(totalTimeCountInMilliseconds, 500) {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            public void onTick(long leftTimeInMilliseconds) {
                long seconds = leftTimeInMilliseconds / 1000;

                if (leftTimeInMilliseconds < timeBlinkInMilliseconds) {
                    chronometer.setTextAppearance(getApplicationContext(),
                            R.style.blinkText);
                    // change the style of the textview .. giving a red
                    // alert style

                    if (blink) {
                        chronometer.setVisibility(View.VISIBLE);
                    } else {
                        chronometer.setVisibility(View.INVISIBLE);
                    }

                    blink = !blink; // toggle the value of blink
                }

                chronometer.setText(String.format("%02d", seconds / 60)
                        + ":" + String.format("%02d", seconds % 60));
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                // this function will be called when the timecount is finished
                prepareForStop();
                stopRecording();
                chronometer.setText("00:00");
                chronometer.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void prepareForRecording() {
        recordStopImageButton.setImageResource(R.drawable.ic_stop);
        playImageButton.setVisibility(View.INVISIBLE);
    }

    private void startRecording() {
        stopAudioPlay();
        isRecording = true;

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        NoiseSuppressor.create(MediaRecorder.AudioSource.MIC);

        File root = android.os.Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios");
        if (!file.exists()) {
            file.mkdirs();
        }

        voiceFileName = String.valueOf(System.currentTimeMillis() + ".mp3");
        voiceFilePath = root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios/" +
                voiceFileName;
        mRecorder.setOutputFile(voiceFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setTimer();
        startTimer();
    }

    private void prepareForStop() {
        recordStopImageButton.setImageResource(android.R.drawable.ic_btn_speak_now);
        playImageButton.setVisibility(View.VISIBLE);
        playImageButton.setEnabled(true);
    }

    private void stopRecording() {
        isRecording = false;

        if (countDownTimer != null)
            countDownTimer.cancel();

        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    private void prepareForPlaying() {
        setTimer();
        startTimer();
        playLastStoredAudioMusic();
        mediaPlayerPlaying();
    }

    private void playLastStoredAudioMusic() {
        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(this);
        try {
            mPlayer.setDataSource(voiceFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.start();
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

    private void uploadUserVoice() {
        File voiceFile = new File(voiceFilePath);

        if (voiceFile.exists()) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading voice file");
            progressDialog.show();

            Uri voiceFileUri = Uri.fromFile(voiceFile);
            StorageReference riversRef = mStorageRef.child("Voices/" + voiceFileName);

            riversRef.putFile(voiceFileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Voice file uploaded", Toast.LENGTH_LONG).show();
                            uploadUserAvatar();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        } else {
            //you can display an error toast
            Toast.makeText(getApplicationContext(), "Voice file not found!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void uploadUserAvatar() {
        File avatarFile = new File(avatarFilePath);

        if (avatarFile.exists()) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading avatar file");
            progressDialog.show();

            Uri voiceFileUri = Uri.fromFile(avatarFile);
            StorageReference riversRef = mStorageRef.child("Avatars/" + avatarFileName);

            riversRef.putFile(voiceFileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Avatar file uploaded", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        } else {
            //you can display an error toast
            Toast.makeText(getApplicationContext(), "Avatar file not found!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(RecordActivity.this);
                if (items[item].equals("Take Photo")) {
                    userChoosenTask = "Take Photo";
                    if (result)
                        cameraIntent();
                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask = "Choose from Library";
                    if (result)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");
        avatarFilePath = destination.getPath();
        avatarFileName = destination.getName();

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        userAvatar.setImageBitmap(thumbnail);
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
                avatarFilePath = Utility.getRealPathFromURI_API19(this, data.getData());
                avatarFileName = System.currentTimeMillis() + ".jpg";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        userAvatar.setImageBitmap(bm);
    }
}
