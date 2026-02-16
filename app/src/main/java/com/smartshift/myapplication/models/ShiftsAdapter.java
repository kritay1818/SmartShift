package com.smartshift.myapplication.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartshift.myapplication.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShiftsAdapter extends RecyclerView.Adapter<ShiftsAdapter.ShiftViewHolder> {

    private List<Shift> shiftsList;

    public ShiftsAdapter(List<Shift> shiftsList) {
        this.shiftsList = shiftsList;
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift, parent, false);
        return new ShiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
        Shift shift = shiftsList.get(position);

        holder.tvName.setText(shift.userName);

        // המרת הזמנים לשעות קריאות
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        String start = sdf.format(new Date(shift.startTime));
        String end = (shift.endTime == 0) ? "פעיל" : sdf.format(new Date(shift.endTime));

        holder.tvHours.setText(start + " - " + end);

        // הצגת סה"כ שעות בדיוק של ספרה אחת אחרי הנקודה
        holder.tvTotal.setText(String.format("%.1f שעות", shift.totalHours));
    }

    @Override
    public int getItemCount() {
        return shiftsList.size();
    }

    public static class ShiftViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvHours, tvTotal;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvHours = itemView.findViewById(R.id.tvShiftHours);
            tvTotal = itemView.findViewById(R.id.tvTotalTime);
        }
    }
}