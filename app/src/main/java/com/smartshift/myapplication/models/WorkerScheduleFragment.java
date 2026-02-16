package com.smartshift.myapplication.models;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smartshift.myapplication.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WorkerScheduleFragment extends Fragment {

    private Button btnSyncCalendar, btnBack, btnPrevWeek, btnNextWeek;
    private TextView tvDateRange;
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private List<Shift> shiftsToSync = new ArrayList<>();
    private View rootView;
    private String currentBusinessId = "";
    private int currentWeekOffset = 0;
    private static final int CALENDAR_PERMISSION_CODE = 101;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // טוען את העיצוב החדש
        rootView = inflater.inflate(R.layout.fragment_worker_schedule, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            mAuth = FirebaseAuth.getInstance();
            database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");

            // חיבור בטוח לרכיבים
            btnSyncCalendar = view.findViewById(R.id.btnSyncCalendar);
            btnBack = view.findViewById(R.id.btnBack);
            btnPrevWeek = view.findViewById(R.id.btnPrevWeek);
            btnNextWeek = view.findViewById(R.id.btnNextWeek);
            tvDateRange = view.findViewById(R.id.tvDateRange);

            // בדיקות למניעת קריסה אם הרכיב לא נמצא
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
            }

            if (btnSyncCalendar != null) {
                btnSyncCalendar.setOnClickListener(v -> {
                    // בדיקת הרשאות לפני גישה ליומן
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION_CODE);
                    } else {
                        syncShiftsToCalendar();
                    }
                });
            }

            if (btnPrevWeek != null) {
                btnPrevWeek.setOnClickListener(v -> {
                    currentWeekOffset--;
                    fetchUserDataAndLoadSchedule();
                });
            }

            if (btnNextWeek != null) {
                btnNextWeek.setOnClickListener(v -> {
                    currentWeekOffset++;
                    fetchUserDataAndLoadSchedule();
                });
            }

            fetchUserDataAndLoadSchedule();

        } catch (Exception e) {
            e.printStackTrace();
            // במקום לקרוס, רק נציג הודעה
            Toast.makeText(getContext(), "טעינת המסך נכשלה חלקית", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserDataAndLoadSchedule() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        updateDateRangeUI();
        resetTableColors();

        if (!currentBusinessId.isEmpty()) {
            loadMySchedule(uid);
            return;
        }

        database.getReference("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                user user = null;
                try { user = snapshot.getValue(user.class); } catch (Exception e) {}

                if (user != null && user.businessId != null && !user.businessId.isEmpty()) {
                    currentBusinessId = user.businessId;
                    loadMySchedule(uid);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDateRangeUI() {
        if (tvDateRange == null) return;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Date startDate = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 6);
        Date endDate = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        tvDateRange.setText(sdf.format(startDate) + " - " + sdf.format(endDate));
    }

    private void resetTableColors() {
        if (rootView == null) return;
        int[] allCells = {
                R.id.tvSunMorn, R.id.tvSunNoon, R.id.tvSunNight,
                R.id.tvMonMorn, R.id.tvMonNoon, R.id.tvMonNight,
                R.id.tvTueMorn, R.id.tvTueNoon, R.id.tvTueNight,
                R.id.tvWedMorn, R.id.tvWedNoon, R.id.tvWedNight,
                R.id.tvThuMorn, R.id.tvThuNoon, R.id.tvThuNight,
                R.id.tvFriMorn, R.id.tvFriNoon, R.id.tvFriNight,
                R.id.tvSatMorn, R.id.tvSatNoon, R.id.tvSatNight
        };
        for (int id : allCells) {
            View v = rootView.findViewById(id);
            if (v instanceof TextView) {
                TextView cell = (TextView) v;
                cell.setText("-");
                cell.setBackgroundColor(Color.parseColor("#EEEEEE"));
                cell.setTextColor(Color.BLACK);
                cell.setOnLongClickListener(null);
            }
        }
    }

    private void loadMySchedule(String uid) {
        if (currentBusinessId.isEmpty()) return;

        database.getReference("Businesses").child(currentBusinessId).child("Shifts")
                .orderByChild("userId").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            shiftsToSync.clear();
                            resetTableColors();
                            long now = System.currentTimeMillis();
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);
                            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
                            long startOfWeek = cal.getTimeInMillis();
                            cal.add(Calendar.DAY_OF_YEAR, 7);
                            long endOfWeek = cal.getTimeInMillis();

                            for (DataSnapshot s : snapshot.getChildren()) {
                                Shift shift = s.getValue(Shift.class);
                                if (shift != null) {
                                    if (shift.startTime > now) shiftsToSync.add(shift);
                                    if (shift.startTime >= startOfWeek && shift.startTime < endOfWeek) {
                                        updateTableCell(shift);
                                    }
                                }
                            }
                            if (btnSyncCalendar != null) {
                                if (shiftsToSync.isEmpty()) {
                                    btnSyncCalendar.setEnabled(false);
                                    btnSyncCalendar.setText("אין משמרות עתידיות לסנכרון");
                                } else {
                                    btnSyncCalendar.setEnabled(true);
                                    btnSyncCalendar.setText("📅 סנכרן " + shiftsToSync.size() + " משמרות עתידיות");
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateTableCell(Shift shift) {
        if (rootView == null) return;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(shift.startTime);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int hourOfDay = c.get(Calendar.HOUR_OF_DAY);

        int cellId = -1;
        String timeSlot = "night";
        if (hourOfDay >= 6 && hourOfDay < 12) timeSlot = "morn";
        else if (hourOfDay >= 12 && hourOfDay < 17) timeSlot = "noon";

        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                if(timeSlot.equals("morn")) cellId = R.id.tvSunMorn;
                if(timeSlot.equals("noon")) cellId = R.id.tvSunNoon;
                if(timeSlot.equals("night")) cellId = R.id.tvSunNight;
                break;
            case Calendar.MONDAY:
                if(timeSlot.equals("morn")) cellId = R.id.tvMonMorn;
                if(timeSlot.equals("noon")) cellId = R.id.tvMonNoon;
                if(timeSlot.equals("night")) cellId = R.id.tvMonNight;
                break;
            case Calendar.TUESDAY:
                if(timeSlot.equals("morn")) cellId = R.id.tvTueMorn;
                if(timeSlot.equals("noon")) cellId = R.id.tvTueNoon;
                if(timeSlot.equals("night")) cellId = R.id.tvTueNight;
                break;
            case Calendar.WEDNESDAY:
                if(timeSlot.equals("morn")) cellId = R.id.tvWedMorn;
                if(timeSlot.equals("noon")) cellId = R.id.tvWedNoon;
                if(timeSlot.equals("night")) cellId = R.id.tvWedNight;
                break;
            case Calendar.THURSDAY:
                if(timeSlot.equals("morn")) cellId = R.id.tvThuMorn;
                if(timeSlot.equals("noon")) cellId = R.id.tvThuNoon;
                if(timeSlot.equals("night")) cellId = R.id.tvThuNight;
                break;
            case Calendar.FRIDAY:
                if(timeSlot.equals("morn")) cellId = R.id.tvFriMorn;
                if(timeSlot.equals("noon")) cellId = R.id.tvFriNoon;
                if(timeSlot.equals("night")) cellId = R.id.tvFriNight;
                break;
            case Calendar.SATURDAY:
                if(timeSlot.equals("morn")) cellId = R.id.tvSatMorn;
                if(timeSlot.equals("noon")) cellId = R.id.tvSatNoon;
                if(timeSlot.equals("night")) cellId = R.id.tvSatNight;
                break;
        }

        if (cellId != -1) {
            View v = rootView.findViewById(cellId);
            if (v instanceof TextView) {
                TextView targetCell = (TextView) v;
                targetCell.setText("משמרת");
                targetCell.setTextColor(Color.WHITE);
                if (shift.isAvailableForSwap) {
                    targetCell.setBackgroundColor(Color.parseColor("#FFB74D"));
                    targetCell.setText("ממתין\nלהחלפה");
                } else {
                    targetCell.setBackgroundColor(Color.parseColor("#4CAF50"));
                }

                // הוספת לחיצה ארוכה להחלפה
                if (shift.startTime > System.currentTimeMillis() && !shift.isAvailableForSwap) {
                    targetCell.setOnLongClickListener(v1 -> {
                        new android.app.AlertDialog.Builder(getContext())
                                .setTitle("שוק ההחלפות")
                                .setMessage("להציע להחלפה?")
                                .setPositiveButton("כן", (dialog, which) -> {
                                    database.getReference("Businesses").child(currentBusinessId)
                                            .child("Shifts").child(shift.shiftId).child("isAvailableForSwap").setValue(true);
                                    Toast.makeText(getContext(), "פורסם!", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("ביטול", null).show();
                        return true;
                    });
                }
            }
        }
    }

    private void syncShiftsToCalendar() {
        if (shiftsToSync.isEmpty()) return;
        long calId = getPrimaryCalendarId();
        if (calId == -1) {
            Toast.makeText(getContext(), "לא נמצא יומן פעיל", Toast.LENGTH_SHORT).show();
            return;
        }
        ContentResolver cr = requireContext().getContentResolver();
        int count = 0;
        for (Shift shift : shiftsToSync) {
            try {
                ContentValues values = new ContentValues();
                values.put(CalendarContract.Events.DTSTART, shift.startTime);
                long endTime = (shift.endTime > 0) ? shift.endTime : shift.startTime + (8 * 60 * 60 * 1000);
                values.put(CalendarContract.Events.DTEND, endTime);
                values.put(CalendarContract.Events.TITLE, "עבודה: " + shift.userName);
                values.put(CalendarContract.Events.CALENDAR_ID, calId);
                values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
                if (cr.insert(CalendarContract.Events.CONTENT_URI, values) != null) count++;
            } catch (Exception e) {}
        }
        Toast.makeText(getContext(), count + " סונכרנו ליומן!", Toast.LENGTH_SHORT).show();
    }

    private long getPrimaryCalendarId() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return -1;
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY},
                CalendarContract.Calendars.VISIBLE + " = 1", null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getLong(0);
        } catch (Exception e) {}
        return -1;
    }
}