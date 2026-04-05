package rafayel.hakobyan.EatForFit;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private LinearLayout homeButton, mealPlanButton, profileButton;
    private ImageView homeIcon, mealPlanIcon, profileIcon;
    private TextView homeLabel, mealPlanLabel, profileLabel;

    private int currentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeButton     = findViewById(R.id.home_button);
        mealPlanButton = findViewById(R.id.meal_plan_button);
        profileButton  = findViewById(R.id.profile_button);
        homeIcon       = findViewById(R.id.home_icon);
        mealPlanIcon   = findViewById(R.id.meal_plan_icon);
        profileIcon    = findViewById(R.id.profile_icon);
        homeLabel      = findViewById(R.id.home_label);
        mealPlanLabel  = findViewById(R.id.meal_plan_label);
        profileLabel   = findViewById(R.id.profile_label);

        if (savedInstanceState == null) {
            showFragment(0);
            updateNavigation(0);
        }

        homeButton.setOnClickListener(v -> {
            showFragment(0);
            updateNavigation(0);
        });

        mealPlanButton.setOnClickListener(v -> {
            showFragment(1);
            updateNavigation(1);
        });

        profileButton.setOnClickListener(v -> {
            showFragment(2);
            updateNavigation(2);
        });
    }

    private void showFragment(int index) {
        if (currentIndex == index) return;
        currentIndex = index;

        Fragment fragment;
        if (index == 0)      fragment = new HomeFragment();
        else if (index == 1) fragment = new MealPlanFragment();
        else                 fragment = new ProfileFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.container, fragment)
                .commit();
    }

    private void updateNavigation(int selectedIndex) {
        int activeColor   = ContextCompat.getColor(this, R.color.bronze_spice);
        int inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray);

        homeIcon.setColorFilter(inactiveColor);
        homeLabel.setTextColor(inactiveColor);
        homeButton.setBackgroundColor(0x00000000);

        mealPlanIcon.setColorFilter(inactiveColor);
        mealPlanLabel.setTextColor(inactiveColor);
        mealPlanButton.setBackgroundColor(0x00000000);

        profileIcon.setColorFilter(inactiveColor);
        profileLabel.setTextColor(inactiveColor);
        profileButton.setBackgroundColor(0x00000000);

        if (selectedIndex == 0) {
            homeIcon.setColorFilter(activeColor);
            homeLabel.setTextColor(activeColor);
            homeButton.setBackgroundColor(0x14CD7F32);
        } else if (selectedIndex == 1) {
            mealPlanIcon.setColorFilter(activeColor);
            mealPlanLabel.setTextColor(activeColor);
            mealPlanButton.setBackgroundColor(0x14CD7F32);
        } else {
            profileIcon.setColorFilter(activeColor);
            profileLabel.setTextColor(activeColor);
            profileButton.setBackgroundColor(0x14CD7F32);
        }
    }
}