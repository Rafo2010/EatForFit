package rafayel.hakobyan.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import rafayel.hakobyan.project.ui.login.LoginFragment;

public class SignInActivity extends AppCompatActivity {

    private MaterialButton btnSignIn, btnGoToRegister;
    private ProgressBar    progressBar;
    private TextView       tvForgotPassword;

    private FirebaseAuth      mAuth;
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        mAuth     = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        bindViews();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frl, new LoginFragment())
                .commit();

        setupListeners();
    }

    private void bindViews() {
        btnSignIn        = findViewById(R.id.btnSignIn);
        btnGoToRegister  = findViewById(R.id.btnGoToRegister);
        progressBar      = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setupListeners() {

        btnSignIn.setOnClickListener(v -> attemptSignIn());

        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, Register.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = viewModel.getEmail();
            if (email == null || email.isEmpty()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this,
                            "Password reset email sent to " + email,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this,
                            "Could not send reset email. Check the address.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void attemptSignIn() {
        String email    = viewModel.getEmail();
        String password = viewModel.getPassword();

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password == null || password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        setLoading(false);
                        goToMain();
                    } else {
                        setLoading(false);
                        try {
                            throw task.getException();
                        } catch (Exception e) {
                            Toast.makeText(this,
                                    "Sign in failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignIn.setEnabled(!loading);
        btnGoToRegister.setEnabled(!loading);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}