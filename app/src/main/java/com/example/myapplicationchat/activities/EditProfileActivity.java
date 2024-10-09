package com.example.myapplicationchat.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NotificationCompat;

import com.example.myapplicationchat.R;
import com.example.myapplicationchat.databinding.ActivityEditProfileBinding;
import com.example.myapplicationchat.utilities.Constants;
import com.example.myapplicationchat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditProfileActivity extends BaseActivity {

    private ActivityEditProfileBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;

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

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayInputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayInputStream);
        byte[] bytesImage = byteArrayInputStream.toByteArray();
        return Base64.encodeToString(bytesImage, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void setListeners() {
        final Animation clickAnimation = AnimationUtils.loadAnimation(this, R.anim.button_click_animation);

        binding.buttonSave.setOnClickListener(v -> saveUserProfile());

        binding.editImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            v.startAnimation(clickAnimation);
            pickImage.launch(intent);
        });
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

        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_NAME, newName);
        updates.put(Constants.KEY_EMAIL, newEmail);

        if (encodedImage != null) {
            updates.put(Constants.KEY_IMAGE, encodedImage);
        }

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    showToast("Profile updated");
                    preferenceManager.putString(Constants.KEY_NAME, newName);
                    preferenceManager.putString(Constants.KEY_EMAIL, newEmail);
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    updateSenderImageInConversions(encodedImage);
                    showNotification("Profile Updated", "Your profile has been updated successfully.");
                    loading(false);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to update profile");
                    loading(false);
                });
    }

    private void updateSenderImageInConversions(String newImage) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        document.getReference().update(Constants.KEY_SENDER_IMAGE, newImage);
                    }
                })
                .addOnFailureListener(e -> showToast("Failed to update sender image in conversations"));
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