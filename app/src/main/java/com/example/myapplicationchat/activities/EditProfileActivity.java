package com.example.myapplicationchat.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.myapplicationchat.R;
import com.example.myapplicationchat.databinding.ActivityEditProfileBinding;
import com.example.myapplicationchat.models.User;
import com.example.myapplicationchat.utilities.Constants;
import com.example.myapplicationchat.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class EditProfileActivity extends BaseActivity {

    private ActivityEditProfileBinding binding;
    private PreferenceManager preferenceManager;
    private User currentUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        loadUserDetails();
        setListeners();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadUserDetails() {
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.textGoBack.setOnClickListener(v -> onBackPressed());
        binding.username.setText(preferenceManager.getString(Constants.KEY_NAME));
        binding.etName.setText(preferenceManager.getString(Constants.KEY_NAME));
        binding.etEmail.setText(preferenceManager.getString(Constants.KEY_EMAIL));
    }

    private void setListeners() {
        binding.buttonSave.setOnClickListener(v -> saveUserProfile());
    }

    private void saveUserProfile() {
        loading(true);

        String newName = Objects.requireNonNull(binding.etName.getText()).toString().trim();
        String newEmail = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            showToast("Please fill all fields");
            return;
        }

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(
                        Constants.KEY_NAME, newName,
                        Constants.KEY_EMAIL, newEmail
                )
                .addOnSuccessListener(unused -> {
                    showToast("Profile updated");
                    preferenceManager.putString(Constants.KEY_NAME, newName);
                    preferenceManager.putString(Constants.KEY_EMAIL, newEmail);
                    showNotification("Profile Updated", "Your profile has been updated successfully.");
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to update profile"));
    }

    private void loading(Boolean idLoading) {
        if (idLoading) {
            binding.buttonSave.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSave.setVisibility(View.VISIBLE);
        }
    }

    private void showNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "profile_update_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Profile Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for profile updates");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_profile)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(1, notificationBuilder.build());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            notificationManager.cancel(1);
        }, 15000);
    }
}