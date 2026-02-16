package com.smartshift.myapplication.models;

public class user {
    public String fullName;
    public String email;
    public String phone;
    public String role;
    public double hourlyRate;
    public String businessId; // --- השדה החדש והקריטי ---

    public user() {}

    public user(String fullName, String email, String phone, String role, double hourlyRate, String businessId) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.hourlyRate = hourlyRate;
        this.businessId = businessId;
    }
}