package rafayel.hakobyan.project.ui.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import rafayel.hakobyan.project.RegisterViewModel;
import rafayel.hakobyan.project.R;

public class Characteristics extends Fragment {

    private RegisterViewModel registerViewModel;
    private TextInputEditText weightEditText, heightEditText, nameEditText;
    private AutoCompleteTextView diseaseSpinner;

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

        // Diseases dropdown
        String[] diseases = new String[]{"None", "Diabetes", "Hypertension",
                "Heart Disease", "Asthma", "Obesity", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                diseases
        );
        diseaseSpinner.setAdapter(adapter);
        diseaseSpinner.setOnItemClickListener((parent, v, position, id) ->
                registerViewModel.setDisease(diseases[position])
        );
    }

    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}