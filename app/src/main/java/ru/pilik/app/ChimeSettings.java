package ru.pilik.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Все настройки хранятся на телефоне. Подключение к интернету не требуется. */
public final class ChimeSettings {
    private static final String FILE = "pilik_preferences";
    public static final String ON = "enabled";
    public static final String INTERVAL = "interval_minutes";
    public static final String WEEKDAYS_ON = "weekdays_on";
    public static final String WEEKDAYS_START = "weekdays_start";
    public static final String WEEKDAYS_END = "weekdays_end";
    public static final String WEEKENDS_ON = "weekends_on";
    public static final String WEEKENDS_START = "weekends_start";
    public static final String WEEKENDS_END = "weekends_end";
    public static final String SOUND = "sound";
    public static final String VIBRATION = "vibration";
    public static final String NEXT_AT = "next_at";

    private ChimeSettings() { }

    public static SharedPreferences get(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context) {
        return get(context).getBoolean(ON, true);
    }

    public static int interval(Context context) {
        int interval = get(context).getInt(INTERVAL, 60);
        return interval == 15 || interval == 30 || interval == 60 ? interval : 60;
    }

    public static boolean vibration(Context context) {
        return get(context).getBoolean(VIBRATION, false);
    }

    public static int soundId(Context context) {
        return get(context).getInt(SOUND, 1) == 0 ? 0 : 1;
    }
}
