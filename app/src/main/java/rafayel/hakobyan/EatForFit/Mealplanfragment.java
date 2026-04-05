package rafayel.hakobyan.EatForFit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MealPlanFragment extends Fragment {

    private static final String GROQ_API_KEY    = "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";
    private static final String GROQ_CHAT_MODEL = "llama-3.3-70b-versatile";
    private static final String GROQ_URL        = "https://api.groq.com/openai/v1/chat/completions";
    private static final String PREFS_NAME      = "food_scanner_prefs";

    private LinearLayout layoutBreakfast, layoutLunch, layoutDinner, layoutSnacks;
    private TextView tvBreakfastCal, tvLunchCal, tvDinnerCal, tvSnacksCal;
    private TextView tvGoalSummary, tvPlanDate, tvGoalType, tvTotalPlanCal;
    private TextView tvEmptyState;
    private MaterialButton btnGeneratePlan, btnRegeneratePlan;
    private ProgressBar progressBar;
    private CardView cardPlanContent;
    private LinearLayout layoutTips;
    private TextView tvTips;

    private String currentUserId = "default";
    private FirebaseFirestore db;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meal_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) currentUserId = fbUser.getUid();

        layoutBreakfast   = view.findViewById(R.id.layoutBreakfast);
        layoutLunch       = view.findViewById(R.id.layoutLunch);
        layoutDinner      = view.findViewById(R.id.layoutDinner);
        layoutSnacks      = view.findViewById(R.id.layoutSnacks);
        tvBreakfastCal    = view.findViewById(R.id.tvBreakfastCal);
        tvLunchCal        = view.findViewById(R.id.tvLunchCal);
        tvDinnerCal       = view.findViewById(R.id.tvDinnerCal);
        tvSnacksCal       = view.findViewById(R.id.tvSnacksCal);
        tvGoalSummary     = view.findViewById(R.id.tvGoalSummary);
        tvPlanDate        = view.findViewById(R.id.tvPlanDate);
        tvGoalType        = view.findViewById(R.id.tvGoalType);
        tvTotalPlanCal    = view.findViewById(R.id.tvTotalPlanCal);
        tvEmptyState      = view.findViewById(R.id.tvEmptyState);
        btnGeneratePlan   = view.findViewById(R.id.btnGeneratePlan);
        btnRegeneratePlan = view.findViewById(R.id.btnRegeneratePlan);
        progressBar       = view.findViewById(R.id.progressBar);
        cardPlanContent   = view.findViewById(R.id.cardPlanContent);
        layoutTips        = view.findViewById(R.id.layoutTips);
        tvTips            = view.findViewById(R.id.tvTips);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault());
        tvPlanDate.setText(sdf.format(new java.util.Date()));

        loadUserDataAndSetup();

        btnGeneratePlan.setOnClickListener(v -> generateMealPlan());
        btnRegeneratePlan.setOnClickListener(v -> generateMealPlan());
    }

    private void loadUserDataAndSetup() {
        if (db == null || currentUserId.equals("default")) {
            showEmptyState("Please log in to generate a personalized meal plan.");
            return;
        }
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc == null || !doc.exists()) {
                        showEmptyState("Complete your profile first to get a personalized plan.");
                        return;
                    }

                    String weight      = doc.getString("weight");
                    String goalWeightS = doc.getString("goalWeight");
                    String height      = doc.getString("height");
                    String ageS        = doc.getString("age");
                    String disease     = doc.getString("disease");
                    String activity    = doc.getString("activity");

                    SharedPreferences prefs = requireContext()
                            .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
                    int dailyGoal = prefs.getInt("daily_calorie_goal", 2000);

                    StringBuilder summary = new StringBuilder();
                    if (weight != null && !weight.isEmpty())      summary.append("⚖️ ").append(weight).append(" kg");
                    if (goalWeightS != null && !goalWeightS.isEmpty()) {
                        summary.append("  →  🎯 ").append(goalWeightS).append(" kg");
                        try {
                            float cw = weight != null ? Float.parseFloat(weight) : 0;
                            float gw = Float.parseFloat(goalWeightS);
                            if (gw < cw)       tvGoalType.setText("🔥 Lose Weight");
                            else if (gw > cw)  tvGoalType.setText("💪 Gain Weight");
                            else               tvGoalType.setText("⚖️ Maintain Weight");
                        } catch (Exception ignored) {}
                    }
                    tvGoalSummary.setText(summary.toString());
                    tvTotalPlanCal.setText("Daily target: " + dailyGoal + " kcal");

                    if (weight == null || weight.isEmpty() || goalWeightS == null || goalWeightS.isEmpty()) {
                        showEmptyState("Set your current weight and goal weight in Profile to get a personalized meal plan.");
                    } else {
                        showGenerateButton();
                    }
                })
                .addOnFailureListener(e -> showEmptyState("Could not load profile. Check your connection."));
    }

    private void generateMealPlan() {
        setLoading(true);

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    String weight      = doc != null ? doc.getString("weight")     : null;
                    String goalWeight  = doc != null ? doc.getString("goalWeight")  : null;
                    String height      = doc != null ? doc.getString("height")      : null;
                    String age         = doc != null ? doc.getString("age")         : null;
                    String disease     = doc != null ? doc.getString("disease")     : null;
                    String activity    = doc != null ? doc.getString("activity")    : null;

                    SharedPreferences prefs = requireContext()
                            .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
                    int dailyGoal = prefs.getInt("daily_calorie_goal", 2000);

                    String goalType = "maintain weight";
                    try {
                        float cw = weight != null ? Float.parseFloat(weight) : 0;
                        float gw = goalWeight != null ? Float.parseFloat(goalWeight) : 0;
                        if (gw < cw && gw > 0)  goalType = "lose weight";
                        else if (gw > cw)        goalType = "gain weight";
                    } catch (Exception ignored) {}

                    String activityLabel;
                    if (activity == null) activityLabel = "moderately active";
                    else switch (activity) {
                        case "sedentary":      activityLabel = "sedentary (desk job, no exercise)"; break;
                        case "lightly_active": activityLabel = "lightly active (1-3 days/week exercise)"; break;
                        case "very_active":    activityLabel = "very active (6-7 days/week exercise)"; break;
                        case "extra_active":   activityLabel = "extra active (athlete/physical job)"; break;
                        default:               activityLabel = "moderately active (3-5 days/week exercise)"; break;
                    }

                    final String prompt = buildPrompt(weight, goalWeight, height, age,
                            disease, activityLabel, goalType, dailyGoal);

                    new Thread(() -> {
                        try {
                            String result = callGroq(prompt);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (!isAdded()) return;
                                setLoading(false);
                                parsAndDisplayPlan(result, dailyGoal);
                            });
                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (!isAdded()) return;
                                setLoading(false);
                                showEmptyState("Failed to generate plan: " + e.getMessage());
                            });
                        }
                    }).start();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showEmptyState("Could not load profile.");
                });
    }

    private String buildPrompt(String weight, String goalWeight, String height,
                               String age, String disease, String activity,
                               String goalType, int dailyGoal) {
        StringBuilder p = new StringBuilder();
        p.append("You are a professional nutritionist and dietitian. Create a detailed, personalized daily meal plan.\n\n");
        p.append("USER PROFILE:\n");
        if (weight  != null && !weight.isEmpty())      p.append("- Current weight: ").append(weight).append(" kg\n");
        if (goalWeight != null && !goalWeight.isEmpty()) p.append("- Goal weight: ").append(goalWeight).append(" kg\n");
        if (height  != null && !height.isEmpty())      p.append("- Height: ").append(height).append(" cm\n");
        if (age     != null && !age.isEmpty())         p.append("- Age: ").append(age).append(" years\n");
        p.append("- Activity level: ").append(activity).append("\n");
        p.append("- Health goal: ").append(goalType).append("\n");
        p.append("- Daily calorie target: ").append(dailyGoal).append(" kcal\n");
        if (disease != null && !disease.isEmpty() && !disease.equalsIgnoreCase("none"))
            p.append("- Health condition: ").append(disease).append(" (MUST avoid unsafe foods for this condition)\n");
        p.append("\nCreate a full day meal plan split into breakfast, lunch, dinner, and snacks.\n");
        p.append("Distribute calories as: breakfast 25%, lunch 35%, dinner 30%, snacks 10%.\n\n");
        p.append("Reply ONLY with this exact JSON (no markdown, no extra text):\n");
        p.append("{\n");
        p.append("  \"breakfast\": {\n");
        p.append("    \"calories\": 500,\n");
        p.append("    \"items\": [\n");
        p.append("      {\"name\": \"Oatmeal with banana\", \"grams\": 300, \"calories\": 280, \"note\": \"Rich in fiber\"},\n");
        p.append("      {\"name\": \"Boiled eggs\", \"grams\": 100, \"calories\": 155, \"note\": \"High protein\"}\n");
        p.append("    ]\n");
        p.append("  },\n");
        p.append("  \"lunch\": {\n");
        p.append("    \"calories\": 700,\n");
        p.append("    \"items\": [\n");
        p.append("      {\"name\": \"Grilled chicken breast\", \"grams\": 200, \"calories\": 330, \"note\": \"Lean protein\"}\n");
        p.append("    ]\n");
        p.append("  },\n");
        p.append("  \"dinner\": {\n");
        p.append("    \"calories\": 600,\n");
        p.append("    \"items\": [\n");
        p.append("      {\"name\": \"Baked salmon\", \"grams\": 180, \"calories\": 360, \"note\": \"Omega-3 rich\"}\n");
        p.append("    ]\n");
        p.append("  },\n");
        p.append("  \"snacks\": {\n");
        p.append("    \"calories\": 200,\n");
        p.append("    \"items\": [\n");
        p.append("      {\"name\": \"Greek yogurt\", \"grams\": 150, \"calories\": 130, \"note\": \"Probiotic\"}\n");
        p.append("    ]\n");
        p.append("  },\n");
        p.append("  \"tips\": \"Drink 2-3 liters of water daily. Avoid processed foods. Space meals 3-4 hours apart.\"\n");
        p.append("}\n\n");
        p.append("Make all items realistic, specific, and delicious. Vary the foods. Match the goal strictly.");
        return p.toString();
    }

    private String callGroq(String prompt) throws Exception {
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        JSONArray messages = new JSONArray();
        messages.put(userMsg);
        JSONObject body = new JSONObject();
        body.put("model", GROQ_CHAT_MODEL);
        body.put("messages", messages);
        body.put("max_tokens", 2048);
        body.put("temperature", 0.7);

        RequestBody rb = RequestBody.create(body.toString(),
                MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(GROQ_URL)
                .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(rb)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) throw new Exception("Empty response");
            String responseStr = response.body().string();
            if (!response.isSuccessful()) throw new Exception("Groq error " + response.code());
            JSONObject json    = new JSONObject(responseStr);
            JSONArray  choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) throw new Exception("No response from AI");
            JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
            if (msg == null) throw new Exception("Malformed response");
            return msg.optString("content", "");
        }
    }

    private void parsAndDisplayPlan(String raw, int dailyGoal) {
        try {
            String cleaned = raw.replaceAll("```json", "").replaceAll("```", "").trim();
            int start = cleaned.indexOf('{'), end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) cleaned = cleaned.substring(start, end + 1);

            JSONObject plan = new JSONObject(cleaned);

            int bfCal = renderMealSection(layoutBreakfast, plan.optJSONObject("breakfast"), tvBreakfastCal, "🌅");
            int lCal  = renderMealSection(layoutLunch,     plan.optJSONObject("lunch"),     tvLunchCal,     "☀️");
            int dCal  = renderMealSection(layoutDinner,    plan.optJSONObject("dinner"),    tvDinnerCal,    "🌙");
            int sCal  = renderMealSection(layoutSnacks,    plan.optJSONObject("snacks"),    tvSnacksCal,    "🍎");

            int totalPlan = bfCal + lCal + dCal + sCal;
            tvTotalPlanCal.setText("Plan total: " + totalPlan + " kcal  ·  Target: " + dailyGoal + " kcal");

            String tips = plan.optString("tips", "");
            if (!tips.isEmpty()) {
                tvTips.setText(tips);
                layoutTips.setVisibility(View.VISIBLE);
            }

            tvEmptyState.setVisibility(View.GONE);
            cardPlanContent.setVisibility(View.VISIBLE);
            btnRegeneratePlan.setVisibility(View.VISIBLE);
            btnGeneratePlan.setVisibility(View.GONE);

        } catch (Exception e) {
            showEmptyState("Could not parse meal plan. Please try again.");
        }
    }

    private int renderMealSection(LinearLayout container, JSONObject meal,
                                  TextView calLabel, String timeEmoji) {
        container.removeAllViews();
        if (meal == null) return 0;

        int mealCal = meal.optInt("calories", 0);
        calLabel.setText(mealCal + " kcal");

        JSONArray items = meal.optJSONArray("items");
        if (items == null) return mealCal;

        for (int i = 0; i < items.length(); i++) {
            try {
                JSONObject item = items.getJSONObject(i);
                String itemName = item.optString("name", "");
                int    itemCal  = item.optInt("calories", 0);
                int    itemGrams = item.optInt("grams", 0);
                String itemNote = item.optString("note", "");

                View row = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_meal_row, container, false);

                TextView tvName  = row.findViewById(R.id.tvMealItemName);
                TextView tvCal   = row.findViewById(R.id.tvMealItemCal);
                TextView tvGrams = row.findViewById(R.id.tvMealItemGrams);
                TextView tvNote  = row.findViewById(R.id.tvMealItemNote);

                tvName.setText(itemName);
                tvCal.setText(itemCal + " kcal");
                tvGrams.setText(itemGrams + "g");
                if (!itemNote.isEmpty()) {
                    tvNote.setText(itemNote);
                    tvNote.setVisibility(View.VISIBLE);
                } else {
                    tvNote.setVisibility(View.GONE);
                }

                container.addView(row);

                if (i < items.length() - 1) {
                    View divider = new View(requireContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    lp.setMargins(0, dpToPx(6), 0, dpToPx(6));
                    divider.setLayoutParams(lp);
                    divider.setBackgroundColor(0xFFF0E6D6);
                    container.addView(divider);
                }
            } catch (Exception ignored) {}
        }
        return mealCal;
    }

    private void showEmptyState(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
        cardPlanContent.setVisibility(View.GONE);
        btnGeneratePlan.setVisibility(View.VISIBLE);
        btnRegeneratePlan.setVisibility(View.GONE);
        layoutTips.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void showGenerateButton() {
        tvEmptyState.setVisibility(View.GONE);
        cardPlanContent.setVisibility(View.GONE);
        btnGeneratePlan.setVisibility(View.VISIBLE);
        btnRegeneratePlan.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGeneratePlan.setEnabled(!loading);
        btnRegeneratePlan.setEnabled(!loading);
        btnGeneratePlan.setText(loading ? "Generating..." : "✨  Generate My Meal Plan");
        btnRegeneratePlan.setText(loading ? "Generating..." : "🔄  Regenerate Plan");
        if (loading) {
            tvEmptyState.setVisibility(View.GONE);
            cardPlanContent.setVisibility(View.GONE);
            layoutTips.setVisibility(View.GONE);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}