package com.smartshift.myapplication.models;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartshift.myapplication.R;
import java.util.List;

public class EmployeesAdapter extends RecyclerView.Adapter<EmployeesAdapter.EmployeeViewHolder> {

    // הגדרת המצבים האפשריים
    public static final int STATUS_UNAVAILABLE = 0; // אדום
    public static final int STATUS_WARNING = 1;     // צהוב (כפול)
    public static final int STATUS_AVAILABLE = 2;   // ירוק (מומלץ)

    public static class EmployeeItem {
        public String uid;
        public String name;
        public int status; // שינינו מבוליאני למספר (0, 1, 2)

        public EmployeeItem(String uid, String name, int status) {
            this.uid = uid;
            this.name = name;
            this.status = status;
        }
    }

    private List<EmployeeItem> employeesList;
    private OnAssignClickListener listener;

    public interface OnAssignClickListener {
        void onAssignClick(EmployeeItem employee);
    }

    public EmployeesAdapter(List<EmployeeItem> list, OnAssignClickListener listener) {
        this.employeesList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_availability, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        EmployeeItem item = employeesList.get(position);

        holder.tvName.setText(item.name);

        switch (item.status) {
            case STATUS_AVAILABLE:
                // --- ירוק: מושלם לשיבוץ ---
                holder.viewColor.setBackgroundColor(Color.parseColor("#4CAF50"));
                holder.tvStatus.setText("פנוי לשיבוץ");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                holder.btnAssign.setEnabled(true);
                holder.btnAssign.setAlpha(1.0f);
                break;

            case STATUS_WARNING:
                // --- צהוב: זהירות, כפול! ---
                holder.viewColor.setBackgroundColor(Color.parseColor("#FFC107")); // צהוב
                holder.tvStatus.setText("אזהרה: משמרת כפולה/צמודה!");
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // כתום כהה לטקסט
                holder.btnAssign.setEnabled(true); // עדיין אפשר לשבץ, אבל בזהירות
                holder.btnAssign.setAlpha(1.0f);
                break;

            case STATUS_UNAVAILABLE:
                // --- אדום: לא יכול ---
                holder.viewColor.setBackgroundColor(Color.parseColor("#F44336"));
                holder.tvStatus.setText("לא פנוי");
                holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
                holder.btnAssign.setEnabled(false);
                holder.btnAssign.setAlpha(0.5f);
                break;
        }

        holder.btnAssign.setOnClickListener(v -> listener.onAssignClick(item));
    }

    @Override
    public int getItemCount() {
        return employeesList.size();
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        View viewColor;
        Button btnAssign;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvStatus = itemView.findViewById(R.id.tvStatusText);
            viewColor = itemView.findViewById(R.id.viewStatusColor);
            btnAssign = itemView.findViewById(R.id.btnAssign);
        }
    }
}