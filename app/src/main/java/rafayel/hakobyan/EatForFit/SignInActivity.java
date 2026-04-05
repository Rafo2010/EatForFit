package rafayel.hakobyan.EatForFit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class SignInActivity extends AppCompatActivity {

    private MaterialButton btnSignIn, btnGoToRegister;
    private ProgressBar    progressBar;
    private TextView       tvForgotPassword;

    private FirebaseAuth   mAuth;
    private SignInFragment signInFragment;

    private FirebaseAuth.AuthStateListener authStateListener;
    private boolean didNavigate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        mAuth = FirebaseAuth.getInstance();

        // Navigates to main the instant Firebase confirms sign-in
        authStateListener = auth -> {
            if (auth.getCurrentUser() != null && !didNavigate) {
                goToMain();
            }
        };

        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        btnSignIn        = findViewById(R.id.btnSignIn);
        btnGoToRegister  = findViewById(R.id.btnGoToRegister);
        progressBar      = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Load SignInFragment — commitNow so view is ready immediately
        signInFragment = new SignInFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frl, signInFragment)
                .commitNow();

        btnSignIn.setOnClickListener(v -> attemptSignIn());

        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, Register.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(authStateListener);
    }

    private void attemptSignIn() {
        // Read directly from fragment views at tap time — no ViewModel, no timing issues
        String email    = signInFragment.getEmail();
        String password = signInFragment.getPassword();

        signInFragment.clearErrors();

        if (email.isEmpty()) {
            signInFragment.setEmailError("Email is required");
            signInFragment.focusEmail();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signInFragment.setEmailError("Enter a valid email");
            signInFragment.focusEmail();
            return;
        }
        if (password.isEmpty()) {
            signInFragment.setPasswordError("Password is required");
            signInFragment.focusPassword();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                getFriendlyError(task.getException()),
                                Toast.LENGTH_LONG).show();
                    }
                    // On success, AuthStateListener handles navigation automatically
                });
    }

    private void handleForgotPassword() {
        String email = signInFragment.getEmail();

        if (email.isEmpty()) {
            signInFragment.setEmailError("Enter your email first");
            signInFragment.focusEmail();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signInFragment.setEmailError("Enter a valid email");
            signInFragment.focusEmail();
            return;
        }
        signInFragment.clearErrors();

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(a ->
                        Toast.makeText(this,
                                "Reset email sent to " + email,
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not send reset email: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private String getFriendlyError(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException)
            return "No account found with this email.";
        if (e instanceof FirebaseAuthInvalidCredentialsException)
            return "Wrong password. Please try again.";
        return e != null ? e.getMessage() : "Sign in failed. Please try again.";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignIn.setEnabled(!loading);
        btnGoToRegister.setEnabled(!loading);
    }

    private void goToMain() {
        if (didNavigate) return;
        didNavigate = true;
        runOnUiThread(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}