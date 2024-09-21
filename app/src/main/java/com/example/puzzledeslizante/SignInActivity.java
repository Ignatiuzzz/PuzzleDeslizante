package com.example.puzzledeslizante;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // Import TextView
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText etUsername, etPassword;
    private Button btnSignIn;
    private TextView tvAlreadyHaveAccount; // TextView to navigate to login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        db = FirebaseFirestore.getInstance();
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);  // Initialize TextView

        // Handle sign-in button click
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                if (!username.isEmpty() && !password.isEmpty()) {
                    // Create a new account
                    signIn(username, password);
                } else {
                    Toast.makeText(SignInActivity.this, "Por favor ingrese nombre y contraseña", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Handle navigation to LoginActivity if user already has an account
        tvAlreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to LoginActivity
                Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void signIn(String username, String password) {
        // Check if the user already exists
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().isEmpty()) {
                        // User doesn't exist, create new account
                        Map<String, Object> user = new HashMap<>();
                        user.put("username", username);
                        user.put("password", password);

                        db.collection("users").add(user)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(SignInActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();

                                    // Redirect to LoginActivity after successful registration
                                    Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignInActivity.this, "Error al registrar, intente de nuevo", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // User already exists
                        Toast.makeText(SignInActivity.this, "El usuario ya existe", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
