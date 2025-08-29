package com.fraudcallcnn;

import java.util.ArrayList;
import java.util.List;

public class SlidingWindowProcessor {
    public static final int SAMPLE_RATE = 16000;
    public static final int WINDOW_SIZE = SAMPLE_RATE * 5;    // 5s
    public static final int HOP_SIZE    = SAMPLE_RATE * 3 / 2; // 1.5s (24k)

    private final List<Float> buffer = new ArrayList<>();

    public interface WindowCallback {
        void onWindowReady(float[] window);
    }

    public synchronized void addAudio(float[] audioChunk, WindowCallback callback) {
        for (float v : audioChunk) buffer.add(v);

        while (buffer.size() >= WINDOW_SIZE) {
            float[] window = new float[WINDOW_SIZE];
            for (int i = 0; i < WINDOW_SIZE; i++) window[i] = buffer.get(i);

            callback.onWindowReady(window);

            // hop forward
            int remove = Math.min(HOP_SIZE, buffer.size());
            buffer.subList(0, remove).clear();
        }
    }

    /**
     * Flush remaining audio as a final window.
     * Pads with zeros to 5s if needed. Returns null if nothing left.
     */
    public synchronized float[] flush() {
        if (buffer.isEmpty()) return null;

        float[] window = new float[WINDOW_SIZE];
        int n = Math.min(buffer.size(), WINDOW_SIZE);
        for (int i = 0; i < n; i++) window[i] = buffer.get(i);
        // remaining part stays 0 (padding)

        buffer.clear();
        return window;
    }
}