package rafayel.hakobyan.EatForFit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "food_scanner_prefs";
    private static final String KEY_HISTORY = "food_history";
    private static final String KEY_DAILY   = "daily_calorie_goal";

    private TextView    tvSelectedDate, tvDayCalories, tvDayGoal, tvDayStatus;
    private LinearLayout layoutMeals;
    private String      currentUserId = "default";
    private int         dailyGoal     = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUserId = user.getUid();

        SharedPreferences prefs = getSharedPreferences(
                PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
        dailyGoal = prefs.getInt(KEY_DAILY, 2000);

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvDayCalories  = findViewById(R.id.tvDayCalories);
        tvDayGoal      = findViewById(R.id.tvDayGoal);
        tvDayStatus    = findViewById(R.id.tvDayStatus);
        layoutMeals    = findViewById(R.id.layoutMeals);

        tvDayGoal.setText(String.valueOf(dailyGoal));

        findViewById(R.id.btnCalendarBack).setOnClickListener(v -> finish());

        CalendarView calendarView = findViewById(R.id.calendarView);

        String todayLabel = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvSelectedDate.setText(todayLabel);
        loadDayData(new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date()));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            String fullLabel  = new SimpleDateFormat("dd MMM yyyy",
                    Locale.getDefault()).format(cal.getTime());
            String shortLabel = new SimpleDateFormat("dd MMM",
                    Locale.getDefault()).format(cal.getTime());
            tvSelectedDate.setText(fullLabel);
            loadDayData(shortLabel);
        });
    }

    private void loadDayData(String dayPrefix) {
        layoutMeals.removeAllViews();
        try {
            SharedPreferences prefs = getSharedPreferences(
                    PREFS_NAME + "_" + currentUserId, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_HISTORY, null);
            if (json == null || json.isEmpty()) {
                showEmptyState();
                return;
            }

            JSONArray arr        = new JSONArray(json);
            int       totalCal   = 0;
            int       totalProt  = 0;
            int       totalCarbs = 0;
            int       totalFats  = 0;
            boolean   hasData    = false;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject entry = arr.getJSONObject(i);
                String date = entry.optString("date", "");
                if (!date.startsWith(dayPrefix)) continue;

                hasData    = true;
                int cal    = entry.optInt("calories", 0);
                int prot   = entry.optInt("protein",  0);
                int carbs  = entry.optInt("carbs",    0);
                int fats   = entry.optInt("fats",     0);
                totalCal   += cal;
                totalProt  += prot;
                totalCarbs += carbs;
                totalFats  += fats;

                String name  = entry.optString("name", "Food");
                String emoji = entry.optString("emoji", "🍽️");
                String time  = date.contains(",") ? date.split(",")[1].trim() : "";
                int    grams = entry.optInt("estimatedGrams", 0);

                layoutMeals.addView(buildMealCard(emoji, name, cal, prot, carbs, fats, grams, time));
            }

            tvDayCalories.setText(String.valueOf(totalCal));
            tvDayGoal.setText(String.valueOf(dailyGoal));

            if (!hasData) {
                showEmptyState();
                tvDayCalories.setText("0");
                tvDayStatus.setText("No meals logged this day");
                tvDayStatus.setTextColor(0xFF888888);
            } else {
                float pct = dailyGoal > 0 ? (float) totalCal / dailyGoal : 0f;
                if (pct >= 1.0f) {
                    tvDayStatus.setText("🎉 Goal reached!  💪 " + totalProt + "g protein  🌾 " + totalCarbs + "g carbs  🫙 " + totalFats + "g fats");
                    tvDayStatus.setTextColor(0xFF4CAF50);
                } else if (pct >= 0.6f) {
                    tvDayStatus.setText("📈 " + Math.round(pct * 100) + "% of goal  💪 " + totalProt + "g  🌾 " + totalCarbs + "g  🫙 " + totalFats + "g");
                    tvDayStatus.setTextColor(0xFFFFC107);
                } else {
                    tvDayStatus.setText("📉 " + Math.round(pct * 100) + "% of goal  💪 " + totalProt + "g  🌾 " + totalCarbs + "g  🫙 " + totalFats + "g");
                    tvDayStatus.setTextColor(0xFFE53935);
                }
            }

        } catch (Exception e) {
            showEmptyState();
        }
    }

    private CardView buildMealCard(String emoji, String name, int cal,
                                   int prot, int carbs, int fats, int grams, String time) {
        CardView card = new CardView(this);
        CardView.LayoutParams cp = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(10);
        card.setLayoutParams(cp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackgroundColor(0xFFFFFFFF);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(28);
        tvEmoji.setPadding(0, 0, dp(12), 0);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(14);
        tvName.setTextColor(0xFFCC5803);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvMacros = new TextView(this);
        tvMacros.setText("protein " + prot + "g   carbs " + carbs + "g   fat " + fats + "g" +
                (grams > 0 ? "   (" + grams + "g eaten)" : ""));
        tvMacros.setTextSize(11);
        tvMacros.setTextColor(0xFF888888);
        tvMacros.setPadding(0, dp(2), 0, 0);

        TextView tvTime = new TextView(this);
        tvTime.setText(time);
        tvTime.setTextSize(11);
        tvTime.setTextColor(0xFFBBBBBB);

        LinearLayout calBox = new LinearLayout(this);
        calBox.setOrientation(LinearLayout.VERTICAL);
        calBox.setGravity(Gravity.CENTER);
        calBox.setBackgroundColor(0xFFFFF0D6);
        calBox.setPadding(dp(10), dp(6), dp(10), dp(6));

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFFFFF0D6);
        bg.setCornerRadius(dp(8));
        calBox.setBackground(bg);

        TextView tvCal = new TextView(this);
        tvCal.setText(String.valueOf(cal));
        tvCal.setTextSize(16);
        tvCal.setTypeface(null, Typeface.BOLD);
        tvCal.setTextColor(0xFFCC5803);

        TextView tvKcal = new TextView(this);
        tvKcal.setText("kcal");
        tvKcal.setTextSize(10);
        tvKcal.setTextColor(0xFF888888);

        calBox.addView(tvCal);
        calBox.addView(tvKcal);

        info.addView(tvName);
        info.addView(tvMacros);
        info.addView(tvTime);

        row.addView(tvEmoji);
        row.addView(info);
        row.addView(calBox);
        card.addView(row);
        return card;
    }

    private void showEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("🍽️\nNo meals logged this day");
        tv.setTextSize(14);
        tv.setTextColor(0xFF888888);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(24), 0, dp(24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(lp);
        layoutMeals.addView(tv);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}