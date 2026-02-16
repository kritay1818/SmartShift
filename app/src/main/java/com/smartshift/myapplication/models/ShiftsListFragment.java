package com.smartshift.myapplication.models;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smartshift.myapplication.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShiftsListFragment extends Fragment {

    private RecyclerView rvShifts;
    private ShiftsAdapter adapter;
    private List<Shift> shiftsList;
    private FirebaseDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shifts_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvShifts = view.findViewById(R.id.rvShifts);
        rvShifts.setLayoutManager(new LinearLayoutManager(getContext()));

        shiftsList = new ArrayList<>();
        adapter = new ShiftsAdapter(shiftsList);
        rvShifts.setAdapter(adapter);

        // --- כאן לשים את ה-URL שלך! ---
        database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");

        loadShifts();
    }

    private void loadShifts() {
        DatabaseReference shiftsRef = database.getReference("Shifts");

        shiftsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shiftsList.clear();
                for (DataSnapshot shiftSnap : snapshot.getChildren()) {
                    // הוספנו מנגנון הגנה (Try-Catch)
                    try {
                        // מנסה להמיר למשמרת. אם זה סתם מספר - הוא ילך ל-Catch
                        Shift shift = shiftSnap.getValue(Shift.class);
                        if (shift != null) {
                            shiftsList.add(shift);
                        }
                    } catch (Exception e) {
                        // אם יש נתון "זבל" (כמו Double), נתעלם ממנו ולא נקרוס
                        android.util.Log.e("ShiftsList", "Error parsing shift: " + e.getMessage());
                    }
                }

                Collections.reverse(shiftsList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
            }
        });
    }
}