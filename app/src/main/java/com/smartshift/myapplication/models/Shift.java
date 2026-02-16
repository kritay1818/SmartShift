package com.smartshift.myapplication.models;

public class Shift {
    public String shiftId;
    public String userId;
    public String userName;
    public long startTime;

    public boolean isAvailableForSwap = false;
    public long endTime;

    // שדות חדשים שחייבים להיות כאן!
    public double totalHours;
    public double totalWage;

    public Shift() {
        // בנאי ריק חובה ל-Firebase
    }

    public Shift(String shiftId, String userId, String userName, long startTime) {
        this.shiftId = shiftId;
        this.userId = userId;
        this.userName = userName;
        this.startTime = startTime;
        this.endTime = 0;
        this.totalHours = 0;
        this.totalWage = 0;
    }
}