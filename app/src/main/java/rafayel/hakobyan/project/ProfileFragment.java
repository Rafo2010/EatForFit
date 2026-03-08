package rafayel.hakobyan.project;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView profileName, profileEmail, profileWeight, profileHeight, profileDisease;
    private TextView tvCurrentGoal, tvGoalWeight, tvProgressStatus;
    private MaterialButton btnLogout, btnLoseWeight, btnGainWeight;
    private MaterialButton btnUpdateWeight, btnUpdateHeight, btnSetGoalWeight;
    private MaterialButton btnWeightMinus, btnWeightPlus;
    private TextInputEditText etNewWeight, etNewHeight, etGoalWeight;
    private TextInputLayout tilNewWeight, tilNewHeight, tilGoalWeight;

    private FirebaseFirestore db;
    private String userId;
    private float currentWeight = 0f;
    private float goalWeight    = 0f;

    private static final float WEIGHT_STEP = 1f;

    private ListenerRegistration userListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views
        profileName      = view.findViewById(R.id.profileName);
        profileEmail     = view.findViewById(R.id.profileEmail);
        profileWeight    = view.findViewById(R.id.profileWeight);
        profileHeight    = view.findViewById(R.id.profileHeight);
        profileDisease   = view.findViewById(R.id.profileDisease);
        tvCurrentGoal    = view.findViewById(R.id.tvCurrentGoal);
        tvGoalWeight     = view.findViewById(R.id.tvGoalWeight);
        tvProgressStatus = view.findViewById(R.id.tvProgressStatus);
        btnLogout        = view.findViewById(R.id.btnLogout);
        btnLoseWeight    = view.findViewById(R.id.btnLoseWeight);
        btnGainWeight    = view.findViewById(R.id.btnGainWeight);
        btnUpdateWeight  = view.findViewById(R.id.btnUpdateWeight);
        btnUpdateHeight  = view.findViewById(R.id.btnUpdateHeight);
        btnSetGoalWeight = view.findViewById(R.id.btnSetGoalWeight);
        btnWeightMinus   = view.findViewById(R.id.btnWeightMinus);
        btnWeightPlus    = view.findViewById(R.id.btnWeightPlus);
        etNewWeight      = view.findViewById(R.id.etNewWeight);
        etNewHeight      = view.findViewById(R.id.etNewHeight);
        etGoalWeight     = view.findViewById(R.id.etGoalWeight);

        // Grab TextInputLayout wrappers for proper clearing
        tilNewWeight  = (TextInputLayout) etNewWeight.getParent().getParent();
        tilNewHeight  = (TextInputLayout) etNewHeight.getParent().getParent();
        tilGoalWeight = (TextInputLayout) etGoalWeight.getParent().getParent();

        // Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;
        userId = firebaseUser.getUid();

        profileEmail.setText(firebaseUser.getEmail());

        // Start real-time listener — keeps UI always in sync with Firestore
        startRealtimeListener();

        // ── Logout ──
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireActivity(), Register.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // ── Goal type buttons ──
        btnLoseWeight.setOnClickListener(v -> updateGoal("Lose Weight 🔥"));
        btnGainWeight.setOnClickListener(v -> updateGoal("Gain Weight 💪"));

        // ── Weight MINUS: subtract 1 from field value, seed from saved weight if field is empty ──
        btnWeightMinus.setOnClickListener(v -> {
            float current = parseFieldValue(etNewWeight, currentWeight);
            float newVal  = Math.max(0f, current - WEIGHT_STEP);
            etNewWeight.setText(formatWeight(newVal));
            etNewWeight.setSelection(etNewWeight.getText().length());
        });

        // ── Weight PLUS: add 1 to field value, seed from saved weight if field is empty ──
        btnWeightPlus.setOnClickListener(v -> {
            float current = parseFieldValue(etNewWeight, currentWeight);
            float newVal  = current + WEIGHT_STEP;
            etNewWeight.setText(formatWeight(newVal));
            etNewWeight.setSelection(etNewWeight.getText().length());
        });

        // ── Save weight ──
        btnUpdateWeight.setOnClickListener(v -> {
            String val = etNewWeight.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(requireContext(), "Enter weight first", Toast.LENGTH_SHORT).show();
                return;
            }
            try { Float.parseFloat(val); } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid weight value", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("weight", val);
            db.collection("users").document(userId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        clearField(etNewWeight, tilNewWeight);
                        Toast.makeText(requireContext(), "Weight updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // ── Save height ──
        btnUpdateHeight.setOnClickListener(v -> {
            String val = etNewHeight.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(requireContext(), "Enter height first", Toast.LENGTH_SHORT).show();
                return;
            }
            try { Float.parseFloat(val); } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid height value", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("height", val);
            db.collection("users").document(userId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        clearField(etNewHeight, tilNewHeight);
                        Toast.makeText(requireContext(), "Height updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // ── Save goal weight ──
        btnSetGoalWeight.setOnClickListener(v -> {
            String val = etGoalWeight.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(requireContext(), "Enter goal weight first", Toast.LENGTH_SHORT).show();
                return;
            }
            try { Float.parseFloat(val); } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid goal weight value", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("goalWeight", val);
            db.collection("users").document(userId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        clearField(etGoalWeight, tilGoalWeight);
                        Toast.makeText(requireContext(), "Goal weight set!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // ── Real-time Firestore listener ──
    // Fires immediately on attach (loads data) AND every time the document changes.
    // This means the UI is always in sync when navigating back to this fragment.
    private void startRealtimeListener() {
        if (userId == null) return;

        userListener = db.collection("users").document(userId)
                .addSnapshotListener((document, error) -> {
                    if (error != null || document == null || !document.exists()) return;
                    if (!isAdded()) return;

                    String name          = document.getString("name");
                    String weight        = document.getString("weight");
                    String height        = document.getString("height");
                    String disease       = document.getString("disease");
                    String goal          = document.getString("goal");
                    String goalWeightStr = document.getString("goalWeight");

                    profileName.setText(name != null && !name.isEmpty() ? name : "No name set");
                    profileWeight.setText(weight != null && !weight.isEmpty() ? weight + " kg" : "— kg");
                    profileHeight.setText(height != null && !height.isEmpty() ? height + " cm" : "— cm");
                    profileDisease.setText(disease != null && !disease.isEmpty() ? disease : "None");
                    tvCurrentGoal.setText(goal != null && !goal.isEmpty()
                            ? "Current goal: " + goal : "No goal selected");

                    if (goalWeightStr != null && !goalWeightStr.isEmpty()) {
                        tvGoalWeight.setText("Goal: " + goalWeightStr + " kg");
                        try { goalWeight = Float.parseFloat(goalWeightStr); } catch (Exception ignored) {}
                    } else {
                        tvGoalWeight.setText("Goal weight: not set");
                        goalWeight = 0f;
                    }

                    if (weight != null && !weight.isEmpty()) {
                        try { currentWeight = Float.parseFloat(weight); } catch (Exception ignored) {}
                    } else {
                        currentWeight = 0f;
                    }

                    updateProgressStatus();
                });
    }

    // ── Remove listener when fragment view is destroyed to prevent memory leaks ──
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    // ── Progress status label ──
    private void updateProgressStatus() {
        if (currentWeight == 0f || goalWeight == 0f) {
            tvProgressStatus.setText("Set your goal weight to track progress");
            tvProgressStatus.setTextColor(0xFF888888);
            return;
        }
        float diff = currentWeight - goalWeight;
        if (Math.abs(diff) < 0.5f) {
            tvProgressStatus.setText("You reached your goal weight! 🎉");
            tvProgressStatus.setTextColor(0xFF4CAF50);
        } else if (diff > 0) {
            tvProgressStatus.setText(String.format("%.1f kg to lose to reach your goal", diff));
            tvProgressStatus.setTextColor(0xFFFF7043);
        } else {
            tvProgressStatus.setText(String.format("%.1f kg to gain to reach your goal", Math.abs(diff)));
            tvProgressStatus.setTextColor(0xFF42A5F5);
        }
    }

    // ── Save goal type to Firestore ──
    private void updateGoal(String goal) {
        Map<String, Object> data = new HashMap<>();
        data.put("goal", goal);
        db.collection("users").document(userId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(a ->
                        Toast.makeText(requireContext(),
                                "Goal set to: " + goal, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Parse float from EditText, return fallback on empty or error ──
    private float parseFieldValue(TextInputEditText et, float fallback) {
        try {
            String s = et.getText().toString().trim();
            return s.isEmpty() ? fallback : Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ── Format float: no decimal for whole numbers, 1 decimal otherwise ──
    private String formatWeight(float value) {
        if (value == Math.floor(value) && !Float.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    // ── Clear input field, collapse hint, hide keyboard ──
    private void clearField(TextInputEditText editText, TextInputLayout layout) {
        editText.setText("");
        editText.clearFocus();
        layout.setError(null);
        layout.clearFocus();
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }
}