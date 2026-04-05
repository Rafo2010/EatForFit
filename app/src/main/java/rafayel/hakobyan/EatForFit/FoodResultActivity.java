package rafayel.hakobyan.EatForFit;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
    private int portions         = 1;
    private int estGrams         = 100;
    private int caloriesPer100;
    private float proteinPer100, carbsPer100, fatsPer100;
    private boolean fistDetected = false;

    private TextView tvPortionCount, tvPortionCalc;

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

        ImageView ivPhoto = findViewById(R.id.ivFoodPhoto);
        if (imagePath != null && !imagePath.isEmpty()) {
            Bitmap bmp = BitmapFactory.decodeFile(imagePath);
            if (bmp != null) ivPhoto.setImageBitmap(bmp);
        }

        ((TextView) findViewById(R.id.tvResultEmoji)).setText(emoji != null ? emoji : "🍽️");
        ((TextView) findViewById(R.id.tvResultFoodName)).setText(name != null ? name : "");
        ((TextView) findViewById(R.id.tvResultCategory)).setText(category != null ? category : "");

        int   totalCalories = Math.round((caloriesPer100 * estGrams) / 100f);
        float totalProtein  = Math.round((proteinPer100  * estGrams) / 100f * 10f) / 10f;
        float totalCarbs    = Math.round((carbsPer100    * estGrams) / 100f * 10f) / 10f;
        float totalFats     = Math.round((fatsPer100     * estGrams) / 100f * 10f) / 10f;

        ((TextView) findViewById(R.id.tvResultCalories)).setText(String.valueOf(totalCalories));
        ((TextView) findViewById(R.id.tvResultProtein)).setText(totalProtein + "g");
        ((TextView) findViewById(R.id.tvResultCarbs)).setText(totalCarbs + "g");
        ((TextView) findViewById(R.id.tvResultFats)).setText(totalFats + "g");

        TextView tvDesc = findViewById(R.id.tvResultDescription);
        if (description != null && !description.isEmpty()) {
            tvDesc.setText(description);
            tvDesc.setVisibility(View.VISIBLE);
        }

        TextView tvRec = findViewById(R.id.tvResultRecommendation);
        if (recommendation != null && !recommendation.isEmpty()) {
            tvRec.setText("🎯 " + recommendation);
            tvRec.setVisibility(View.VISIBLE);
        }

        String healthWarning = buildHealthWarning(userDisease, userActivity,
                name != null ? name : "", caloriesPer100, category != null ? category : "");
        boolean isDangerous = isFoodDangerousForCondition(
                userDisease, name != null ? name : "", caloriesPer100, category != null ? category : "");

        if (!healthWarning.isEmpty()) {
            TextView tvWarning = new TextView(this);
            tvWarning.setText(isDangerous ? "🚫 " + healthWarning : "⚠️ " + healthWarning);
            tvWarning.setTextSize(13);
            tvWarning.setTextColor(0xFFFFFFFF);
            tvWarning.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvWarning.setBackgroundColor(isDangerous ? 0xFFB71C1C : 0xFFE53935);
            tvWarning.setPadding(24, 16, 24, 16);
            tvWarning.setLineSpacing(4f, 1f);
            android.widget.LinearLayout descCard = (android.widget.LinearLayout) tvRec.getParent();
            descCard.addView(tvWarning);
        }

        int onePortionCal = Math.round((caloriesPer100 * estGrams) / 100f);
        TextView tvAiPortions = findViewById(R.id.tvAiPortions);
        if (fistDetected) {
            tvAiPortions.setText(
                    "✊ Fist used as reference — AI estimated ~" + estGrams + "g  —  " + onePortionCal + " kcal/portion");
            tvAiPortions.setBackgroundColor(0xFFE8F5E9);
            tvAiPortions.setTextColor(0xFF2E7D32);
        } else {
            tvAiPortions.setText(
                    "📸 AI estimated ~" + estGrams + "g in photo  —  1 portion = " + onePortionCal + " kcal");
        }

        View fistTipCard = findViewById(R.id.layoutFistTip);
        if (fistTipCard != null) {
            fistTipCard.setVisibility(fistDetected ? View.GONE : View.VISIBLE);
        }

        tvPortionCount = findViewById(R.id.tvPortionCount);
        tvPortionCalc  = findViewById(R.id.tvPortionCalc);
        updatePortionUI();

        MaterialButton btnEat = findViewById(R.id.btnEatThis);

        if (isDangerous) {
            btnEat.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFB71C1C));
            btnEat.setText("🚫  Not Recommended — Eat Anyway?");
            btnEat.setTextSize(13);
        }

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnSkip).setOnClickListener(v -> finish());

        findViewById(R.id.btnPortionMinus).setOnClickListener(v -> {
            if (portions > 1) { portions--; updatePortionUI(); }
        });

        findViewById(R.id.btnPortionPlus).setOnClickListener(v -> {
            portions++;
            updatePortionUI();
        });

        String finalName       = name;
        String finalImagePath  = imagePath;
        boolean finalDangerous = isDangerous;

        btnEat.setOnClickListener(v -> {
            if (finalDangerous) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("🚫 Health Warning")
                        .setMessage("This food is not recommended for your condition: " + userDisease +
                                "\n\nAre you sure you want to log it?")
                        .setPositiveButton("Log Anyway", (dialog, which) -> logAndFinish(
                                finalName, finalImagePath))
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }
            logAndFinish(finalName, finalImagePath);
        });
    }

    private void logAndFinish(String foodName, String imagePath) {
        float totalGrams    = (float) estGrams * portions;
        int   loggedCal     = Math.round((caloriesPer100 * totalGrams) / 100f);
        int   loggedProtein = Math.round((proteinPer100  * totalGrams) / 100f);
        int   loggedCarbs   = Math.round((carbsPer100    * totalGrams) / 100f);
        int   loggedFats    = Math.round((fatsPer100     * totalGrams) / 100f);

        logCaloriesToday(loggedCal, foodName, loggedProtein, loggedCarbs, loggedFats,
                imagePath, portions);

        Toast.makeText(this,
                "✅ " + loggedCal + " kcal logged for " + portions +
                        (portions == 1 ? " portion" : " portions") + " of " + foodName,
                Toast.LENGTH_SHORT).show();

        checkGoalReached(loggedCal);
        finish();
    }

    private void updatePortionUI() {
        tvPortionCount.setText(String.valueOf(portions));
        float totalG = estGrams * portions;
        int   cal    = Math.round((caloriesPer100 * totalG) / 100f);
        int   prot   = Math.round((proteinPer100  * totalG) / 100f);
        String fistNote = fistDetected ? "  ✊" : "";
        tvPortionCalc.setText("= " + (int) totalG + "g  ·  " + cal + " kcal  ·  " + prot + "g protein" + fistNote);
    }

    private void logCaloriesToday(int calories, String foodName,
                                  int protein, int carbs, int fats,
                                  String imagePath, int loggedPortions) {
        try {
            SharedPreferences prefs = getSharedPreferences(
                    PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            String    json    = prefs.getString(KEY_HISTORY, "[]");
            JSONArray arr     = new JSONArray(json);

            JSONObject entry = new JSONObject();
            String foodEmoji    = getIntent().getStringExtra(EXTRA_EMOJI);
            String foodCategory = getIntent().getStringExtra(EXTRA_CATEGORY);
            String foodDesc     = getIntent().getStringExtra(EXTRA_DESCRIPTION);
            String foodRec      = getIntent().getStringExtra(EXTRA_RECOMMENDATION);

            entry.put("id",             UUID.randomUUID().toString());
            entry.put("name",           foodName != null ? foodName : "Food");
            entry.put("emoji",          foodEmoji != null ? foodEmoji : "🍽️");
            entry.put("category",       foodCategory != null ? foodCategory : "Food");
            entry.put("date",           new SimpleDateFormat("dd MMM, HH:mm",
                    Locale.getDefault()).format(new Date()));
            entry.put("calories",       calories);
            entry.put("protein",        protein);
            entry.put("carbs",          carbs);
            entry.put("fats",           fats);
            entry.put("portions",       loggedPortions);
            entry.put("savedImagePath", imagePath != null ? imagePath : "");
            entry.put("aiDescription",  foodDesc  != null ? foodDesc  : "");
            entry.put("recommendation", foodRec   != null ? foodRec   : "");
            entry.put("estimatedGrams", estGrams * loggedPortions);
            entry.put("fistDetected",   fistDetected);

            JSONArray newArr = new JSONArray();
            newArr.put(entry);
            for (int i = 0; i < arr.length() && i < 99; i++) {
                newArr.put(arr.getJSONObject(i));
            }
            prefs.edit().putString(KEY_HISTORY, newArr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void checkGoalReached(int justAdded) {
        try {
            SharedPreferences prefs = getSharedPreferences(
                    PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            int       dailyGoal = prefs.getInt(KEY_DAILY, 2000);
            String    json      = prefs.getString(KEY_HISTORY, "[]");
            String    today     = new SimpleDateFormat("dd MMM",
                    Locale.getDefault()).format(new Date());
            int       total     = 0;
            JSONArray arr       = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject e = arr.getJSONObject(i);
                if (e.optString("date", "").startsWith(today)) {
                    total += e.optInt("calories", 0);
                }
            }
            if (total >= dailyGoal && total - justAdded < dailyGoal) {
                sendGoalNotification(total, dailyGoal);
            }
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
                            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                nm.notify(1001, builder.build());
            }
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

    private boolean isFoodDangerousForCondition(String disease, String foodName,
                                                int kcalPer100, String category) {
        if (disease == null || disease.equalsIgnoreCase("None") || disease.isEmpty()) return false;
        String food = foodName.toLowerCase(java.util.Locale.getDefault());
        String cat  = category.toLowerCase(java.util.Locale.getDefault());
        String dis  = disease.toLowerCase(java.util.Locale.getDefault());

        boolean isSweet = food.contains("chocolate") || food.contains("candy") ||
                food.contains("cake") || food.contains("cookie") || food.contains("donut") ||
                food.contains("sugar") || food.contains("ice cream") || food.contains("waffle") ||
                food.contains("pancake") || food.contains("honey") || food.contains("syrup") ||
                cat.contains("snack") || cat.contains("dessert") || kcalPer100 > 350;

        boolean isFatty = cat.contains("fast food") || food.contains("fries") ||
                food.contains("burger") || food.contains("pizza") || kcalPer100 > 400;

        boolean isSalty = food.contains("chip") || food.contains("pickle") ||
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

        String food    = foodName.toLowerCase(java.util.Locale.getDefault());
        String cat     = category.toLowerCase(java.util.Locale.getDefault());
        String dis     = disease.toLowerCase(java.util.Locale.getDefault());
        StringBuilder warning = new StringBuilder();

        boolean isSweet   = food.contains("chocolate") || food.contains("candy") ||
                food.contains("cake") || food.contains("cookie") ||
                food.contains("donut") || food.contains("sugar") ||
                food.contains("ice cream") || food.contains("waffle") ||
                food.contains("pancake") || cat.contains("snack") ||
                cat.contains("dessert") || kcalPer100 > 300;

        boolean isFatty   = cat.contains("fast food") || food.contains("fries") ||
                food.contains("burger") || food.contains("pizza") ||
                kcalPer100 > 400;

        boolean isSalty   = food.contains("chip") || food.contains("pickle") ||
                food.contains("sausage") || food.contains("bacon");

        boolean isHighProt = cat.contains("meat") || cat.contains("fish") ||
                cat.contains("protein");

        if (dis.contains("diabetes")) {
            if (isSweet) {
                warning.append("WARNING: DIABETES\n");
                warning.append("This food is high in sugar and carbs. It can spike your blood glucose. ");
                warning.append("Consider avoiding or eating a very small portion.");
            } else if (isFatty) {
                warning.append("CAUTION: DIABETES\n");
                warning.append("High-fat foods can increase insulin resistance. ");
                warning.append("Prefer lean proteins and vegetables instead.");
            }
        }

        if (dis.contains("hypertension") || dis.contains("heart")) {
            if (isSalty || isFatty) {
                if (warning.length() > 0) warning.append("\n\n");
                warning.append("WARNING: HEART / HYPERTENSION\n");
                warning.append("This food is high in sodium or saturated fat which can raise blood pressure. ");
                warning.append("Choose fruits, vegetables or grilled fish instead.");
            }
        }

        if (dis.contains("kidney")) {
            if (isHighProt) {
                if (warning.length() > 0) warning.append("\n\n");
                warning.append("CAUTION: KIDNEY DISEASE\n");
                warning.append("High-protein foods put extra strain on kidneys. ");
                warning.append("Consult your doctor about safe protein limits.");
            }
        }

        if (dis.contains("obesity") || dis.contains("cholesterol")) {
            if (isFatty || isSweet) {
                if (warning.length() > 0) warning.append("\n\n");
                warning.append("CAUTION: WEIGHT / CHOLESTEROL\n");
                warning.append("This food is calorie-dense or high in fat. ");
                warning.append("Consider a smaller portion or a healthier alternative.");
            }
        }

        if (dis.contains("asthma")) {
            if (food.contains("wine") || food.contains("beer") ||
                    food.contains("preserved") || food.contains("processed")) {
                if (warning.length() > 0) warning.append("\n\n");
                warning.append("CAUTION: ASTHMA\n");
                warning.append("Processed or sulfite-containing foods may trigger asthma symptoms.");
            }
        }

        return warning.toString();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_down);
    }
}