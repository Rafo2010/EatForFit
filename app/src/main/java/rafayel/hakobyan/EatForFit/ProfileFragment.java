package rafayel.hakobyan.EatForFit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private TextView       profileName, profileEmail, profileWeight, profileHeight, profileDisease, profileAge;
    private TextView       tvCurrentGoal, tvGoalWeight, tvProgressStatus, tvGoalType;
    private MaterialButton btnLogout, btnUpdateWeight, btnUpdateHeight;
    private android.widget.NumberPicker goalWeightPicker;
    private MaterialButton btnWeightMinus, btnWeightPlus;
    private TextInputEditText etNewWeight, etNewHeight, etNewAge;
    private TextInputLayout   tilNewWeight, tilNewHeight, tilNewAge;

    private FirebaseFirestore db;
    private String userId;
    private float currentWeight   = 0f;
    private float goalWeight      = 0f;
    private float currentHeight   = 0f;
    private int   currentAge      = 0;
    private String currentActivity = "moderately_active";
    private int   dailyCalorieGoal = 2000;

    private static final float  WEIGHT_STEP = 1f;
    private static final String PREFS_NAME  = "food_scanner_prefs";
    private static final String KEY_DAILY   = "daily_calorie_goal";

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

        profileName      = view.findViewById(R.id.profileName);
        profileEmail     = view.findViewById(R.id.profileEmail);
        profileWeight    = view.findViewById(R.id.profileWeight);
        profileHeight    = view.findViewById(R.id.profileHeight);
        profileDisease   = view.findViewById(R.id.profileDisease);
        profileAge       = view.findViewById(R.id.profileAge);
        tvCurrentGoal    = view.findViewById(R.id.tvCurrentGoal);
        tvGoalWeight     = view.findViewById(R.id.tvGoalWeight);
        tvProgressStatus = view.findViewById(R.id.tvProgressStatus);
        btnLogout        = view.findViewById(R.id.btnLogout);
        tvGoalType       = view.findViewById(R.id.tvGoalType);
        goalWeightPicker = view.findViewById(R.id.goalWeightPicker);
        btnUpdateWeight  = view.findViewById(R.id.btnUpdateWeight);
        btnUpdateHeight  = view.findViewById(R.id.btnUpdateHeight);
        btnWeightMinus   = view.findViewById(R.id.btnWeightMinus);
        btnWeightPlus    = view.findViewById(R.id.btnWeightPlus);
        etNewWeight      = view.findViewById(R.id.etNewWeight);
        etNewHeight      = view.findViewById(R.id.etNewHeight);
        etNewAge         = view.findViewById(R.id.etNewAge);

        tilNewWeight = (TextInputLayout) etNewWeight.getParent().getParent();
        tilNewHeight = (TextInputLayout) etNewHeight.getParent().getParent();
        tilNewAge    = (TextInputLayout) etNewAge.getParent().getParent();

        db = FirebaseFirestore.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;
        userId = firebaseUser.getUid();

        profileEmail.setText(firebaseUser.getEmail());

        startRealtimeListener();

        btnLogout.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Log Out", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            requireContext().getSharedPreferences(
                                            PREFS_NAME + "_" + userId, Context.MODE_PRIVATE)
                                    .edit().clear().apply();
                            Intent intent = new Intent(requireActivity(), Register.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show()
        );

        goalWeightPicker.setMinValue(1);
        goalWeightPicker.setMaxValue(200);
        goalWeightPicker.setValue(70);
        goalWeightPicker.setWrapSelectorWheel(false);
        goalWeightPicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                saveGoalWeightAndAutoDetect(newVal));

        btnWeightMinus.setOnClickListener(v -> {
            float current = parseFieldValue(etNewWeight, currentWeight);
            float newVal  = Math.max(0f, current - WEIGHT_STEP);
            etNewWeight.setText(formatWeight(newVal));
            etNewWeight.setSelection(etNewWeight.getText().length());
        });

        btnWeightPlus.setOnClickListener(v -> {
            float current = parseFieldValue(etNewWeight, currentWeight);
            float newVal  = current + WEIGHT_STEP;
            etNewWeight.setText(formatWeight(newVal));
            etNewWeight.setSelection(etNewWeight.getText().length());
        });

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
            db.collection("users").document(userId).set(data, SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        clearField(etNewWeight, tilNewWeight);
                        Toast.makeText(requireContext(), "Weight updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

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
            db.collection("users").document(userId).set(data, SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        clearField(etNewHeight, tilNewHeight);
                        Toast.makeText(requireContext(), "Height updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        MaterialButton btnUpdateAge = view.findViewById(R.id.btnUpdateAge);
        if (btnUpdateAge != null) {
            btnUpdateAge.setOnClickListener(v -> {
                String val = etNewAge.getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter age first", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    int age = Integer.parseInt(val);
                    if (age < 1 || age > 120) {
                        Toast.makeText(requireContext(), "Invalid age", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid age", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, Object> data = new HashMap<>();
                data.put("age", val);
                db.collection("users").document(userId).set(data, SetOptions.merge())
                        .addOnSuccessListener(a -> {
                            clearField(etNewAge, tilNewAge);
                            Toast.makeText(requireContext(), "Age updated!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }
    }

    // onResume: no loadTodayCalories() call — calories only shown on Home tab
    @Override
    public void onResume() {
        super.onResume();
    }

    private int calculateDailyCalories(float weightKg, float heightCm, int age, float goalWt) {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0) return 2000;
        float bmr = (10f * weightKg) + (6.25f * heightCm) - (5f * age) + 5f;
        float multiplier;
        switch (currentActivity) {
            case "sedentary":      multiplier = 1.2f;   break;
            case "lightly_active": multiplier = 1.375f; break;
            case "very_active":    multiplier = 1.725f; break;
            case "extra_active":   multiplier = 1.9f;   break;
            default:               multiplier = 1.55f;  break;
        }
        float tdee = bmr * multiplier;
        if (goalWt > 0 && goalWt < weightKg)  tdee -= 500f;
        else if (goalWt > weightKg)            tdee += 300f;
        return Math.max(1200, Math.round(tdee));
    }

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
                    String goalWeightStr = document.getString("goalWeight");
                    String ageStr        = document.getString("age");
                    String activityStr   = document.getString("activity");

                    profileName.setText(name != null && !name.isEmpty() ? name : "No name set");
                    profileWeight.setText(weight != null && !weight.isEmpty() ? weight + " kg" : "— kg");
                    profileHeight.setText(height != null && !height.isEmpty() ? height + " cm" : "— cm");
                    profileDisease.setText(disease != null && !disease.isEmpty() ? disease : "None");
                    if (profileAge != null)
                        profileAge.setText(ageStr != null && !ageStr.isEmpty() ? ageStr + " y" : "— y");

                    if (goalWeightStr != null && !goalWeightStr.isEmpty()) {
                        try {
                            int gw = (int) Float.parseFloat(goalWeightStr);
                            gw = Math.max(1, Math.min(200, gw));
                            goalWeightPicker.setValue(gw);
                            goalWeight = gw;
                        } catch (Exception ignored) {}
                        tvGoalWeight.setText("Goal: " + goalWeightStr + " kg");
                    } else {
                        tvGoalWeight.setText("Goal weight: not set");
                        goalWeight = 0f;
                    }

                    if (weight != null && !weight.isEmpty()) {
                        try { currentWeight = Float.parseFloat(weight); } catch (Exception ignored) {}
                    } else { currentWeight = 0f; }

                    if (height != null && !height.isEmpty()) {
                        try { currentHeight = Float.parseFloat(height); } catch (Exception ignored) {}
                    } else { currentHeight = 0f; }

                    if (ageStr != null && !ageStr.isEmpty()) {
                        try { currentAge = Integer.parseInt(ageStr); } catch (Exception ignored) {}
                    } else { currentAge = 0; }

                    if (activityStr != null && !activityStr.isEmpty()) {
                        currentActivity = activityStr;
                    }

                    dailyCalorieGoal = calculateDailyCalories(currentWeight, currentHeight, currentAge, goalWeight);

                    // Save calorie goal to prefs so HomeFragment can read it
                    SharedPreferences prefs = requireContext()
                            .getSharedPreferences(PREFS_NAME + "_" + userId, Context.MODE_PRIVATE);
                    prefs.edit()
                            .putString("current_weight", weight != null ? weight : "")
                            .putString("goal_weight", goalWeightStr != null ? goalWeightStr : "")
                            .putString("activity", currentActivity)
                            .putString("disease", disease != null ? disease : "None")
                            .putInt(KEY_DAILY, dailyCalorieGoal)
                            .apply();

                    updateGoalTypeBadge();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) { userListener.remove(); userListener = null; }
    }

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

    private void saveGoalWeightAndAutoDetect(int pickerValue) {
        goalWeight = pickerValue;
        String autoGoal = detectGoalType(currentWeight, goalWeight);
        Map<String, Object> data = new HashMap<>();
        data.put("goalWeight", String.valueOf(pickerValue));
        data.put("goal", autoGoal);
        db.collection("users").document(userId).set(data, SetOptions.merge())
                .addOnSuccessListener(a -> updateGoalTypeBadge())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String detectGoalType(float current, float goal) {
        if (current == 0f || goal == 0f) return "No goal";
        if (goal < current) return "Lose Weight 🔥";
        if (goal > current) return "Gain Weight 💪";
        return "Maintain Weight ⚖️";
    }

    private void updateGoalTypeBadge() {
        if (tvGoalType == null) return;
        String type = detectGoalType(currentWeight, goalWeight);
        tvCurrentGoal.setText("Goal: " + (int) goalWeight + " kg");
        if (type.contains("Lose")) {
            tvGoalType.setText("🔥 Lose Weight");
            tvGoalType.setTextColor(0xFFFF7043);
        } else if (type.contains("Gain")) {
            tvGoalType.setText("💪 Gain Weight");
            tvGoalType.setTextColor(0xFF42A5F5);
        } else if (type.contains("Maintain")) {
            tvGoalType.setText("⚖️ Maintain Weight");
            tvGoalType.setTextColor(0xFF4CAF50);
        } else {
            tvGoalType.setText("Set a goal weight");
            tvGoalType.setTextColor(0xFF888888);
        }
        updateProgressStatus();
    }

    private float parseFieldValue(TextInputEditText et, float fallback) {
        try {
            String s = et.getText().toString().trim();
            return s.isEmpty() ? fallback : Float.parseFloat(s);
        } catch (NumberFormatException e) { return fallback; }
    }

    private String formatWeight(float value) {
        if (value == Math.floor(value) && !Float.isInfinite(value)) return String.valueOf((int) value);
        return String.format("%.1f", value);
    }

    private void clearField(TextInputEditText editText, TextInputLayout layout) {
        editText.setText("");
        editText.clearFocus();
        layout.setError(null);
        layout.clearFocus();
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}