package com.smartshift.myapplication.models;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smartshift.myapplication.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageScheduleFragment extends Fragment {

    private FirebaseDatabase database;
    private View rootView;
    private int currentWeekOffset = 0; // 0 = שבוע נוכחי
    private TextView tvDateRange;

    // רשימה מקומית שתחזיק את כל המשמרות של השבוע (למניעת כפילויות)
    private List<Shift> allShiftsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_manage_schedule, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");

        tvDateRange = view.findViewById(R.id.tvDateRange);
        Button btnPrev = view.findViewById(R.id.btnPrevWeek);
        Button btnNext = view.findViewById(R.id.btnNextWeek);
        Button btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        btnPrev.setOnClickListener(v -> {
            currentWeekOffset--;
            updateUI();
        });

        btnNext.setOnClickListener(v -> {
            currentWeekOffset++;
            updateUI();
        });

        setupTableClicks();
        updateUI();
    }

    private void updateUI() {
        updateDateRangeHeader();
        resetTable();
        loadSchedule();
    }

    private void updateDateRangeHeader() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);

        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Date start = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 6);
        Date end = cal.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        tvDateRange.setText(sdf.format(start) + " - " + sdf.format(end));
    }

    private void resetTable() {
        int[] allCells = {
                R.id.cellSunMorn, R.id.cellSunNoon, R.id.cellSunNight,
                R.id.cellMonMorn, R.id.cellMonNoon, R.id.cellMonNight,
                R.id.cellTueMorn, R.id.cellTueNoon, R.id.cellTueNight,
                R.id.cellWedMorn, R.id.cellWedNoon, R.id.cellWedNight,
                R.id.cellThuMorn, R.id.cellThuNoon, R.id.cellThuNight,
                R.id.cellFriMorn, R.id.cellFriNoon, R.id.cellFriNight,
                R.id.cellSatMorn, R.id.cellSatNoon, R.id.cellSatNight
        };

        for (int id : allCells) {
            TextView cell = rootView.findViewById(id);
            if (cell != null) {
                cell.setText("+");
                cell.setBackgroundColor(Color.parseColor("#E0E0E0"));
                cell.setTextColor(Color.BLACK);
                cell.setTag(id);
            }
        }
    }

    private void loadSchedule() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) return;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);

        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        long startOfWeek = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_YEAR, 7);
        long endOfWeek = cal.getTimeInMillis();

        // מאזין לכל השינויים בטבלת המשמרות
        database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID).child("Shifts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        resetTable();
                        allShiftsList.clear(); // מנקים את הרשימה המקומית

                        for (DataSnapshot s : snapshot.getChildren()) {
                            try {
                                Shift shift = s.getValue(Shift.class);
                                if (shift != null) {
                                    if (shift.shiftId == null || shift.shiftId.isEmpty()) {
                                        shift.shiftId = s.getKey();
                                    }

                                    if (shift.startTime >= startOfWeek && shift.startTime < endOfWeek) {
                                        allShiftsList.add(shift);
                                        updateCellWithShift(shift);
                                    }
                                }
                            } catch (Exception e) {}
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateCellWithShift(Shift shift) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(shift.startTime);
        int day = c.get(Calendar.DAY_OF_WEEK);
        int hour = c.get(Calendar.HOUR_OF_DAY);

        String timeSlot = "night";
        if (hour >= 6 && hour < 12) timeSlot = "morn";
        else if (hour >= 12 && hour < 17) timeSlot = "noon";

        TextView target = getCellByDayAndSlot(day, timeSlot);
        if (target != null) {
            String currentText = target.getText().toString();
            if (currentText.equals("+")) currentText = "";
            else currentText += "\n";

            target.setText(currentText + shift.userName);
            target.setBackgroundColor(Color.parseColor("#81C784")); // ירוק
        }
    }

    private TextView getCellByDayAndSlot(int day, String slot) {
        String idName = "cell";
        switch(day) {
            case Calendar.SUNDAY: idName += "Sun"; break;
            case Calendar.MONDAY: idName += "Mon"; break;
            case Calendar.TUESDAY: idName += "Tue"; break;
            case Calendar.WEDNESDAY: idName += "Wed"; break;
            case Calendar.THURSDAY: idName += "Thu"; break;
            case Calendar.FRIDAY: idName += "Fri"; break;
            case Calendar.SATURDAY: idName += "Sat"; break;
        }

        if (slot.equals("morn")) idName += "Morn";
        if (slot.equals("noon")) idName += "Noon";
        if (slot.equals("night")) idName += "Night";

        int resId = getResources().getIdentifier(idName, "id", requireContext().getPackageName());
        return rootView.findViewById(resId);
    }

    // פונקציית עזר לתרגום היום והזמן למפתח של Firebase (למשל Sunday_Morning)
    private String getShiftKey(int dayOfWeek, String timeSlot) {
        String dayStr = "";
        switch (dayOfWeek) {
            case Calendar.SUNDAY: dayStr = "Sunday"; break;
            case Calendar.MONDAY: dayStr = "Monday"; break;
            case Calendar.TUESDAY: dayStr = "Tuesday"; break;
            case Calendar.WEDNESDAY: dayStr = "Wednesday"; break;
            case Calendar.THURSDAY: dayStr = "Thursday"; break;
            case Calendar.FRIDAY: dayStr = "Friday"; break;
            case Calendar.SATURDAY: dayStr = "Saturday"; break;
        }

        String slotStr = "";
        if (timeSlot.equals("morn")) slotStr = "Morning";
        else if (timeSlot.equals("noon")) slotStr = "Noon";
        else if (timeSlot.equals("night")) slotStr = "Night";

        return dayStr + "_" + slotStr;
    }

    private void setupTableClicks() {
        int[] days = {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};
        String[] slots = {"morn", "noon", "night"};

        for (int day : days) {
            for (String slot : slots) {
                TextView cell = getCellByDayAndSlot(day, slot);
                if (cell != null) {
                    cell.setOnClickListener(v -> openAssignShiftDialog(day, slot));
                }
            }
        }
    }

    private void openAssignShiftDialog(int dayOfWeek, String timeSlot) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);
        cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);

        int startHour = 8;
        if (timeSlot.equals("noon")) startHour = 12;
        if (timeSlot.equals("night")) startHour = 17;

        cal.set(Calendar.HOUR_OF_DAY, startHour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long targetStartTime = cal.getTimeInMillis();
        String shiftKey = getShiftKey(dayOfWeek, timeSlot);

        Map<String, String> assignedUsers = new HashMap<>();
        for (Shift s : allShiftsList) {
            if (Math.abs(s.startTime - targetStartTime) < 60000) {
                assignedUsers.put(s.userId, s.shiftId);
            }
        }

        showEmployeeListDialog(assignedUsers, targetStartTime, shiftKey);
    }

    private void showEmployeeListDialog(Map<String, String> assignedUsers, long shiftStartTime, String shiftKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("בחר עובד לשיבוץ");

        // שלב 1: שולפים קודם את הזמינות של כולם
        database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID).child("Availability")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot availabilitySnapshot) {

                        // שלב 2: שולפים את רשימת העובדים
                        database.getReference("Users").orderByChild("businessId").equalTo(DashboardFragment.GLOBAL_BUSINESS_ID)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        List<CharSequence> displayNames = new ArrayList<>(); // CharSequence תומך בצבעים
                                        List<String> userIds = new ArrayList<>();
                                        List<String> cleanNames = new ArrayList<>();
                                        List<Boolean> isAvailableList = new ArrayList<>(); // נשמור מי זמין ומי לא

                                        for (DataSnapshot s : snapshot.getChildren()) {
                                            try {
                                                user u = s.getValue(user.class);
                                                if (u != null && !"Manager".equalsIgnoreCase(u.role) && !"מנהל".equalsIgnoreCase(u.role)) {
                                                    String uid = s.getKey();
                                                    userIds.add(uid);
                                                    cleanNames.add(u.fullName);

                                                    // בודקים האם העובד הספציפי סימן TRUE למשמרת הספציפית הזו
                                                    Boolean isAvail = availabilitySnapshot.child(uid).child(shiftKey).getValue(Boolean.class);
                                                    boolean available = (isAvail != null && isAvail);
                                                    isAvailableList.add(available);

                                                    String displayText = u.fullName;

                                                    if (assignedUsers.containsKey(uid)) {
                                                        displayText = "✅ " + displayText + " (לחץ להסרה)";
                                                    } else if (!available) {
                                                        displayText = "❌ " + displayText + " (לא הגיש זמינות)";
                                                    }

                                                    // שימוש ב-SpannableString כדי לצבוע באדום
                                                    SpannableString spannable = new SpannableString(displayText);
                                                    if (!available && !assignedUsers.containsKey(uid)) {
                                                        spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, displayText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                    }

                                                    displayNames.add(spannable);
                                                }
                                            } catch (Exception e) {}
                                        }

                                        builder.setItems(displayNames.toArray(new CharSequence[0]), (dialog, which) -> {
                                            String selectedUserId = userIds.get(which);
                                            String selectedName = cleanNames.get(which);
                                            boolean isWorkerAvailable = isAvailableList.get(which);

                                            if (assignedUsers.containsKey(selectedUserId)) {
                                                // העובד כבר משובץ -> נמחק אותו
                                                String shiftIdToDelete = assignedUsers.get(selectedUserId);
                                                removeShiftFromFirebase(shiftIdToDelete, selectedName);
                                            } else {
                                                // העובד לא משובץ -> נוסיף אותו
                                                if (!isWorkerAvailable) {
                                                    // העובד באדום! נקפיץ אזהרה למנהל
                                                    new AlertDialog.Builder(getContext())
                                                            .setTitle("אזהרת שיבוץ ⚠️")
                                                            .setMessage("העובד " + selectedName + " לא הגיש זמינות למשמרת זו.\nהאם אתה בטוח שברצונך לשבץ אותו בכל זאת?")
                                                            .setPositiveButton("כן, שבץ אותו", (d, w) -> saveShiftToFirebase(selectedUserId, selectedName, shiftStartTime))
                                                            .setNegativeButton("ביטול", null)
                                                            .show();
                                                } else {
                                                    // העובד זמין וירוק -> משבצים רגיל
                                                    saveShiftToFirebase(selectedUserId, selectedName, shiftStartTime);
                                                }
                                            }
                                        });

                                        builder.setNegativeButton("סגור", null);
                                        builder.show();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveShiftToFirebase(String userId, String userName, long startTime) {
        DatabaseReference shiftsRef = database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID).child("Shifts");
        String shiftId = shiftsRef.push().getKey();

        long endTime = startTime + (8 * 60 * 60 * 1000);

        Shift shift = new Shift(shiftId, userId, userName, startTime);
        shift.endTime = endTime;

        shiftsRef.child(shiftId).setValue(shift).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), userName + " שובץ בהצלחה", Toast.LENGTH_SHORT).show();
        });
    }

    private void removeShiftFromFirebase(String shiftId, String userName) {
        if (shiftId == null) return;

        database.getReference("Businesses")
                .child(DashboardFragment.GLOBAL_BUSINESS_ID)
                .child("Shifts")
                .child(shiftId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "השיבוץ של " + userName + " הוסר", Toast.LENGTH_SHORT).show();
                });
    }
}