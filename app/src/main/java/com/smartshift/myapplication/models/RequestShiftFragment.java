package com.smartshift.myapplication.models;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.smartshift.myapplication.R;
import java.util.HashMap;
import java.util.Map;

public class RequestShiftFragment extends Fragment {

    // משתנים לכל הצ'קבוקסים
    private CheckBox cbSunMorn, cbSunNoon, cbSunNight;
    private CheckBox cbMonMorn, cbMonNoon, cbMonNight;
    private CheckBox cbTueMorn, cbTueNoon, cbTueNight;
    private CheckBox cbWedMorn, cbWedNoon, cbWedNight;
    private CheckBox cbThuMorn, cbThuNoon, cbThuNight;
    private CheckBox cbFriMorn, cbFriNoon, cbFriNight;
    private CheckBox cbSatMorn, cbSatNoon, cbSatNight;

    private Button btnSubmit, btnBack;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // טוען את העיצוב החדש שיצרנו
        return inflater.inflate(R.layout.fragment_request_shift, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // אתחול פיירבייס
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        // חיבור הכפתורים (שים לב לשם המעודכן: btnSubmitRequest)
        btnSubmit = view.findViewById(R.id.btnSubmitRequest);
        btnBack = view.findViewById(R.id.btnBack);

        // חיבור כל הצ'קבוקסים
        initCheckBoxes(view);

        // בדיקה למניעת קריסה אם הכפתור לא נמצא
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> submitAvailability());
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }
    }

    private void initCheckBoxes(View view) {
        // ראשון
        cbSunMorn = view.findViewById(R.id.cbSunMorn);
        cbSunNoon = view.findViewById(R.id.cbSunNoon);
        cbSunNight = view.findViewById(R.id.cbSunNight);

        // שני
        cbMonMorn = view.findViewById(R.id.cbMonMorn);
        cbMonNoon = view.findViewById(R.id.cbMonNoon);
        cbMonNight = view.findViewById(R.id.cbMonNight);

        // שלישי
        cbTueMorn = view.findViewById(R.id.cbTueMorn);
        cbTueNoon = view.findViewById(R.id.cbTueNoon);
        cbTueNight = view.findViewById(R.id.cbTueNight);

        // רביעי
        cbWedMorn = view.findViewById(R.id.cbWedMorn);
        cbWedNoon = view.findViewById(R.id.cbWedNoon);
        cbWedNight = view.findViewById(R.id.cbWedNight);

        // חמישי
        cbThuMorn = view.findViewById(R.id.cbThuMorn);
        cbThuNoon = view.findViewById(R.id.cbThuNoon);
        cbThuNight = view.findViewById(R.id.cbThuNight);

        // שישי
        cbFriMorn = view.findViewById(R.id.cbFriMorn);
        cbFriNoon = view.findViewById(R.id.cbFriNoon);
        cbFriNight = view.findViewById(R.id.cbFriNight);

        // שבת
        cbSatMorn = view.findViewById(R.id.cbSatMorn);
        cbSatNoon = view.findViewById(R.id.cbSatNoon);
        cbSatNight = view.findViewById(R.id.cbSatNight);
    }

    private void submitAvailability() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID == null || DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) {
            Toast.makeText(getContext(), "שגיאה: לא זוהה מזהה עסק. נסה להתחבר מחדש.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> availabilityMap = new HashMap<>();

        try {
            // איסוף הנתונים (בטוח מפני קריסה)
            availabilityMap.put("Sunday_Morning", cbSunMorn != null && cbSunMorn.isChecked());
            availabilityMap.put("Sunday_Noon", cbSunNoon != null && cbSunNoon.isChecked());
            availabilityMap.put("Sunday_Night", cbSunNight != null && cbSunNight.isChecked());

            availabilityMap.put("Monday_Morning", cbMonMorn != null && cbMonMorn.isChecked());
            availabilityMap.put("Monday_Noon", cbMonNoon != null && cbMonNoon.isChecked());
            availabilityMap.put("Monday_Night", cbMonNight != null && cbMonNight.isChecked());

            availabilityMap.put("Tuesday_Morning", cbTueMorn != null && cbTueMorn.isChecked());
            availabilityMap.put("Tuesday_Noon", cbTueNoon != null && cbTueNoon.isChecked());
            availabilityMap.put("Tuesday_Night", cbTueNight != null && cbTueNight.isChecked());

            availabilityMap.put("Wednesday_Morning", cbWedMorn != null && cbWedMorn.isChecked());
            availabilityMap.put("Wednesday_Noon", cbWedNoon != null && cbWedNoon.isChecked());
            availabilityMap.put("Wednesday_Night", cbWedNight != null && cbWedNight.isChecked());

            availabilityMap.put("Thursday_Morning", cbThuMorn != null && cbThuMorn.isChecked());
            availabilityMap.put("Thursday_Noon", cbThuNoon != null && cbThuNoon.isChecked());
            availabilityMap.put("Thursday_Night", cbThuNight != null && cbThuNight.isChecked());

            availabilityMap.put("Friday_Morning", cbFriMorn != null && cbFriMorn.isChecked());
            availabilityMap.put("Friday_Noon", cbFriNoon != null && cbFriNoon.isChecked());
            availabilityMap.put("Friday_Night", cbFriNight != null && cbFriNight.isChecked());

            availabilityMap.put("Saturday_Morning", cbSatMorn != null && cbSatMorn.isChecked());
            availabilityMap.put("Saturday_Noon", cbSatNoon != null && cbSatNoon.isChecked());
            availabilityMap.put("Saturday_Night", cbSatNight != null && cbSatNight.isChecked());

            // שמירה ב-Firebase
            mDatabase.child("Businesses")
                    .child(DashboardFragment.GLOBAL_BUSINESS_ID)
                    .child("Availability")
                    .child(userId)
                    .setValue(availabilityMap)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "המשמרות הוגשו בהצלחה!", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        } else {
                            Toast.makeText(getContext(), "שגיאה בשליחה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            Toast.makeText(getContext(), "אירעה שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}