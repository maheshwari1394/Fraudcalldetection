package com.fraudcallcnn;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    // ⚠️ Replace with your Flask server IP (e.g., "http://192.168.1.50:5000/predict")
    private static final String SERVER_URL = "http://172.20.10.4:8000/process_audio";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    public interface ApiCallback {
        void onResponse(String label, float probability, String transcript);
    }

    public void sendAudio(float[] window, ApiCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("sample_rate", SlidingWindowProcessor.SAMPLE_RATE);
            json.put("length", window.length);
            json.put("audio", floatArrayToJsonArray(window));

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("ApiClient", "HTTP error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e("ApiClient", "Bad response: " + response.code());
                        return;
                    }
                    try {
                        String respStr = response.body() != null ? response.body().string() : "{}";
                        JSONObject resp = new JSONObject(respStr);

                        String label = resp.optString("label", "unknown");
                        float prob = (float) resp.optDouble("probability", 0.0);
                        String transcript = resp.optString("transcript", "No transcript available");

                        if (callback != null) callback.onResponse(label, prob, transcript);
                    } catch (JSONException e) {
                        Log.e("ApiClient", "JSON parse error", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e("ApiClient", "JSON build error", e);
        } catch (Exception e) {
            Log.e("ApiClient", "sendAudio exception", e);
        }
    }

    // This method is now correctly handling the JSONException
    private JSONArray floatArrayToJsonArray(float[] arr) {
        JSONArray a = new JSONArray();
        for (float v : arr) {
            try {
                a.put(v);
            } catch (JSONException e) {
                Log.e("ApiClient", "Error adding float to JSONArray", e);
            }
        }
        return a;
    }
}