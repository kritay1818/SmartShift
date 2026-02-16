package com.smartshift.myapplication.models;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smartshift.myapplication.R;
import com.google.firebase.database.DatabaseReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalaryReportFragment extends Fragment {

    private RecyclerView rvReport;
    private TextView tvTotalBusinessExpense;
    private Button btnBack;
    private FirebaseDatabase database;
    private double globalTravelAmount = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_salary_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");

        rvReport = view.findViewById(R.id.rvSalaryReport);
        tvTotalBusinessExpense = view.findViewById(R.id.tvTotalBusinessExpense);
        btnBack = view.findViewById(R.id.btnBack);

        rvReport.setLayoutManager(new LinearLayoutManager(getContext()));
        btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        loadTravelAmountAndCalculate();
    }

    private void loadTravelAmountAndCalculate() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) return;

        database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID)
                .child("settings").child("travelAmount")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            try { globalTravelAmount = snapshot.getValue(Double.class); } catch (Exception e) {}
                        }
                        calculateSalaries();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void calculateSalaries() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) return;

        database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID).child("Shifts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot shiftsSnapshot) {
                        List<SalaryAdapter.SalarySummary> tempSummary = new ArrayList<>();
                        final double[] grandTotalWrapper = {0.0};

                        database.getReference("Users").orderByChild("businessId").equalTo(DashboardFragment.GLOBAL_BUSINESS_ID)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                                        for (DataSnapshot userSnap : usersSnapshot.getChildren()) {
                                            user user = null;
                                            try { user = userSnap.getValue(user.class); } catch (Exception e) { continue; }

                                            String uid = userSnap.getKey();
                                            if (user == null) continue;

                                            double userTotalHours = 0;
                                            double userTotalMoney = 0;

                                            for (DataSnapshot shiftSnap : shiftsSnapshot.getChildren()) {
                                                try {
                                                    Shift shift = shiftSnap.getValue(Shift.class);
                                                    if (shift != null && shift.userId != null && shift.userId.equals(uid)) {
                                                        userTotalHours += shift.totalHours;
                                                        userTotalMoney += shift.totalWage;
                                                    }
                                                } catch (Exception e) {}
                                            }

                                            if (userTotalHours > 0) {
                                                tempSummary.add(new SalaryAdapter.SalarySummary(
                                                        uid, user.fullName, user.hourlyRate, userTotalHours, userTotalMoney
                                                ));
                                                grandTotalWrapper[0] += userTotalMoney;
                                            }
                                        }
                                        updateUI(tempSummary, grandTotalWrapper[0]);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateUI(List<SalaryAdapter.SalarySummary> data, double grandTotal) {
        if (getContext() == null) return;
        tvTotalBusinessExpense.setText(String.format("סה\"כ הוצאות שכר: %,.2f ₪", grandTotal));

        SalaryAdapter adapter = new SalaryAdapter(data, (userId, userName) -> {
            showUserShiftsDialog(userId, userName);
        });
        rvReport.setAdapter(adapter);
    }

    // --- חלון רשימת משמרות + הוספה (מעודכן) ---
    private void showUserShiftsDialog(String userId, String userName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // שימוש ב-XML החדש שיצרנו לרשימה
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_user_shifts_list, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        RecyclerView rvShifts = dialogView.findViewById(R.id.rvDialogShifts);
        Button btnAdd = dialogView.findViewById(R.id.btnAddManualShift);
        Button btnClose = dialogView.findViewById(R.id.btnCloseDialog);

        tvTitle.setText("משמרות של " + userName);
        rvShifts.setLayoutManager(new LinearLayoutManager(getContext()));

        // לחיצה על "הוסף משמרת"
        btnAdd.setOnClickListener(v -> {
            // קודם מביאים תעריף, ואז פותחים דיאלוג הוספה
            fetchRateAndAction(userId, userName, (rate) -> showAddShiftDialog(userId, userName, rate));
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // טעינת המשמרות
        database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID).child("Shifts")
                .orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() { // האזנה בזמן אמת כדי שיראה את ההוספה מיד
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Shift> userShifts = new ArrayList<>();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            try {
                                Shift shift = s.getValue(Shift.class);
                                if (shift != null && shift.endTime > 0) {
                                    userShifts.add(shift);
                                }
                            } catch (Exception e) {}
                        }

                        // מיון לפי תאריך (מהחדש לישן)
                        userShifts.sort((s1, s2) -> Long.compare(s2.startTime, s1.startTime));

                        rvShifts.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                            @NonNull
                            @Override
                            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_shift, parent, false);
                                return new RecyclerView.ViewHolder(v) {};
                            }

                            @Override
                            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                                Shift shift = userShifts.get(position);
                                TextView tvDate = holder.itemView.findViewById(R.id.tvShiftDate);
                                TextView tvTime = holder.itemView.findViewById(R.id.tvShiftTime);
                                TextView tvWage = holder.itemView.findViewById(R.id.tvShiftWage);
                                Button btnEdit = holder.itemView.findViewById(R.id.btnEditShift);

                                SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

                                tvDate.setText(sdfDate.format(new Date(shift.startTime)));
                                tvTime.setText(String.format("%s - %s (%.2f שעות)",
                                        sdfTime.format(new Date(shift.startTime)),
                                        sdfTime.format(new Date(shift.endTime)),
                                        shift.totalHours));
                                tvWage.setText(String.format("שכר: %.2f ₪", shift.totalWage));

                                btnEdit.setOnClickListener(v -> {
                                    fetchRateAndAction(shift.userId, shift.userName, (rate) -> showEditSpecificShiftDialog(shift, rate));
                                });
                            }

                            @Override
                            public int getItemCount() { return userShifts.size(); }
                        });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        dialog.show();
    }

    // פונקציית עזר לשליפת תעריף (כדי לא לכתוב פעמיים)
    interface OnRateFetched { void onRate(double rate); }
    private void fetchRateAndAction(String userId, String userName, OnRateFetched action) {
        database.getReference("Users").child(userId).child("hourlyRate")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double rate = snapshot.getValue(Double.class);
                        if (rate != null) {
                            action.onRate(rate);
                        } else {
                            Toast.makeText(getContext(), "לא נמצא תעריף לעובד", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- דיאלוג הוספת משמרת חדשה ---
    private void showAddShiftDialog(String userId, String userName, double hourlyRate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_shift, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle); // נניח שיש ID כזה, אם לא - הקוד יתעלם
        if(tvTitle == null) { // כותרת מותאמת אישית אם משתמשים באותו layout
            // אפשר להוסיף ID ב-dialog_edit_shift לכותרת או פשוט להשאיר "עריכה"
        }

        Button btnStart = dialogView.findViewById(R.id.btnEditStartTime);
        Button btnEnd = dialogView.findViewById(R.id.btnEditEndTime);
        Button btnSave = dialogView.findViewById(R.id.btnSaveChanges);

        btnSave.setText("צור משמרת חדשה"); // שינוי טקסט הכפתור

        // ברירת מחדל: היום, 08:00 עד 16:00
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 8); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        final long[] newStart = {cal.getTimeInMillis()};

        cal.set(Calendar.HOUR_OF_DAY, 16);
        final long[] newEnd = {cal.getTimeInMillis()};

        SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        btnStart.setText("התחלה: " + sdfFull.format(new Date(newStart[0])));
        btnEnd.setText("סיום: " + sdfFull.format(new Date(newEnd[0])));

        // בחירת תאריך ושעה (כי בהוספה צריך לבחור גם תאריך)
        btnStart.setOnClickListener(v -> pickDateAndTime(newStart, () -> btnStart.setText("התחלה: " + sdfFull.format(new Date(newStart[0])))));
        btnEnd.setOnClickListener(v -> pickDateAndTime(newEnd, () -> btnEnd.setText("סיום: " + sdfFull.format(new Date(newEnd[0])))));

        btnSave.setOnClickListener(v -> {
            if (newEnd[0] <= newStart[0]) {
                // טיפול במשמרת לילה אוטומטי
                Calendar cS = Calendar.getInstance(); cS.setTimeInMillis(newStart[0]);
                Calendar cE = Calendar.getInstance(); cE.setTimeInMillis(newEnd[0]);

                // אם השעות נמוכות יותר אבל באותו יום, נוסיף יום
                if (cS.get(Calendar.DAY_OF_YEAR) == cE.get(Calendar.DAY_OF_YEAR)) {
                    newEnd[0] += 24 * 60 * 60 * 1000;
                    Toast.makeText(getContext(), "המשמרת חוצה לילה (יום נוסף)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "שים לב: זמן הסיום לפני זמן ההתחלה", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            createNewShiftInFirebase(userId, userName, newStart[0], newEnd[0], hourlyRate);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createNewShiftInFirebase(String userId, String userName, long startTime, long endTime, double hourlyRate) {
        DatabaseReference shiftsRef = database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID).child("Shifts");
        String newShiftId = shiftsRef.push().getKey();

        double wage = calculateComplexWage(startTime, endTime, hourlyRate, globalTravelAmount);
        double durationMs = (double) (endTime - startTime);
        double hours = durationMs / (1000.0 * 60.0 * 60.0);

        Shift newShift = new Shift(newShiftId, userId, userName, startTime);
        newShift.endTime = endTime;
        newShift.totalHours = hours;
        newShift.totalWage = wage;

        if (newShiftId != null) {
            shiftsRef.child(newShiftId).setValue(newShift)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "משמרת נוצרה בהצלחה!", Toast.LENGTH_SHORT).show());
        }
    }

    // --- דיאלוג עריכה (נשאר דומה) ---
    private void showEditSpecificShiftDialog(Shift shift, double hourlyRate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_shift, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button btnStart = dialogView.findViewById(R.id.btnEditStartTime);
        Button btnEnd = dialogView.findViewById(R.id.btnEditEndTime);
        Button btnSave = dialogView.findViewById(R.id.btnSaveChanges);

        final long[] newStart = {shift.startTime};
        final long[] newEnd = {shift.endTime};

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

        btnStart.setText("התחלה: " + sdf.format(new Date(newStart[0])));
        btnEnd.setText("סיום: " + sdf.format(new Date(newEnd[0])));

        btnStart.setOnClickListener(v -> pickTime(newStart, () -> btnStart.setText("התחלה: " + sdf.format(new Date(newStart[0])))));
        btnEnd.setOnClickListener(v -> pickTime(newEnd, () -> btnEnd.setText("סיום: " + sdf.format(new Date(newEnd[0])))));

        btnSave.setOnClickListener(v -> {
            if (newEnd[0] <= newStart[0]) {
                newEnd[0] += 24 * 60 * 60 * 1000;
                Toast.makeText(getContext(), "המשמרת חוצה לילה (עודכן ליום הבא)", Toast.LENGTH_SHORT).show();
            }
            recalculateAndUpdateShift(shift.shiftId, newStart[0], newEnd[0], hourlyRate);
            dialog.dismiss();
        });

        dialog.show();
    }

    // בורר תאריך + שעה (להוספה חדשה)
    private void pickDateAndTime(final long[] timeStore, Runnable onDone) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeStore[0]);

        // שלב 1: בחירת תאריך
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            // שלב 2: בחירת שעה
            new TimePickerDialog(getContext(), (view2, hourOfDay, minute) -> {
                c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                c.set(Calendar.MINUTE, minute);
                timeStore[0] = c.getTimeInMillis();
                onDone.run();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();

        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // בורר רק שעה (לעריכה)
    private void pickTime(final long[] timeStore, Runnable onDone) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeStore[0]);

        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            timeStore[0] = c.getTimeInMillis();
            onDone.run();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void recalculateAndUpdateShift(String shiftId, long startTime, long endTime, double hourlyRate) {
        double wage = calculateComplexWage(startTime, endTime, hourlyRate, globalTravelAmount);
        double durationMs = (double) (endTime - startTime);
        double hours = durationMs / (1000.0 * 60.0 * 60.0);

        database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID)
                .child("Shifts").child(shiftId).updateChildren(new java.util.HashMap<String, Object>() {{
                    put("startTime", startTime);
                    put("endTime", endTime);
                    put("totalHours", hours);
                    put("totalWage", wage);
                }}).addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "משמרת עודכנה!", Toast.LENGTH_SHORT).show());
    }

    private double calculateComplexWage(long startTime, long endTime, double baseRate, double travelAmount) {
        double durationMs = (double) (endTime - startTime);
        double totalHours = durationMs / (1000.0 * 60.0 * 60.0);

        double SATURDAY_MULTIPLIER = 1.5;
        double OVERTIME_MULTIPLIER = 1.25;
        double OVERTIME_THRESHOLD = 8.0;

        double finalWage = 0;
        double effectiveRate = baseRate;

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startTime);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        boolean isShabbat = (dayOfWeek == Calendar.SATURDAY) ||
                (dayOfWeek == Calendar.FRIDAY && c.get(Calendar.HOUR_OF_DAY) >= 17);

        if (isShabbat) {
            effectiveRate = baseRate * SATURDAY_MULTIPLIER;
        }

        if (totalHours > OVERTIME_THRESHOLD) {
            finalWage += (OVERTIME_THRESHOLD * effectiveRate);
            double overtimeHours = totalHours - OVERTIME_THRESHOLD;
            finalWage += (overtimeHours * (effectiveRate * OVERTIME_MULTIPLIER));
        } else {
            finalWage += (totalHours * effectiveRate);
        }

        finalWage += travelAmount;
        return finalWage;
    }
}