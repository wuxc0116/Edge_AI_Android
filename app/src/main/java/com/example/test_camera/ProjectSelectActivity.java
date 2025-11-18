package com.example.test_camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.*;

public class ProjectSelectActivity extends AppCompatActivity {

    private ListView listView;
    private List<ProjectItem> projects = new ArrayList<>();
    private String jwtToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listView = new ListView(this);
        setContentView(listView);

        jwtToken = getIntent().getStringExtra("JWT_TOKEN");
        fetchProjects();
    }

    private void fetchProjects() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://studio.edgeimpulse.com/v1/api/projects")
                .addHeader("x-jwt-token", jwtToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProjectSelectActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                try {
                    JSONObject json = new JSONObject(resp);
                    if (json.optBoolean("success")) {
                        JSONArray arr = json.optJSONArray("projects");
                        projects.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject proj = arr.getJSONObject(i);
                            projects.add(new ProjectItem(proj.getInt("id"), proj.getString("name")));
                        }
                        runOnUiThread(() -> populateList());
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(ProjectSelectActivity.this, "Failed to fetch projects.", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void populateList() {
        List<String> names = new ArrayList<>();
        for (ProjectItem p : projects) names.add(p.name);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            ProjectItem selected = projects.get(position);
            fetchApiKey(selected.id);
        });
    }

    private void fetchApiKey(int projectId) {
        Toast.makeText(this, "Fetching API key...", Toast.LENGTH_SHORT).show();
        OkHttpClient client = new OkHttpClient();
        String url = "https://studio.edgeimpulse.com/v1/api/" + projectId + "/apikeys";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("x-jwt-token", jwtToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ProjectSelectActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                try {
                    JSONObject json = new JSONObject(resp);
                    if (json.optBoolean("success")) {
                        JSONArray keys = json.getJSONArray("apiKeys");
                        String apiKey = keys.getJSONObject(0).getString("apiKey");

                        runOnUiThread(() -> {
                            Toast.makeText(ProjectSelectActivity.this, "Project selected!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ProjectSelectActivity.this, Sampling.class);
                            intent.putExtra("API_KEY", apiKey);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(ProjectSelectActivity.this, "Failed to fetch API key", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    static class ProjectItem {
        int id;
        String name;
        ProjectItem(int i, String n) { id = i; name = n; }
    }
}
