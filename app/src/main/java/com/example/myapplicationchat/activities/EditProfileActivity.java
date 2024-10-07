package com.example.myapplicationchat.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.myapplicationchat.databinding.ActivityEditProfileBinding;
import com.example.myapplicationchat.models.User;
import com.example.myapplicationchat.utilities.Constants;
import com.example.myapplicationchat.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

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
        binding.editTextName.setText(preferenceManager.getString(Constants.KEY_NAME));
        binding.editTextEmail.setText(preferenceManager.getString(Constants.KEY_EMAIL));
    }

    private void setListeners() {
        binding.buttonSave.setOnClickListener(v -> saveUserProfile());
    }

    private void saveUserProfile() {
        String newName = binding.editTextName.getText().toString().trim();
        String newEmail = binding.editTextEmail.getText().toString().trim();

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
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to update profile"));
    }
}
