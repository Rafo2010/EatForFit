package rafayel.hakobyan.project;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rafayel.hakobyan.project.ui.login.Characteristics;
import rafayel.hakobyan.project.ui.login.LoginFragment;

public class Register extends AppCompatActivity {

    private static final String TAG = "Register";

    private final Fragment[] fragments = {
            new LoginFragment(),
            new Characteristics()
    };

    private MaterialButton   btnRegister, btnSignIn;
    private MaterialCheckBox termsCheckbox;
    private TextView         requestText;
    private ProgressBar      progressBar;
    private View             rootView;

    private int     fragmentIndex = 0;
    private boolean isProcessing  = false;
    private RegisterViewModel viewModel;
    private FirebaseAuth mAuth;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private static final int TIMEOUT_MS = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        btnRegister   = findViewById(R.id.btnRegister);
        btnSignIn     = findViewById(R.id.btnSignIn);
        termsCheckbox = findViewById(R.id.termsCheckbox);
        requestText   = findViewById(R.id.requestText);
        progressBar   = findViewById(R.id.progressBar);
        rootView      = findViewById(android.R.id.content);

        showFragment(fragmentIndex);
        updateButtonText();
        updateTermsVisibility();

        btnRegister.setOnClickListener(v -> handleNext());

        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(Register.this, SignInActivity.class));
            finish();
        });
    }

    private void handleNext() {
        if (isProcessing) return;

        Fragment current = fragments[fragmentIndex];
        View fragmentView = current.getView();

        if (fragmentView != null) {
            List<EditText> fields = new ArrayList<>();
            collectVisibleEditTexts(fragmentView, fields);

            for (EditText et : fields) {
                if (et.getText().toString().trim().isEmpty()) {
                    Snackbar.make(rootView, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show();
                    et.requestFocus();
                    return;
                }
            }

            pushFieldsToViewModel(fragmentIndex, fields);
        }

        if (fragmentIndex == fragments.length - 1) {
            if (!termsCheckbox.isChecked()) {
                Toast.makeText(this, "Please accept Terms and Conditions first", Toast.LENGTH_SHORT).show();
                return;
            }
            completeRegistration();
        } else {
            fragmentIndex++;
            showFragment(fragmentIndex);
            updateButtonText();
            updateTermsVisibility();
            animate();
        }
    }

    private void collectVisibleEditTexts(View root, List<EditText> result) {
        if (root == null || root.getVisibility() != View.VISIBLE) return;
        if (root instanceof EditText) {
            result.add((EditText) root);
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectVisibleEditTexts(group.getChildAt(i), result);
            }
        }
    }

    private void pushFieldsToViewModel(int step, List<EditText> fields) {
        try {
            if (step == 0) {
                if (fields.size() >= 1) viewModel.setEmail(fields.get(0).getText().toString().trim());
                if (fields.size() >= 2) viewModel.setPassword(fields.get(1).getText().toString().trim());
                Log.d(TAG, "Step 0 pushed → email=" + viewModel.getEmail());
            } else if (step == 1) {
                if (fields.size() >= 1) viewModel.setName(fields.get(0).getText().toString().trim());
                if (fields.size() >= 2) viewModel.setWeight(fields.get(1).getText().toString().trim());
                if (fields.size() >= 3) viewModel.setHeight(fields.get(2).getText().toString().trim());
                if (fields.size() >= 4) viewModel.setDisease(fields.get(3).getText().toString().trim());
                Log.d(TAG, "Step 1 pushed → name=" + viewModel.getName().getValue());
            }
        } catch (Exception e) {
            Log.e(TAG, "pushFieldsToViewModel error: " + e.getMessage());
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void completeRegistration() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        String email    = viewModel.getEmail();
        String password = viewModel.getPassword();

        Log.d(TAG, "Registering → email=" + email + " | passwordLen=" + (password != null ? password.length() : "null"));

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Email missing — go back to step 1", Toast.LENGTH_LONG).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_LONG).show();
            return;
        }
        if (password == null || password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_LONG).show();
            return;
        }

        isProcessing = true;
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        btnSignIn.setEnabled(false);

        timeoutHandler.postDelayed(() -> {
            if (isProcessing) {
                Log.e(TAG, "Firebase timed out");
                resetUI("Connection timed out. Check your internet and try again.");
            }
        }, TIMEOUT_MS);

        mAuth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener(this, task -> {
                    timeoutHandler.removeCallbacksAndMessages(null);
                    if (!isProcessing) return;

                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        Log.d(TAG, "Auth success, saving to Firestore...");
                        saveUserToFirestore(mAuth.getCurrentUser().getUid());
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Auth failed: " + msg);
                        resetUI("Registration failed: " + msg);
                    }
                });
    }

    private void saveUserToFirestore(String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("email",   viewModel.getEmail());
        user.put("name",    viewModel.getName().getValue());
        user.put("weight",  viewModel.getWeight().getValue());
        user.put("height",  viewModel.getHeight().getValue());
        user.put("disease", viewModel.getDisease().getValue());

        Log.d(TAG, "Saving to Firestore: " + user);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    isProcessing = false;
                    Log.d(TAG, "Firestore save success, navigating to main");
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    // Auth succeeded — navigate anyway, profile can be updated later
                    Log.e(TAG, "Firestore failed: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    isProcessing = false;
                    Toast.makeText(this,
                            "Account created! You can update your profile later.",
                            Toast.LENGTH_LONG).show();
                    navigateToMain();
                });
    }

    private void resetUI(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            btnSignIn.setEnabled(true);
            isProcessing = false;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void showFragment(int index) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frl, fragments[index])
                .commitNow();
        requestText.setText("Step " + (index + 1) + " of " + fragments.length);
    }

    private void updateButtonText() {
        btnRegister.setText(fragmentIndex == fragments.length - 1 ? "Create Account" : "Next");
    }

    private void updateTermsVisibility() {
        termsCheckbox.setVisibility(fragmentIndex == fragments.length - 1 ? View.VISIBLE : View.GONE);
    }

    private void animate() {
        View container = findViewById(R.id.frl);
        ObjectAnimator.ofFloat(container, "alpha", 0f, 1f).setDuration(300).start();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacksAndMessages(null);
    }
}