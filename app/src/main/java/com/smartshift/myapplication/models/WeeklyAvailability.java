package com.smartshift.myapplication.models;

public class WeeklyAvailability {
    public String userId;
    public String userName;

    // ראשון
    public boolean sunMorn, sunNoon, sunNight;
    // שני
    public boolean monMorn, monNoon, monNight;
    // שלישי
    public boolean tueMorn, tueNoon, tueNight;
    // רביעי
    public boolean wedMorn, wedNoon, wedNight;
    // חמישי
    public boolean thuMorn, thuNoon, thuNight;
    // שישי
    public boolean friMorn, friNoon, friNight;
    // שבת
    public boolean satMorn, satNoon, satNight;

    public WeeklyAvailability() {
        // בנאי ריק חובה ל-Firebase
    }

    public WeeklyAvailability(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }
}