package ru.pilik.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/** Проверяет выбранные дни и временные окна, ставит только ближайший сигнал. */
public final class AlarmScheduler {
    private static final int REQUEST_CODE = 501;

    private AlarmScheduler() { }

    public static PendingIntent pending(Context context) {
        Intent intent = new Intent(context, ChimeReceiver.class)
                .setAction("ru.pilik.app.CHIME");
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean canBeExact(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms());
    }

    public static boolean isWithinWindow(SharedPreferences prefs, Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        boolean weekend = day == Calendar.SATURDAY || day == Calendar.SUNDAY;
        String prefix = weekend ? "weekends" : "weekdays";
        if (!prefs.getBoolean(prefix + "_on", true)) return false;
        int start = prefs.getInt(prefix + "_start", weekend ? 10 * 60 : 7 * 60);
        int end = prefs.getInt(prefix + "_end", 22 * 60);
        int time = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        // Начало включительно, конец не включительно.
        return start < end && time >= start && time < end;
    }

    /** Находит ближайший момент строго после afterMillis. */
    public static long findNext(Context context, long afterMillis) {
        if (!ChimeSettings.enabled(context)) return -1L;
        SharedPreferences prefs = ChimeSettings.get(context);
        int interval = ChimeSettings.interval(context);
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(afterMillis);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        time.add(Calendar.MINUTE, 1);
        // Недели достаточно: расписание повторяется каждые 7 дней.
        for (int i = 0; i <= 8 * 24 * 60; i++) {
            if (time.get(Calendar.MINUTE) % interval == 0 && isWithinWindow(prefs, time)) {
                return time.getTimeInMillis();
            }
            time.add(Calendar.MINUTE, 1);
        }
        return -1L;
    }

    public static synchronized void schedule(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        PendingIntent pendingIntent = pending(context);
        manager.cancel(pendingIntent);
        long next = findNext(context, System.currentTimeMillis());
        ChimeSettings.get(context).edit().putLong(ChimeSettings.NEXT_AT, next).apply();
        if (next < 0) return;

        // Без специального разрешения Android может отложить сигнал.
        if (canBeExact(context)) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent);
                return;
            } catch (SecurityException ignored) {
                // Разрешение успело измениться: ставим неточный будильник.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent);
    }
}
