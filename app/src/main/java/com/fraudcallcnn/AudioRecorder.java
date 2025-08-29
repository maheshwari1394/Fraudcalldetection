package com.fraudcallcnn;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioRecorder {
    private static final int SAMPLE_RATE = 16000; // Hz, must match server
    private AudioRecord recorder;
    private volatile boolean isRecording = false;
    private static final String TAG = "AudioRecorder";

    public interface AudioCallback {
        void onAudioData(float[] floatBuffer);
    }

    public void startRecording(AudioCallback callback) {
        int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            minBufferSize = SAMPLE_RATE * 2; // fallback
        }
        final int bufferSize = minBufferSize; // effectively final for lambda/thread

        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC, // MIC; capturing downlink/telephony audio is restricted by Android
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord init failed");
            return;
        }

        recorder.startRecording();
        isRecording = true;

        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                try {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        float[] floatBuffer = pcm16ToFloat32(buffer, read);
                        if (callback != null && floatBuffer.length > 0) {
                            callback.onAudioData(floatBuffer);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Audio read error", e);
                }
            }
        }, "AudioRecorderThread").start();
    }

    public void stopRecording() {
        isRecording = false;
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception e) { Log.e(TAG, "stop error", e); }
            recorder.release();
            recorder = null;
        }
    }

    private float[] pcm16ToFloat32(byte[] buffer, int read) {
        if ((read & 1) == 1) read--; // ensure even number of bytes
        int samples = read / 2;
        short[] shorts = new short[samples];
        try {
            ByteBuffer.wrap(buffer, 0, read)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(shorts);
        } catch (Exception e) {
            Log.e(TAG, "PCM→short conversion failed", e);
            return new float[0];
        }

        float[] floats = new float[samples];
        for (int i = 0; i < samples; i++) {
            floats[i] = shorts[i] / 32768f; // normalize to [-1,1]
        }
        return floats;
    }
}