package com.smartshift.myapplication.models;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smartshift.myapplication.R;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    public static String GLOBAL_BUSINESS_ID = "";

    private TextView tvWelcome, tvRole, tvMonthlyWage;
    private CardView cvMonthlyWage;
    private Button btnClockInOut, btnLogout;

    // כפתור חדש לייצוא PDF
    private Button btnExportPdf;

    // כפתורי מנהל
    private Button btnViewShifts, btnManageSchedule, btnManageWages, btnViewSalaries;
    // --- כפתור מנהל: מי עובד עכשיו ---
    private Button btnLiveStatus;

    // כפתורי עובד (וגם שוק ההחלפות שייך לכולם)
    private Button btnSubmitSchedule, btnViewMySchedule;
    // --- כפתור חדש: שוק ההחלפות ---
    private Button btnMarketplace;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    private boolean isWorking = false;
    private String currentShiftId = "";
    private String currentUserName = "";
    private double currentUserRate = 0.0;

    // רשימה לשמירת המשמרות של החודש הנוכחי (עבור ה-PDF)
    private List<Shift> currentMonthShifts = new ArrayList<>();

    public DashboardFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvRole = view.findViewById(R.id.tvRole);
        tvMonthlyWage = view.findViewById(R.id.tvMonthlyWage);
        cvMonthlyWage = view.findViewById(R.id.cvMonthlyWage);

        btnClockInOut = view.findViewById(R.id.btnClockInOut);
        btnLogout = view.findViewById(R.id.btnLogout);

        // אתחול כפתורים מיוחדים
        btnExportPdf = view.findViewById(R.id.btnExportPdf);
        btnLiveStatus = view.findViewById(R.id.btnLiveStatus);
        btnMarketplace = view.findViewById(R.id.btnMarketplace); // <--- אתחול הכפתור החדש

        // כפתורי מנהל
        btnViewShifts = view.findViewById(R.id.btnViewShifts);
        btnManageSchedule = view.findViewById(R.id.btnManageSchedule);
        btnManageWages = view.findViewById(R.id.btnManageWages);
        btnViewSalaries = view.findViewById(R.id.btnViewSalaries);

        // כפתורי עובד
        btnSubmitSchedule = view.findViewById(R.id.btnSubmitSchedule);
        btnViewMySchedule = view.findViewById(R.id.btnViewMySchedule);

        loadUserData();

        btnClockInOut.setOnClickListener(v -> handleClockInOut());

        cvMonthlyWage.setOnClickListener(v -> showMonthlyShiftsDialog());

        // מאזין לכפתור ייצוא PDF
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> {
                if (currentMonthShifts.isEmpty()) {
                    Toast.makeText(getContext(), "אין נתונים לייצוא החודש", Toast.LENGTH_SHORT).show();
                } else {
                    createAndSavePdf();
                }
            });
        }

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Navigation.findNavController(view).navigate(R.id.action_global_loginFragment);
        });

        // הגדרת ניווט לכפתורים
        setupNavigation(view, btnViewShifts, R.id.action_dashboard_to_shiftsList);
        setupNavigation(view, btnManageSchedule, R.id.action_dashboard_to_managerSchedule);
        setupNavigation(view, btnSubmitSchedule, R.id.action_dashboard_to_request);
        setupNavigation(view, btnViewMySchedule, R.id.action_dashboard_to_workerSchedule);
        setupNavigation(view, btnManageWages, R.id.action_dashboard_to_manageWages);
        setupNavigation(view, btnViewSalaries, R.id.action_dashboard_to_salaryReport);
        setupNavigation(view, btnLiveStatus, R.id.action_dashboard_to_liveStatus);

        // --- ניווט לכפתור שוק ההחלפות ---
        setupNavigation(view, btnMarketplace, R.id.action_dashboard_to_marketplace);
    }

    // --- לוגיקה ליצירת PDF ושמירה ---
    private void createAndSavePdf() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // דף A4
        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page myPage = pdfDocument.startPage(myPageInfo);
        Canvas canvas = myPage.getCanvas();

        // כותרת
        titlePaint.setTextAlign(Paint.Align.RIGHT);
        titlePaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        titlePaint.setTextSize(24);
        canvas.drawText("תלוש שכר / דוח שעות חודשי", 550, 50, titlePaint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(14);
        paint.setColor(Color.BLACK);
        canvas.drawText("עובד/ת: " + currentUserName, 550, 80, paint);
        canvas.drawText("הופק ע\"י SmartShift", 550, 100, paint);

        // קו מפריד
        paint.setStrokeWidth(2);
        canvas.drawLine(50, 110, 550, 110, paint);

        // כותרות טבלה
        int y = 150;
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        canvas.drawText("תאריך", 550, y, paint);
        canvas.drawText("שעות", 400, y, paint);
        canvas.drawText("שכר יומי", 250, y, paint);

        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));
        y += 30;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        double totalHoursSum = 0;

        // מיון המשמרות לפי תאריך (מהישן לחדש)
        Collections.sort(currentMonthShifts, (s1, s2) -> Long.compare(s1.startTime, s2.startTime));

        for (Shift shift : currentMonthShifts) {
            double hours = shift.totalHours;
            double dailyWage = shift.totalWage;
            totalHoursSum += hours;

            canvas.drawText(sdf.format(new Date(shift.startTime)), 550, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", hours), 400, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "₪%.2f", dailyWage), 250, y, paint);

            y += 25;

            // בדיקה אם הדף נגמר
            if (y > 800) {
                pdfDocument.finishPage(myPage);
                myPage = pdfDocument.startPage(myPageInfo);
                canvas = myPage.getCanvas();
                y = 50;
            }
        }

        y += 10;
        canvas.drawLine(50, y, 550, y, paint);
        y += 30;

        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        paint.setTextSize(16);
        canvas.drawText("סה\"כ שעות: " + String.format(Locale.getDefault(), "%.2f", totalHoursSum), 550, y, paint);

        y += 25;
        canvas.drawText("סה\"כ לתשלום: " + tvMonthlyWage.getText().toString(), 550, y, paint);

        pdfDocument.finishPage(myPage);
        savePdfToDownloads(pdfDocument);
    }

    private void savePdfToDownloads(PdfDocument pdfDocument) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String fileName = "Payslip_" + timeStamp + ".pdf";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        ContentResolver resolver = requireContext().getContentResolver();
        Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

        try {
            if (uri != null) {
                OutputStream outputStream = resolver.openOutputStream(uri);
                pdfDocument.writeTo(outputStream);
                pdfDocument.close();
                outputStream.close();
                Toast.makeText(getContext(), "הדוח נשמר בהצלחה בתיקיית ההורדות!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- שאר הפונקציות הקיימות ---

    private void showMonthlyShiftsDialog() {
        if (GLOBAL_BUSINESS_ID.isEmpty()) return;
        String uid = mAuth.getCurrentUser().getUid();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_worker_shifts, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RecyclerView rvShifts = dialogView.findViewById(R.id.rvWorkerShifts);
        Button btnClose = dialogView.findViewById(R.id.btnCloseDialog);

        rvShifts.setLayoutManager(new LinearLayoutManager(getContext()));
        btnClose.setOnClickListener(v -> dialog.dismiss());

        List<Shift> dialogList = new ArrayList<>(currentMonthShifts);
        Collections.sort(dialogList, (s1, s2) -> Long.compare(s2.startTime, s1.startTime));

        rvShifts.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_shift, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Shift shift = dialogList.get(position);
                TextView tvDate = holder.itemView.findViewById(R.id.tvShiftDate);
                TextView tvTime = holder.itemView.findViewById(R.id.tvShiftTime);
                TextView tvWage = holder.itemView.findViewById(R.id.tvShiftWage);
                Button btnEdit = holder.itemView.findViewById(R.id.btnEditShift);

                btnEdit.setVisibility(View.GONE);

                SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

                tvDate.setText(sdfDate.format(new Date(shift.startTime)));
                tvTime.setText(String.format("%s - %s (%.2f שעות)",
                        sdfTime.format(new Date(shift.startTime)),
                        sdfTime.format(new Date(shift.endTime)),
                        shift.totalHours));
                tvWage.setText(String.format("שכר: %.2f ₪", shift.totalWage));
            }

            @Override
            public int getItemCount() { return dialogList.size(); }
        });

        dialog.show();
    }

    private void setupNavigation(View view, Button button, int actionId) {
        if (button != null) {
            button.setOnClickListener(v -> {
                try { Navigation.findNavController(view).navigate(actionId); } catch (Exception e) {}
            });
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        database.getReference("Users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user userData = null;
                    try { userData = snapshot.getValue(user.class); } catch (Exception e) {}

                    if (userData != null) {
                        currentUserName = userData.fullName;
                        currentUserRate = userData.hourlyRate;
                        GLOBAL_BUSINESS_ID = userData.businessId;

                        tvWelcome.setText("שלום, " + userData.fullName);
                        tvRole.setText("תפקיד: " + userData.role);

                        String role = userData.role != null ? userData.role.trim() : "";

                        if (role.equalsIgnoreCase("מנהל") || role.equalsIgnoreCase("Manager")) {
                            showManagerButtons();
                            cvMonthlyWage.setVisibility(View.GONE);
                            if (btnExportPdf != null) btnExportPdf.setVisibility(View.GONE);
                        } else {
                            showWorkerButtons();
                            checkOpenShift();
                            calculateMonthlyWage();
                            if (btnExportPdf != null) btnExportPdf.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void calculateMonthlyWage() {
        if (GLOBAL_BUSINESS_ID.isEmpty()) return;
        String uid = mAuth.getCurrentUser().getUid();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        database.getReference("Businesses").child(GLOBAL_BUSINESS_ID).child("Shifts")
                .orderByChild("userId").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalWage = 0.0;
                        currentMonthShifts.clear();

                        for (DataSnapshot shiftSnap : snapshot.getChildren()) {
                            try {
                                Shift shift = shiftSnap.getValue(Shift.class);
                                if (shift != null && shift.startTime >= startOfMonth) {
                                    totalWage += shift.totalWage;
                                    currentMonthShifts.add(shift);
                                }
                            } catch (Exception e) {}
                        }
                        tvMonthlyWage.setText(String.format("%,.2f ₪", totalWage));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showManagerButtons() {
        setVisible(btnViewShifts, true);
        setVisible(btnManageSchedule, true);
        setVisible(btnManageWages, true);
        setVisible(btnViewSalaries, true);
        setVisible(btnLiveStatus, true);

        // מנהל יכול לראות את שוק ההחלפות (כדי לפקח), אפשר לשנות ל-false אם לא תרצי
        setVisible(btnMarketplace, true);

        setVisible(btnClockInOut, false);
        setVisible(btnSubmitSchedule, false);
        setVisible(btnViewMySchedule, false);
    }

    private void showWorkerButtons() {
        setVisible(btnClockInOut, true);
        setVisible(btnSubmitSchedule, true);
        setVisible(btnViewMySchedule, true);

        // עובד רואה את שוק ההחלפות
        setVisible(btnMarketplace, true);

        setVisible(btnViewShifts, false);
        setVisible(btnManageSchedule, false);
        setVisible(btnManageWages, false);
        setVisible(btnViewSalaries, false);
        setVisible(btnLiveStatus, false);
    }

    private void setVisible(Button btn, boolean visible) {
        if (btn != null) btn.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void checkOpenShift() {
        if (GLOBAL_BUSINESS_ID.isEmpty()) return;
        String uid = mAuth.getCurrentUser().getUid();

        database.getReference("Businesses").child(GLOBAL_BUSINESS_ID).child("Shifts")
                .orderByChild("userId").equalTo(uid).limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isWorking = false;
                        currentShiftId = "";
                        if (snapshot.exists()) {
                            for (DataSnapshot shiftSnapshot : snapshot.getChildren()) {
                                Shift shift = shiftSnapshot.getValue(Shift.class);
                                if (shift != null && shift.endTime == 0) {
                                    isWorking = true;
                                    currentShiftId = shift.shiftId;
                                }
                            }
                        }
                        updateClockButtonUI();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void handleClockInOut() {
        if (GLOBAL_BUSINESS_ID.isEmpty()) {
            Toast.makeText(getContext(), "שגיאה: טרם נטען מזהה עסק", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference shiftsRef = database.getReference("Businesses").child(GLOBAL_BUSINESS_ID).child("Shifts");

        if (!isWorking) {
            String newShiftId = shiftsRef.push().getKey();
            Shift newShift = new Shift(newShiftId, uid, currentUserName, System.currentTimeMillis());

            shiftsRef.child(newShiftId).setValue(newShift).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "נכנסת למשמרת!", Toast.LENGTH_SHORT).show();
                    isWorking = true;
                    currentShiftId = newShiftId;
                    updateClockButtonUI();
                }
            });

        } else {
            if (currentShiftId.isEmpty()) {
                checkOpenShift();
                return;
            }

            long endTime = System.currentTimeMillis();

            shiftsRef.child(currentShiftId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Shift shift = snapshot.getValue(Shift.class);
                    if (shift != null) {
                        database.getReference("Businesses").child(GLOBAL_BUSINESS_ID)
                                .child("settings").child("travelAmount")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot settingsSnap) {
                                        double travelAmount = 0.0;
                                        if (settingsSnap.exists()) {
                                            try { travelAmount = settingsSnap.getValue(Double.class); } catch(Exception e){}
                                        }

                                        double wage = calculateComplexWage(shift.startTime, endTime, currentUserRate, travelAmount);
                                        double durationMs = (double) (endTime - shift.startTime);
                                        double hours = durationMs / (1000.0 * 60.0 * 60.0);

                                        shiftsRef.child(currentShiftId).child("endTime").setValue(endTime);
                                        shiftsRef.child(currentShiftId).child("totalHours").setValue(hours);
                                        shiftsRef.child(currentShiftId).child("totalWage").setValue(wage)
                                                .addOnSuccessListener(aVoid -> {
                                                    String msg = String.format("משמרת הסתיימה.\nשעות: %.2f\nשכר: %.2f ₪", hours, wage);
                                                    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                                                    isWorking = false;
                                                    currentShiftId = "";
                                                    updateClockButtonUI();
                                                });
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
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

    private void updateClockButtonUI() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (isWorking) {
                btnClockInOut.setText("יציאה ממשמרת");
                btnClockInOut.setBackgroundColor(Color.RED);
            } else {
                btnClockInOut.setText("כניסה למשמרת");
                btnClockInOut.setBackgroundColor(Color.parseColor("#4CAF50"));
            }
        });
    }
}