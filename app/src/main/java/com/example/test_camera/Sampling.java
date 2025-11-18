package com.example.test_camera;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.Intent;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Sampling extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private EditText inputLabel;
    private ImageView imagePreview;
    private Button btnTakePicture, btnSubmit, btnRetake, btnCancel;
    private LinearLayout confirmLayout;

    private Bitmap lastBitmap;
    private String userLabel;

    // Modern Activity Result API launcher
    private ActivityResultLauncher<Void> takePicturePreviewLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sampling);

        inputLabel = findViewById(R.id.inputLabel);
        imagePreview = findViewById(R.id.imagePreview);
        btnTakePicture = findViewById(R.id.btnTakePicture);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnRetake = findViewById(R.id.btnRetake);
        btnCancel = findViewById(R.id.btnCancel);
        confirmLayout = (LinearLayout) btnSubmit.getParent();

        // Initialize ActivityResultLauncher
        takePicturePreviewLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        lastBitmap = bitmap;
                        showPreview(bitmap);

                        //Do not quit the camera, and retake the picture
                        File file = saveBitmapToFile(lastBitmap);
                        String apiKey = getIntent().getStringExtra("API_KEY");
                        EdgeImpulseUploader uploader = new EdgeImpulseUploader();
                        uploader.uploadImage(file.getAbsolutePath(), userLabel, this, apiKey);
                        Toast.makeText(this, "Uploaded to the cloud!", Toast.LENGTH_SHORT).show();
                        takePicturePreviewLauncher.launch(null);
                    } else {
                        Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // TAKE PICTURE button
        btnTakePicture.setOnClickListener(v -> {
            userLabel = inputLabel.getText().toString().trim();
            if (userLabel.isEmpty()) {
                Toast.makeText(this, "Please enter a label!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                openCamera();
            }
        });

        // SUBMIT button
        btnSubmit.setOnClickListener(v -> {
            if (lastBitmap == null) {
                Toast.makeText(this, "No photo to upload!", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = saveBitmapToFile(lastBitmap);
            if (file == null) {
                Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show();
                return;
            }

            String apiKey = getIntent().getStringExtra("API_KEY");
            EdgeImpulseUploader uploader = new EdgeImpulseUploader();
            uploader.uploadImage(file.getAbsolutePath(), userLabel, this, apiKey);
            resetUI();
        });

        // RETAKE button
        btnRetake.setOnClickListener(v -> openCamera());

        // CANCEL button
        btnCancel.setOnClickListener(v -> {
            resetUI();
            Toast.makeText(this, "Cancelled.", Toast.LENGTH_SHORT).show();
        });

        // GO TO MAIN ACTIVITY button
        Button btnGoToMain = findViewById(R.id.btnGoToMain);
        btnGoToMain.setOnClickListener(v -> {
            Intent intent = new Intent(Sampling.this, MainActivity.class);
            startActivity(intent);
        });
    }

    // Handle camera permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Launch modern camera preview
    private void openCamera() {
        takePicturePreviewLauncher.launch(null);
    }

    // Save the bitmap to a file for upload
    private File saveBitmapToFile(Bitmap bitmap) {
        File file = new File(getCacheDir(), "photo.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Display preview after capture
    private void showPreview(Bitmap bitmap) {
        imagePreview.setImageBitmap(bitmap);
        imagePreview.setVisibility(View.VISIBLE);
        toggleButtonsAfterPhoto(true);
    }

    // Enable/disable UI buttons
    private void toggleButtonsAfterPhoto(boolean hasPhoto) {
        btnSubmit.setEnabled(hasPhoto);
        btnRetake.setEnabled(hasPhoto);
        btnCancel.setEnabled(hasPhoto);
        btnTakePicture.setEnabled(!hasPhoto);
    }

    // Reset UI state
    private void resetUI() {
        imagePreview.setImageDrawable(null);
        imagePreview.setVisibility(View.GONE);
        toggleButtonsAfterPhoto(false);
        lastBitmap = null;
    }
}
