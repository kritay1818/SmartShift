package com.smartshift.myapplication.models;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smartshift.myapplication.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LiveStatusFragment extends Fragment {

    private RecyclerView recyclerView;
    private ActiveEmployeesAdapter adapter;
    private List<Shift> activeShifts = new ArrayList<>();
    private FirebaseDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");
        recyclerView = view.findViewById(R.id.rvActiveEmployees);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ActiveEmployeesAdapter(activeShifts);
        recyclerView.setAdapter(adapter);

        loadActiveShifts();
    }

    private void loadActiveShifts() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) return;

        // מאזין בזמן אמת - ברגע שמישהו נכנס/יוצא, הרשימה תתעדכן לבד!
        database.getReference("Businesses").child(DashboardFragment.GLOBAL_BUSINESS_ID).child("Shifts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        activeShifts.clear();
                        long now = System.currentTimeMillis();

                        for (DataSnapshot s : snapshot.getChildren()) {
                            try {
                                Shift shift = s.getValue(Shift.class);
                                // התנאי הקריטי: משמרת שהתחילה אבל עוד לא נגמרה (endTime == 0)
                                if (shift != null && shift.endTime == 0) {
                                    activeShifts.add(shift);
                                }
                            } catch (Exception e) {}
                        }

                        // אם הרשימה ריקה, אפשר להוסיף הודעה "אין עובדים פעילים" (אופציונלי)
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- Adapter פנימי (כדי לחסוך קבצים) ---
    private class ActiveEmployeesAdapter extends RecyclerView.Adapter<ActiveEmployeesAdapter.ViewHolder> {
        private List<Shift> list;

        public ActiveEmployeesAdapter(List<Shift> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_employee, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Shift shift = list.get(position);
            holder.tvName.setText(shift.userName);

            // חישוב כמה זמן הוא כבר עובד
            long durationMillis = System.currentTimeMillis() - shift.startTime;
            long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String startTime = sdf.format(new Date(shift.startTime));

            holder.tvTime.setText("נכנס ב: " + startTime + " (עובד " + hours + "ש ו-" + minutes + "ד)");
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvTime;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvEmployeeName);
                tvTime = itemView.findViewById(R.id.tvStartTime);
            }
        }
    }
}