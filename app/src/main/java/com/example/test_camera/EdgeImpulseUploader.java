package com.example.test_camera;

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import okhttp3.*;

public class EdgeImpulseUploader {

    private static final String TAG = "EdgeImpulseUploader";
    private static final String API_URL = "https://ingestion.edgeimpulse.com/api/training/files";

    public void uploadImage(String filePath, String label, Context context, String apiKey) {
        // Allow network on main thread if needed (better to move to background thread in production)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            Toast.makeText(context, "File not found!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "File not found: " + filePath);
            return;
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/png"));
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("data", imageFile.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("x-label", label)
                .addHeader("x-api-key", apiKey)  // ✅ dynamic API key
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Log.d(TAG, "Uploaded successfully: " + response.code());
                Toast.makeText(context, "Uploaded successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Upload failed: " + response.code() + " " + response.message());
                Toast.makeText(context, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException: ", e);
            Toast.makeText(context, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
