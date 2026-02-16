package com.smartshift.myapplication.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartshift.myapplication.R;
import java.util.List;

public class WagesAdapter extends RecyclerView.Adapter<WagesAdapter.WageViewHolder> {

    public interface OnWageEditListener {
        void onEditClick(user user);
    }

    private List<user> usersList;
    private OnWageEditListener listener;

    // הוספנו את ה-Uid למודל User באופן זמני לצורך זיהוי, או שנשתמש בדרך אחרת.
    // לצורך פשטות נניח שאנחנו מעבירים רשימה של אובייקטים.
    // שימי לב: ב-User המקורי שלך אין שדה uid, אז נצטרך להתייחס לזה ב-Fragment.
    // לצורך הפתרון הזה אני אניח שאנחנו מעבירים מחלקה עוטפת או משתמשים בטריק קטן בפרגמנט.

    // תיקון: בואי נוסיף למודל User שדה uid זמני או שננהל אותו כאן.
    // כדי לא לשבור לך את הקוד הקיים, נשתמש במחלקה פנימית בפרגמנט.

    // ** בואי נשתמש במודל User הקיים שלך, ובפרגמנט נחבר את ה-Key **

    // עדכון: יצרתי מחלקה עוטפת פשוטה בתוך הפרגמנט כדי לא לגעת ב-User המקורי.
    // אבל כאן אקבל UserWithId (נגדיר אותה למטה).

    public static class UserWithId {
        public user user;
        public String uid;

        public UserWithId(user user, String uid) {
            this.user = user;
            this.uid = uid;
        }
    }

    private List<UserWithId> dataList;

    public WagesAdapter(List<UserWithId> list, OnWageEditListener listener) {
        this.dataList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_wage, parent, false);
        return new WageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WageViewHolder holder, int position) {
        UserWithId item = dataList.get(position);
        holder.tvName.setText(item.user.fullName);
        holder.tvWage.setText("שכר לשעה: " + item.user.hourlyRate + " ₪");

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(item.user));
        // אנחנו צריכים להעביר גם את ה-UID כדי לעדכן
        holder.itemView.setTag(item.uid);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class WageViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvWage;
        Button btnEdit;

        public WageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvWage = itemView.findViewById(R.id.tvCurrentWage);
            btnEdit = itemView.findViewById(R.id.btnEditWage);
        }
    }
}