package com.fraudcallcnn;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MIC = 101;

    private AudioRecorder recorder;
    private SlidingWindowProcessor processor;
    private ApiClient apiClient;
    private TextView statusText;
    private TextView predictionText;
    private TextView transcriptText;
    private Button startBtn;
    private Button stopBtn;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI references
        statusText = findViewById(R.id.statusText);
        predictionText = findViewById(R.id.predictionText);
        transcriptText = findViewById(R.id.transcriptText);
        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);

        // Initialize components
        recorder = new AudioRecorder();
        processor = new SlidingWindowProcessor();
        apiClient = new ApiClient();

        // Set up click listeners
        startBtn.setOnClickListener(v -> {
            if (!isRecording) {
                if (checkPermission()) {
                    startRecording();
                }
            }
        });

        stopBtn.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            }
        });

        // Initially disable the stop button
        stopBtn.setEnabled(false);
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] perms, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults);
        if (requestCode == REQUEST_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Microphone permission required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecording() {
        isRecording = true;
        statusText.setText("Recording...");
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        predictionText.setText("Prediction: Processing...");
        transcriptText.setText("Transcript: Listening...");

        recorder.startRecording(audioChunk -> {
            processor.addAudio(audioChunk, window -> {
                apiClient.sendAudio(window, (label, prob, transcript) ->
                        runOnUiThread(() -> {
                            statusText.setText("Processing new audio window.");
                            predictionText.setText("Prediction: " + label + " (" + String.format("%.2f", prob) + ")");
                            transcriptText.setText("Transcript: " + transcript);
                        })
                );
            });
        });
    }

    private void stopRecording() {
        isRecording = false;
        recorder.stopRecording();

        float[] lastWindow = processor.flush();
        if (lastWindow != null) {
            apiClient.sendAudio(lastWindow, (label, prob, transcript) ->
                    runOnUiThread(() -> {
                        statusText.setText("Finalizing prediction...");
                        predictionText.setText("Final Prediction: " + label + " (" + String.format("%.2f", prob) + ")");
                        transcriptText.setText("Final Transcript: " + transcript);
                    })
            );
        }

        statusText.setText("Stopped.");
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) stopRecording();
    }
}