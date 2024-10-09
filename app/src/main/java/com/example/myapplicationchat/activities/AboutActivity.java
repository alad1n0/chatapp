package com.example.myapplicationchat.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplicationchat.R;
import com.example.myapplicationchat.databinding.ActivityAboutBinding;

public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.bottomNavigationView.setSelectedItemId(R.id.about);
        setListeners();
    }

    private void setListeners() {
        binding.siteWebButton.setOnClickListener(v -> {
            String url = "https://siteweb.org.ua/";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UserActivity.class)));

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                return true;
            } else if (item.getItemId() == R.id.settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            } else if (item.getItemId() == R.id.about) {
                return true;
            } else if (item.getItemId() == R.id.profile) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}