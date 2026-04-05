package rafayel.hakobyan.EatForFit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import rafayel.hakobyan.EatForFit.R;

public class SignInFragment extends Fragment {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout   emailLayout, passwordLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etEmail       = view.findViewById(R.id.etEmail);
        etPassword    = view.findViewById(R.id.etPassword);
        emailLayout   = view.findViewById(R.id.emailLayout);
        passwordLayout = view.findViewById(R.id.passwordLayout);
    }

    /** Called by SignInActivity to read email at submit time */
    public String getEmail() {
        if (etEmail == null || etEmail.getText() == null) return "";
        return etEmail.getText().toString().trim();
    }

    /** Called by SignInActivity to read password at submit time */
    public String getPassword() {
        if (etPassword == null || etPassword.getText() == null) return "";
        return etPassword.getText().toString();
    }

    /** Show error on email field */
    public void setEmailError(String error) {
        if (emailLayout != null) emailLayout.setError(error);
    }

    /** Show error on password field */
    public void setPasswordError(String error) {
        if (passwordLayout != null) passwordLayout.setError(error);
    }

    /** Clear all field errors */
    public void clearErrors() {
        if (emailLayout  != null) emailLayout.setError(null);
        if (passwordLayout != null) passwordLayout.setError(null);
    }

    /** Focus the email field */
    public void focusEmail() {
        if (etEmail != null) etEmail.requestFocus();
    }

    /** Focus the password field */
    public void focusPassword() {
        if (etPassword != null) etPassword.requestFocus();
    }
}