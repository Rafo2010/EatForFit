package rafayel.hakobyan.EatForFit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private static final String TAG = "REG";

    private TextInputEditText    etEmail, etPassword, etConfirmPassword;
    private TextInputEditText    etName, etWeight, etHeight, etAge;
    private TextInputLayout      emailLayout, passwordLayout, confirmPasswordLayout;
    private View                 step1Layout, step2Layout;
    private AutoCompleteTextView etActivity, etDisease;
    private TextView             tvActivityDesc;
    private MaterialButton       btnRegister, btnSignIn;
    private MaterialCheckBox     termsCheckbox;
    private TextView             requestText;
    private ProgressBar          progressBar;
    private View                 stepDot1, stepDot2;

    private int     step         = 1;
    private boolean isProcessing = false;
    private boolean didNavigate  = false;

    private String email_saved = "", password_saved = "";
    private String selectedActivity = "moderately_active";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        authStateListener = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null && !didNavigate) {
                navigateToMain();
            }
        };

        if (mAuth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        etEmail               = findViewById(R.id.etEmail);
        etPassword            = findViewById(R.id.etPassword);
        etConfirmPassword     = findViewById(R.id.etConfirmPassword);
        emailLayout           = findViewById(R.id.emailLayout);
        passwordLayout        = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        step1Layout           = findViewById(R.id.step1Layout);
        etActivity            = findViewById(R.id.etActivity);
        tvActivityDesc        = findViewById(R.id.tvActivityDesc);
        step2Layout           = findViewById(R.id.step2Layout);
        etName                = findViewById(R.id.etName);
        etWeight              = findViewById(R.id.etWeight);
        etHeight              = findViewById(R.id.etHeight);
        etDisease             = findViewById(R.id.etDisease);
        etAge                 = findViewById(R.id.etAge);
        stepDot1              = findViewById(R.id.stepDot1);
        stepDot2              = findViewById(R.id.stepDot2);
        btnRegister           = findViewById(R.id.btnRegister);
        btnSignIn             = findViewById(R.id.btnSignIn);
        termsCheckbox         = findViewById(R.id.termsCheckbox);
        requestText           = findViewById(R.id.requestText);
        progressBar           = findViewById(R.id.progressBar);

        showStep(1);

        final String[] ACTIVITY_LEVELS = {
                "Sedentary — little or no exercise",
                "Lightly active — 1-2 days/week",
                "Moderately active — 3-4 days/week",
                "Very active — 5-6 days/week",
                "Extra active — intense daily training"
        };
        final String[] ACTIVITY_KEYS = {
                "sedentary", "lightly_active", "moderately_active", "very_active", "extra_active"
        };
        final String[] ACTIVITY_DESCS = {
                "Desk job, minimal movement during the day",
                "Light workouts or walks 1-2 times per week",
                "Gym or sports 3-4 times per week",
                "Hard training 5-6 days per week",
                "Athletes or physical jobs with daily intense training"
        };
        String[] diseases = {"None","Diabetes","Hypertension","Heart Disease","Asthma","Obesity","High Cholesterol","Kidney Disease","Other"};
        android.widget.ArrayAdapter<String> diseaseAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, diseases);
        etDisease.setAdapter(diseaseAdapter);

        android.widget.ArrayAdapter<String> actAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, ACTIVITY_LEVELS);
        etActivity.setAdapter(actAdapter);
        etActivity.setText(ACTIVITY_LEVELS[2], false);
        etActivity.setOnItemClickListener((parent, v, position, id) -> {
            selectedActivity = ACTIVITY_KEYS[position];
            if (tvActivityDesc != null) tvActivityDesc.setText(ACTIVITY_DESCS[position]);
        });

        btnRegister.setOnClickListener(v -> handleNext());
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) mAuth.removeAuthStateListener(authStateListener);
    }

    private void handleNext() {
        if (isProcessing) return;

        if (step == 1) {
            String email   = getText(etEmail);
            String pass    = getText(etPassword);
            String confirm = getText(etConfirmPassword);

            if (email.isEmpty())   { emailLayout.setError("Email is required"); return; }
            emailLayout.setError(null);
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailLayout.setError("Enter a valid email"); return; }
            emailLayout.setError(null);
            if (pass.isEmpty())    { passwordLayout.setError("Password is required"); return; }
            passwordLayout.setError(null);
            if (pass.length() < 6) { passwordLayout.setError("Min 6 characters"); return; }
            passwordLayout.setError(null);
            if (confirm.isEmpty()) { confirmPasswordLayout.setError("Please confirm password"); return; }
            if (!pass.equals(confirm)) { confirmPasswordLayout.setError("Passwords do not match"); return; }
            confirmPasswordLayout.setError(null);

            showStep(2);

        } else {
            String name    = etName    != null && etName.getText()    != null ? etName.getText().toString().trim()    : "";
            String weight  = etWeight  != null && etWeight.getText()  != null ? etWeight.getText().toString().trim()  : "";
            String height  = etHeight  != null && etHeight.getText()  != null ? etHeight.getText().toString().trim()  : "";
            String disease = etDisease != null && etDisease.getText() != null ? etDisease.getText().toString().trim() : "None";
            String age     = etAge     != null && etAge.getText()     != null ? etAge.getText().toString().trim()     : "";
            String activity = selectedActivity;

            if (name.isEmpty())   { Toast.makeText(this, "Enter your name",   Toast.LENGTH_SHORT).show(); return; }
            if (weight.isEmpty()) { Toast.makeText(this, "Enter your weight", Toast.LENGTH_SHORT).show(); return; }
            if (height.isEmpty()) { Toast.makeText(this, "Enter your height", Toast.LENGTH_SHORT).show(); return; }
            if (age.isEmpty())    { Toast.makeText(this, "Enter your age",    Toast.LENGTH_SHORT).show(); return; }
            if (!termsCheckbox.isChecked()) { Toast.makeText(this, "Accept Terms first", Toast.LENGTH_SHORT).show(); return; }

            completeRegistration(name, weight, height, age,
                    disease.isEmpty() ? "None" : disease,
                    activity.isEmpty() ? "moderately_active" : activity);
        }
    }

    private void showStep(int s) {
        step = s;
        if (s == 1) {
            step1Layout.setVisibility(View.VISIBLE);
            step2Layout.setVisibility(View.GONE);
            termsCheckbox.setVisibility(View.GONE);
            btnRegister.setText("Next");
            requestText.setText("Step 1 of 2");
            stepDot1.setBackgroundColor(getColor(R.color.chocolate));
            stepDot2.setBackgroundColor(0xFFDDDDDD);
        } else {
            email_saved    = getText(etEmail);
            password_saved = getText(etPassword);
            step2Layout.setAlpha(0f);
            step1Layout.setVisibility(View.GONE);
            step2Layout.setVisibility(View.VISIBLE);
            termsCheckbox.setVisibility(View.VISIBLE);
            btnRegister.setText("Create Account");
            requestText.setText("Step 2 of 2");
            stepDot1.setBackgroundColor(0xFFDDDDDD);
            stepDot2.setBackgroundColor(getColor(R.color.chocolate));
            step2Layout.animate().alpha(1f).setDuration(300).start();
        }
    }

    private void completeRegistration(String name, String weight, String height, String age,
                                      String disease, String activity) {
        if (email_saved.isEmpty() || password_saved.isEmpty()) {
            Toast.makeText(this, "Please re-enter your email and password", Toast.LENGTH_LONG).show();
            showStep(1);
            return;
        }

        isProcessing = true;
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        btnSignIn.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email_saved, password_saved)
                .addOnCompleteListener(this, task -> {
                    isProcessing = false;
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveToFirestore(uid, name, weight, height, age, disease, activity);
                    } else {
                        btnRegister.setEnabled(true);
                        btnSignIn.setEnabled(true);
                        Exception ex = task.getException();
                        Toast.makeText(this, "Failed: " + (ex != null ? ex.getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveToFirestore(String uid, String name, String weight,
                                 String height, String age, String disease, String activity) {
        Map<String, Object> user = new HashMap<>();
        user.put("email",    email_saved);
        user.put("name",     name);
        user.put("weight",   weight);
        user.put("height",   height);
        user.put("disease",  disease);
        user.put("age",      age);
        user.put("activity", activity);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(a -> navigateToMain())
                .addOnFailureListener(e -> navigateToMain());
    }

    private void navigateToMain() {
        if (didNavigate) return;
        didNavigate = true;
        runOnUiThread(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String getText(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    @Override
    public void onBackPressed() {
        if (step == 2) showStep(1);
        else super.onBackPressed();
    }
}