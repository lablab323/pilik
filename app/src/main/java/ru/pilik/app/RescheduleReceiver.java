package ru.pilik.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Восстанавливает расписание после перезапуска телефона и смены времени. */
public class RescheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmScheduler.schedule(context);
    }
}
