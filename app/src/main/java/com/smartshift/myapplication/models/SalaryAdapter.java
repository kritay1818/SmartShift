package com.smartshift.myapplication.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartshift.myapplication.R;
import java.util.List;

public class SalaryAdapter extends RecyclerView.Adapter<SalaryAdapter.SalaryViewHolder> {

    // הוספנו ממשק לחיצה
    public interface OnItemClickListener {
        void onItemClick(String userId, String userName);
    }

    public static class SalarySummary {
        public String userId; // שדה חדש!
        public String name;
        public double hourlyRate;
        public double totalHours;
        public double totalWage;

        public SalarySummary(String userId, String name, double hourlyRate, double totalHours, double totalWage) {
            this.userId = userId;
            this.name = name;
            this.hourlyRate = hourlyRate;
            this.totalHours = totalHours;
            this.totalWage = totalWage;
        }
    }

    private List<SalarySummary> summaryList;
    private OnItemClickListener listener; // המאזין שלנו

    // בנאי מעודכן שמקבל גם את המאזין
    public SalaryAdapter(List<SalarySummary> list, OnItemClickListener listener) {
        this.summaryList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SalaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_salary_row, parent, false);
        return new SalaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SalaryViewHolder holder, int position) {
        SalarySummary item = summaryList.get(position);

        holder.tvName.setText(item.name);
        holder.tvRate.setText(String.format("תעריף: %.2f ₪/שעה", item.hourlyRate));
        holder.tvHours.setText(String.format("%.2f שעות", item.totalHours));
        holder.tvMoney.setText(String.format("%,.2f ₪", item.totalWage));

        // הופך את השורה ללחיצה
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item.userId, item.name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    public static class SalaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRate, tvHours, tvMoney;

        public SalaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmpName);
            tvRate = itemView.findViewById(R.id.tvEmpRate);
            tvHours = itemView.findViewById(R.id.tvTotalHours);
            tvMoney = itemView.findViewById(R.id.tvTotalMoney);
        }
    }
}