package com.smartshift.myapplication.models;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import com.smartshift.myapplication.R;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvForgotPassword; // המשתנה החדש
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnRegister = view.findViewById(R.id.btnRegister);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword); // חיבור לכפתור החדש

        // אם המשתמש כבר מחובר - נעביר אותו לדשבורד
        if (mAuth.getCurrentUser() != null) {
            Navigation.findNavController(view).navigate(R.id.action_login_to_dashboard);
        }

        btnLogin.setOnClickListener(v -> loginUser(view));

        btnRegister.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_register)
        );

        // מאזין ל"שכחתי סיסמה"
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showRecoverPasswordDialog());
        }
    }

    private void loginUser(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false); // למנוע לחיצות כפולות
        btnLogin.setText("מתחבר...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("התחבר");

                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).navigate(R.id.action_login_to_dashboard);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "שגיאה";
                        Toast.makeText(getContext(), "שגיאה בהתחברות: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- הפונקציה החדשה לשחזור סיסמה ---
    private void showRecoverPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("שחזור סיסמה");
        builder.setMessage("הכנס את כתובת האימייל שלך ונשלח לך קישור לאיפוס הסיסמה:");

        // יצירת שדה טקסט בתוך הדיאלוג
        final EditText emailInput = new EditText(getContext());
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setHint("email@example.com");
        builder.setView(emailInput);

        // כפתור "שלח"
        builder.setPositiveButton("שלח קישור", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getContext(), "נא להזין כתובת אימייל תקינה", Toast.LENGTH_SHORT).show();
                return;
            }

            beginRecovery(email);
        });

        // כפתור "ביטול"
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void beginRecovery(String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "נשלח אימייל לאיפוס סיסמה לכתובת: " + email, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "שגיאה בשליחת האימייל. וודא שהכתובת נכונה.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}