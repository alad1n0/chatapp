package com.example.myapplicationchat.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.myapplicationchat.R;
import com.example.myapplicationchat.databinding.ActivityProfileBinding;
import com.example.myapplicationchat.utilities.Constants;
import com.example.myapplicationchat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private ActivityProfileBinding binding;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding.bottomNavigationView.setSelectedItemId(R.id.profile);
        loadUserDetails();
        setListeners();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        binding.bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    private void setListeners() {
        final Animation clickAnimation = AnimationUtils.loadAnimation(this, R.anim.button_click_animation);

        binding.fabNewChat.setOnClickListener(v ->{
            v.startAnimation(clickAnimation);
            startActivity(new Intent(getApplicationContext(), UserActivity.class));
        });

        binding.aboutButton.setOnClickListener(v -> {
            v.startAnimation(clickAnimation);
            startActivity(new Intent(getApplicationContext(), AboutActivity.class));
        });

        binding.SettingsButton.setOnClickListener(v -> {
            v.startAnimation(clickAnimation);
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        });

        binding.EditButton.setOnClickListener(v -> {
            v.startAnimation(clickAnimation);
            startActivity(new Intent(getApplicationContext(), EditProfileActivity.class));
        });

        binding.editImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            v.startAnimation(clickAnimation);
            pickImage.launch(intent);
        });

        binding.userLogout.setOnClickListener(v -> signOut());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                return true;
            } else if (item.getItemId() == R.id.settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            } else if (item.getItemId() == R.id.about) {
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                return true;
            } else return item.getItemId() == R.id.profile;
        });
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
                            saveUserProfile();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void saveUserProfile() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        Map<String, Object> updates = new HashMap<>();

        if (encodedImage != null) {
            updates.put(Constants.KEY_IMAGE, encodedImage);
        }

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    showToast("Profile updated");
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    updateSenderImageInConversions(encodedImage);
                    showNotification("Profile Updated", "Your profile has been updated successfully.");
                })
                .addOnFailureListener(e -> showToast("Failed to update profile"));
    }

    private void updateSenderImageInConversions(String newImage) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        Log.d("TAG", "updateSenderImageInConversions: " + newImage);

        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("TAG", "No conversations found for user: " + userId);
                        return;
                    }

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        database.runTransaction(transaction -> {
                            DocumentSnapshot snapshot = transaction.get(document.getReference());
                            transaction.update(snapshot.getReference(), Constants.KEY_SENDER_IMAGE, newImage);
                            return null;
                        }).addOnSuccessListener(unused -> {
                            Log.d("TAG", "Sender image updated successfully for document: " + document.getId());
                        }).addOnFailureListener(e -> {
                            Log.e("TAG", "Failed to update sender image for document: " + document.getId(), e);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG", "Failed to fetch conversations", e);
                    showToast("Failed to update sender image in conversations");
                });
    }

    private void loadUserDetails() {
        binding.username.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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
                .setAutoCancel(true)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_profile));

        notificationManager.notify(1, notificationBuilder.build());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            notificationManager.cancel(1);
        }, 15000);
    }

    private void signOut() {
        showToast("Sign out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }
}