package rafayel.hakobyan.EatForFit.ui.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import rafayel.hakobyan.EatForFit.RegisterViewModel;
import rafayel.hakobyan.EatForFit.R;

public class Characteristics extends Fragment {

    private RegisterViewModel registerViewModel;
    private TextInputEditText weightEditText, heightEditText, nameEditText;
    private AutoCompleteTextView diseaseSpinner, activitySpinner;
    private TextView tvActivityDesc;

    private final String[] ACTIVITY_LEVELS = {
            "Sedentary — little or no exercise",
            "Lightly active — 1-2 days/week",
            "Moderately active — 3-4 days/week",
            "Very active — 5-6 days/week",
            "Extra active — intense daily training"
    };

    private final String[] ACTIVITY_KEYS = {
            "sedentary",
            "lightly_active",
            "moderately_active",
            "very_active",
            "extra_active"
    };

    private final String[] ACTIVITY_DESCRIPTIONS = {
            "Desk job, minimal movement during the day",
            "Light workouts or walks 1-2 times per week",
            "Gym or sports 3-4 times per week",
            "Hard training 5-6 days per week",
            "Athletes or physical jobs with daily intense training"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_characteristics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameEditText   = view.findViewById(R.id.name);
        weightEditText = view.findViewById(R.id.weight);
        heightEditText = view.findViewById(R.id.height);
        diseaseSpinner = view.findViewById(R.id.spinner);
        activitySpinner = view.findViewById(R.id.activitySpinner);
        tvActivityDesc  = view.findViewById(R.id.tvActivityDesc);

        registerViewModel = new ViewModelProvider(requireActivity()).get(RegisterViewModel.class);

        nameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                registerViewModel.setName(s.toString());
            }
        });

        weightEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                registerViewModel.setWeight(s.toString());
            }
        });

        heightEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                registerViewModel.setHeight(s.toString());
            }
        });

        String[] diseases = new String[]{
                "None", "Diabetes", "Hypertension",
                "Heart Disease", "Asthma", "Obesity",
                "High Cholesterol", "Kidney Disease", "Other"
        };
        ArrayAdapter<String> diseaseAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                diseases
        );
        diseaseSpinner.setAdapter(diseaseAdapter);
        diseaseSpinner.setOnItemClickListener((parent, v, position, id) ->
                registerViewModel.setDisease(diseases[position])
        );

        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ACTIVITY_LEVELS
        );
        activitySpinner.setAdapter(activityAdapter);
        activitySpinner.setOnItemClickListener((parent, v, position, id) -> {
            registerViewModel.setActivity(ACTIVITY_KEYS[position]);
            tvActivityDesc.setText(ACTIVITY_DESCRIPTIONS[position]);
        });

        registerViewModel.setActivity(ACTIVITY_KEYS[2]);
        activitySpinner.setText(ACTIVITY_LEVELS[2], false);
        tvActivityDesc.setText(ACTIVITY_DESCRIPTIONS[2]);
    }

    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}