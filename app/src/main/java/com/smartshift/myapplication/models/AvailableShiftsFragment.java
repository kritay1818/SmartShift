package com.smartshift.myapplication.models;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smartshift.myapplication.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AvailableShiftsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FirebaseDatabase database;
    private String myUserId;
    private String myUserName; // נצטרך את השם שלך כדי לעדכן את המשמרת

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_available_shifts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");
        myUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = view.findViewById(R.id.rvAvailableShifts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fetchMyNameAndLoadShifts();
    }

    private void fetchMyNameAndLoadShifts() {
        // קודם מביאים את השם שלי, ורק אז טוענים את המשמרות
        database.getReference("Users").child(myUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                user u = snapshot.getValue(user.class);
                if (u != null) {
                    myUserName = u.fullName;
                    loadAvailableShifts();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAvailableShifts() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) return;

        database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID).child("Shifts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Shift> availableList = new ArrayList<>();
                        long now = System.currentTimeMillis();

                        for (DataSnapshot s : snapshot.getChildren()) {
                            try {
                                Shift shift = s.getValue(Shift.class);
                                // התנאים להצגה:
                                // 1. המשמרת מסומנת להחלפה
                                // 2. המשמרת עדיין לא התחילה (בעתיד)
                                // 3. המשמרת לא שלי (אני לא יכול להחליף עם עצמי)
                                if (shift != null &&
                                        shift.isAvailableForSwap &&
                                        shift.startTime > now &&
                                        !shift.userId.equals(myUserId)) {

                                    availableList.add(shift);
                                }
                            } catch (Exception e) {}
                        }

                        // עדכון הרשימה
                        recyclerView.setAdapter(new SwapAdapter(availableList));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void claimShift(Shift shift) {
        new AlertDialog.Builder(getContext())
                .setTitle("לקחת משמרת?")
                .setMessage("האם אתה בטוח שאתה רוצה לקחת את המשמרת של " + shift.userName + "?")
                .setPositiveButton("כן, קח אותה!", (dialog, which) -> {

                    // עדכון ב-Firebase: העברת הבעלות אליי
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("userId", myUserId);
                    updates.put("userName", myUserName);
                    updates.put("isAvailableForSwap", false); // מורידים מהשוק

                    database.getReference("Businesses")
                            .child(DashboardFragment.GLOBAL_BUSINESS_ID)
                            .child("Shifts")
                            .child(shift.shiftId)
                            .updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "מזל טוב! המשמרת שלך.", Toast.LENGTH_SHORT).show();
                            });

                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // --- Adapter פנימי ---
    class SwapAdapter extends RecyclerView.Adapter<SwapAdapter.ViewHolder> {
        List<Shift> list;
        public SwapAdapter(List<Shift> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // משתמשים ב-Layout פשוט שיש לך או יוצרים אחד חדש. נשתמש בברירת מחדל פשוטה לצורך הדוגמה
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Shift shift = list.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

            holder.text1.setText("משמרת של: " + shift.userName);
            holder.text2.setText("מועד: " + sdf.format(new Date(shift.startTime)) + " (לחץ כדי לקחת)");

            holder.itemView.setOnClickListener(v -> claimShift(shift));
            holder.itemView.setBackgroundColor(0xFFE6EE9C); // צבע ירקרק להדגשה
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}