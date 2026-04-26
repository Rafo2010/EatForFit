package rafayel.hakobyan.EatForFit;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class FoodResultActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH     = "image_path";
    public static final String EXTRA_NAME           = "food_name";
    public static final String EXTRA_EMOJI          = "food_emoji";
    public static final String EXTRA_CATEGORY       = "food_category";
    public static final String EXTRA_CALORIES       = "food_calories";
    public static final String EXTRA_PROTEIN        = "food_protein";
    public static final String EXTRA_CARBS          = "food_carbs";
    public static final String EXTRA_FATS           = "food_fats";
    public static final String EXTRA_DESCRIPTION    = "food_description";
    public static final String EXTRA_RECOMMENDATION = "food_recommendation";
    public static final String EXTRA_EST_GRAMS      = "food_est_grams";
    public static final String EXTRA_FIST_DETECTED  = "food_fist_detected";

    private static final String PREFS_NAME    = "food_scanner_prefs";
    private static final String KEY_HISTORY   = "food_history";
    private static final String KEY_DAILY     = "daily_calorie_goal";
    private static final String NOTIF_CHANNEL = "calorie_goal_channel";

    private String currentUserId = "default";
    private int    portions      = 1;
    private int    estGrams      = 100;
    private int    caloriesPer100;
    private float  proteinPer100, carbsPer100, fatsPer100;
    private boolean fistDetected = false;

    private String selectedMealSlot = "Breakfast";

    private TextView       tvPortionCount, tvPortionCalc, tvSelectedSlotLabel;
    private MaterialButton btnBreakfast, btnLunch, btnDinner, btnSnacks;

    // Macro bar views
    private View     barProtein, barCarbs, barFats;
    private TextView tvBarProteinPct, tvBarCarbsPct, tvBarFatsPct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_result);

        createNotificationChannel();

        com.google.firebase.auth.FirebaseUser fbUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) currentUserId = fbUser.getUid();

        SharedPreferences userPrefs = getSharedPreferences(
                PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
        String userDisease  = userPrefs.getString("disease",  "None");
        String userActivity = userPrefs.getString("activity", "moderately_active");

        // ── Read intent data ──────────────────────────────────────────────
        String imagePath      = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        String name           = getIntent().getStringExtra(EXTRA_NAME);
        String emoji          = getIntent().getStringExtra(EXTRA_EMOJI);
        String category       = getIntent().getStringExtra(EXTRA_CATEGORY);
        caloriesPer100        = getIntent().getIntExtra(EXTRA_CALORIES, 0);
        proteinPer100         = getIntent().getFloatExtra(EXTRA_PROTEIN, 0);
        carbsPer100           = getIntent().getFloatExtra(EXTRA_CARBS, 0);
        fatsPer100            = getIntent().getFloatExtra(EXTRA_FATS, 0);
        String description    = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        String recommendation = getIntent().getStringExtra(EXTRA_RECOMMENDATION);
        estGrams              = getIntent().getIntExtra(EXTRA_EST_GRAMS, 100);
        fistDetected          = getIntent().getBooleanExtra(EXTRA_FIST_DETECTED, false);
        if (estGrams <= 0) estGrams = 100;

        // ── Hero image (rounded crop) ─────────────────────────────────────
        ImageView ivPhoto = findViewById(R.id.ivFoodPhoto);
        if (imagePath != null && !imagePath.isEmpty()) {
            Bitmap bmp = BitmapFactory.decodeFile(imagePath);
            if (bmp != null) ivPhoto.setImageBitmap(bmp);
        }

        // ── Food identity ─────────────────────────────────────────────────
        ((TextView) findViewById(R.id.tvResultEmoji)).setText(emoji != null ? emoji : "🍽️");
        ((TextView) findViewById(R.id.tvResultFoodName)).setText(name != null ? name : "");
        ((TextView) findViewById(R.id.tvResultCategory)).setText(category != null ? category : "");

        // ── Totals for 1 portion ─────────────────────────────────────────
        int   totalCalories = Math.round((caloriesPer100 * estGrams) / 100f);
        float totalProtein  = Math.round((proteinPer100  * estGrams) / 100f * 10f) / 10f;
        float totalCarbs    = Math.round((carbsPer100    * estGrams) / 100f * 10f) / 10f;
        float totalFats     = Math.round((fatsPer100     * estGrams) / 100f * 10f) / 10f;

        ((TextView) findViewById(R.id.tvHeroCalories)).setText(String.valueOf(totalCalories));
        ((TextView) findViewById(R.id.tvResultCalories)).setText(String.valueOf(totalCalories));
        ((TextView) findViewById(R.id.tvResultProtein)).setText(totalProtein + "g");
        ((TextView) findViewById(R.id.tvResultCarbs)).setText(totalCarbs + "g");
        ((TextView) findViewById(R.id.tvResultFats)).setText(totalFats + "g");

        // ── Description ──────────────────────────────────────────────────
        TextView tvDesc = findViewById(R.id.tvResultDescription);
        if (description != null && !description.isEmpty()) {
            tvDesc.setText(description);
            tvDesc.setVisibility(View.VISIBLE);
        }

        // ── Recommendation ───────────────────────────────────────────────
        TextView tvRec = findViewById(R.id.tvResultRecommendation);
        if (recommendation != null && !recommendation.isEmpty()) {
            tvRec.setText(recommendation);
            tvRec.setVisibility(View.VISIBLE);
        }

        // ── Health warning (inject dynamically below recommendation) ─────
        String healthWarning = buildHealthWarning(userDisease, userActivity,
                name != null ? name : "", caloriesPer100, category != null ? category : "");
        boolean isDangerous  = isFoodDangerousForCondition(
                userDisease, name != null ? name : "", caloriesPer100, category != null ? category : "");

        if (!healthWarning.isEmpty()) {
            LinearLayout recCard = (LinearLayout) tvRec.getParent();
            TextView tvWarning   = new TextView(this);
            tvWarning.setText(isDangerous ? "🚫 " + healthWarning : "⚠️ " + healthWarning);
            tvWarning.setTextSize(13);
            tvWarning.setTextColor(0xFFFFFFFF);
            tvWarning.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvWarning.setBackgroundColor(isDangerous ? 0xFFB71C1C : 0xFFE65100);
            tvWarning.setPadding(dp(14), dp(12), dp(14), dp(12));
            tvWarning.setLineSpacing(4f, 1f);
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            wlp.topMargin = dp(8);
            tvWarning.setLayoutParams(wlp);
            recCard.addView(tvWarning);
        }


        barProtein       = findViewById(R.id.barProtein);
        barCarbs         = findViewById(R.id.barCarbs);
        barFats          = findViewById(R.id.barFats);
        tvBarProteinPct  = findViewById(R.id.tvBarProteinPct);
        tvBarCarbsPct    = findViewById(R.id.tvBarCarbsPct);
        tvBarFatsPct     = findViewById(R.id.tvBarFatsPct);

        barProtein.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        barProtein.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        animateMacroBars(proteinPer100, carbsPer100, fatsPer100);
                    }
                });

        // ── AI portion estimate ───────────────────────────────────────────
        int      onePortionCal = Math.round((caloriesPer100 * estGrams) / 100f);
        TextView tvAiPortions  = findViewById(R.id.tvAiPortions);
        if (fistDetected) {
            tvAiPortions.setText("✊ Fist reference used — AI estimated ~" + estGrams + "g  ≈  " + onePortionCal + " kcal/portion");
            tvAiPortions.setBackgroundColor(0xFFE8F5E9);
            tvAiPortions.setTextColor(0xFF2E7D32);
        } else {
            tvAiPortions.setText("📸 AI estimated ~" + estGrams + "g in your photo  ≈  " + onePortionCal + " kcal/portion");
        }

        View fistTipCard = findViewById(R.id.layoutFistTip);
        if (fistTipCard != null) fistTipCard.setVisibility(fistDetected ? View.GONE : View.VISIBLE);

        // ── Portion stepper ───────────────────────────────────────────────
        tvPortionCount = findViewById(R.id.tvPortionCount);
        tvPortionCalc  = findViewById(R.id.tvPortionCalc);
        updatePortionUI();

        // ── Meal slot buttons ─────────────────────────────────────────────
        btnBreakfast      = findViewById(R.id.btnSlotBreakfast);
        btnLunch          = findViewById(R.id.btnSlotLunch);
        btnDinner         = findViewById(R.id.btnSlotDinner);
        btnSnacks         = findViewById(R.id.btnSlotSnacks);
        tvSelectedSlotLabel = findViewById(R.id.tvSelectedSlotLabel);

        selectSlot("Breakfast");

        btnBreakfast.setOnClickListener(v -> selectSlot("Breakfast"));
        btnLunch.setOnClickListener(v    -> selectSlot("Lunch"));
        btnDinner.setOnClickListener(v   -> selectSlot("Dinner"));
        btnSnacks.setOnClickListener(v   -> selectSlot("Snacks"));

        // ── Log button ────────────────────────────────────────────────────
        MaterialButton btnEat = findViewById(R.id.btnEatThis);
        if (isDangerous) {
            btnEat.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFB71C1C));
            btnEat.setText("🚫  Not Recommended — Log Anyway?");
            btnEat.setTextSize(13);
        }

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnSkip).setOnClickListener(v  -> finish());

        findViewById(R.id.btnPortionMinus).setOnClickListener(v -> {
            if (portions > 1) { portions--; updatePortionUI(); }
        });
        findViewById(R.id.btnPortionPlus).setOnClickListener(v -> {
            portions++;
            updatePortionUI();
        });

        String  finalName      = name;
        String  finalImagePath = imagePath;
        boolean finalDangerous = isDangerous;

        btnEat.setOnClickListener(v -> {
            if (finalDangerous) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("🚫 Health Warning")
                        .setMessage("This food is not recommended for your condition: " + userDisease +
                                "\n\nAre you sure you want to log it?")
                        .setPositiveButton("Log Anyway", (dialog, which) ->
                                logAndFinish(finalName, finalImagePath))
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }
            logAndFinish(finalName, finalImagePath);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Macro bar animation
    // ─────────────────────────────────────────────────────────────────────────

    private void animateMacroBars(float protein, float carbs, float fats) {
        float total = protein + carbs + fats;
        if (total <= 0) total = 1;

        float protPct  = protein / total;
        float carbsPct = carbs   / total;
        float fatsPct  = fats    / total;

        animateBar(barProtein,    tvBarProteinPct, protPct);
        animateBar(barCarbs,      tvBarCarbsPct,   carbsPct);
        animateBar(barFats,       tvBarFatsPct,    fatsPct);
    }

    private void animateBar(View bar, TextView label, float fraction) {
        int parentWidth = ((View) bar.getParent()).getWidth();
        int targetWidth = (int)(parentWidth * fraction);

        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofInt(0, targetWidth);
        anim.setDuration(700);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            ViewGroup.LayoutParams lp = bar.getLayoutParams();
            lp.width = (int) a.getAnimatedValue();
            bar.setLayoutParams(lp);
        });
        anim.start();

        label.setText(Math.round(fraction * 100) + "%");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Meal slot selection
    // ─────────────────────────────────────────────────────────────────────────

    private void selectSlot(String slot) {
        selectedMealSlot = slot;

        resetSlotButton(btnBreakfast);
        resetSlotButton(btnLunch);
        resetSlotButton(btnDinner);
        resetSlotButton(btnSnacks);

        MaterialButton active;
        String emoji;
        String label;
        switch (slot) {
            case "Lunch":   active = btnLunch;   emoji = "☀️"; label = "Lunch";     break;
            case "Dinner":  active = btnDinner;  emoji = "🌙"; label = "Dinner";    break;
            case "Snacks":  active = btnSnacks;  emoji = "🍎"; label = "Snacks";    break;
            default:        active = btnBreakfast; emoji = "🌅"; label = "Breakfast"; break;
        }

        active.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFCC5803));
        active.setTextColor(0xFFFFFFFF);

        if (tvSelectedSlotLabel != null)
            tvSelectedSlotLabel.setText(emoji + " " + label + " selected");
    }

    private void resetSlotButton(MaterialButton btn) {
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFF5E6D0));
        btn.setTextColor(0xFFCC5803);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logging
    // ─────────────────────────────────────────────────────────────────────────

    private void logAndFinish(String foodName, String imagePath) {
        float totalGrams    = (float) estGrams * portions;
        int   loggedCal     = Math.round((caloriesPer100 * totalGrams) / 100f);
        int   loggedProtein = Math.round((proteinPer100  * totalGrams) / 100f);
        int   loggedCarbs   = Math.round((carbsPer100    * totalGrams) / 100f);
        int   loggedFats    = Math.round((fatsPer100     * totalGrams) / 100f);

        logCaloriesToday(loggedCal, foodName, loggedProtein, loggedCarbs, loggedFats,
                imagePath, portions);

        String slotEmoji;
        switch (selectedMealSlot) {
            case "Lunch":   slotEmoji = "☀️"; break;
            case "Dinner":  slotEmoji = "🌙"; break;
            case "Snacks":  slotEmoji = "🍎"; break;
            default:        slotEmoji = "🌅"; break;
        }

        Toast.makeText(this,
                slotEmoji + " " + loggedCal + " kcal added to " + selectedMealSlot,
                Toast.LENGTH_SHORT).show();

        checkGoalReached(loggedCal);
        finish();
    }

    private void updatePortionUI() {
        tvPortionCount.setText(String.valueOf(portions));
        float totalG = estGrams * portions;
        int   cal    = Math.round((caloriesPer100 * totalG) / 100f);
        int   prot   = Math.round((proteinPer100  * totalG) / 100f);
        int   carbs  = Math.round((carbsPer100    * totalG) / 100f);
        int   fats   = Math.round((fatsPer100     * totalG) / 100f);
        String fistNote = fistDetected ? "  ✊" : "";
        tvPortionCalc.setText(
                (int) totalG + "g  ·  🔥 " + cal + " kcal\n" +
                        "💪 " + prot + "g protein  ·  🌾 " + carbs + "g  ·  🫙 " + fats + "g" + fistNote);
    }

    private void logCaloriesToday(int calories, String foodName,
                                  int protein, int carbs, int fats,
                                  String imagePath, int loggedPortions) {
        try {
            SharedPreferences prefs = getSharedPreferences(
                    PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            String    json  = prefs.getString(KEY_HISTORY, "[]");
            JSONArray arr   = new JSONArray(json);

            JSONObject entry    = new JSONObject();
            String     foodEmoji    = getIntent().getStringExtra(EXTRA_EMOJI);
            String     foodCategory = getIntent().getStringExtra(EXTRA_CATEGORY);
            String     foodDesc     = getIntent().getStringExtra(EXTRA_DESCRIPTION);
            String     foodRec      = getIntent().getStringExtra(EXTRA_RECOMMENDATION);

            entry.put("id",             UUID.randomUUID().toString());
            entry.put("name",           foodName     != null ? foodName     : "Food");
            entry.put("emoji",          foodEmoji    != null ? foodEmoji    : "🍽️");
            entry.put("category",       foodCategory != null ? foodCategory : "Food");
            entry.put("date",           new SimpleDateFormat("dd MMM, HH:mm",
                    Locale.getDefault()).format(new Date()));
            entry.put("calories",       calories);
            entry.put("protein",        protein);
            entry.put("carbs",          carbs);
            entry.put("fats",           fats);
            entry.put("portions",       loggedPortions);
            entry.put("savedImagePath", imagePath    != null ? imagePath    : "");
            entry.put("aiDescription",  foodDesc     != null ? foodDesc     : "");
            entry.put("recommendation", foodRec      != null ? foodRec      : "");
            entry.put("estimatedGrams", estGrams * loggedPortions);
            entry.put("fistDetected",   fistDetected);
            entry.put("mealSlot",       selectedMealSlot);

            JSONArray newArr = new JSONArray();
            newArr.put(entry);
            for (int i = 0; i < arr.length() && i < 99; i++) newArr.put(arr.getJSONObject(i));
            prefs.edit().putString(KEY_HISTORY, newArr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void checkGoalReached(int justAdded) {
        try {
            SharedPreferences prefs    = getSharedPreferences(
                    PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            int       dailyGoal = prefs.getInt(KEY_DAILY, 2000);
            String    json      = prefs.getString(KEY_HISTORY, "[]");
            String    today     = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());
            int       total     = 0;
            JSONArray arr       = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject e = arr.getJSONObject(i);
                if (e.optString("date", "").startsWith(today)) total += e.optInt("calories", 0);
            }
            if (total >= dailyGoal && total - justAdded < dailyGoal) sendGoalNotification(total, dailyGoal);
        } catch (Exception ignored) {}
    }

    private void sendGoalNotification(int total, int goal) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIF_CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("🎉 Daily Calorie Goal Reached!")
                    .setContentText("You've consumed " + total + " kcal of your " + goal + " kcal goal!")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("You've consumed " + total + " kcal of your " + goal +
                                    " kcal goal today. You've hit your target — consider stopping for today! 🌟"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            NotificationManagerCompat nm = NotificationManagerCompat.from(this);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED)
                nm.notify(1001, builder.build());
        } catch (Exception ignored) {}
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIF_CHANNEL, "Calorie Goal", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifies when daily calorie goal is reached");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Health checks  (unchanged logic)
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isFoodDangerousForCondition(String disease, String foodName,
                                                int kcalPer100, String category) {
        if (disease == null || disease.equalsIgnoreCase("None") || disease.isEmpty()) return false;
        String food = foodName.toLowerCase(Locale.getDefault());
        String cat  = category.toLowerCase(Locale.getDefault());
        String dis  = disease.toLowerCase(Locale.getDefault());

        boolean isSweet   = food.contains("chocolate") || food.contains("candy") ||
                food.contains("cake") || food.contains("cookie") || food.contains("donut") ||
                food.contains("sugar") || food.contains("ice cream") || food.contains("waffle") ||
                food.contains("pancake") || food.contains("honey") || food.contains("syrup") ||
                cat.contains("snack") || cat.contains("dessert") || kcalPer100 > 350;
        boolean isFatty   = cat.contains("fast food") || food.contains("fries") ||
                food.contains("burger") || food.contains("pizza") || kcalPer100 > 400;
        boolean isSalty   = food.contains("chip") || food.contains("pickle") ||
                food.contains("sausage") || food.contains("bacon") || food.contains("salt");
        boolean isHighProt = cat.contains("meat") || cat.contains("fish") || cat.contains("protein");

        if (dis.contains("diabetes") && isSweet) return true;
        if ((dis.contains("hypertension") || dis.contains("heart")) && (isSalty || isFatty)) return true;
        if (dis.contains("kidney") && isHighProt) return true;
        if ((dis.contains("obesity") || dis.contains("cholesterol")) && (isFatty || isSweet)) return true;
        return false;
    }

    private String buildHealthWarning(String disease, String activity,
                                      String foodName, int kcalPer100, String category) {
        if (disease == null || disease.equalsIgnoreCase("None") || disease.isEmpty()) return "";
        String        food = foodName.toLowerCase(Locale.getDefault());
        String        cat  = category.toLowerCase(Locale.getDefault());
        String        dis  = disease.toLowerCase(Locale.getDefault());
        StringBuilder w    = new StringBuilder();

        boolean isSweet   = food.contains("chocolate") || food.contains("candy") ||
                food.contains("cake") || food.contains("cookie") || food.contains("donut") ||
                food.contains("sugar") || food.contains("ice cream") || food.contains("waffle") ||
                food.contains("pancake") || cat.contains("snack") || cat.contains("dessert") || kcalPer100 > 300;
        boolean isFatty   = cat.contains("fast food") || food.contains("fries") ||
                food.contains("burger") || food.contains("pizza") || kcalPer100 > 400;
        boolean isSalty   = food.contains("chip") || food.contains("pickle") ||
                food.contains("sausage") || food.contains("bacon");
        boolean isHighProt = cat.contains("meat") || cat.contains("fish") || cat.contains("protein");

        if (dis.contains("diabetes")) {
            if (isSweet)
                w.append("WARNING: DIABETES\nThis food is high in sugar and carbs. It can spike your blood glucose. Consider avoiding or eating a very small portion.");
            else if (isFatty)
                w.append("CAUTION: DIABETES\nHigh-fat foods can increase insulin resistance. Prefer lean proteins and vegetables instead.");
        }
        if (dis.contains("hypertension") || dis.contains("heart")) {
            if (isSalty || isFatty) {
                if (w.length() > 0) w.append("\n\n");
                w.append("WARNING: HEART / HYPERTENSION\nThis food is high in sodium or saturated fat which can raise blood pressure.");
            }
        }
        if (dis.contains("kidney")) {
            if (isHighProt) {
                if (w.length() > 0) w.append("\n\n");
                w.append("CAUTION: KIDNEY DISEASE\nHigh-protein foods put extra strain on kidneys. Consult your doctor about safe protein limits.");
            }
        }
        if (dis.contains("obesity") || dis.contains("cholesterol")) {
            if (isFatty || isSweet) {
                if (w.length() > 0) w.append("\n\n");
                w.append("CAUTION: WEIGHT / CHOLESTEROL\nThis food is calorie-dense or high in fat. Consider a smaller portion or a healthier alternative.");
            }
        }
        if (dis.contains("asthma")) {
            if (food.contains("wine") || food.contains("beer") ||
                    food.contains("preserved") || food.contains("processed")) {
                if (w.length() > 0) w.append("\n\n");
                w.append("CAUTION: ASTHMA\nProcessed or sulfite-containing foods may trigger asthma symptoms.");
            }
        }
        return w.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_down);
    }
}