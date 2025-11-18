package com.example.test_camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginButton;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === Build simple UI programmatically ===
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 120, 60, 60);

        usernameInput = new EditText(this);
        usernameInput.setHint("Email / Username");

        passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // 👁️ Add show/hide password icon on the right
        Drawable eyeIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_view);
        passwordInput.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeIcon, null);

        // Detect taps on the eye icon
        passwordInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRight = 2; // index for right drawable
                Drawable rightDrawable = passwordInput.getCompoundDrawables()[drawableRight];
                if (rightDrawable != null) {
                    int leftEdgeOfRightDrawable = passwordInput.getRight()
                            - rightDrawable.getBounds().width()
                            - passwordInput.getPaddingEnd();
                    if (event.getRawX() >= leftEdgeOfRightDrawable) {
                        togglePasswordVisibility(passwordInput);
                        return true;
                    }
                }
            }
            return false;
        });

        loginButton = new Button(this);
        loginButton.setText("Login");

        layout.addView(usernameInput);
        layout.addView(passwordInput);
        layout.addView(loginButton);
        setContentView(layout);

        loginButton.setOnClickListener(v -> doLogin());
    }

    /** Toggle password visibility when user taps the eye icon */
    private void togglePasswordVisibility(EditText editText) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (passwordVisible) {
            // Hide password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordVisible = false;
        } else {
            // Show password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordVisible = true;
        }

        // Keep the same compound drawable (eye icon)
        Drawable eyeIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_view);
        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeIcon, null);

        // Restore cursor position
        editText.setSelection(start, end);
    }

    /** Perform Edge Impulse login and navigate on success */
    private void doLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        username = "gowifa2176@elygifts.com";
        password = "gowifa2176@elygifts.com";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        OkHttpClient client = new OkHttpClient();
        JSONObject payload = new JSONObject();
        try {
            payload.put("username", username);
            payload.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                payload.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://studio.edgeimpulse.com/v1/api-login")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                try {
                    JSONObject json = new JSONObject(respStr);
                    if (json.optBoolean("success")) {
                        String jwt = json.optString("token");
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, ProjectSelectActivity.class);
                            intent.putExtra("JWT_TOKEN", jwt);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        String error = json.optString("error", "Login failed");
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
