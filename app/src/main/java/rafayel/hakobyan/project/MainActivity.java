package rafayel.hakobyan.project;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private final Fragment[] fragments = {
            new HomeFragment(),
            new ProfileFragment()
    };

    private LinearLayout homeButton, profileButton;
    private ImageView homeIcon, profileIcon;
    private TextView homeLabel, profileLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeButton    = findViewById(R.id.home_button);
        profileButton = findViewById(R.id.profile_button);
        homeIcon      = findViewById(R.id.home_icon);
        profileIcon   = findViewById(R.id.profile_icon);
        homeLabel     = findViewById(R.id.home_label);
        profileLabel  = findViewById(R.id.profile_label);

        if (savedInstanceState == null) {
            showFragment(0);
            updateNavigation(0);
        }

        homeButton.setOnClickListener(v -> {
            showFragment(0);
            updateNavigation(0);
        });

        profileButton.setOnClickListener(v -> {
            showFragment(1);
            updateNavigation(1);
        });
    }

    private void showFragment(int index) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.container, fragments[index])
                .commit();
    }

    private void updateNavigation(int selectedIndex) {

        int activeColor   = ContextCompat.getColor(this, R.color.bronze_spice);
        int inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray);

        homeIcon.setColorFilter(inactiveColor);
        homeLabel.setTextColor(inactiveColor);
        homeButton.setBackgroundColor(0x00000000);

        profileIcon.setColorFilter(inactiveColor);
        profileLabel.setTextColor(inactiveColor);
        profileButton.setBackgroundColor(0x00000000);

        if (selectedIndex == 0) {
            homeIcon.setColorFilter(activeColor);
            homeLabel.setTextColor(activeColor);
            homeButton.setBackgroundColor(0x14CD7F32);
        } else {
            profileIcon.setColorFilter(activeColor);
            profileLabel.setTextColor(activeColor);
            profileButton.setBackgroundColor(0x14CD7F32);
        }
    }
}