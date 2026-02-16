package com.smartshift.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.smartshift.myapplication.models.user; // הנה, חזרנו ל-user קטן!

public class RegisterFragment extends Fragment {

    private EditText etFullName, etEmail, etPassword;
    private EditText etBusinessId;
    private RadioGroup rgRole;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    public RegisterFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");

        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etBusinessId = view.findViewById(R.id.etBusinessId);

        rgRole = view.findViewById(R.id.rgRole);
        btnRegister = view.findViewById(R.id.btnRegister);
        tvGoToLogin = view.findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> performRegistration());

        tvGoToLogin.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_register_to_login)
        );
    }

    private void performRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String businessId = etBusinessId.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || businessId.isEmpty()) {
            Toast.makeText(getContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedRoleId = rgRole.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(getContext(), "נא לבחור תפקיד", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbSelected = getView().findViewById(selectedRoleId);
        String role = rbSelected.getText().toString();

        // שכר התחלתי 0
        double initialHourlyRate = 0.0;

        Toast.makeText(getContext(), "יוצר משתמש...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToDatabase(uid, fullName, email, role, businessId, initialHourlyRate);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(getContext(), "שגיאה: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String fullName, String email, String role, String businessId, double hourlyRate) {
        DatabaseReference usersRef = database.getReference("Users");

        // שימוש במחלקה user (עם אות קטנה)
        user newUser = new user(fullName, email, "", role, hourlyRate, businessId);

        usersRef.child(uid).setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "נרשמת בהצלחה!", Toast.LENGTH_LONG).show();
                        if (getView() != null) {
                            Navigation.findNavController(getView()).navigate(R.id.action_register_to_login);
                        }
                    } else {
                        Toast.makeText(getContext(), "שגיאה בשמירה", Toast.LENGTH_LONG).show();
                    }
                });
    }
}