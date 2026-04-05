package rafayel.hakobyan.EatForFit.ui.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import rafayel.hakobyan.EatForFit.RegisterViewModel;
import rafayel.hakobyan.EatForFit.R;

public class LoginFragment extends Fragment {

    private RegisterViewModel registerViewModel;
    private TextInputEditText emailEditText, passwordEditText, confirmPasswordEditText;
    private TextInputLayout confirmPasswordLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailEditText          = view.findViewById(R.id.username);
        passwordEditText       = view.findViewById(R.id.password);
        confirmPasswordEditText = view.findViewById(R.id.confirmPassword);
        confirmPasswordLayout  = view.findViewById(R.id.confirmPasswordLayout);

        registerViewModel = new ViewModelProvider(requireActivity()).get(RegisterViewModel.class);

        emailEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                registerViewModel.setEmail(s.toString().trim());
            }
        });

        passwordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                registerViewModel.setPassword(s.toString());
                // Re-validate confirm field live
                validateConfirmPassword();
            }
        });

        confirmPasswordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                validateConfirmPassword();
            }
        });
    }

    private void validateConfirmPassword() {
        String password = passwordEditText.getText() != null
                ? passwordEditText.getText().toString() : "";
        String confirm  = confirmPasswordEditText.getText() != null
                ? confirmPasswordEditText.getText().toString() : "";

        if (!confirm.isEmpty() && !confirm.equals(password)) {
            confirmPasswordLayout.setError("Passwords do not match");
        } else {
            confirmPasswordLayout.setError(null);
        }
    }

    /** Called by Register.java before moving to step 2 */
    public boolean passwordsMatch() {
        String password = passwordEditText.getText() != null
                ? passwordEditText.getText().toString() : "";
        String confirm  = confirmPasswordEditText.getText() != null
                ? confirmPasswordEditText.getText().toString() : "";
        return password.equals(confirm);
    }

    /** Called by Register.java to check confirm field is not empty */
    public boolean confirmFilled() {
        String confirm = confirmPasswordEditText.getText() != null
                ? confirmPasswordEditText.getText().toString() : "";
        return !confirm.trim().isEmpty();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}