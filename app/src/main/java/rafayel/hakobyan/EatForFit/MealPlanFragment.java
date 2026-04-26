package rafayel.hakobyan.EatForFit;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private static final String KEY_HISTORY     = "food_history";
    private static final String KEY_MEAL_PLAN   = "meal_plan_cache";
    private static final String KEY_PLAN_DATE   = "meal_plan_date";
    private static final String KEY_WATER       = "water_glasses_today";
    private static final String KEY_WATER_DATE  = "water_date";
    private static final String KEY_WATER_GOAL  = "water_goal_cups";

    private String currentUserId = "default";

    private LinearLayout layoutTimeline, layoutLoading, layoutError;
    private TextView     tvErrorMsg, tvPlanDate, tvDailyGoalSummary;
    private LinearLayout timelineSlots;
    private MaterialButton btnRetry, btnRegenerate;

    // Water tracker views
    private LinearLayout layoutWaterGlasses;
    private TextView     tvWaterCount;
    private TextView     tvWaterCupCount;
    private TextView     tvWaterGoalBadge;
    private TextView     tvWaterHint;
    private ProgressBar  waterProgressBar;
    private TextView     btnWaterPlus;
    private TextView     btnWaterMinus;
    private int          waterCups = 0;
    private int          waterGoal = 8;

    private MealSlotData breakfast, lunch, dinner, snacks;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    public static class ArcProgressView extends View {
        private final Paint bgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float fraction = 0f;
        private int   arcColor = 0xFFCC5803;

        public ArcProgressView(Context c) { super(c); init(); }
        public ArcProgressView(Context c, AttributeSet a) { super(c, a); init(); }

        private void init() {
            bgPaint.setStyle(Paint.Style.STROKE);
            bgPaint.setStrokeWidth(10f);
            bgPaint.setColor(0xFFEEE8E0);
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeWidth(10f);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        public void setProgress(float fraction, int color) {
            this.fraction = Math.min(1f, Math.max(0f, fraction));
            this.arcColor = color;
            arcPaint.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float r  = Math.min(cx, cy) - 8f;
            RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
            canvas.drawArc(oval, 135f, 270f, false, bgPaint);
            if (fraction > 0)
                canvas.drawArc(oval, 135f, 270f * fraction, false, arcPaint);
        }
    }

    static class MealSlotData {
        String slot, emoji, timeLabel, tip;
        String[] suggestions = new String[0];
        int targetCalories, targetProtein, targetCarbs, targetFats;
        int loggedCalories, loggedProtein, loggedCarbs, loggedFats;
        boolean expanded = false;

        MealSlotData(String slot, String emoji, String timeLabel) {
            this.slot      = slot;
            this.emoji     = emoji;
            this.timeLabel = timeLabel;
        }

        float progressPct() {
            if (targetCalories <= 0) return 0f;
            return Math.min(1f, (float) loggedCalories / targetCalories);
        }

        boolean isComplete() { return targetCalories > 0 && loggedCalories >= targetCalories; }

        String statusText() {
            if (targetCalories <= 0) return "No target set";
            int rem = targetCalories - loggedCalories;
            if (rem <= 0) return "✅ Complete!";
            return rem + " kcal left";
        }

        int arcColor() {
            if (isComplete()) return 0xFF4CAF50;
            float p = progressPct();
            if (p > 0.75f) return 0xFFFF9505;
            return 0xFFCC5803;
        }
    }

    public MealPlanFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mealplan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutTimeline     = view.findViewById(R.id.layoutTimeline);
        layoutLoading      = view.findViewById(R.id.layoutMealPlanLoading);
        layoutError        = view.findViewById(R.id.layoutMealPlanError);
        tvErrorMsg         = view.findViewById(R.id.tvMealPlanError);
        btnRetry           = view.findViewById(R.id.btnMealPlanRetry);
        btnRegenerate      = view.findViewById(R.id.btnRegeneratePlan);
        tvPlanDate         = view.findViewById(R.id.tvPlanDate);
        tvDailyGoalSummary = view.findViewById(R.id.tvDailyGoalSummary);
        timelineSlots      = view.findViewById(R.id.timelineSlots);

        // Water tracker views
        layoutWaterGlasses = view.findViewById(R.id.layoutWaterGlasses);
        tvWaterCount       = view.findViewById(R.id.tvWaterCount);
        tvWaterCupCount    = view.findViewById(R.id.tvWaterCupCount);
        tvWaterGoalBadge   = view.findViewById(R.id.tvWaterGoalBadge);
        tvWaterHint        = view.findViewById(R.id.tvWaterHint);
        waterProgressBar   = view.findViewById(R.id.waterProgressBar);
        btnWaterPlus       = view.findViewById(R.id.btnWaterPlus);
        btnWaterMinus      = view.findViewById(R.id.btnWaterMinus);

        com.google.firebase.auth.FirebaseUser fbUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) currentUserId = fbUser.getUid();

        btnRetry.setOnClickListener(v -> generateNewPlan());
        btnRegenerate.setOnClickListener(v -> generateNewPlan());

        if (btnWaterPlus != null) {
            btnWaterPlus.setOnClickListener(v -> {
                waterCups = Math.min(waterCups + 1, waterGoal + 4);
                saveWater();
                renderWaterTracker();
            });
        }
        if (btnWaterMinus != null) {
            btnWaterMinus.setOnClickListener(v -> {
                waterCups = Math.max(0, waterCups - 1);
                saveWater();
                renderWaterTracker();
            });
        }

        breakfast = new MealSlotData("Breakfast", "🌅", "7 – 9 AM");
        lunch     = new MealSlotData("Lunch",     "☀️", "12 – 2 PM");
        dinner    = new MealSlotData("Dinner",    "🌙", "6 – 8 PM");
        snacks    = new MealSlotData("Snacks",    "🍎", "Anytime");

        loadWater();
        renderWaterTracker();
        loadMealPlan();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (breakfast != null && breakfast.targetCalories > 0) {
            loadLoggedTotals();
            renderTimeline();
        }
        loadWater();
        renderWaterTracker();
    }

    // -------------------------------------------------------------------------
    // Water Tracker
    // -------------------------------------------------------------------------

    private void loadWater() {
        if (!isAdded()) return;
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String saved = prefs.getString(KEY_WATER_DATE, "");
        waterCups = today.equals(saved) ? prefs.getInt(KEY_WATER, 0) : 0;
        waterGoal = prefs.getInt(KEY_WATER_GOAL, 8);
        calculateAndSaveWaterGoal(prefs);
    }

    /**
     * AI-based personalised water goal.
     * Formula: weight_kg x 0.033 x activity_multiplier = liters/day
     * 1 cup = 237 ml. Result clamped to [6, 16] cups.
     */
    private void calculateAndSaveWaterGoal(SharedPreferences prefs) {
        String weightStr = prefs.getString("current_weight", "");
        String activity  = prefs.getString("activity", "moderately_active");
        if (weightStr.isEmpty()) return;

        try {
            float weightKg = Float.parseFloat(weightStr);

            float multiplier;
            switch (activity) {
                case "sedentary":         multiplier = 1.0f; break;
                case "lightly_active":    multiplier = 1.1f; break;
                case "moderately_active": multiplier = 1.2f; break;
                case "very_active":       multiplier = 1.3f; break;
                case "extra_active":      multiplier = 1.4f; break;
                default:                  multiplier = 1.2f; break;
            }

            float liters = weightKg * 0.033f * multiplier;
            int cups = Math.round(liters / 0.237f);
            cups = Math.max(6, Math.min(cups, 16));

            waterGoal = cups;
            prefs.edit().putInt(KEY_WATER_GOAL, cups).apply();

            final int finalCups = cups;
            final float finalMult = multiplier;
            final float finalWeight = weightKg;
            final String finalActivity = activity;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                if (tvWaterGoalBadge != null)
                    tvWaterGoalBadge.setText("Goal: " + finalCups + " cups");
                if (tvWaterHint != null) {
                    String actLabel = finalActivity.replace("_", " ");
                    tvWaterHint.setText(String.format(Locale.getDefault(),
                            "Based on %.0f kg × 0.033 × %.1f (%s) = %d cups/day",
                            finalWeight, finalMult, actLabel, finalCups));
                }
                updateWaterLabel();
            });

        } catch (Exception ignored) {}
    }

    private void saveWater() {
        if (!isAdded()) return;
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        requireContext().getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_WATER, waterCups)
                .putString(KEY_WATER_DATE, today)
                .apply();
    }

    private void renderWaterTracker() {
        if (!isAdded() || layoutWaterGlasses == null) return;
        layoutWaterGlasses.removeAllViews();

        int displayCount = Math.min(waterGoal, 12);
        for (int i = 0; i < displayCount; i++) {
            TextView cup = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            cup.setLayoutParams(lp);
            cup.setText(i < waterCups ? "💧" : "🫙");
            cup.setTextSize(16f);
            cup.setGravity(Gravity.CENTER);
            layoutWaterGlasses.addView(cup);
        }
        if (waterGoal > 12) {
            TextView more = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_VERTICAL;
            more.setLayoutParams(lp);
            more.setText("+" + (waterGoal - 12));
            more.setTextSize(11f);
            more.setTextColor(0xFF2196F3);
            more.setGravity(Gravity.CENTER);
            layoutWaterGlasses.addView(more);
        }

        updateWaterLabel();
    }

    private void updateWaterLabel() {
        if (tvWaterCount == null) return;

        if (tvWaterGoalBadge != null)
            tvWaterGoalBadge.setText("Goal: " + waterGoal + " cups");

        if (tvWaterCupCount != null)
            tvWaterCupCount.setText(String.valueOf(waterCups));

        if (waterProgressBar != null) {
            int pct = waterGoal > 0 ? (int) ((waterCups * 100f) / waterGoal) : 0;
            waterProgressBar.setProgress(Math.min(pct, 100));
        }

        if (waterCups >= waterGoal) {
            tvWaterCount.setText("🎉 Goal reached! " + waterCups + "/" + waterGoal + " cups");
            tvWaterCount.setTextColor(0xFF4CAF50);
        } else {
            tvWaterCount.setText(waterCups + " / " + waterGoal + " cups today");
            tvWaterCount.setTextColor(0xFF2196F3);
        }
    }

    // -------------------------------------------------------------------------
    // Meal Plan
    // -------------------------------------------------------------------------

    private void loadMealPlan() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
        String today     = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString(KEY_PLAN_DATE, "");
        String savedPlan = prefs.getString(KEY_MEAL_PLAN, "");

        if (today.equals(savedDate) && !savedPlan.isEmpty()) {
            try {
                applyPlanJson(new JSONObject(savedPlan));
                loadLoggedTotals();
                showTimeline();
                return;
            } catch (Exception ignored) {}
        }
        generateNewPlan();
    }

    private void generateNewPlan() {
        showLoading();
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);

        int    dailyGoal     = prefs.getInt("daily_calorie_goal", 2000);
        String currentWeight = prefs.getString("current_weight", "");
        String goalWeight    = prefs.getString("goal_weight", "");
        String activity      = prefs.getString("activity", "moderately_active");
        String disease       = prefs.getString("disease", "None");

        String goalType;
        try {
            float cw = Float.parseFloat(currentWeight);
            float gw = Float.parseFloat(goalWeight);
            goalType = gw < cw ? "lose weight" : (gw > cw ? "gain weight" : "maintain weight");
        } catch (Exception e) { goalType = "maintain weight"; }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional nutritionist. Create a daily macro target split.\n\n");
        prompt.append("User profile:\n");
        if (!currentWeight.isEmpty()) prompt.append("- Current weight: ").append(currentWeight).append(" kg\n");
        if (!goalWeight.isEmpty())    prompt.append("- Goal weight: ").append(goalWeight).append(" kg\n");
        prompt.append("- Goal: ").append(goalType).append("\n");
        prompt.append("- Activity: ").append(activity).append("\n");
        if (!disease.equals("None") && !disease.isEmpty())
            prompt.append("- Health condition: ").append(disease).append("\n");
        prompt.append("- Daily calorie goal: ").append(dailyGoal).append(" kcal\n\n");
        prompt.append("Split ").append(dailyGoal).append(" kcal into 4 meal slots.\n");
        prompt.append("For each slot: macro targets + a short helpful tip + 3 food suggestions (short names only).\n");
        prompt.append("Reply ONLY with this exact JSON (no markdown):\n");
        prompt.append("{");
        prompt.append("\"breakfast\":{\"calories\":0,\"protein\":0,\"carbs\":0,\"fats\":0,\"tip\":\"...\",\"suggestions\":[\"food1\",\"food2\",\"food3\"]},");
        prompt.append("\"lunch\":{\"calories\":0,\"protein\":0,\"carbs\":0,\"fats\":0,\"tip\":\"...\",\"suggestions\":[\"food1\",\"food2\",\"food3\"]},");
        prompt.append("\"dinner\":{\"calories\":0,\"protein\":0,\"carbs\":0,\"fats\":0,\"tip\":\"...\",\"suggestions\":[\"food1\",\"food2\",\"food3\"]},");
        prompt.append("\"snacks\":{\"calories\":0,\"protein\":0,\"carbs\":0,\"fats\":0,\"tip\":\"...\",\"suggestions\":[\"food1\",\"food2\",\"food3\"]}");
        prompt.append("}");

        new Thread(() -> {
            try {
                String     raw  = callGroqText(prompt.toString(), 700);
                String     json = extractJson(raw);
                JSONObject plan = new JSONObject(json);
                String     today2 = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

                if (isAdded()) {
                    requireContext()
                            .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE)
                            .edit()
                            .putString(KEY_MEAL_PLAN, plan.toString())
                            .putString(KEY_PLAN_DATE, today2)
                            .apply();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            applyPlanJson(plan);
                            loadLoggedTotals();
                            showTimeline();
                        } catch (Exception e) { showError("Could not parse meal plan."); }
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        showError(e.getMessage() != null ? e.getMessage() : "Failed to generate plan."));
            }
        }).start();
    }

    private void applyPlanJson(JSONObject plan) throws Exception {
        applySlot(breakfast, plan.getJSONObject("breakfast"));
        applySlot(lunch,     plan.getJSONObject("lunch"));
        applySlot(dinner,    plan.getJSONObject("dinner"));
        applySlot(snacks,    plan.getJSONObject("snacks"));
    }

    private void applySlot(MealSlotData slot, JSONObject obj) {
        slot.targetCalories = obj.optInt("calories", 0);
        slot.targetProtein  = obj.optInt("protein",  0);
        slot.targetCarbs    = obj.optInt("carbs",    0);
        slot.targetFats     = obj.optInt("fats",     0);
        slot.tip            = obj.optString("tip", "");
        JSONArray sugg = obj.optJSONArray("suggestions");
        if (sugg != null) {
            slot.suggestions = new String[sugg.length()];
            for (int i = 0; i < sugg.length(); i++)
                slot.suggestions[i] = sugg.optString(i, "");
        }
    }

    private void loadLoggedTotals() {
        if (!isAdded()) return;
        try {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            String    json  = prefs.getString(KEY_HISTORY, "[]");
            String    today = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());

            for (MealSlotData s : new MealSlotData[]{breakfast, lunch, dinner, snacks}) {
                s.loggedCalories = s.loggedProtein = s.loggedCarbs = s.loggedFats = 0;
            }

            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject entry = arr.getJSONObject(i);
                if (!entry.optString("date", "").startsWith(today)) continue;
                MealSlotData target = slotForName(entry.optString("mealSlot", ""));
                if (target == null) continue;
                target.loggedCalories += entry.optInt("calories", 0);
                target.loggedProtein  += entry.optInt("protein",  0);
                target.loggedCarbs    += entry.optInt("carbs",    0);
                target.loggedFats     += entry.optInt("fats",     0);
            }
        } catch (Exception ignored) {}
    }

    MealSlotData slotForName(String name) {
        if (name == null) return null;
        switch (name.toLowerCase(Locale.getDefault())) {
            case "breakfast": return breakfast;
            case "lunch":     return lunch;
            case "dinner":    return dinner;
            case "snacks":    return snacks;
            default:          return null;
        }
    }

    private void showTimeline() {
        if (!isAdded()) return;
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutTimeline.setVisibility(View.VISIBLE);

        String today = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvPlanDate.setText(today);
        renderTimeline();
    }

    private void renderTimeline() {
        if (!isAdded() || timelineSlots == null) return;
        timelineSlots.removeAllViews();

        MealSlotData[] slots = {breakfast, lunch, dinner, snacks};
        for (int i = 0; i < slots.length; i++)
            timelineSlots.addView(buildSlotCard(slots[i], i == slots.length - 1));

        if (tvDailyGoalSummary != null) {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            int dailyGoal   = prefs.getInt("daily_calorie_goal", 2000);
            int totalLogged = breakfast.loggedCalories + lunch.loggedCalories
                    + dinner.loggedCalories + snacks.loggedCalories;
            int totalTarget = breakfast.targetCalories + lunch.targetCalories
                    + dinner.targetCalories + snacks.targetCalories;
            float pct = totalTarget > 0 ? (float) totalLogged / totalTarget : 0f;
            tvDailyGoalSummary.setText("🔥 " + totalLogged + " / " + dailyGoal + " kcal  ·  "
                    + Math.round(pct * 100) + "% of plan");
        }
    }

    private View buildSlotCard(MealSlotData slot, boolean isLast) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(14);
        row.setLayoutParams(rowLp);

        LinearLayout timelineCol = new LinearLayout(requireContext());
        timelineCol.setOrientation(LinearLayout.VERTICAL);
        timelineCol.setGravity(Gravity.CENTER_HORIZONTAL);
        timelineCol.setLayoutParams(new LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.MATCH_PARENT));

        View dot = new View(requireContext());
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(16), dp(16));
        dotLp.topMargin = dp(22);
        dot.setLayoutParams(dotLp);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(slot.isComplete() ? 0xFF4CAF50 : 0xFFCC5803);
        dotBg.setStroke(dp(2), slot.isComplete() ? 0xFF2E7D32 : 0xFF7B3F00);
        dot.setBackground(dotBg);

        View line = new View(requireContext());
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(2), 0);
        lineLp.weight = 1;
        line.setLayoutParams(lineLp);
        line.setBackgroundColor(0xFFFFDD99);

        timelineCol.addView(dot);
        if (!isLast) timelineCol.addView(line);

        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cardLp.gravity = dp(10);
        card.setLayoutParams(cardLp);
        card.setRadius(dp(18));
        card.setCardElevation(dp(3));
        card.setCardBackgroundColor(slot.isComplete() ? 0xFFF1FFF4 : 0xFFFFFFFF);
        card.setUseCompatPadding(true);

        LinearLayout cardContent = new LinearLayout(requireContext());
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ArcProgressView arc = new ArcProgressView(requireContext());
        arc.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));
        arc.setProgress(0f, slot.arcColor());

        FrameLayout arcFrame = new FrameLayout(requireContext());
        arcFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));
        arcFrame.addView(arc);
        TextView tvEmojiOverlay = new TextView(requireContext());
        FrameLayout.LayoutParams emojiLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        emojiLp.gravity = Gravity.CENTER;
        tvEmojiOverlay.setLayoutParams(emojiLp);
        tvEmojiOverlay.setText(slot.emoji);
        tvEmojiOverlay.setTextSize(20f);
        arcFrame.addView(tvEmojiOverlay);

        LinearLayout nameCol = new LinearLayout(requireContext());
        nameCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nameColLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameColLp.gravity = dp(10);
        nameCol.setLayoutParams(nameColLp);

        TextView tvName = new TextView(requireContext());
        tvName.setText(slot.slot);
        tvName.setTextSize(17f);
        tvName.setTextColor(slot.isComplete() ? 0xFF2E7D32 : 0xFFCC5803);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);

        TextView tvTime = new TextView(requireContext());
        tvTime.setText(slot.timeLabel);
        tvTime.setTextSize(11f);
        tvTime.setTextColor(0xFF9CA3AF);

        TextView tvTarget = new TextView(requireContext());
        tvTarget.setText(slot.targetCalories + " kcal");
        tvTarget.setTextSize(10f);
        tvTarget.setTextColor(0xFFCC5803);
        tvTarget.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable targetBg = new GradientDrawable();
        targetBg.setColor(0xFFFFF0D6);
        targetBg.setCornerRadius(dp(12));
        tvTarget.setBackground(targetBg);
        tvTarget.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams targetLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        targetLp.topMargin = dp(3);
        tvTarget.setLayoutParams(targetLp);

        nameCol.addView(tvName);
        nameCol.addView(tvTime);
        nameCol.addView(tvTarget);

        TextView tvStatus = new TextView(requireContext());
        tvStatus.setText(slot.statusText());
        tvStatus.setTextSize(11f);
        tvStatus.setTextColor(slot.isComplete() ? 0xFF4CAF50 : 0xFFCC5803);
        tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusLp.gravity = dp(6);
        tvStatus.setLayoutParams(statusLp);

        header.addView(arcFrame);
        header.addView(nameCol);
        header.addView(tvStatus);
        cardContent.addView(header);

        ProgressBar pb = new ProgressBar(requireContext(), null,
                android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(7));
        pbLp.topMargin = dp(10);
        pb.setLayoutParams(pbLp);
        pb.setMax(100);
        pb.setProgress(0);
        pb.setProgressTintList(ColorStateList.valueOf(slot.isComplete() ? 0xFF4CAF50 : 0xFFCC5803));
        pb.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFEEE8E0));
        cardContent.addView(pb);

        LinearLayout macroRow = new LinearLayout(requireContext());
        macroRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams macroLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        macroLp.topMargin = dp(12);
        macroRow.setLayoutParams(macroLp);
        macroRow.setBackground(makeRoundRect(0xFFFAF5EF, dp(10)));
        macroRow.setPadding(dp(6), dp(8), dp(6), dp(8));

        addMacroCell(macroRow, "🔥", slot.loggedCalories + "", slot.targetCalories + "", "kcal",   0xFFCC5803, true);
        addMacroCell(macroRow, "💪", slot.loggedProtein  + "", slot.targetProtein  + "", "protein", 0xFFCC5803, true);
        addMacroCell(macroRow, "🌾", slot.loggedCarbs    + "", slot.targetCarbs    + "", "carbs",   0xFFE2711D, false);
        addMacroCell(macroRow, "🫙", slot.loggedFats     + "", slot.targetFats     + "", "fats",    0xFFFF9505, false);
        cardContent.addView(macroRow);

        if (slot.tip != null && !slot.tip.isEmpty()) {
            TextView tvTip = new TextView(requireContext());
            LinearLayout.LayoutParams tipLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tipLp.topMargin = dp(10);
            tvTip.setLayoutParams(tipLp);
            tvTip.setText("💡 " + slot.tip);
            tvTip.setTextSize(11.5f);
            tvTip.setTextColor(0xFF8D6E63);
            tvTip.setBackground(makeRoundRect(0xFFFFF8F0, dp(8)));
            tvTip.setPadding(dp(10), dp(8), dp(10), dp(8));
            tvTip.setLineSpacing(dp(2), 1f);
            cardContent.addView(tvTip);
        }

        if (slot.suggestions != null && slot.suggestions.length > 0) {
            TextView suggLabel = new TextView(requireContext());
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            slp.topMargin = dp(10);
            suggLabel.setLayoutParams(slp);
            suggLabel.setText("SUGGESTIONS");
            suggLabel.setTextSize(9f);
            suggLabel.setTextColor(0xFFBCAAA4);
            suggLabel.setTypeface(Typeface.DEFAULT_BOLD);
            suggLabel.setLetterSpacing(0.12f);
            cardContent.addView(suggLabel);

            HorizontalScrollView hScroll = new HorizontalScrollView(requireContext());
            LinearLayout.LayoutParams hslp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            hslp.topMargin = dp(6);
            hScroll.setLayoutParams(hslp);
            hScroll.setHorizontalScrollBarEnabled(false);

            LinearLayout chips = new LinearLayout(requireContext());
            chips.setOrientation(LinearLayout.HORIZONTAL);

            for (String s : slot.suggestions) {
                if (s == null || s.isEmpty()) continue;
                TextView chip = new TextView(requireContext());
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                clp.topMargin = dp(8);
                chip.setLayoutParams(clp);
                chip.setText("🍽 " + s);
                chip.setTextSize(12f);
                chip.setTextColor(0xFFCC5803);
                chip.setPadding(dp(12), dp(7), dp(12), dp(7));
                GradientDrawable chipBg = new GradientDrawable();
                chipBg.setColor(0xFFFFF0D6);
                chipBg.setCornerRadius(dp(20));
                chipBg.setStroke(dp(1), 0xFFFFB627);
                chip.setBackground(chipBg);
                chip.setOnClickListener(v ->
                        Toast.makeText(requireContext(), "Add \"" + s + "\" to " + slot.slot, Toast.LENGTH_SHORT).show());
                chips.addView(chip);
            }
            hScroll.addView(chips);
            cardContent.addView(hScroll);
        }

        card.addView(cardContent);

        card.post(() -> {
            ValueAnimator barAnim = ValueAnimator.ofInt(0, (int)(slot.progressPct() * 100));
            barAnim.setDuration(800);
            barAnim.setInterpolator(new DecelerateInterpolator());
            barAnim.addUpdateListener(a -> pb.setProgress((int) a.getAnimatedValue()));
            barAnim.start();

            ValueAnimator arcAnim = ValueAnimator.ofFloat(0f, slot.progressPct());
            arcAnim.setDuration(900);
            arcAnim.setInterpolator(new DecelerateInterpolator());
            arcAnim.addUpdateListener(a -> arc.setProgress((float) a.getAnimatedValue(), slot.arcColor()));
            arcAnim.start();
        });

        row.addView(timelineCol);
        row.addView(card);
        return row;
    }

    private void addMacroCell(LinearLayout parent, String emoji,
                              String logged, String target, String label,
                              int color, boolean addDivider) {
        LinearLayout cell = new LinearLayout(requireContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cell.setLayoutParams(lp);

        TextView tvEmoji = new TextView(requireContext());
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(14f);
        tvEmoji.setGravity(Gravity.CENTER);

        TextView tvVal = new TextView(requireContext());
        tvVal.setText(logged);
        tvVal.setTextSize(14f);
        tvVal.setTextColor(color);
        tvVal.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvVal.setGravity(Gravity.CENTER);

        TextView tvTarget2 = new TextView(requireContext());
        tvTarget2.setText("/ " + target);
        tvTarget2.setTextSize(9f);
        tvTarget2.setTextColor(0xFFBBBBBB);
        tvTarget2.setGravity(Gravity.CENTER);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(9f);
        tvLabel.setTextColor(0xFFAAAAAA);
        tvLabel.setGravity(Gravity.CENTER);

        cell.addView(tvEmoji);
        cell.addView(tvVal);
        cell.addView(tvTarget2);
        cell.addView(tvLabel);

        parent.addView(cell);

        if (addDivider) {
            View div = new View(requireContext());
            div.setLayoutParams(new LinearLayout.LayoutParams(dp(1), dp(36)));
            div.setBackgroundColor(0xFFEEE8E0);
            parent.addView(div);
        }
    }

    private GradientDrawable makeRoundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    // -------------------------------------------------------------------------
    // UI states
    // -------------------------------------------------------------------------

    private void showLoading() {
        if (!isAdded()) return;
        layoutTimeline.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.VISIBLE);
    }

    private void showError(String msg) {
        if (!isAdded()) return;
        layoutLoading.setVisibility(View.GONE);
        layoutTimeline.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        tvErrorMsg.setText(msg);
    }

    // -------------------------------------------------------------------------
    // Groq API
    // -------------------------------------------------------------------------

    private String callGroqText(String prompt, int maxTokens) throws Exception {
        JSONObject userMsg = new JSONObject();
        userMsg.put("role",    "user");
        userMsg.put("content", prompt);
        JSONArray  messages = new JSONArray();
        messages.put(userMsg);
        JSONObject body = new JSONObject();
        body.put("model",       GROQ_CHAT_MODEL);
        body.put("messages",    messages);
        body.put("max_tokens",  maxTokens);
        body.put("temperature", 0.3);

        RequestBody rb = RequestBody.create(body.toString(),
                MediaType.parse("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url(GROQ_URL)
                .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                .addHeader("Content-Type",  "application/json")
                .post(rb).build();

        try (Response resp = httpClient.newCall(req).execute()) {
            if (resp.body() == null) throw new Exception("Empty response");
            String raw = resp.body().string();
            if (!resp.isSuccessful()) throw new Exception("Groq error " + resp.code());
            JSONObject json = new JSONObject(raw);
            JSONArray  ch   = json.optJSONArray("choices");
            if (ch == null || ch.length() == 0) throw new Exception("No response from AI");
            JSONObject msg = ch.getJSONObject(0).optJSONObject("message");
            if (msg == null) throw new Exception("No message");
            return msg.optString("content", "");
        } catch (java.net.UnknownHostException e) {
            throw new Exception("No internet connection.");
        } catch (java.net.SocketTimeoutException e) {
            throw new Exception("Request timed out.");
        }
    }

    private String extractJson(String raw) {
        String s = raw.replaceAll("```json", "").replaceAll("```", "").trim();
        int start = s.indexOf('{'), end = s.lastIndexOf('}');
        if (start >= 0 && end > start) return s.substring(start, end + 1);
        return s;
    }

    private int dp(int v) {
        if (!isAdded()) return v;
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}