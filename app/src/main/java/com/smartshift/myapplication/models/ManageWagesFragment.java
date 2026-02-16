package com.smartshift.myapplication.models;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.ArrayList;
import java.util.List;

public class ManageWagesFragment extends Fragment {

    private RecyclerView recyclerView;
    private WagesAdapter adapter;
    private List<WagesAdapter.UserWithId> employeesList;
    private FirebaseDatabase database;

    // משתנים חדשים לנסיעות
    private EditText etTravelAmount;
    private Button btnSaveTravel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_wages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = FirebaseDatabase.getInstance("https://smartshift-4bdfb-default-rtdb.europe-west1.firebasedatabase.app/");
        employeesList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.rvEmployeesWages);
        etTravelAmount = view.findViewById(R.id.etTravelAmount);
        btnSaveTravel = view.findViewById(R.id.btnSaveTravel);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.btnBack).setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        // שמירת סכום הנסיעות
        btnSaveTravel.setOnClickListener(v -> saveTravelAmount());

        loadEmployees();
        loadTravelAmount(); // טעינת הסכום הנוכחי
    }

    private void loadTravelAmount() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) return;

        database.getReference("Businesses")
                .child(DashboardFragment.GLOBAL_BUSINESS_ID)
                .child("settings")
                .child("travelAmount")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            double amount = snapshot.getValue(Double.class);
                            etTravelAmount.setText(String.valueOf(amount));
                        } else {
                            etTravelAmount.setText("0");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveTravelAmount() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) return;

        String amountStr = etTravelAmount.getText().toString().trim();
        if (amountStr.isEmpty()) return;

        double amount = Double.parseDouble(amountStr);

        database.getReference("Businesses")
                .child(DashboardFragment.GLOBAL_BUSINESS_ID)
                .child("settings")
                .child("travelAmount")
                .setValue(amount)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "סכום נסיעות עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "שגיאה בעדכון", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadEmployees() {
        if (DashboardFragment.GLOBAL_BUSINESS_ID.isEmpty()) return;

        database.getReference("Users").orderByChild("businessId").equalTo(DashboardFragment.GLOBAL_BUSINESS_ID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        employeesList.clear();
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            // הגנה מפני נתונים פגומים
                            user user = null;
                            try { user = userSnap.getValue(user.class); } catch (Exception e) { continue; }

                            if (user != null) {
                                employeesList.add(new WagesAdapter.UserWithId(user, userSnap.getKey()));
                            }
                        }

                        adapter = new WagesAdapter(employeesList, user -> {
                            // מציאת ה-UID לעריכה
                            String uidToEdit = null;
                            for (WagesAdapter.UserWithId u : employeesList) {
                                if (u.user == user) {
                                    uidToEdit = u.uid;
                                    break;
                                }
                            }
                            if (uidToEdit != null) {
                                showEditWageDialog(uidToEdit, user);
                            }
                        });
                        recyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showEditWageDialog(String uid, user user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("עדכון שכר עבור " + user.fullName);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("הכנס שכר לשעה");
        input.setText(String.valueOf(user.hourlyRate));
        builder.setView(input);

        builder.setPositiveButton("שמור", (dialog, which) -> {
            String newRateStr = input.getText().toString();
            if (!newRateStr.isEmpty()) {
                double newRate = Double.parseDouble(newRateStr);
                database.getReference("Users").child(uid).child("hourlyRate").setValue(newRate)
                        .addOnCompleteListener(task -> Toast.makeText(getContext(), "עודכן!", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}